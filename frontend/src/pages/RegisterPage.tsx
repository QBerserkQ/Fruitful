import { AuthLayout } from '../components/auth/AuthLayout';
import { RegisterForm } from '../components/auth/RegisterForm';

export const RegisterPage = () => (
    <AuthLayout
        mode="register"
        title="Sign up"
        subtitle="Hello, banana!"
    >
        <RegisterForm />
    </AuthLayout>
);