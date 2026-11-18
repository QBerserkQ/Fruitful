import {useState} from "react";
import {useNavigate} from "react-router-dom";
import {apiCreateConversation} from "../api/chat.ts";
import type {UserSummaryResponse} from "../api/types.ts";
import {useUserSearch} from "../hooks/useUserSearch.ts";
import {useChat} from "./useChat.ts";

interface Props {
    query: string;
    onSelected: () => void;
}

export function UserSearchResults({query, onSelected}: Props) {
    const navigate = useNavigate();
    const {reload} = useChat();
    const {active: isOpen, results, loading, error, showEmpty} = useUserSearch(query);

    const [creatingId, setCreatingId] = useState<number | null>(null);
    const [createError, setCreateError] = useState<string | null>(null);

    async function handleSelect(user: UserSummaryResponse) {
        if (creatingId !== null) return;
        setCreatingId(user.id);
        setCreateError(null);
        try {
            const conversation = await apiCreateConversation({userId: user.id});
            await reload();
            onSelected();
            navigate(`/chats/${conversation.id}`);
        } catch {
            setCreateError("Failed to create chat");
            setCreatingId(null);
        }
    }

    return (
        <div
            aria-hidden={!isOpen}
            inert={!isOpen}
            className={`absolute inset-0 z-10 bg-white transition-[opacity,translate] duration-300 ease-out motion-reduce:transition-none ${
                isOpen
                    ? "translate-y-0 opacity-100"
                    : "pointer-events-none -translate-y-2 opacity-0"
            }`}
        >
            <div className="h-full overflow-y-auto">
                <h2 className="px-3 pt-3 pb-1 text-xs font-semibold uppercase text-gray-500">
                    Users
                </h2>
                {loading && <p className="px-3 py-1 text-sm text-gray-500">Find…</p>}
                {(error || createError) && (
                    <p className="px-3 py-1 text-sm text-red-500">{error ?? createError}</p>
                )}
                {showEmpty && (
                    <p className="px-3 py-1 text-sm text-gray-500">No one found</p>
                )}
                <ul className="pb-2">
                    {results.map((user) => (
                        <li key={user.id}>
                            <button
                                type="button"
                                onClick={() => handleSelect(user)}
                                disabled={creatingId !== null}
                                className="w-full px-3 py-2 text-left hover:bg-black/5 disabled:opacity-50"
                            >
                                {user.username}
                            </button>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
}