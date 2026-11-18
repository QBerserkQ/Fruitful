import {Link, useNavigate, useParams} from "react-router-dom";
import {ChatList} from "../chat/ChatList.tsx";
import {ChatWindow} from "../chat/ChatWindow.tsx";
import {useChat} from "../chat/useChat.ts";

export default function ChatsPage() {
    const {conversations, isLoading} = useChat();
    const navigate = useNavigate();

    const {conversationId} = useParams();
    const parsedId = conversationId ? Number(conversationId) : null;
    const selectedId = parsedId !== null && !Number.isNaN(parsedId) ? parsedId : null;

    const selected = conversations.find((c) => c.id === selectedId) ?? null;

    return (
        <div className="h-full overflow-hidden rounded-2xl border bg-white shadow lg:h-[calc(100dvh-2.5rem)]">
            {selectedId === null ? (
                <div className="flex h-full flex-col">
                    <h1 className="border-b p-3 text-lg font-semibold">Чаты</h1>
                    <div className="min-h-0 flex-1 overflow-y-auto">
                        <ChatList selectedId={null} onSelect={(id) => navigate(`/chats/${id}`)} />
                    </div>
                </div>
            ) : selected ? (
                <ChatWindow
                    key={selected.id}
                    conversationId={selected.id}
                    partnerId={selected.partnerId}
                    partnerUsername={selected.partnerUsername}
                />
            ) : isLoading ? (
                <div className="flex h-full items-center justify-center text-gray-500">Загрузка...</div>
            ) : (
                <div className="flex h-full flex-col items-center justify-center gap-2 text-gray-500">
                    <p>Беседа не найдена</p>
                    <Link to="/chats" className="underline">К диалогам</Link>
                </div>
            )}
        </div>
    );
}