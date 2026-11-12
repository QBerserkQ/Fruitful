import {tokenStorage} from "./tokenStorage.ts";

const BASE_URL = import.meta.env.VITE_API_URL;

export class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
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
        throw new ApiError(response.status, await response.text());
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

export function authRequest<T>(path: string, options: RequestInit ={}): Promise<T> {
    const token = tokenStorage.getAccess();
    return request<T> (path, {
        ...options,
        headers: {
            ...options.headers,
            Authorization: `Bearer ${token}`,
        },
    })
}