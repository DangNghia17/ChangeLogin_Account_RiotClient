import { useCallback, useEffect, useRef, useState } from "react";
import type { Account, RiotStatus } from "./types";
import { api } from "./lib/api";
import { useI18n } from "./i18n/I18nContext";
import { useToast } from "./components/Toast";
import { AccountTable } from "./components/AccountTable";
import { ConfigPanel } from "./components/ConfigPanel";
import { ConfirmDialog } from "./components/ConfirmDialog";
import { AccountDialog } from "./dialogs/AccountDialog";
import { SettingsDialog } from "./dialogs/SettingsDialog";
import { AboutDialog } from "./dialogs/AboutDialog";
import { WelcomeDialog } from "./dialogs/WelcomeDialog";
import { LoginStatusDialog } from "./dialogs/LoginStatusDialog";

import addIcon from "./assets/add_account.png";
import editIcon from "./assets/edit_account.png";
import deleteIcon from "./assets/delete_account.png";
import loginIcon from "./assets/login__account.png";
import infoIcon from "./assets/information-button.png";
import appIcon from "./assets/change-user-icon.jpg";

const WELCOME_KEY = "ram.welcome_shown";

export default function App() {
  const { t, lang, setLang } = useI18n();
  const toast = useToast();

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selected, setSelected] = useState(-1);
  const [status, setStatus] = useState<RiotStatus | null>(null);
  const [riotPath, setRiotPath] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [editing, setEditing] = useState<{ account: Account | null; index: number } | null>(null);
  const [pendingDelete, setPendingDelete] = useState<{ index: number; account: Account } | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [showAbout, setShowAbout] = useState(false);
  const [showWelcome, setShowWelcome] = useState(
    () => localStorage.getItem(WELCOME_KEY) !== "true",
  );
  // Username of the account whose credentials were last sent to the Riot Client.
  // Used purely on the client side to reflect the active login session in the UI.
  const [session, setSession] = useState<string | null>(null);
  const [showLoginStatus, setShowLoginStatus] = useState(false);

  const loginInFlight = useRef(false);

  const reloadAccounts = useCallback(() => {
    api.listAccounts().then(setAccounts).catch((e) => toast.show(String(e), "error"));
  }, [toast]);

  useEffect(() => {
    reloadAccounts();
    api.getRiotPath().then(setRiotPath).catch(() => undefined);
  }, [reloadAccounts]);

  // Poll Riot Client status every 3s (matches Java behaviour).
  useEffect(() => {
    let active = true;
    const tick = () => {
      api.riotStatus().then((s) => active && setStatus(s)).catch(() => undefined);
    };
    tick();
    const id = window.setInterval(tick, 3000);
    return () => {
      active = false;
      window.clearInterval(id);
    };
  }, []);

  const onSaveAccount = async (account: Account) => {
    try {
      if (editing?.account) {
        await api.updateAccount(editing.index, account);
        // Keep the active session badge in sync if its username changed.
        if (session && editing.account.username === session) setSession(account.username);
      } else {
        await api.addAccount(account);
      }
      setEditing(null);
      reloadAccounts();
    } catch (e) {
      toast.show(`${t("error.title")}: ${String(e)}`, "error");
    }
  };

  const requestDelete = () => {
    if (selected < 0 || selected >= accounts.length) {
      toast.show(t("account.select.toDelete"), "warning");
      return;
    }
    setPendingDelete({ index: selected, account: accounts[selected] });
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    const { index, account } = pendingDelete;
    try {
      await api.deleteAccount(index);
      if (session && account.username === session) setSession(null);
      setSelected(-1);
      setPendingDelete(null);
      reloadAccounts();
    } catch (e) {
      toast.show(`${t("error.title")}: ${String(e)}`, "error");
    }
  };

  const onEdit = () => {
    if (selected < 0 || selected >= accounts.length) {
      toast.show(t("account.select.toEdit"), "warning");
      return;
    }
    setEditing({ account: accounts[selected], index: selected });
  };

  const performLogin = async (index: number) => {
    if (loginInFlight.current) return;
    if (index < 0) {
      toast.show(t("login.select.account"), "warning");
      return;
    }
    const account = accounts[index];
    // Already signed in with this account: show status/logout instead of re-running login.
    if (account && session && account.username === session) {
      setShowLoginStatus(true);
      return;
    }
    loginInFlight.current = true;
    setBusy(true);
    try {
      const s = await api.riotStatus();
      if (!s.running) {
        toast.show(t("login.client.notRunning"), "warning");
        return;
      }
      if (!s.window_visible) {
        await api.focusRiot().catch(() => undefined);
      }
      await api.autoLogin(index);
      const submitted = (await api.getSettings()).auto_click_login;
      if (account) setSession(account.username);
      toast.show(submitted ? t("login.success.submitted") : t("login.success"), "success");
    } catch (e) {
      toast.show(t("login.failed.details") + " (" + String(e) + ")", "error");
    } finally {
      loginInFlight.current = false;
      setBusy(false);
    }
  };

  const onLogout = () => {
    setSession(null);
    setShowLoginStatus(false);
    toast.show(t("login.logout.success"), "success");
  };

  const launchOrFocus = async () => {
    setBusy(true);
    try {
      const s = await api.riotStatus();
      if (s.running && s.window_visible) {
        const ok = await api.focusRiot();
        toast.show(ok ? t("config.riot.client.focused") : t("config.riot.client.focus.failed"),
          ok ? "success" : "error");
      } else {
        if (!riotPath) {
          toast.show(t("config.riot.client.path.required"), "warning");
          return;
        }
        const ok = await api.launchRiot();
        toast.show(ok ? t("config.riot.client.opened") : t("config.riot.client.launch.failed"),
          ok ? "success" : "error");
      }
    } catch (e) {
      toast.show(`${t("error.title")}: ${String(e)}`, "error");
    } finally {
      setBusy(false);
    }
  };

  const closeWelcome = (dontShowAgain: boolean) => {
    if (dontShowAgain) localStorage.setItem(WELCOME_KEY, "true");
    setShowWelcome(false);
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <img src={appIcon} className="header-icon" alt="" />
          <span className="header-title">{t("app.title")}</span>
        </div>
        <div className="header-right">
          <select
            className="lang-select"
            value={lang}
            onChange={(e) => setLang(e.target.value === "en" ? "en" : "vi")}
          >
            <option value="vi">VI</option>
            <option value="en">EN</option>
          </select>
          <button className="icon-btn ghost" title={t("settings.title")} onClick={() => setShowSettings(true)}>
            ⚙
          </button>
          <button className="icon-btn ghost" title={t("about.title")} onClick={() => setShowAbout(true)}>
            <img src={infoIcon} alt="" width={18} height={18} />
          </button>
        </div>
      </header>

      <main className="app-main">
        <div className="card">
          <div className="card-title">{t("account.list.title")}</div>
          <AccountTable
            accounts={accounts}
            selected={selected}
            loggedInUsername={session}
            onSelect={setSelected}
            onLogin={performLogin}
          />
          <div className="toolbar">
            <button
              className="toolbar-btn toolbar-btn-icon toolbar-btn-add tooltip"
              data-tooltip={t("account.add")}
              aria-label={t("account.add")}
              onClick={() => setEditing({ account: null, index: -1 })}
            >
              <img src={addIcon} alt="" aria-hidden="true" />
            </button>
            <button
              className="toolbar-btn toolbar-btn-icon toolbar-btn-edit tooltip"
              data-tooltip={t("account.edit")}
              aria-label={t("account.edit")}
              onClick={onEdit}
            >
              <img src={editIcon} alt="" aria-hidden="true" />
            </button>
            <button
              className="toolbar-btn toolbar-btn-icon toolbar-btn-delete tooltip"
              data-tooltip={t("account.delete")}
              aria-label={t("account.delete")}
              onClick={requestDelete}
            >
              <img src={deleteIcon} alt="" aria-hidden="true" />
            </button>
            <span className="toolbar-sep" />
            <button
              className="toolbar-btn toolbar-btn-login"
              aria-label={t("account.login")}
              disabled={busy}
              onClick={() => performLogin(selected)}
            >
              <img src={loginIcon} alt="" aria-hidden="true" />
              <span className="toolbar-btn-label">{t("account.login")}</span>
            </button>
          </div>
        </div>
      </main>

      <footer className="app-footer">
        <ConfigPanel
          riotPath={riotPath}
          status={status}
          busy={busy}
          onPathChanged={setRiotPath}
          onLaunchOrFocus={launchOrFocus}
        />
      </footer>

      {editing && (
        <AccountDialog
          initial={editing.account}
          onSave={onSaveAccount}
          onCancel={() => setEditing(null)}
        />
      )}
      {showSettings && <SettingsDialog onClose={() => setShowSettings(false)} />}
      {showAbout && (
        <AboutDialog
          onClose={() => setShowAbout(false)}
          onShowWelcome={() => {
            setShowAbout(false);
            localStorage.removeItem(WELCOME_KEY);
            setShowWelcome(true);
          }}
        />
      )}
      {showWelcome && <WelcomeDialog onClose={closeWelcome} />}
      {pendingDelete && (
        <ConfirmDialog
          title={t("account.delete.title")}
          message={
            pendingDelete.account.username.trim()
              ? t("account.delete.confirm.named").replace(
                  "{name}",
                  pendingDelete.account.username.trim(),
                )
              : t("account.delete.confirm")
          }
          detail={t("account.delete.detail")}
          confirmLabel={t("account.delete.button")}
          cancelLabel={t("account.cancel")}
          danger
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
      {showLoginStatus && session && (
        <LoginStatusDialog
          username={session}
          onLogout={onLogout}
          onClose={() => setShowLoginStatus(false)}
        />
      )}
    </div>
  );
}
