/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        oled: '#000000',
        surface01: '#050505',
        surface02: '#090909',
        surface03: '#121212',
        surfaceSelected: '#1c1c1c',
        cardBorder: 'rgba(255, 255, 255, 0.08)',
        hairline: 'rgba(255, 255, 255, 0.12)',
        white92: 'rgba(255, 255, 255, 0.92)',
        white80: 'rgba(255, 255, 255, 0.80)',
        white64: 'rgba(255, 255, 255, 0.64)',
        white48: 'rgba(255, 255, 255, 0.48)',
        white20: 'rgba(255, 255, 255, 0.20)',
        white14: 'rgba(255, 255, 255, 0.14)',
        white08: 'rgba(255, 255, 255, 0.08)',
        accentGreen: '#22c55e',
        accentAmber: '#f59e0b',
        accentRed: '#ef4444',
        accentBlue: '#3b82f6',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Cascadia Code', 'Consolas', 'monospace'],
      },
      borderRadius: {
        'r8': '8px',
        'r10': '10px',
        'r12': '12px',
        'r14': '14px',
        'r16': '16px',
        'r18': '18px',
        'r20': '20px',
        'r22': '22px',
        'r24': '24px',
        'r32': '32px',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'orbit': 'spin 8s linear infinite',
      }
    },
  },
  plugins: [],
}
