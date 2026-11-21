import { useCallback, useEffect, useRef, useState } from "react";
import type { PageResponse, PostResponse } from "../api/types.ts";

const PAGE_SIZE = 20;

// must be a stable reference (a module-level function like apiGetMyPosts is fine)
type FetchPage = (page: number, size: number) => Promise<PageResponse<PostResponse>>;

export function useFeed(fetchPage: FetchPage) {
    const [posts, setPosts] = useState<PostResponse[]>([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [hasNext, setHasNext] = useState(true);

    // refs, not state: they must be readable immediately, even between renders
    const nextPage = useRef(0);
    const busy = useRef(false);
    const hasNextRef = useRef(true);

    const loadMore = useCallback(async () => {
        if (busy.current || !hasNextRef.current) return;   // ignore double clicks
        busy.current = true;
        setLoading(true);
        setError(null);
        try {
            const data = await fetchPage(nextPage.current, PAGE_SIZE);
            setPosts(prev => {
                // offsets shift when new posts appear, so skip posts we already have
                const known = new Set(prev.map(p => p.id));
                return [...prev, ...data.content.filter(p => !known.has(p.id))];
            });
            setTotal(data.totalElements);
            hasNextRef.current = data.hasNext;
            setHasNext(data.hasNext);
            nextPage.current += 1;
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to load posts");
        } finally {
            busy.current = false;
            setLoading(false);
        }
    }, [fetchPage]);

    useEffect(() => {
        void loadMore();   // first page on mount
    }, [loadMore]);

    const prependPost = useCallback((post: PostResponse) => {
        setPosts(prev => [post, ...prev]);
        setTotal(prev => prev + 1);
    }, []);

    return { posts, total, loading, error, hasNext, loadMore, prependPost };
}