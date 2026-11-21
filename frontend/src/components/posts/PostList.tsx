import type { PostResponse } from "../../api/types.ts";
import PostCard from "./PostCard.tsx";

interface PostListProps {
    posts: PostResponse[];
    loading: boolean;
    error: string | null;
    hasNext: boolean;
    onLoadMore: () => void;
    emptyText?: string;
}

export default function PostList({
                                     posts, loading, error, hasNext, onLoadMore, emptyText = "No posts yet",
                                 }: PostListProps) {
    return (
        <>
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
                <p className="mt-6 text-center text-sm text-gray-400">{emptyText}</p>
            )}

            {error && <p className="mt-6 text-center text-sm text-red-500">{error}</p>}

            {hasNext && posts.length > 0 && (
                <button
                    onClick={onLoadMore}
                    disabled={loading}
                    className="mx-auto mt-6 block rounded-full border bg-white px-5 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                >
                    {loading ? "Loading..." : "Load more"}
                </button>
            )}
        </>
    );
}