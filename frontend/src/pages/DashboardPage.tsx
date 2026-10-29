import Sidebar from "../components/Sidebar.tsx";
import ProfileCard from "../components/ProfileCard.tsx";
import RecommendationProfile from "../components/RecommendationsProfile.tsx";
import {useAuth} from "../context/AuthContext.tsx";
import FloatingFruits from "../components/FloatingFruits.tsx";

export const DashboardPage = ()=> {
    const { user } = useAuth();

    return (
        <>
            <FloatingFruits />
            <div className="relative z-10 mx-auto grid max-w-[1200px] gap-5 p-5 lg:grid-cols-[220px_minmax(0,1fr)_300px]">
                <Sidebar />
                <ProfileCard username={user?.username ?? "Guest"} />
                <RecommendationProfile />
            </div>
        </>
    );
}