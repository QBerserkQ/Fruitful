import type { User } from "../api/types.ts";
import { apiGetMyPosts } from "../api/posts.ts";
import { useFeed } from "../hooks/useFeed.ts";
import PostCard from "./posts/PostCard.tsx";
import PostForm from "./posts/PostForm.tsx";

interface ProfileCardProps {
    user: User;
}

export default function ProfileCard({ user }: ProfileCardProps) {
    const { posts, total, loading, error, hasNext, loadMore, prependPost } = useFeed(apiGetMyPosts);

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
                    <h1 className="text-2xl font-extrabold leading-tight">{user.username}</h1>
                    <p className="text-sm text-gray-500">{user.email}</p>
                </div>
            </div>

            <div className="flex flex-wrap gap-x-6 gap-y-2 border-b px-6 py-4 text-sm text-gray-500">
                <span><b className="mr-1 text-lg text-gray-900">{total}</b>Posts</span>
                <span><b className="mr-1 text-lg text-gray-900">0</b>Subscribers</span>
                <span><b className="mr-1 text-lg text-gray-900">0</b>Subscriptions</span>
            </div>

            <PostForm onCreated={prependPost} />

            <section className="flex-1 bg-gray-50 p-6">
                <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-gray-400">
                    Posts
                </h2>

                <div className="space-y-4">
                    {posts.map((post) => (
                        <PostCard key={post.id} post={post} />
                    ))}

                    {loading && posts.length === 0 &&
                        [0, 1, 2].map((i) => (
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

                {!loading && !error && posts.length === 0 && (
                    <p className="mt-6 text-center text-sm text-gray-400">No posts yet</p>
                )}

                {error && (
                    <p className="mt-6 text-center text-sm text-red-500">{error}</p>
                )}

                {hasNext && posts.length > 0 && (
                    <button
                        onClick={() => void loadMore()}
                        disabled={loading}
                        className="mx-auto mt-6 block rounded-full border bg-white px-5 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                    >
                        {loading ? "Loading..." : "Load more"}
                    </button>
                )}
            </section>
        </main>
    );
}