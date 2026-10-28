interface ProfileCardProps {
    username: string;
}

export default function ProfileCard ({ username } : ProfileCardProps) {
    return (
        <main className="flex min-h-[calc(100dvh-2rem)] flex-col overflow-hidden rounded-2xl border bg-white">
            <div className="relative h-40 overflow-hidden bg-purple-900">
                <svg
                    className="absolute inset-0 h-full w-full"
                    viewBox="0 0 800 170"
                    preserveAspectRatio="xMidYMid slice"
                    aria-hidden="true"
                >
                    <circle cx="680" cy="40" r="120" fill="#8E5BA8" />
                    <circle cx="500" cy="170" r="90" fill="#8E5BA8" />
                    <circle cx="130" cy="30" r="60" fill="#8E5BA8" />
                </svg>
            </div>

            <div className="flex items-end gap-4 px-6">
                <div className="grid h-20 w-20 flex-none place-items-center rounded-3xl border-4 border-white bg-purple-800 text-3xl">
                    🫐
                </div>
                <div className="pb-1">
                    <h1 className="text-2xl font-extrabold leading-tight">{username}</h1>
                    <p className="text-sm text-gray-500">@{username}</p>
                </div>
            </div>

            <div className="flex flex-wrap gap-x-6 gap-y-2 border-b px-6 py-4 text-sm text-gray-500">
                <span><b className="mr-1 text-lg text-gray-900">128</b>Posts</span>
                <span><b className="mr-1 text-lg text-gray-900">342</b>Subscribers</span>
                <span><b className="mr-1 text-lg text-gray-900">96</b>Subscriptions</span>
            </div>

            <section className="flex-1 bg-gray-50 p-6">
                <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">
                    Posts
                </h2>

                <div className="space-y-4">
                    {[0, 1, 2].map((i) => (
                        <div key={i} className="animate-pulse rounded-2xl border bg-white p-4">
                            <div className="flex items-center gap-3">
                                <div className="h-10 w-10 rounded-full bg-gray-200" />
                                <div className="h-3 w-32 rounded bg-gray-200" />
                            </div>
                            <div className="mt-4 h-3 w-full rounded bg-gray-200" />
                            <div className="mt-2 h-3 w-2/3 rounded bg-gray-200" />
                        </div>
                    ))}
                </div>

                <p className="mt-6 text-center text-sm text-gray-400">Posts are coming soon</p>
            </section>
        </main>
    );
}