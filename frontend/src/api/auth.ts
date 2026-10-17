import type {AuthResponse
    , LoginRequest
    , RefreshTokenRequest
    , RegisterRequest}
from "./types";
import {request} from "./client.ts";

const AUTH_PATH = "/api/auth"

export function apiLogin(data: LoginRequest) {
    return request<AuthResponse>(`${AUTH_PATH}/login`, {
        method: "POST",
        body: JSON.stringify(data),
    });
}

export function apiRegister(data: RegisterRequest) {
    return request<AuthResponse>(`${AUTH_PATH}/register`, {
       method: "POST",
       body: JSON.stringify(data)
    });
}

export function apiRefreshToken(data: RefreshTokenRequest) {
    return request<AuthResponse>(`${AUTH_PATH}/refresh`,{
       method: "POST",
       body: JSON.stringify(data)
    });
}