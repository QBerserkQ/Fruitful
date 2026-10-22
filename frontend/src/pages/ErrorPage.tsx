import { Link } from 'react-router-dom';

interface ErrorPageProps {
    code?: string;
    title?: string;
    message?: string;
    linkTo?: string;
    linkText?: string;
}

export const ErrorPage = ({
                              code = '404',
                              title = 'Page not found',
                              message = 'Саня лох!',
                              linkTo = '/dashboard',
                              linkText = 'Go back',
                          }: ErrorPageProps) => (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 px-4 text-center">
        <p className="text-5xl font-bold text-slate-900">{code}</p>
        <h1 className="text-xl font-semibold text-slate-800">{title}</h1>
        <p className="text-sm text-slate-500">{message}</p>
        <Link to={linkTo} className="mt-2 rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800">
            {linkText}
        </Link>
    </div>
);