export interface JwtPayload {
    sub: string;
    exp: number;
}

export function decodeJwt(token: string): JwtPayload | null {
    try{
        const payload = token.split('.')[1];
        const base64 = payload.replace(/-/g, '+')
                                     .replace(/_/g, '/');

        return JSON.parse(atob(base64));
    }catch {
        return null;
    }
}