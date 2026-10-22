import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';

interface AuthLayoutProps {
    mode: 'login' | 'register';
    title: string;
    subtitle: string;
    children: ReactNode;
}

export const AuthLayout = ({ mode, title, subtitle, children }: AuthLayoutProps) => {
    const navigate = useNavigate();
    const isLogin = mode === 'login';

    const tabClass = (active: boolean) =>
        `flex-1 rounded-lg py-2 text-sm font-medium transition-all ${
            active ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
        }`;

    return (
        <div className="grid min-h-screen lg:grid-cols-2">
            {/* Левая жёлтая часть с карточкой */}
            <div className="flex items-center justify-center bg-linear-to-br from-amber-300 to-orange-400 px-4 py-12 lg:order-2">
                <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">
                    {/* Вкладки */}
                    <div className="flex gap-1 rounded-lg bg-slate-100 p-1">
                        <button type="button" onClick={() => navigate('/login')} className={tabClass(isLogin)}>
                            Sign in
                        </button>
                        <button type="button" onClick={() => navigate('/register')} className={tabClass(!isLogin)}>
                            Create account
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
                                Don't have an account?{' '}
                                <Link to="/register" className="font-medium text-orange-600 hover:underline">
                                    Create one
                                </Link>
                            </>
                        ) : (
                            <>
                                Do you have an account?{' '}
                                <Link to="/login" className="font-medium text-orange-600 hover:underline">
                                    Login
                                </Link>
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* Правая белая часть с бананом (только на широких экранах) */}
            <div className="hidden flex-col items-center justify-center bg-white pb-32 lg:order-1 lg:flex">
                <img
                    src="/banana1upscale.png"
                    alt="Banana"
                    className="w-[130%] max-w-none drop-shadow-xl"
                />
                <p className="-mt-20 text-lg font-medium tracking-wide text-slate-500">
                    Social media for "fruitful" people
                </p>
            </div>
        </div>
    );
};