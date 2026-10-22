export const WavePattern = () => (
    <svg
        className="absolute inset-0 h-full w-full"
        viewBox="0 0 800 800"
        preserveAspectRatio="xMidYMid slice"
        aria-hidden="true"
    >
        {/* Кремовый фон */}
        <rect width="800" height="800" fill="#FBF3DC" />

        {/* Оранжевые волны */}
        <g fill="none" stroke="#F97F0B" strokeLinecap="round">
            <path strokeWidth="70" d="M-50 100 C100 0, 200 200, 350 120 S600 60, 850 150" />
            <path strokeWidth="90" d="M-50 300 C120 220, 220 420, 400 320 S650 250, 850 330" />
            <path strokeWidth="60" d="M-50 520 C100 440, 250 640, 420 520 S680 450, 850 540" />
            <path strokeWidth="80" d="M-50 740 C120 660, 260 860, 450 740 S700 680, 850 760" />
        </g>
    </svg>
);