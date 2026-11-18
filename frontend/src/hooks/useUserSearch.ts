import {useEffect, useState} from "react";
import {searchUsers} from "../api/users.ts";
import type {UserSummaryResponse} from "../api/types.ts";
import {useDebounce} from "./useDebounce.ts";

export const MIN_QUERY_LENGTH = 3;

export function useUserSearch(query: string) {
    const trimmed = query.trim();
    const debounced = useDebounce(trimmed);

    // поиск активен, только если и текущий текст, и "устоявшийся" достаточно длинные
    const active = trimmed.length >= MIN_QUERY_LENGTH && debounced.length >= MIN_QUERY_LENGTH;

    const [results, setResults] = useState<UserSummaryResponse[]>([]);
    const [searchedQuery, setSearchedQuery] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!active) return;

        let cancelled = false;
        setLoading(true);
        setError(null);

        searchUsers(debounced)
            .then((res) => {
                if (cancelled) return;
                setResults(res);
                setSearchedQuery(debounced);
            })
            .catch(() => {
                if (!cancelled) setError("Failed when search");
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [debounced, active]);

    const showEmpty = active && !loading && !error && searchedQuery === debounced && results.length === 0;

    return {active, results, loading, error, showEmpty};
}