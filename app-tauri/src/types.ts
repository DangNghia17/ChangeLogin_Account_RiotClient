export interface Account {
  username: string;
  password: string;
  region: string;
  note: string;
}

export interface RiotStatus {
  running: boolean;
  window_visible: boolean;
}

export interface Settings {
  run_at_startup: boolean;
  auto_click_login: boolean;
  language: string;
}

export const REGIONS = [
  "VN", "NA", "EUW", "EUNE", "KR", "JP", "BR", "LAN", "LAS", "OCE", "RU", "TR",
] as const;

export type Lang = "vi" | "en";
