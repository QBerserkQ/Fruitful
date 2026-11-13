import type {LoginRequest, RegisterRequest, User} from "../api/types.ts";
import {createContext, type ReactNode, useCallback, useContext, useEffect, useState} from "react";
import {apiLogin, apiLogout, apiRegister} from "../api/auth.ts";
import {tokenStorage} from "../api/tokenStorage.ts";
import {getMe} from "../api/users.ts";

interface AuthContextValue {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (data: LoginRequest) => Promise<void>;
    register: (data: RegisterRequest) => Promise<void>;
    logout: () => Promise<void>;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState<User | null>(null);

    const refreshUser = useCallback(async () => {
        const me = await getMe();
        setUser(me);
    }, []);

   const login = async (data: LoginRequest) => {
       const response = await apiLogin(data);
       tokenStorage.set(response.accessToken, response.refreshToken);
       await refreshUser();
   }

    const register = async (data: RegisterRequest) => {
        const response = await apiRegister(data);
        tokenStorage.set(response.accessToken, response.refreshToken);
        await refreshUser();
    }

    const logout = async () => {
        const refreshToken = tokenStorage.getRefresh();
        try {
            if (refreshToken) {
                await apiLogout({refreshToken});
            }
        } catch {}
        finally {
            localStorage.clear();
            setUser(null);
        }
    };

    useEffect(() => {
        const init = async () => {
            if (tokenStorage.getAccess()) {
                try {
                    await refreshUser();
                } catch {
                    tokenStorage.clear();
                }
            }
            setIsLoading(false);
        };
        init();
    }, [refreshUser]);

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: user !== null, isLoading, login, register, logout, refreshUser }}>
            {children}
        </AuthContext.Provider>
    );
}


export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
    return ctx;
}

export function useCurrentUser(): User {
    const { user } = useAuth();
    if (!user) throw new Error("useCurrentUser must be used on a protected route");
    return user;
}