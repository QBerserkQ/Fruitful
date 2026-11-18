import {Client} from "@stomp/stompjs";
import {ApiError, getFreshAccessToken} from "../api/client.ts";
import type {MessageResponse} from "../api/types.ts";

const WS_URL = import.meta.env.VITE_API_URL.replace(/^http/, "ws") + "/ws";

export function connectChatSocket(onMessage: (message: MessageResponse) => void): () => void {
    const client = new Client({
        brokerURL: WS_URL,

        reconnectDelay: 5000,

        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        // вызывается перед КАЖДОЙ попыткой подключения, включая автореконнект.
        // Тут берём свежий токен и кладём его в заголовки CONNECT-фрейма
        beforeConnect: async () => {
            try {
                const token = await getFreshAccessToken();
                client.connectHeaders = {Authorization: `Bearer ${token}`};
            } catch (e) {
                if (e instanceof ApiError) {
                    void client.deactivate();
                }
            }
        },

        onConnect: () => {
            client.subscribe("/user/queue/messages", (frame) => {
                try {
                    onMessage(JSON.parse(frame.body) as MessageResponse);
                } catch (e) {
                    console.warn("Failed to parse the socket message", e);
                }
            });
        },

        onStompError: (frame) => {
            console.warn("STOMP error:", frame.headers["message"]);
        },
    });

    client.activate();

    return () => {
        void client.deactivate();
    };
}