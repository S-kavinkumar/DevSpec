/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        dark: {
          900: '#030712', // Deep pitch charcoal
          800: '#0f172a', // Slate card backgrounds
          700: '#1e293b', // Borders
          600: '#334155', // Button hover / secondary elements
          500: '#64748b', // Text sub title
        },
        cyan: {
          400: '#22d3ee', // Neon Cyan glow primary
          500: '#06b6d4',
          600: '#0891b2',
        },
        emerald: {
          400: '#34d399', // Clean success/pass emerald
          500: '#10b981',
        },
        rose: {
          500: '#f43f5e', // Warning critical rose
        },
        amber: {
          500: '#f59e0b', // Warning amber
        }
      }
    },
  },
  plugins: [],
}
