import type { Account, RiotStatus, Settings } from "../types";
import { isTauri } from "./platform";
import { mockStore } from "./mock-store";

async function invoke<T>(cmd: string, args: Record<string, unknown> = {}): Promise<T> {
  const { invoke: tauriInvoke } = await import("@tauri-apps/api/core");
  return tauriInvoke<T>(cmd, args);
}

export const api = {
  listAccounts(): Promise<Account[]> {
    return isTauri() ? invoke("list_accounts") : Promise.resolve(mockStore.listAccounts());
  },

  addAccount(account: Account): Promise<Account[]> {
    return isTauri()
      ? invoke("add_account", { account })
      : Promise.resolve(mockStore.addAccount(account));
  },

  updateAccount(index: number, account: Account): Promise<Account[]> {
    return isTauri()
      ? invoke("update_account", { index, account })
      : Promise.resolve(mockStore.updateAccount(index, account));
  },

  deleteAccount(index: number): Promise<Account[]> {
    return isTauri()
      ? invoke("delete_account", { index })
      : Promise.resolve(mockStore.deleteAccount(index));
  },

  riotStatus(): Promise<RiotStatus> {
    return isTauri() ? invoke("riot_status") : Promise.resolve(mockStore.riotStatus());
  },

  focusRiot(): Promise<boolean> {
    return isTauri() ? invoke("focus_riot") : Promise.resolve(mockStore.focusRiot());
  },

  launchRiot(): Promise<boolean> {
    return isTauri()
      ? invoke("launch_riot")
      : Promise.resolve(mockStore.launchRiot());
  },

  autoLogin(index: number): Promise<void> {
    return isTauri()
      ? invoke("auto_login", { index })
      : Promise.resolve(mockStore.autoLogin(index));
  },

  getRiotPath(): Promise<string | null> {
    return isTauri() ? invoke("get_riot_path") : Promise.resolve(mockStore.getRiotPath());
  },

  setRiotPath(path: string): Promise<void> {
    return isTauri()
      ? invoke("set_riot_path", { path })
      : Promise.resolve(mockStore.setRiotPath(path));
  },

  defaultRiotPath(): Promise<string | null> {
    return isTauri() ? invoke("default_riot_path") : Promise.resolve(mockStore.defaultRiotPath());
  },

  validateRiotPath(path: string): Promise<boolean> {
    return isTauri()
      ? invoke("validate_riot_path", { path })
      : Promise.resolve(mockStore.validateRiotPath(path));
  },

  async pickRiotPath(): Promise<string | null> {
    if (!isTauri()) return mockStore.pickRiotPath();

    const { open } = await import("@tauri-apps/plugin-dialog");
    const selected = await open({
      multiple: false,
      directory: false,
      title: "Riot Client",
      filters: [{ name: "RiotClientServices.exe", extensions: ["exe"] }],
    });
    return typeof selected === "string" ? selected : null;
  },

  getSettings(): Promise<Settings> {
    return isTauri() ? invoke("get_settings") : Promise.resolve(mockStore.getSettings());
  },

  setSettings(settings: Settings): Promise<Settings> {
    return isTauri()
      ? invoke("set_settings", { settings })
      : Promise.resolve(mockStore.setSettings(settings));
  },

  setLanguage(language: string): Promise<void> {
    return isTauri()
      ? invoke("set_language", { language })
      : Promise.resolve(mockStore.setLanguage(language));
  },
};
