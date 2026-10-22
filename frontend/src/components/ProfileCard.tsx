interface ProfileCardProps {
    username: string;
}

export default function ProfileCard ({ username } : ProfileCardProps) {
    return (
        <main className="overflow-hidden rounded-2xl border bg-white">
            <div className="relative h-40 overflow-hidden bg-purple-900">
                <svg
                    className="absolute inset-0 h-full w-full"
                    viewBox="0 0 800 170"
                    preserveAspectRatio="xMidYMid slice"
                    aria-hidden="true"
                >
                    <circle cx="680" cy="40" r="120" fill="#FF9E7A" />
                    <circle cx="500" cy="170" r="90" fill="#C8D96F" />
                    <circle cx="130" cy="30" r="60" fill="#8E5BA8" />
                </svg>
            </div>

            <div className="flex items-end gap-4 px-6">
                <div className="grid h-20 w-20 flex-none place-items-center rounded-3xl border-4 border-white bg-lime-300 text-3xl">
                    🍑
                </div>
                <div className="pb-1">
                    <h1 className="text-2xl font-extrabold leading-tight">{username}</h1>
                    <p className="text-sm text-gray-500">@{username}</p>
                </div>
            </div>

            <div className="flex gap-6 border-b px-6 py-4 text-sm text-gray-500">
                <span><b className="mr-1 text-lg text-gray-900">128</b>Posts</span>
                <span><b className="mr-1 text-lg text-gray-900">342</b>Subscribers</span>
                <span><b className="mr-1 text-lg text-gray-900">96</b>Subscriptions</span>
            </div>
        </main>
    );
}