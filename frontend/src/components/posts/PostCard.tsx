import { useState } from "react";
import type { PostResponse } from "../../api/types.ts";

const PREVIEW_LENGTH = 500;

interface PostCardProps {
    post: PostResponse;
}

export default function PostCard({ post }: PostCardProps) {
    const [expanded, setExpanded] = useState(false);

    // Array.from splits by real characters, so an emoji is never cut in half
    const chars = Array.from(post.content);
    const isLong = chars.length > PREVIEW_LENGTH;
    const visibleText = isLong && !expanded
        ? chars.slice(0, PREVIEW_LENGTH).join("").trimEnd() + "…"
        : post.content;

    return (
        <article className="rounded-2xl border bg-white p-4">
            <header className="flex items-center gap-3">
                <div className="grid h-10 w-10 place-items-center rounded-full bg-purple-800 text-lg">
                    🫐
                </div>
                <div>
                    <p className="text-sm font-semibold text-gray-900">{post.author.username}</p>
                    <time className="text-xs text-gray-400" dateTime={post.createdAt}>
                        {new Date(post.createdAt).toLocaleString()}
                    </time>
                </div>
            </header>

            <p className="mt-3 whitespace-pre-wrap break-words text-gray-800">{visibleText}</p>

            {isLong && (
                <button
                    onClick={() => setExpanded((prev) => !prev)}
                    className="mt-2 text-sm font-medium text-purple-700 hover:underline"
                >
                    {expanded ? "See less" : "See more"}
                </button>
            )}
        </article>
    );
}