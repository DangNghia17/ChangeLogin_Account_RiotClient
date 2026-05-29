import { open } from "@tauri-apps/plugin-dialog";
import type { RiotStatus } from "../types";
import { useI18n } from "../i18n/I18nContext";
import { api } from "../lib/api";
import { useToast } from "./Toast";

interface Props {
  riotPath: string | null;
  status: RiotStatus | null;
  busy: boolean;
  onPathChanged: (path: string | null) => void;
  onLaunchOrFocus: () => void;
}

export function ConfigPanel({ riotPath, status, busy, onPathChanged, onLaunchOrFocus }: Props) {
  const { t } = useI18n();
  const toast = useToast();

  const browse = async () => {
    const selected = await open({
      multiple: false,
      directory: false,
      title: t("filechooser.riot.title"),
      filters: [{ name: "RiotClientServices.exe", extensions: ["exe"] }],
    });
    if (typeof selected === "string") {
      const valid = await api.validateRiotPath(selected);
      if (!valid) {
        toast.show(t("config.riot.path.invalid"), "error");
        return;
      }
      try {
        await api.setRiotPath(selected);
        onPathChanged(selected);
        toast.show(t("config.riot.path.saved"), "success");
      } catch (e) {
        toast.show(`${t("error.title")}: ${String(e)}`, "error");
      }
    }
  };

  const statusText = () => {
    if (!status) return t("config.riot.client.status.checking");
    if (status.running) {
      return status.window_visible
        ? t("config.riot.client.status.running")
        : t("config.riot.client.status.running.tray");
    }
    return t("config.riot.client.status.notRunning");
  };

  const fileName = riotPath ? riotPath.split(/[\\/]/).pop() : t("config.riot.path.notFound");
  const launchLabel = status?.running ? t("config.riot.client.focus") : t("config.riot.client.open");

  return (
    <div className="config-card">
      <div className="config-label">{t("config.riot.path")}</div>
      <div className="config-row">
        <input className="path-field" readOnly value={fileName ?? ""} title={riotPath ?? ""} />
        <button className="btn btn-gray" onClick={browse}>{t("config.riot.path.select")}</button>
        <button className="btn btn-primary" disabled={busy} onClick={onLaunchOrFocus}>
          {launchLabel}
        </button>
      </div>
      {riotPath && <div className="path-full" title={riotPath}>{riotPath}</div>}
      <div className={`status-line ${status?.running ? "ok" : "err"}`}>{statusText()}</div>
    </div>
  );
}
