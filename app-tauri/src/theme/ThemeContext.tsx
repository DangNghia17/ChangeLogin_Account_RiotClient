import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

export type ThemePreference = "light" | "dark" | "system";
export type ResolvedTheme = "light" | "dark";

interface ThemeValue {
  /** What the user selected (may be "system"). */
  theme: ThemePreference;
  /** The concrete theme actually applied right now. */
  resolved: ResolvedTheme;
  setTheme: (theme: ThemePreference) => void;
  /** Quick toggle between light and dark (turns "system" into an explicit choice). */
  toggle: () => void;
}

const ThemeContext = createContext<ThemeValue | null>(null);

const STORAGE_KEY = "ram.theme";

function readStored(): ThemePreference {
  const saved = localStorage.getItem(STORAGE_KEY);
  return saved === "light" || saved === "dark" || saved === "system" ? saved : "system";
}

function systemPrefersDark(): boolean {
  return typeof window !== "undefined" && window.matchMedia
    ? window.matchMedia("(prefers-color-scheme: dark)").matches
    : false;
}

function resolve(theme: ThemePreference): ResolvedTheme {
  if (theme === "system") return systemPrefersDark() ? "dark" : "light";
  return theme;
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemePreference>(readStored);
  const [resolved, setResolved] = useState<ResolvedTheme>(() => resolve(readStored()));

  // Apply the resolved theme to <html> so CSS variables switch instantly.
  useEffect(() => {
    const next = resolve(theme);
    setResolved(next);
    document.documentElement.setAttribute("data-theme", next);
  }, [theme]);

  // Follow OS changes only while "system" is selected.
  useEffect(() => {
    if (theme !== "system" || !window.matchMedia) return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => {
      const next: ResolvedTheme = mq.matches ? "dark" : "light";
      setResolved(next);
      document.documentElement.setAttribute("data-theme", next);
    };
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, [theme]);

  const setTheme = useCallback((next: ThemePreference) => {
    setThemeState(next);
    localStorage.setItem(STORAGE_KEY, next);
  }, []);

  const toggle = useCallback(() => {
    setThemeState((prev) => {
      const next: ThemePreference = resolve(prev) === "dark" ? "light" : "dark";
      localStorage.setItem(STORAGE_KEY, next);
      return next;
    });
  }, []);

  const value = useMemo<ThemeValue>(
    () => ({ theme, resolved, setTheme, toggle }),
    [theme, resolved, setTheme, toggle],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error("useTheme must be used within ThemeProvider");
  }
  return ctx;
}
