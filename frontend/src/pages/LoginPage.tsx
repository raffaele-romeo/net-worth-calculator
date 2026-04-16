import { useAuth } from '@/context/AuthContext';
import { useId, useState } from 'react';
import { useForm, SubmitHandler } from 'react-hook-form';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router';

type FormData = {
  username: string;
  password: string;
};

export default function LoginPage() {
  const [isRegister, setIsRegister] = useState(false);
  const formId = useId();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>();

  const { login, register: signUp } = useAuth();
  const navigate = useNavigate();

  const onSubmit: SubmitHandler<FormData> = async (data) => {
    try {
      if (isRegister) {
        await signUp(data.username, data.password);
        toast.success('Account created!');
      } else {
        await login(data.username, data.password);
        toast.success('Welcome back');
      }
      navigate('/');
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Something went wrong';
      toast.error(message);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <header className="mb-6 text-center">
          <h1 className="text-2xl font-semibold text-slate-900">
            {isRegister ? 'Create account' : 'Welcome back'}
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {isRegister ? 'Sign up to track your net worth' : 'Sign in to continue'}
          </p>
        </header>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          <div className="flex flex-col gap-1">
            <label htmlFor={`${formId}-username`} className="text-sm font-medium text-slate-700">
              Username
            </label>
            <input
              id={`${formId}-username`}
              autoComplete="username"
              aria-invalid={!!errors.username}
              aria-describedby={errors.username ? `${formId}-username-error` : undefined}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              {...register('username', { required: 'Username required' })}
            />
            {errors.username && (
              <span id={`${formId}-username-error`} role="alert" className="text-xs text-rose-600">
                {errors.username.message}
              </span>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <label htmlFor={`${formId}-password`} className="text-sm font-medium text-slate-700">
              Password
            </label>
            <input
              id={`${formId}-password`}
              type="password"
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              aria-invalid={!!errors.password}
              aria-describedby={errors.password ? `${formId}-password-error` : undefined}
              className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              {...register('password', {
                required: 'Password required',
                minLength: { value: 8, message: 'Min 8 characters' },
              })}
            />
            {errors.password && (
              <span id={`${formId}-password-error`} role="alert" className="text-xs text-rose-600">
                {errors.password.message}
              </span>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50"
          >
            {isSubmitting ? 'Please wait…' : isRegister ? 'Register' : 'Login'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <button
            type="button"
            onClick={() => setIsRegister((v) => !v)}
            className="text-sm text-indigo-600 hover:text-indigo-700"
          >
            {isRegister ? 'Have an account? Login' : 'New user? Register'}
          </button>
        </div>
      </div>
    </div>
  );
}
