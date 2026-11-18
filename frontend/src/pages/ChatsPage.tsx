import {useState} from "react";
import {Link, useNavigate, useParams} from "react-router-dom";
import {ChatList} from "../chat/ChatList.tsx";
import {ChatWindow} from "../chat/ChatWindow.tsx";
import {UserSearchResults} from "../chat/UserSearchResults.tsx";
import {useChat} from "../chat/useChat.ts";

export default function ChatsPage() {
    const {conversations, isLoading} = useChat();
    const navigate = useNavigate();
    const [searchQuery, setSearchQuery] = useState("");

    const {conversationId} = useParams();
    const parsedId = conversationId ? Number(conversationId) : null;
    const selectedId = parsedId !== null && !Number.isNaN(parsedId) ? parsedId : null;

    const selected = conversations.find((c) => c.id === selectedId) ?? null;

    return (
        <div className="h-full overflow-hidden rounded-2xl border bg-white shadow lg:h-[calc(100dvh-2.5rem)]">
            {selectedId === null ? (
                <div className="flex h-full flex-col">
                    <div className="flex items-center gap-3 border-b p-3">
                        <h1 className="text-lg font-semibold">Chats</h1>
                        <input
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Find user"
                            maxLength={50}
                            className="ml-auto min-w-0 flex-1 rounded-lg border px-3 py-1.5 text-sm sm:max-w-xs"
                        />
                    </div>
                    <div className="relative min-h-0 flex-1">
                        <div className="h-full overflow-y-auto">
                            <ChatList selectedId={null} onSelect={(id) => navigate(`/chats/${id}`)}/>
                        </div>
                        <UserSearchResults
                            query={searchQuery}
                            onSelected={() => setSearchQuery("")}
                        />
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
                    <p>Conversation not found</p>
                    <Link to="/chats" className="underline">To the dialogues</Link>
                </div>
            )}
        </div>
    );
}