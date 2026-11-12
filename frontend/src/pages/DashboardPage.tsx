import ProfileCard from "../components/ProfileCard.tsx";
import {useCurrentUser} from "../context/AuthContext.tsx";

export const DashboardPage = ()=> {
    const user = useCurrentUser();
    return <ProfileCard user={user} />;
}