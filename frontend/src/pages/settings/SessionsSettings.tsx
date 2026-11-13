import { useEffect, useState } from "react";
import { getSessions, deleteSession } from "../../api/users.ts";
import type { Session } from "../../api/users.ts";
import { useAuth } from "../../context/AuthContext.tsx";

export default function SessionsSettings() {
    const { logout } = useAuth();
    const [sessions, setSessions] = useState<Session[]>([]);
    const [openId, setOpenId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        getSessions()
            .then(setSessions)
            .catch((e) => setError(e instanceof Error ? e.message : "Не удалось загрузить сессии"))
            .finally(() => setLoading(false));
    }, []);

    async function handleLogout(s: Session) {
        try {
            await deleteSession(s.id);
            if (s.current) {
                await logout(); // вышли из текущей сессии
                return;
            }
            setSessions((prev) => prev.filter((x) => x.id !== s.id));
            setOpenId(null);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Не удалось завершить сессию");
        }
    }

    return (
        <div className="rounded-2xl bg-white p-6 shadow">
            <h1 className="mb-4 text-2xl font-bold">Sessions</h1>

            {loading && <p className="text-sm text-gray-500">Loading...</p>}
            {error && <p className="mb-3 text-sm text-red-600">{error}</p>}

            <ul className="space-y-2">
                {sessions.map((s) => {
                    const open = openId === s.id;
                    return (
                        <li key={s.id} className="rounded-xl border border-gray-200">
                            <button
                                onClick={() => setOpenId(open ? null : s.id)}
                                className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left"
                            >
                                <span className="min-w-0 truncate text-sm font-medium">
                                    {s.userAgent ?? "Unknown device"}
                                </span>
                                {s.current && (
                                    <span className="shrink-0 rounded-md bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
                                        Current
                                    </span>
                                )}
                            </button>

                            {open && (
                                <div className="space-y-1 border-t border-gray-200 px-4 py-3 text-sm text-gray-600">
                                    <p>Last active: {new Date(s.lastUsedAt).toLocaleString()}</p>
                                    <button
                                        onClick={() => handleLogout(s)}
                                        className="mt-2 rounded-md bg-red-500 px-3 py-1 text-xs font-medium text-white"
                                    >
                                        {s.current ? "Log out" : "End session"}
                                    </button>
                                </div>
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}