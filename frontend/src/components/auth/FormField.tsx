import type { ChangeEvent } from 'react';

interface FormFieldProps {
    label: string;
    name: string;
    type?: string;
    value: string;
    onChange: (e: ChangeEvent<HTMLInputElement>) => void;
    error?: string;
}

export const FormField = ({
                              label,
                              name,
                              type = 'text',
                              value,
                              onChange,
                              error,
                          }: FormFieldProps) => (
    <div>
        <label htmlFor={name} className="block text-sm font-medium text-slate-700">
            {label}
        </label>

        <input
            id={name}
            name={name}
            type={type}
            value={value}
            onChange={onChange}
            className={`mt-1 w-full rounded-lg border px-3 py-2 outline-none transition focus:ring-2 ${
                error
                    ? 'border-red-400 focus:border-red-500 focus:ring-red-100'
                    : 'border-slate-300 focus:border-slate-500 focus:ring-slate-200'
            }`}
        />

        {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
);