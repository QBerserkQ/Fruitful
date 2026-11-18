import {useCallback, useEffect, useRef, useState, type ReactNode} from "react";
import {ChatContext, type MessageListener} from "./chatContext.ts";
import {apiGetConversations} from "../api/chat.ts";
import {ApiError} from "../api/client.ts";
import type {ConversationResponse, MessageResponse} from "../api/types.ts";
import {connectChatSocket} from "../ws/chatSocket.ts";

function activityTime(c: ConversationResponse): number {
    return Date.parse(c.lastMessageAt ?? c.createdAt);
}

export function ChatProvider({children}: { children: ReactNode }) {
    const [conversations, setConversations] = useState<ConversationResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const conversationsRef = useRef<ConversationResponse[]>([]);
    useEffect(() => {
        conversationsRef.current = conversations;
    }, [conversations]);

    // слушатели входящих сообщений (окна переписки). Лежат в ref, чтобы их
    // добавление и удаление не перерисовывало провайдер
    const listenersRef = useRef<Set<MessageListener>>(new Set());

    const subscribe = useCallback((listener: MessageListener) => {
        listenersRef.current.add(listener);
        return () => {
            listenersRef.current.delete(listener);
        };
    }, []);

    const reload = useCallback(async () => {
        try {
            setError(null);
            const data = await apiGetConversations();
            setConversations(data.sort((a, b) => activityTime(b) - activityTime(a)));
        } catch (e) {
            setError(e instanceof ApiError ? e.message : "Не удалось загрузить чаты");
        } finally {
            setIsLoading(false);
        }
    }, []);

    const applyMessage = useCallback((m: MessageResponse) => {
        if (!conversationsRef.current.some((c) => c.id === m.conversationId)) {
            void reload();
            return;
        }
        setConversations((prev) =>
            prev
                .map((c) =>
                    c.id === m.conversationId
                        ? {...c, lastMessage: m.content, lastMessageAt: m.createdAt}
                        : c,
                )
                .sort((a, b) => activityTime(b) - activityTime(a)),
        );
    }, [reload]);

    // сообщение из сокета: обновляем список и раздаём слушателям
    const handleSocketMessage = useCallback((m: MessageResponse) => {
        applyMessage(m);
        listenersRef.current.forEach((listener) => listener(m));
    }, [applyMessage]);

    useEffect(() => {
        void reload();
    }, [reload]);

    useEffect(() => connectChatSocket(handleSocketMessage), [handleSocketMessage]);

    return (
        <ChatContext.Provider value={{conversations, isLoading, error, reload, applyMessage, subscribe}}>
            {children}
        </ChatContext.Provider>
    );
}