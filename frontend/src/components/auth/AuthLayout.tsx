import type {ReactNode} from "react";
import {Link, useNavigate} from "react-router-dom";

interface AuthLayoutProps {
    mode: 'login' | 'register';
    title: string;
    subtitle: string;
    children: ReactNode;
}

export const AuthLayout = ({ mode, title, subtitle, children }: AuthLayoutProps) => {
    const navigate = useNavigate();
    const isLogin = mode === 'login';

    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-500 px-4">
            <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">
                {/* Вкладки */}
                <div className="flex gap-1 rounded-lg bg-slate-100 p-1">
                    <button
                        type="button"
                        onClick={() => navigate('/login')}
                        className={`flex-1 rounded-lg py-2 text-sm font-medium transition-all ${
                            isLogin
                                ? 'bg-white text-slate-900 shadow-sm'
                                : 'text-slate-500 hover:text-slate-700'
                        }`}
                    >
                        Вход
                    </button>

                    <button
                        type="button"
                        onClick={() => navigate('/register')}
                        className={`flex-1 rounded-lg py-2 text-sm font-medium transition-all ${
                            !isLogin
                                ? 'bg-white text-slate-900 shadow-sm'
                                : 'text-slate-500 hover:text-slate-700'
                        }`}
                    >
                        Регистрация
                    </button>
                </div>

                {/* Заголовок */}
                <h2 className="mt-6 text-2xl font-bold text-slate-900">{title}</h2>
                <p className="mt-2 text-sm text-slate-500">{subtitle}</p>

                {/* Сюда подставится форма */}
                <div className="mt-6">{children}</div>

                {/* Ссылка внизу */}
                <div className="mt-6 text-center text-sm text-slate-500">
                    {isLogin ? (
                        <>
                            Нет аккаунта?{' '}
                            <Link to="/register" className="font-medium text-slate-900 hover:underline">
                                Зарегистрироваться
                            </Link>
                        </>
                    ) : (
                        <>
                            Уже есть аккаунт?{' '}
                            <Link to="/login" className="font-medium text-slate-900 hover:underline">
                                Войти
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};