import { AuthLayout } from '../components/auth/AuthLayout';
import { LoginForm } from '../components/auth/LoginForm';

export const LoginPage = () => (
    <AuthLayout
        mode="login"
        title="Welcome back!"
        subtitle="Hello, carrot!"
    >
        <LoginForm />
    </AuthLayout>
);