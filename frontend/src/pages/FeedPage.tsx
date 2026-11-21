import { apiGetFeed } from "../api/posts.ts";
import { useFeed } from "../hooks/useFeed.ts";
import PostForm from "../components/posts/PostForm.tsx";
import PostList from "../components/posts/PostList.tsx";

export default function FeedPage() {
    const { posts, loading, error, hasNext, loadMore, prependPost } = useFeed(apiGetFeed);

    return (
        <main className="mx-auto max-w-2xl p-4">
            <h1 className="mb-4 text-2xl font-extrabold">Feed</h1>

            <PostForm onCreated={prependPost} />

            <PostList
                posts={posts}
                loading={loading}
                error={error}
                hasNext={hasNext}
                onLoadMore={() => void loadMore()}
                emptyText="No posts yet. Be the first to write one!"
            />
        </main>
    );
}