import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  // На GitHub Pages приложение живёт не в корне, а по пути /<repo>/ —
  // base задаётся через переменную окружения в workflow деплоя (см.
  // .github/workflows/deploy-pages.yml), локально остаётся "/".
  base: process.env.VITE_BASE_PATH || "/",
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
