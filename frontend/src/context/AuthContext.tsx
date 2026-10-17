import type {LoginRequest, RegisterRequest, User} from "../api/types.ts";
import {createContext, type ReactNode, useContext, useEffect, useState} from "react";
import {apiLogin, apiRegister} from "../api/auth.ts";
import {tokenStorage} from "../api/tokenStorage.ts";
import {decodeJwt} from "../api/jwt.ts";

interface AuthContextValue {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (data: LoginRequest) => Promise<void>;
    register: (data: RegisterRequest) => Promise<void>;
    logout: () => void;
}

const userFromToken = (token: string): User | null => {
    const payload = decodeJwt(token);
    return payload ? {email: payload.sub} : null;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

   const login = async (data: LoginRequest) => {
       const response = await apiLogin(data);
       tokenStorage.set(response.accessToken, response.refreshToken);
       setUser(userFromToken(response.accessToken));
   }

    const register = async (data: RegisterRequest) => {
        const response = await apiRegister(data);
        tokenStorage.set(response.accessToken, response.refreshToken);
        setUser(userFromToken(response.accessToken));
    }

    const logout = () => {
        tokenStorage.clear();
        setUser(null);
    };

    useEffect(() => {
        const token = tokenStorage.getAccess();
        if (token) {
            setUser(userFromToken(token));
        }
        setIsLoading(false);
    }, []);

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: user !== null, isLoading, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
}


export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
    return ctx;
}
