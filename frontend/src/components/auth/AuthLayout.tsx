import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState} from 'react';

interface AuthLayoutProps {
    mode: 'login' | 'register';
    title: string;
    subtitle: string;
    children: ReactNode;
}

const fruits = [
    { src: '/melon.png',    className: '-left-10 top-1/16 w-40 rotate-12', from: '-translate-x-[200%]'},
    { src: '/melon.png',    className: '-right-5 -top-7 w-36 -rotate-6', from: '-translate-y-[200%]' },
    { src: '/mandarin.png', className: '-top-6 left-3/8 w-32 -rotate-12', from: '-translate-y-[200%]' },
    { src: '/mandarin.png',    className: '-bottom-10 right-1/4 w-40 -rotate-12', from: 'translate-y-[200%]' },
    { src: '/mango.png',    className: '-right-12 top-1/2 w-44 rotate-6', from: 'translate-x-[200%]' },
    { src: '/mango.png', className: 'bottom-6 left-6 w-36 rotate-175', from: '-translate-x-[200%]' },
];

export const AuthLayout = ({ mode, title, subtitle, children }: AuthLayoutProps) => {
    const navigate = useNavigate();
    const isLogin = mode === 'login';
    const [shown, setShown] = useState(false);

    useEffect(() => {
        const id = setTimeout(() => setShown(true), 100);
        return () => clearTimeout(id);
    }, []);

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
                    <h2 className="mt-6 text-2xl font- bold text-slate-900">{title}</h2>
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
            <div className="relative hidden flex-col items-center justify-center bg-white pb-32 lg:order-1 lg:flex">
                <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
                    {fruits.map((fruit, i) => (
                        <img
                            key={i}
                            src={fruit.src}
                            alt=""
                            className={`absolute select-none transition-all duration-700 ease-out motion-reduce:transition-none ${fruit.className} ${
                                shown ? 'translate-x-0 translate-y-0 opacity-100' : `${fruit.from} opacity-0`
                            }`}
                        />
                    ))}
                </div>

                <img
                    src="/bananalogo.png"
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