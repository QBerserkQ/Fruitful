import { useState } from "react";
import type { RecommendedUser } from "../api/types.ts";
import {Check} from "lucide-react";

export default function FriendItem({ user }: { user: RecommendedUser }) {
    const [following, setFollowing] = useState(false);

    return (
        <li className="flex items-center gap-3 py-2.5">
            <div className="grid h-10 w-10 flex-none place-items-center rounded-full bg-orange-300 font-extrabold">
                {user.username[0].toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
                <b className="block truncate leading-tight">{user.username}</b>
                <small className="text-gray-500">{user.tagline}</small>
            </div>
            <button
                onClick={() => setFollowing((prev) => !prev)}
                aria-pressed={following}
                className={`rounded-full border-2 border-purple-900 px-3.5 py-1 text-sm font-semibold ${
                    following ? "bg-purple-900 text-white" : "hover:bg-gray-100"
                }`}
            >
                {following ? <Check size={18} strokeWidth={3}/> : "Follow"}
            </button>
        </li>
    );
}