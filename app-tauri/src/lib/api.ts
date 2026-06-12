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

  replaceAccounts(accounts: Account[]): Promise<Account[]> {
    return isTauri()
      ? invoke("replace_accounts", { accounts })
      : Promise.resolve(mockStore.replaceAccounts(accounts));
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

  /**
   * Saves text to a file the user chooses. On desktop this uses the native save
   * dialog + a Rust write command; in the web preview it falls back to a browser
   * download. Returns false if the user cancelled the save dialog.
   */
  async saveTextFile(defaultName: string, contents: string): Promise<boolean> {
    if (!isTauri()) {
      const blob = new Blob([contents], { type: "application/octet-stream" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = defaultName;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
      return true;
    }
    const { save } = await import("@tauri-apps/plugin-dialog");
    const path = await save({
      defaultPath: defaultName,
      filters: [{ name: "Backup", extensions: ["backup"] }],
    });
    if (typeof path !== "string") return false;
    await invoke("write_text_file", { path, contents });
    return true;
  },

  /**
   * Lets the user pick a text file and returns its contents. On desktop this uses
   * the native open dialog + a Rust read command; in the web preview it uses a
   * hidden file input. Returns null if the user cancelled.
   */
  async pickAndReadTextFile(): Promise<string | null> {
    if (!isTauri()) {
      return new Promise<string | null>((resolve) => {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".backup,.enc,.json,application/json,application/octet-stream";
        input.onchange = () => {
          const file = input.files?.[0];
          if (!file) {
            resolve(null);
            return;
          }
          const reader = new FileReader();
          reader.onload = () => resolve(String(reader.result ?? ""));
          reader.onerror = () => resolve(null);
          reader.readAsText(file);
        };
        input.oncancel = () => resolve(null);
        input.click();
      });
    }
    const { open } = await import("@tauri-apps/plugin-dialog");
    const selected = await open({
      multiple: false,
      directory: false,
      title: "Backup",
      filters: [{ name: "Backup", extensions: ["backup", "enc", "json"] }],
    });
    if (typeof selected !== "string") return null;
    return invoke<string>("read_text_file", { path: selected });
  },
};
