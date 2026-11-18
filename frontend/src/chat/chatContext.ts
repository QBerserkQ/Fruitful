import {createContext} from "react";
import type {ConversationResponse, MessageResponse} from "../api/types.ts";

export type MessageListener = (message: MessageResponse) => void;
export type ReconnectListener = () => void;

export interface ChatContextValue {
    conversations: ConversationResponse[];
    isLoading: boolean;
    error: string | null;
    reload: () => Promise<void>;
    applyMessage: (message: MessageResponse) => void;
    subscribe: (listener: MessageListener) => () => void;
    subscribeReconnect: (listener: ReconnectListener) => () => void;
}

export const ChatContext = createContext<ChatContextValue | null>(null);