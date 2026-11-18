import { Outlet, useLocation } from "react-router-dom";
import Sidebar from "./Sidebar.tsx";
import FloatingFruits from "./FloatingFruits.tsx";
import RecommendationsProfile from "./RecommendationsProfile.tsx"
import SettingsMenu from "./SettingsMenu.tsx";

export default function AppLayout() {
    const { pathname } = useLocation();
    const inSettings = pathname.startsWith("/settings");
    const inChats = pathname.startsWith("/chats");
    const phoneChatFit = inChats
        ? "h-dvh grid-rows-[auto_minmax(0,1fr)] lg:h-auto lg:grid-rows-none"
        : "";

    return (
        <>
            <FloatingFruits />
            <div className={`relative z-10 mx-auto grid max-w-300 grid-cols-1 gap-5 p-5 lg:grid-cols-[220px_minmax(0,1fr)_300px] ${phoneChatFit}`}>
                <Sidebar />
                <main className={`min-h-0 min-w-0 ${inSettings ? "order-last lg:order-none" : ""}`}>
                    <Outlet />
                </main>
                {inSettings ? (
                    <SettingsMenu />
                ) : inChats ? (
                    <div className="hidden lg:block">
                        <RecommendationsProfile />
                    </div>
                ) : (
                    <RecommendationsProfile />
                )}
            </div>
        </>
    );
}