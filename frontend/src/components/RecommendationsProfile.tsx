import { useEffect, useState } from "react";
import { getRecommendedUsers } from "../api/users";
import type { RecommendedUser } from "../api/types";
import FriendItem from "./FriendItem";

export default function Recommendations() {
    const [users, setUsers] = useState<RecommendedUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    useEffect(() => {
        let cancelled = false;
        getRecommendedUsers()
            .then((data) => {
                if (!cancelled) setUsers(data);
            })
            .catch(() => {
                if (!cancelled) setError(true);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <aside className="lg:self-start lg:sticky lg:top-4 rounded-2xl border bg-white p-4">
            <h2 className="text-lg font-extrabold">You might like</h2>

            {loading && <p className="text-sm text-gray-500">Loading…</p>}
            {error && <p className="text-sm text-red-600">Failed to load recommendations</p>}
            {!loading && !error && (
                <ul className="divide-y">
                    {users.map((user) => (
                        <FriendItem key={user.id} user={user} />
                    ))}
                </ul>
            )}
        </aside>
    );
}