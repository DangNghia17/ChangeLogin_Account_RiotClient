import { useEffect, useState } from "react";
import type { Settings } from "../types";
import { useI18n } from "../i18n/I18nContext";
import { api } from "../lib/api";
import { useToast } from "../components/Toast";

interface Props {
  onClose: () => void;
}

export function SettingsDialog({ onClose }: Props) {
  const { t } = useI18n();
  const toast = useToast();
  const [settings, setSettings] = useState<Settings | null>(null);

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

        <div className="modal-actions">
          <button className="btn btn-gray" onClick={onClose}>{t("account.cancel")}</button>
          <button className="btn btn-primary" onClick={save}>{t("account.save")}</button>
        </div>
      </div>
    </div>
  );
}
