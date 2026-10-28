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
        <aside className="lg:self-start lg:sticky lg:top-4 lg:rounded-2xl lg:border lg:bg-white lg:p-3">
            <button
                onClick={() => setOpen((prev) => !prev)}
                aria-expanded={open}
                aria-controls="main-nav"
                aria-label={open ? "Close menu" : "Open menu"}
                className="fixed left-4 top-4 z-50 rounded-xl border bg-white p-2 shadow-sm hover:bg-gray-100 lg:hidden"
            >
                {open ? <X size={22} /> : <Menu size={22} />}
            </button>
            <div
                onClick={() => setOpen(false)}
                className={`fixed inset-0 z-30 bg-black/50 transition-opacity duration-300 lg:hidden ${
                    open ? "opacity-100" : "pointer-events-none opacity-0"
                }`}
            />
            <div
                className={`fixed inset-y-0 left-0 z-40 w-64 border-r bg-white p-3 pt-16 transition-transform duration-300 ${
                    open ? "translate-x-0" : "-translate-x-full"
                } lg:static lg:z-auto lg:w-auto lg:translate-x-0 lg:border-0 lg:p-0`}
            >
            <span className="block px-3 pb-2 pt-2 text-xl font-extrabold lg:pb-4">
                Fruitful
            </span>

                <nav id="main-nav" className="flex flex-col gap-1">
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
            </div>
        </aside>
    );
}