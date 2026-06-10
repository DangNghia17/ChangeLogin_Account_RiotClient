import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import type { Lang } from "../types";
import { STRINGS } from "./strings";
import { api } from "../lib/api";

interface I18nValue {
  lang: Lang;
  t: (key: string) => string;
  setLang: (lang: Lang) => void;
}

const I18nContext = createContext<I18nValue | null>(null);

const STORAGE_KEY = "ram.language";

function initialLang(): Lang {
  const saved = localStorage.getItem(STORAGE_KEY);
  return saved === "en" ? "en" : "vi";
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(initialLang);

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    localStorage.setItem(STORAGE_KEY, next);
    // Persist in backend config.json too (best effort).
    void api.setLanguage(next).catch(() => undefined);
  }, []);

  const t = useCallback(
    (key: string) => STRINGS[lang][key] ?? STRINGS.vi[key] ?? key,
    [lang],
  );

  const value = useMemo<I18nValue>(() => ({ lang, t, setLang }), [lang, t, setLang]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nValue {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error("useI18n must be used within I18nProvider");
  }
  return ctx;
}
