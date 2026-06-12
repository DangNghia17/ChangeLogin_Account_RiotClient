import type { Account, RiotStatus, Settings } from "../types";

const ACCOUNTS_KEY = "ram.mock.accounts";
const CONFIG_KEY = "ram.mock.config";

interface MockConfig {
  riotClientPath: string | null;
  settings: Settings;
}

function readConfig(): MockConfig {
  try {
    const raw = localStorage.getItem(CONFIG_KEY);
    if (raw) return JSON.parse(raw) as MockConfig;
  } catch {
    /* ignore */
  }
  return {
    riotClientPath: null,
    settings: {
      run_at_startup: false,
      auto_click_login: true,
      language: "vi",
    },
  };
}

function writeConfig(config: MockConfig): void {
  localStorage.setItem(CONFIG_KEY, JSON.stringify(config));
}

function readAccounts(): Account[] {
  try {
    const raw = localStorage.getItem(ACCOUNTS_KEY);
    if (raw) return JSON.parse(raw) as Account[];
  } catch {
    /* ignore */
  }
  return [];
}

function writeAccounts(accounts: Account[]): void {
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
}

export const mockStore = {
  listAccounts(): Account[] {
    return readAccounts();
  },

  addAccount(account: Account): Account[] {
    const accounts = readAccounts();
    accounts.push(account);
    writeAccounts(accounts);
    return accounts;
  },

  updateAccount(index: number, account: Account): Account[] {
    const accounts = readAccounts();
    if (index >= accounts.length) throw new Error("account index out of range");
    accounts[index] = account;
    writeAccounts(accounts);
    return accounts;
  },

  deleteAccount(index: number): Account[] {
    const accounts = readAccounts();
    if (index >= accounts.length) throw new Error("account index out of range");
    accounts.splice(index, 1);
    writeAccounts(accounts);
    return accounts;
  },

  replaceAccounts(accounts: Account[]): Account[] {
    writeAccounts(accounts);
    return accounts;
  },

  riotStatus(): RiotStatus {
    return { running: false, window_visible: false };
  },

  focusRiot(): boolean {
    console.info("[web preview] focusRiot() — no-op");
    return false;
  },

  launchRiot(): boolean {
    const path = readConfig().riotClientPath;
    if (!path) throw new Error("Riot Client path is not configured");
    console.info("[web preview] launchRiot()", path);
    return true;
  },

  autoLogin(index: number): void {
    const accounts = readAccounts();
    const account = accounts[index];
    if (!account) throw new Error("account index out of range");
    console.info("[web preview] autoLogin()", account.username);
  },

  getRiotPath(): string | null {
    return readConfig().riotClientPath;
  },

  setRiotPath(path: string): void {
    if (!mockStore.validateRiotPath(path)) {
      throw new Error("invalid path: must point to RiotClientServices.exe");
    }
    const config = readConfig();
    config.riotClientPath = path;
    writeConfig(config);
  },

  defaultRiotPath(): string | null {
    return null;
  },

  validateRiotPath(path: string): boolean {
    const base = path.replace(/\\/g, "/").split("/").pop()?.toLowerCase() ?? "";
    return base === "riotclientservices.exe";
  },

  async pickRiotPath(): Promise<string | null> {
    const value = window.prompt(
      "Web preview: nhập đường dẫn RiotClientServices.exe\n(Enter path to RiotClientServices.exe)",
      readConfig().riotClientPath ?? "C:\\Riot Games\\Riot Client\\RiotClientServices.exe",
    );
    return value?.trim() || null;
  },

  getSettings(): Settings {
    return { ...readConfig().settings };
  },

  setSettings(settings: Settings): Settings {
    const config = readConfig();
    config.settings = { ...settings, run_at_startup: false };
    writeConfig(config);
    console.info("[web preview] startup registry skipped");
    return config.settings;
  },

  setLanguage(language: string): void {
    const config = readConfig();
    config.settings.language = language;
    writeConfig(config);
  },
};
