import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

interface AuthCardProps {
  mode: 'login' | 'register';
}

export const AuthCard: React.FC<AuthCardProps> = ({ mode }) => {
  const navigate = useNavigate();
  const isLogin = mode === 'login';

  // Состояние формы
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (isLogin) {
      console.log('--- Вход ---', {
        identifier: formData.email,
        password: formData.password,
      });
    } else {
      console.log('--- Регистрация ---', formData);
    }

    alert(
      `Форма [${isLogin ? 'Вход' : 'Регистрация'}] отправлена! Подробности в консоли.`,
    );
  };

  return (
      <div className="flex min-h-screen items-center justify-center bg-slate-500 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-lg">

              {/* Tabs */}
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
              <h2 className="mt-6 text-2xl font-bold text-slate-900">
                  {isLogin ? 'Добро пожаловать' : 'Создать аккаунт'}
              </h2>

              <p className="mt-2 text-sm text-slate-500">
                  {isLogin
                      ? 'Введите свои данные для входа в систему'
                      : 'Заполните поля для регистрации аккаунта'}
              </p>

              {/* Форма */}
              <form onSubmit={handleSubmit} className="mt-6 space-y-4">

                  {/* Username */}
                  {!isLogin && (
                      <div>
                          <label
                              htmlFor="username"
                              className="block text-sm font-medium text-slate-700"
                          >
                              Имя пользователя
                          </label>

                          <input
                              id="username"
                              name="username"
                              type="text"
                              value={formData.username}
                              onChange={handleChange}
                              className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
                          />
                      </div>
                  )}

                  {/* Email */}
                  <div>
                      <label
                          htmlFor="email"
                          className="block text-sm font-medium text-slate-700"
                      >
                          Email {isLogin && 'или логин'}
                      </label>

                      <input
                          id="email"
                          name="email"
                          type="text"
                          value={formData.email}
                          onChange={handleChange}
                          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
                      />
                  </div>

                  {/* Password */}
                  <div>
                      <label
                          htmlFor="password"
                          className="block text-sm font-medium text-slate-700"
                      >
                          Пароль
                      </label>

                      <input
                          id="password"
                          name="password"
                          type="password"
                          value={formData.password}
                          onChange={handleChange}
                          className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 outline-none transition focus:border-slate-500 focus:ring-2 focus:ring-slate-200"
                      />
                  </div>

                  {/* Submit */}
                  <button
                      type="submit"
                      className="w-full rounded-lg bg-slate-900 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800"
                  >
                      {isLogin ? 'Войти' : 'Зарегистрироваться'}
                  </button>
              </form>

              {/* Bottom link */}
              <div className="mt-6 text-center text-sm text-slate-500">
                  {isLogin ? (
                      <>
                          Нет аккаунта?{' '}
                          <Link
                              to="/register"
                              className="font-medium text-slate-900 hover:underline"
                          >
                              Зарегистрироваться
                          </Link>
                      </>
                  ) : (
                      <>
                          Уже есть аккаунт?{' '}
                          <Link
                              to="/login"
                              className="font-medium text-slate-900 hover:underline"
                          >
                              Войти
                          </Link>
                      </>
                  )}
              </div>

          </div>
      </div>
  );
};
