import { useState, type ChangeEvent, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ApiError } from '../../api/client';
import { FormField } from './FormField';

interface RegisterErrors {
    username?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
}

export const RegisterForm = () => {
    const navigate = useNavigate();
    const { register } = useAuth();

    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [errors, setErrors] = useState<RegisterErrors>({});
    const [serverError, setServerError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const validate = (): RegisterErrors => {
        const found: RegisterErrors = {};
        if (!formData.username.trim()) {
            found.username = 'Введите имя пользователя';
        }
        if (!/^\S+@\S+\.\S+$/.test(formData.email)) {
            found.email = 'Введите корректный email';
        }
        if (formData.password.length < 6) {
            found.password = 'Пароль должен быть не короче 6 символов';
        }
        if (formData.confirmPassword !== formData.password) {
            found.confirmPassword = 'Пароли не совпадают';
        }
        return found;
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setServerError(null);

        const found = validate();
        setErrors(found);
        if (Object.keys(found).length > 0) return;

        setIsSubmitting(true);
        try {
            // confirmPassword нужен только для проверки, на бэк его отправлять не надо
            const { username, email, password } = formData;
            await register({ username, email, password });
            navigate('/');
        } catch (err) {
            setServerError(err instanceof ApiError ? err.message : 'Что-то пошло не так, попробуйте позже');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <FormField
                label="Имя пользователя"
                name="username"
                value={formData.username}
                onChange={handleChange}
                error={errors.username}
            />

            <FormField
                label="Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                error={errors.email}
            />

            <FormField
                label="Пароль"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                error={errors.password}
            />

            <FormField
                label="Повторите пароль"
                name="confirmPassword"
                type="password"
                value={formData.confirmPassword}
                onChange={handleChange}
                error={errors.confirmPassword}
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
                {isSubmitting ? 'Подождите…' : 'Зарегистрироваться'}
            </button>
        </form>
    );
};