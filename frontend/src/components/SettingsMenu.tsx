import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext.tsx";

const categories = [
    { to: "/settings/account", label: "Account" },
    { to: "/settings/sessions", label: "Sessions" },
];

export default function SettingsMenu() {
    const { logout } = useAuth();

    return (
        <aside className="min-w-0 rounded-2xl bg-white p-4 shadow lg:p-5">
            <div className="mb-3 flex items-center justify-between">
                <h2 className="text-lg font-bold">Settings</h2>
                <button
                    onClick={logout}
                    className="rounded-md bg-red-500 px-3 py-1 text-xs font-medium text-white"
                >
                    Log out
                </button>
            </div>

            <nav className="flex flex-row gap-2 overflow-x-auto pb-1 scrollbar-thin lg:flex-col lg:overflow-visible lg:pb-0">
                {categories.map((c) => (
                    <NavLink
                        key={c.to}
                        to={c.to}
                        className={({ isActive }) =>
                            `shrink-0 whitespace-nowrap rounded-lg px-4 py-2 text-sm font-medium ${
                                isActive ? "bg-green-600 text-white" : "bg-gray-100 text-gray-700"
                            }`
                        }
                    >
                        {c.label}
                    </NavLink>
                ))}
            </nav>
        </aside>
    );
}