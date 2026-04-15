/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  corePlugins: {
    // Disable preflight to prevent overriding Angular Material's defaults
    preflight: false,
  },
  theme: {
    extend: {},
  },
  plugins: [],
}
