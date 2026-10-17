import { AuthLayout } from '../components/auth/AuthLayout';
import { LoginForm } from '../components/auth/LoginForm';

export const LoginPage = () => (
    <AuthLayout
        mode="login"
        title="Добро пожаловать"
        subtitle="Введите свои данные для входа в систему"
    >
        <LoginForm />
    </AuthLayout>
);