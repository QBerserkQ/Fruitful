import { useState } from "react";
import { apiCreatePost } from "../../api/posts.ts";
import type { PostResponse } from "../../api/types.ts";

const MAX_LENGTH = 5000;   // same limit as the backend

interface PostFormProps {
    onCreated: (post: PostResponse) => void;
}

export default function PostForm({ onCreated }: PostFormProps) {
    const [content, setContent] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const trimmed = content.trim();
    const canSubmit = trimmed.length > 0 && content.length <= MAX_LENGTH && !submitting;

    async function handleSubmit() {
        if (!canSubmit) return;
        setSubmitting(true);
        setError(null);
        try {
            const created = await apiCreatePost({ content: trimmed });
            onCreated(created);
            setContent("");   // clear only after success, so the text is not lost on error
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to create post");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div className="mb-6 rounded-2xl border bg-white p-4">
            <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="What's on your mind?"
                rows={3}
                className="w-full resize-none rounded-lg border p-3 text-gray-800 outline-none focus:border-purple-500"
            />
            <div className="mt-2 flex items-center justify-between">
                <span className={`text-xs ${content.length > MAX_LENGTH ? "text-red-500" : "text-gray-400"}`}>
                    {content.length} / {MAX_LENGTH}
                </span>
                <button
                    onClick={() => void handleSubmit()}
                    disabled={!canSubmit}
                    className="rounded-full bg-purple-800 px-5 py-2 text-sm font-medium text-white hover:bg-purple-700 disabled:opacity-50"
                >
                    {submitting ? "Posting..." : "Post"}
                </button>
            </div>
            {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
        </div>
    );
}