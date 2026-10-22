import { useState } from "react";
import {NavLink} from "react-router-dom";
import {Home, Menu, MessageCircle, Newspaper, Settings, X} from "lucide-react";


const navItems = [
    { to: "/feed", label: "Feed", icon:  Newspaper},
    { to: "/dashboard", label: "Home", icon: Home },
    { to: "/messages", label: "Messages", icon: MessageCircle },
    { to: "/settings", label: "Settings", icon: Settings },
];

export default function Sidebar () {
    const [open, setOpen] = useState(false);

    return (
        <aside className="rounded-2xl border bg-white p-3">
            <div className="flex items-center justify-between px-3 pb-2 pt-2 lg:pb-4">
                <span className="text-xl font-extrabold">Fruitful</span>
                <button
                    onClick={() => setOpen((prev) => !prev)}
                    aria-expanded={open}
                    aria-controls="main-nav"
                    aria-label={open ? "Close menu" : "Open menu"}
                    className="rounded-xl p-2 hover:bg-gray-100 lg:hidden"
                >
                    {open ? <X size={22} /> : <Menu size={22} />}
                </button>
            </div>

            <nav
                id="main-nav"
                className={`${open ? "flex" : "hidden"} flex-col gap-1 lg:flex`}
            >
                {navItems.map((item) => (
                    <NavLink
                        key={item.to}
                        to={item.to}
                        onClick={() => setOpen(false)}
                        className={({ isActive }) =>
                            `flex items-center gap-3 rounded-xl px-3 py-2.5 font-semibold ${
                                isActive ? "bg-purple-900 text-white" : "text-gray-500 hover:bg-gray-100"
                            }`
                        }
                    >
                        <item.icon size={20} />
                        {item.label}
                    </NavLink>
                ))}
            </nav>
        </aside>
    );
}