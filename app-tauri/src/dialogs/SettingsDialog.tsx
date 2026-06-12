import { useEffect, useState } from "react";
import type { Settings } from "../types";
import { useI18n } from "../i18n/I18nContext";
import { useTheme, type ThemePreference } from "../theme/ThemeContext";
import { api } from "../lib/api";
import { useToast } from "../components/Toast";

interface Props {
  onClose: () => void;
  onExport: () => void;
  onImport: () => void;
}

export function SettingsDialog({ onClose, onExport, onImport }: Props) {
  const { t } = useI18n();
  const { theme, setTheme } = useTheme();
  const toast = useToast();
  const [settings, setSettings] = useState<Settings | null>(null);

  const themeOptions: ThemePreference[] = ["light", "dark", "system"];

  useEffect(() => {
    api.getSettings().then(setSettings).catch(() => setSettings({
      run_at_startup: false,
      auto_click_login: true,
      language: "vi",
    }));
  }, []);

  const save = async () => {
    if (!settings) return;
    try {
      const applied = await api.setSettings(settings);
      onClose();
      if (settings.run_at_startup && !applied.run_at_startup) {
        toast.show(t("settings.startup.failed"), "warning");
      } else {
        toast.show(t("settings.saved"), "success");
      }
    } catch (e) {
      toast.show(`${t("error.title")}: ${String(e)}`, "error");
    }
  };

  if (!settings) return null;

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{t("settings.title")}</h2>

        <label className="setting-row">
          <input
            type="checkbox"
            checked={settings.run_at_startup}
            onChange={(e) => setSettings({ ...settings, run_at_startup: e.target.checked })}
          />
          <span>
            <strong>{t("settings.runAtStartup")}</strong>
            <small>{t("settings.runAtStartup.desc")}</small>
          </span>
        </label>

        <label className="setting-row">
          <input
            type="checkbox"
            checked={settings.auto_click_login}
            onChange={(e) => setSettings({ ...settings, auto_click_login: e.target.checked })}
          />
          <span>
            <strong>{t("settings.autoClickLogin")}</strong>
            <small>{t("settings.autoClickLogin.desc")}</small>
          </span>
        </label>

        <div className="setting-block">
          <div className="setting-block-title">{t("theme.title")}</div>
          <div className="setting-block-desc">{t("theme.desc")}</div>
          <div className="theme-options" role="group" aria-label={t("theme.title")}>
            {themeOptions.map((opt) => (
              <button
                key={opt}
                type="button"
                className={`theme-option ${theme === opt ? "active" : ""}`}
                aria-pressed={theme === opt}
                onClick={() => setTheme(opt)}
              >
                {t(`theme.${opt}`)}
              </button>
            ))}
          </div>
        </div>

        <div className="setting-block">
          <div className="setting-block-title">{t("backup.section.title")}</div>
          <div className="backup-actions">
            <button className="btn btn-gray" onClick={onExport}>{t("backup.export")}</button>
            <button className="btn btn-gray" onClick={onImport}>{t("backup.import")}</button>
          </div>
          <div className="setting-block-desc">{t("backup.export.desc")}</div>
        </div>

        <div className="modal-actions">
          <button className="btn btn-gray" onClick={onClose}>{t("account.cancel")}</button>
          <button className="btn btn-primary" onClick={save}>{t("account.save")}</button>
        </div>
      </div>
    </div>
  );
}
