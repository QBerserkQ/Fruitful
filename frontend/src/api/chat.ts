import {authRequest} from "./client.ts";
import type {ConversationResponse, CreateConversationRequest, MessageResponse, SendMessageRequest} from "./types.ts";


const CONV_PATH = "/api/conversations";

export function apiGetConversations() {
    return authRequest<ConversationResponse[]>(CONV_PATH);
}

export function apiCreateConversation(data: CreateConversationRequest) {
    return authRequest<ConversationResponse>(CONV_PATH, {
        method: "POST",
        body: JSON.stringify(data)
    });
}

export function apiGetMessages(conversationId: number, page = 0, size = 30) {
    return authRequest<MessageResponse[]>(
        `${CONV_PATH}/${conversationId}/messages?page=${page}&size=${size}`
    );
}

export function apiSendMessage(conversationId: number, body: SendMessageRequest) {
    return authRequest<MessageResponse>(`${CONV_PATH}/${conversationId}/messages`, {
        method: "POST",
        body: JSON.stringify(body),
    });
}