import type { RecommendedUser } from "./types.ts";

export async function getRecommendedUsers(): Promise<RecommendedUser[]> {
    // TODO: заменить на запрос через client.ts, когда будет эндпоинт
    await new Promise((resolve) => setTimeout(resolve, 500));
    return [
        { id: 1, username: "elena_k", tagline: "Java · Spring" },
        { id: 2, username: "nikita_r", tagline: "React · TypeScript" },
        { id: 3, username: "sofia_l", tagline: "PostgreSQL · DevOps" },
        { id: 4, username: "timur_b", tagline: "Backend · Docker" },
    ];
}