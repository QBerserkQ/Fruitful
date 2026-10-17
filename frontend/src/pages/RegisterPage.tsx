import { AuthLayout } from '../components/auth/AuthLayout';
import { RegisterForm } from '../components/auth/RegisterForm';

export const RegisterPage = () => (
    <AuthLayout
        mode="register"
        title="Создать аккаунт"
        subtitle="Заполните поля для регистрации аккаунта"
    >
        <RegisterForm />
    </AuthLayout>
);