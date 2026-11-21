export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RefreshTokenRequest {
    refreshToken: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
}

export interface User {
    email: string;
    username: string;
    role: string;
}

export interface RecommendedUser {
    id: number;
    username: string;
    tagline: string;
}

export interface ConversationResponse {
    id: number;
    partnerId: number;
    partnerUsername: string;
    lastMessage: string | null;
    createdAt: string;
    lastMessageAt: string | null;
}

export interface SendMessageRequest {
    content: string; //max = 2000
}

export interface MessageSentEvent {
    recipientId: number;
    message: MessageResponse;
}

export interface MessageResponse {
    id: number;
    conversationId: number;
    senderId: number;
    content: string;
    createdAt: string;
}

export interface CreateConversationRequest {
    userId: number; //not null and positive
}

export interface UserSummaryResponse {
    id: number;
    username: string;
}

export interface CreatePostRequest {
    content: string;
}

export interface PostResponse {
    id: number
    , content: string
    , createdAt: string
    , author: UserSummaryResponse
}

export interface PageResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    hasNext: boolean;
}