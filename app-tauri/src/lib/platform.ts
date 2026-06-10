/** True when running inside a Tauri WebView (not plain browser preview). */
export function isTauri(): boolean {
  if (typeof window === "undefined") return false;
  return "__TAURI_INTERNALS__" in window || "__TAURI__" in window;
}

/** True when Vite dev/preview serves the UI without Tauri shell. */
export function isWebPreview(): boolean {
  return !isTauri();
}
