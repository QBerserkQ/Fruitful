import { useState } from "react";
import type { FormEvent } from "react";
import { useCurrentUser } from "../../context/AuthContext.tsx";
import { useAuth } from "../../context/AuthContext.tsx";
import { changeUsername, changePassword } from "../../api/users.ts";
import { tokenStorage } from "../../api/tokenStorage.ts";

const USERNAME_MIN = 3;
const USERNAME_MAX = 50;
const PASSWORD_MIN = 8;

const inputClass =
    "w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-green-600 focus:ring-1 focus:ring-green-600";
const buttonClass =
    "rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:bg-gray-300";

function UsernameForm({ currentUsername }: { currentUsername: string }) {
    const { refreshUser } = useAuth();
    const [value, setValue] = useState(currentUsername);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);

    const trimmed = value.trim();
    const unchanged = trimmed === currentUsername;

    function handleChange(next: string) {
        setValue(next);
        setError(null);
        setSuccess(false);
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (trimmed.length < USERNAME_MIN || trimmed.length > USERNAME_MAX) {
            setError(`Username must be between ${USERNAME_MIN} and ${USERNAME_MAX} characters long`);
            return;
        }
        setLoading(true);
        setError(null);
        try {
            await changeUsername(trimmed);
            await refreshUser();
            setSuccess(true);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Could not update username.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-3">
            <div>
                <label htmlFor="username" className="mb-1 block text-sm font-medium text-gray-700">
                    Username
                </label>
                <input
                    id="username"
                    value={value}
                    onChange={(e) => handleChange(e.target.value)}
                    className={inputClass}
                    autoComplete="username"
                />
                {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
                {success && <p className="mt-1 text-sm text-green-600">Username updated</p>}
            </div>
            <button type="submit" disabled={unchanged || trimmed === ""} className={buttonClass}>
                {loading ? "Saving.." : "Save username"}
            </button>
        </form>
    );
}

function PasswordForm() {
    const [current, setCurrent] = useState("");
    const [next, setNext] = useState("");
    const [confirm, setConfirm] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);

    const allFilled = current !== "" && next !== "" && confirm !== "";

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setSuccess(false);

        if (next.length < PASSWORD_MIN) {
            setError(`Password must be at least ${PASSWORD_MIN} characters long.`);
            return;
        }
        if (next !== confirm) {
            setError("Passwords do not match.");
            return;
        }
        if (next === current) {
            setError("New password must be different from the current password.");
            return;
        }

        const refreshToken = tokenStorage.getRefresh();
        if (!refreshToken) {
            setError("Session not found. Please log in again.");
            return;
        }

        setLoading(true);
        setError(null);

        try {
            await changePassword(current, next, refreshToken);
            setCurrent("");
            setNext("");
            setConfirm("");
            setSuccess(true);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to change password.")
        } finally {
            setLoading(false);
        }
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-3">
            <div>
                <label htmlFor="current-password" className="mb-1 block text-sm font-medium text-gray-700">
                    Current password
                </label>
                <input
                    id="current-password"
                    type="password"
                    value={current}
                    onChange={(e) => setCurrent(e.target.value)}
                    className={inputClass}
                    autoComplete="current-password"
                />
            </div>
            <div>
                <label htmlFor="new-password" className="mb-1 block text-sm font-medium text-gray-700">
                    New password
                </label>
                <input
                    id="new-password"
                    type="password"
                    value={next}
                    onChange={(e) => setNext(e.target.value)}
                    className={inputClass}
                    autoComplete="new-password"
                />
            </div>
            <div>
                <label htmlFor="confirm-password" className="mb-1 block text-sm font-medium text-gray-700">
                    Confirm new password
                </label>
                <input
                    id="confirm-password"
                    type="password"
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                    className={inputClass}
                    autoComplete="new-password"
                />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            {success && <p className="text-sm text-green-600">Password updated</p>}
            <button type="submit" disabled={!allFilled} className={buttonClass}>
                {loading ? "Saving.." : "Change password"}
            </button>
        </form>
    );
}

export default function AccountSettings() {
    const user = useCurrentUser();

    return (
        <div className="rounded-2xl bg-white p-6 shadow">
            <h1 className="mb-6 text-2xl font-bold">Account</h1>

            <section className="max-w-md">
                <h2 className="mb-3 text-lg font-semibold">Username</h2>
                <UsernameForm currentUsername={user.username} />
            </section>

            <hr className="my-6 border-gray-200" />

            <section className="max-w-md">
                <h2 className="mb-3 text-lg font-semibold">Password</h2>
                <PasswordForm />
            </section>
        </div>
    );
}