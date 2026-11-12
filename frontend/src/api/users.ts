import type {RecommendedUser, User} from "./types.ts";
import { authRequest } from "./client";

export async function getRecommendedUsers(): Promise<RecommendedUser[]> {
    // TODO: заменить на запрос через client.ts, когда будет эндпоинт
    await new Promise((resolve) => setTimeout(resolve, 500));
    return [
        { id: 1, username: "elena_k", tagline: "Java · Spring" },
        { id: 2, username: "nikita_r", tagline: "React · TypeScript" },
        { id: 3, username: "sofia_l", tagline: "PostgreSQL · DevOps" },
        { id: 4, username: "timur_b", tagline: "Backend · Docker" },
        { id: 5, username: "dmitri_q", tagline: "Java · Spring" },
        { id: 6, username: "vlad_r", tagline: "Backend · Docker" },
        { id: 7, username: "alex_t", tagline: "React · TypeScript" },
    ];
}

export const getMe = () => authRequest<User>("/api/users/me");

export function changeUsername(username: string) {
    return authRequest<void>("/api/users/me/username", {
        method: "PUT",
        body: JSON.stringify({username}),
    })
}

export function changePassword(oldPassword: string, newPassword: string, refreshToken: string) {
    return authRequest<void>("/api/auth/password", {
        method: "PUT",
        body: JSON.stringify({oldPassword, newPassword, refreshToken})
    })
}

export type Session = {
    id: number;
    userAgent: string;
    lastUsedAt: string;
    current: boolean;
}

export function getSessions() {
    return authRequest<Session[]>("/api/users/me/sessions");
}

export function deleteSession(id: number) {
    return authRequest<void>(`/api/users/me/sessions${id}`, { method: "DELETE" })
}