import {useChat} from "./useChat.ts";

interface Props {
    selectedId: number | null;
    onSelect: (conversationId: number) => void;
}

// сегодня: "14:05", раньше: "12.11"
function formatTime(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
        return d.toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"});
    }
    return d.toLocaleDateString([], {day: "2-digit", month: "2-digit"});
}

export function ChatList({selectedId, onSelect}: Props) {
    const {conversations, isLoading, error, reload} = useChat();

    if (isLoading) return <p className="p-4 text-sm text-gray-500">Загрузка...</p>;

    if (error) {
        return (
            <div className="p-4 text-sm">
                <p className="text-red-600">{error}</p>
                <button onClick={() => void reload()} className="mt-2 underline">
                    Повторить
                </button>
            </div>
        );
    }

    if (conversations.length === 0) {
        return <p className="p-4 text-sm text-gray-500">Пока нет переписок</p>;
    }

    return (
        <ul className="divide-y">
            {conversations.map((c) => (
                <li key={c.id}>
                    <button
                        onClick={() => onSelect(c.id)}
                        className={`flex w-full items-center gap-3 p-3 text-left hover:bg-gray-100 ${
                            c.id === selectedId ? "bg-gray-100" : ""
                        }`}
                    >
                        {/* заглушка вместо аватара: первая буква имени */}
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-200 font-semibold">
                            {c.partnerUsername.charAt(0).toUpperCase()}
                        </div>
                        <div className="min-w-0 flex-1">
                            <div className="flex justify-between gap-2">
                                <span className="truncate font-medium">{c.partnerUsername}</span>
                                <span className="shrink-0 text-xs text-gray-500">
                                    {formatTime(c.lastMessageAt ?? c.createdAt)}
                                </span>
                            </div>
                            <p className="truncate text-sm text-gray-600">
                                {c.lastMessage ?? "Нет сообщений"}
                            </p>
                        </div>
                    </button>
                </li>
            ))}
        </ul>
    );
}