import { useState, type ChangeEvent, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../api/client';
import { FormField } from './FormField';

interface LoginErrors {
    email?: string;
    password?: string;
}

export const LoginForm = () => {
    const navigate = useNavigate();
    const { login } = useAuth();

    // Что пользователь ввёл в полях
    const [formData, setFormData] = useState({ email: '', password: '' });
    // Ошибки проверки по полям (например, «Некорректный email»)
    const [errors, setErrors] = useState<LoginErrors>({});
    // Ошибка, которую вернул сервер (например, «Invalid email or password»)
    const [serverError, setServerError] = useState<string | null>(null);
    // Идёт ли сейчас запрос: нужно, чтобы блокировать кнопку
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const validate = (): LoginErrors => {
        const found: LoginErrors = {};
        if (!/^\S+@\S+\.\S+$/.test(formData.email)) {
            found.email = 'Please enter a valid email address';
        }
        if (formData.password.length < 6) {
            found.password = 'Password must be at least 6 characters';
        }
        return found;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault(); // не даём браузеру перезагрузить страницу
        setServerError(null);

        const found = validate();
        setErrors(found);
        if (Object.keys(found).length > 0) return; // есть ошибки, запрос не отправляем

        setIsSubmitting(true);
        try {
            await login(formData);
            navigate('/dashboard', {replace: true});
        } catch (err) {
            setServerError(err instanceof ApiError ? err.message : 'Something went wrong, repeat later');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <FormField
                label="Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                error={errors.email}
            />

            <FormField
                label="Password"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                error={errors.password}
            />

            {serverError && (
                <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {serverError}
                </div>
            )}

            <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-lg bg-slate-900 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting ? 'Wait…' : 'Sign in'}
            </button>
        </form>
    );
};