import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Tauri expects a fixed dev server port and ignores src-tauri changes.
// `npm run web` / `npm run dev` serves browser preview with mock native APIs.
export default defineConfig({
  plugins: [react()],
  clearScreen: false,
  server: {
    host: true,
    port: 1420,
    strictPort: true,
    watch: {
      ignored: ["**/src-tauri/**"],
    },
  },
  build: {
    target: "es2021",
    outDir: "dist",
  },
});
