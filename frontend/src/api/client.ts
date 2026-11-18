import {tokenStorage} from "./tokenStorage.ts";
import {apiRefreshToken} from "./auth.ts";
import type {RefreshTokenRequest} from "./types.ts";

const BASE_URL = import.meta.env.VITE_API_URL;

export class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

let onAuthFailure: (() => void) | null = null;

export function setAuthFailureHandler(handler: (() => void) | null) {
    onAuthFailure = handler;
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch( `${BASE_URL}${path}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers,
        },
    });

    if (!response.ok) {
        let message = "Something went wrong :(";
        try {
            const body = await response.json();
            message = body.message ?? message;
        } catch (e){
            console.warn("Failed to parse the error body", e)
        }
        throw new ApiError(response.status, message);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

async function doRefresh(): Promise<string> {
    const refreshToken = tokenStorage.getRefresh();
    if (!refreshToken) {
        throw new ApiError(401, "No refresh token");
    }

    const body: RefreshTokenRequest = {refreshToken: refreshToken};
    const data = await apiRefreshToken(body);

    tokenStorage.set(data.accessToken, data.refreshToken);

    return data.accessToken;
}

let refreshPromise: Promise<string> | null = null;

function refreshAccessToken(): Promise<string> {
    if (!refreshPromise) {
        refreshPromise = doRefresh().finally(() => {
            refreshPromise = null;
        });
    }
    return refreshPromise;
}

export async function authRequest<T>(path: string, options: RequestInit ={}): Promise<T> {
    const withToken = (token: string | null) =>
        request<T>(path, {
            ...options,
            headers: {
                ...options.headers,
                Authorization: `Bearer ${token}`,
            },
        });

    //basic try
    try {
        return await withToken(tokenStorage.getAccess());
    } catch (e) {
        if (!(e instanceof ApiError) || e.status !==401) throw e;
    }

    //if receive 401 then update refresh token
    let newToken: string;
    try {
        newToken = await refreshAccessToken();
    } catch (e) {
        if (e instanceof ApiError) {
            tokenStorage.clear();
            onAuthFailure?.();
        }
        throw e;
    }

    //the only one repeat with new token
    return withToken(newToken);
}

function getTokenExpiry(token: string): number | null {
    try {
        const payload = token.split(".")[1];
        const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
        const {exp} = JSON.parse(json);

        return typeof exp === "number" ? exp * 1000 : null;
    } catch {
        return null;
    }
}

export async function getFreshAccessToken(): Promise<string> {
    const LEEWAY_MS = 30_000;

    const access = tokenStorage.getAccess();
    const expiresAt = access ? getTokenExpiry(access) : null;

    if (access && expiresAt && expiresAt - Date.now() > LEEWAY_MS) {
        return access; // токен ещё живой, ничего не делаем
    }

    try {
        return await refreshAccessToken();
    } catch (e) {
        if (e instanceof ApiError) {
            tokenStorage.clear();
            onAuthFailure?.();
        }
        throw e;
    }
}