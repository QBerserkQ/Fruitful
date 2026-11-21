import { authRequest } from "./client";
import type {CreatePostRequest, PageResponse, PostResponse} from "./types.ts";

export function apiGetFeed(page:number, size: number) {
    return authRequest<PageResponse<PostResponse>>(`/api/posts?page=${page}&size=${size}`);
}

export function apiCreatePost(request: CreatePostRequest) {
    return authRequest<PostResponse>("/api/posts", {
        method: "POST",
        body: JSON.stringify(request)
    })
}

export function apiGetMyPosts(page:number, size: number) {
    return authRequest<PageResponse<PostResponse>>(`/api/posts/me?page=${page}&size=${size}`);
}