import {useEffect, useRef, useState, type FormEvent, type KeyboardEvent} from "react";
import {apiGetMessages, apiSendMessage} from "../api/chat.ts";
import {ApiError} from "../api/client.ts";
import type {MessageResponse} from "../api/types.ts";
import {useChat} from "./useChat.ts";
import {Link} from "react-router-dom";

const PAGE_SIZE = 30;
const MAX_LENGTH = 2000;

// Склеивает два набора сообщений: убирает дубли по id и сортирует от старых к новым.
// Через неё идут ВСЕ добавления (история, сокет, ответ на отправку)
function mergeMessages(prev: MessageResponse[], incoming: MessageResponse[]): MessageResponse[] {
    const byId = new Map<number, MessageResponse>();
    for (const m of prev) byId.set(m.id, m);
    for (const m of incoming) byId.set(m.id, m);
    return [...byId.values()].sort((a, b) => a.id - b.id);
}

function formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"});
}

interface Props {
    conversationId: number;
    partnerId: number;
    partnerUsername: string;
}

export function ChatWindow({conversationId, partnerId, partnerUsername}: Props) {
    const {subscribe, applyMessage} = useChat();

    const [messages, setMessages] = useState<MessageResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [hasMore, setHasMore] = useState(false);
    const [isLoadingMore, setIsLoadingMore] = useState(false);

    const [text, setText] = useState("");
    const [isSending, setIsSending] = useState(false);
    const [sendError, setSendError] = useState<string | null>(null);

    const nextPageRef = useRef(1);                  // какую страницу истории грузить при "Загрузить ещё"
    const bottomRef = useRef<HTMLDivElement>(null); // невидимый элемент в конце списка для автоскролла

    // 1. История: первая страница при открытии беседы.
    // Флаг cancelled защищает от ответа, который пришёл уже после закрытия окна
    useEffect(() => {
        let cancelled = false;
        apiGetMessages(conversationId, 0, PAGE_SIZE)
            .then((page) => {
                if (cancelled) return;
                setMessages((prev) => mergeMessages(prev, page));
                setHasMore(page.length === PAGE_SIZE);
            })
            .catch((e) => {
                if (!cancelled) setLoadError(e instanceof ApiError ? e.message : "Не удалось загрузить сообщения");
            })
            .finally(() => {
                if (!cancelled) setIsLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [conversationId]);

    // 2. Живые сообщения: берём из сокета только те, что относятся к этой беседе
    useEffect(
        () =>
            subscribe((m) => {
                if (m.conversationId === conversationId) {
                    setMessages((prev) => mergeMessages(prev, [m]));
                }
            }),
        [subscribe, conversationId],
    );

    // 3. Автоскролл вниз, только когда изменилось ПОСЛЕДНЕЕ сообщение
    // (при подгрузке старых оно не меняется, и скролл не прыгает)
    const lastId = messages.length > 0 ? messages[messages.length - 1].id : null;
    useEffect(() => {
        bottomRef.current?.scrollIntoView({block: "end"});
    }, [lastId]);

    async function loadMore() {
        setIsLoadingMore(true);
        try {
            const page = await apiGetMessages(conversationId, nextPageRef.current, PAGE_SIZE);
            nextPageRef.current += 1;
            setMessages((prev) => mergeMessages(prev, page));
            setHasMore(page.length === PAGE_SIZE);
        } catch (e) {
            setLoadError(e instanceof ApiError ? e.message : "Не удалось загрузить сообщения");
        } finally {
            setIsLoadingMore(false);
        }
    }

    async function send() {
        const content = text.trim();
        if (!content || isSending) return;

        setIsSending(true);
        setSendError(null);
        try {
            const sent = await apiSendMessage(conversationId, {content});
            setMessages((prev) => mergeMessages(prev, [sent])); // себе по сокету не приходит, добавляем сами
            applyMessage(sent);                                 // обновить список чатов слева
            setText("");
        } catch (e) {
            setSendError(e instanceof ApiError ? e.message : "Не удалось отправить сообщение");
        } finally {
            setIsSending(false);
        }
    }

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        void send();
    }

    // Enter отправляет, Shift+Enter делает перенос строки
    function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            void send();
        }
    }

    return (
        <div className="flex h-full flex-col">
            <header className="flex items-center gap-2 border-b p-3">
                {/* стрелка "назад к диалогам": только на телефоне (на компьютере список и так рядом) */}
                <Link
                    to="/chats"
                    aria-label="Back"
                    className="-ml-1 rounded-full p-2 hover:bg-gray-100"
                >
                    <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor"
                         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M15 18l-6-6 6-6" />
                    </svg>
                </Link>
                <span className="truncate font-semibold">{partnerUsername}</span>
            </header>

            <div className="min-h-0 flex-1 space-y-2 overflow-y-auto p-4">
                {isLoading && <p className="text-sm text-gray-500">Загрузка...</p>}
                {loadError && <p className="text-sm text-red-600">{loadError}</p>}

                {hasMore && (
                    <button
                        onClick={() => void loadMore()}
                        disabled={isLoadingMore}
                        className="mx-auto block text-sm underline disabled:opacity-50"
                    >
                        {isLoadingMore ? "Загрузка..." : "Загрузить ещё"}
                    </button>
                )}

                {!isLoading && !loadError && messages.length === 0 && (
                    <p className="text-center text-sm text-gray-500">Сообщений пока нет, напиши первым</p>
                )}

                {messages.map((m) => {
                    const isMine = m.senderId !== partnerId;
                    return (
                        <div key={m.id} className={`flex ${isMine ? "justify-end" : "justify-start"}`}>
                            <div
                                className={`max-w-[85%] rounded-2xl px-3 py-2 md:max-w-[70%] ${
                                    isMine ? "bg-green-200" : "bg-gray-200"
                                }`}
                            >
                                <p className="whitespace-pre-wrap break-words">{m.content}</p>
                                <p className="mt-1 text-right text-xs text-gray-500">{formatTime(m.createdAt)}</p>
                            </div>
                        </div>
                    );
                })}
                <div ref={bottomRef} />
            </div>

            <form onSubmit={handleSubmit} className="border-t p-3">
                {sendError && <p className="mb-2 text-sm text-red-600">{sendError}</p>}
                <div className="flex items-end gap-2">
                    <textarea
                        value={text}
                        onChange={(e) => setText(e.target.value)}
                        onKeyDown={handleKeyDown}
                        maxLength={MAX_LENGTH}
                        rows={1}
                        placeholder="Сообщение..."
                        className="max-h-32 flex-1 resize-none rounded-lg border p-2"
                    />
                    <button
                        type="submit"
                        disabled={isSending || text.trim() === ""}
                        className="rounded-lg bg-green-500 px-4 py-2 text-white disabled:opacity-50"
                    >
                        Отправить
                    </button>
                </div>
            </form>
        </div>
    );
}