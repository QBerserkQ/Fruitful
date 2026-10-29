import { useEffect, useState } from 'react';

const fruits = [
    { src: '/blueberry.png',  className: '-left-8 top-[15%] w-28 rotate-12',        from: '-translate-x-[200%]' },
    { src: '/fig.png',       className: '-left-10 bottom-[22%] w-32 -rotate-6',    from: '-translate-x-[200%]' },
    { src: '/grape.png',      className: '-top-8 left-[35%] w-32 -rotate-12',       from: '-translate-y-[200%]' },
    { src: '/blackberry.png', className: '-top-6 right-[28%] w-28 rotate-12',       from: '-translate-y-[200%]' },
    { src: '/plum.png',        className: '-right-10 top-[30%] w-36 rotate-6',       from: 'translate-x-[200%]' },
    { src: '/blueberry.png',  className: '-right-8 bottom-[15%] w-28 -rotate-12',   from: 'translate-x-[200%]' },
    { src: '/plum.png',       className: '-bottom-8 left-[14%] w-32 rotate-45',     from: 'translate-y-[200%]' },
    { src: '/blackberry.png', className: '-bottom-10 right-[22%] w-36 -rotate-12',  from: 'translate-y-[200%]' },
];

export default function FloatingFruits() {
    const [shown, setShown] = useState(false);

    useEffect(() => {
        const id = setTimeout(() => setShown(true), 100);
        return () => clearTimeout(id);
    }, []);

    return (
        <div
            className="pointer-events-none fixed inset-0 z-0 hidden overflow-hidden lg:block"
            aria-hidden="true"
        >
            {fruits.map((fruit, i) => (
                <img
                    key={i}
                    src={fruit.src}
                    alt=""
                    className={`absolute select-none transition-all duration-700 ease-out motion-reduce:transition-none ${fruit.className} ${
                        shown ? 'translate-x-0 translate-y-0 opacity-100' : `${fruit.from} opacity-0`
                    }`}
                />
            ))}
        </div>
    );
}