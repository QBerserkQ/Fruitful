import {useEffect, useState} from "react";

export function useDebounce<T>(value: T, delay = 300): T {
    const [debounced, setDebounced] = useState(value);
    useEffect(() => {
        const id = setTimeout(() => setDebounced(value), delay);
        return () => clearTimeout(id); // сброс таймера при каждом новом вводе
    }, [value, delay]);
    return debounced;
}