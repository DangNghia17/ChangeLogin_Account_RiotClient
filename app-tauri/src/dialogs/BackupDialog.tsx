import { useState } from "react";
import { useI18n } from "../i18n/I18nContext";
import { useToast } from "../components/Toast";
import { api } from "../lib/api";
import {
  BackupError,
  decryptBackup,
  defaultBackupFileName,
  encryptBackup,
  type BackupPayload,
} from "../lib/backup";

interface Props {
  mode: "export" | "import";
  onClose: () => void;
  /** Called after a successful import so the host can refresh its state. */
  onImported: () => void;
}

type ImportStep = "pick" | "password" | "confirm";

export function BackupDialog({ mode, onClose, onImported }: Props) {
  const { t } = useI18n();
  const toast = useToast();

  const [password, setPassword] = useState("");
  const [confirmPwd, setConfirmPwd] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  // Import-only state
  const [step, setStep] = useState<ImportStep>("pick");
  const [fileText, setFileText] = useState<string | null>(null);
  const [payload, setPayload] = useState<BackupPayload | null>(null);

  const errorMessage = (e: unknown): string => {
    if (e instanceof BackupError) return t(`backup.error.${e.code}`);
    return `${t("backup.error.unknown")} (${String(e)})`;
  };

  // ---- Export ----------------------------------------------------------------
  const runExport = async () => {
    if (!password) {
      setError(t("backup.password.empty"));
      return;
    }
    if (password !== confirmPwd) {
      setError(t("backup.password.mismatch"));
      return;
    }
    setBusy(true);
    setError("");
    try {
      const [accounts, settings, riotPath] = await Promise.all([
        api.listAccounts(),
        api.getSettings().catch(() => undefined),
        api.getRiotPath().catch(() => null),
      ]);
      const text = await encryptBackup(password, { accounts, settings, riotPath });
      const saved = await api.saveTextFile(defaultBackupFileName(), text);
      if (saved) {
        toast.show(t("backup.export.success"), "success");
        onClose();
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  // ---- Import ----------------------------------------------------------------
  const chooseFile = async () => {
    setError("");
    setBusy(true);
    try {
      const text = await api.pickAndReadTextFile();
      if (text == null) return;
      setFileText(text);
      setStep("password");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const runDecrypt = async () => {
    if (!fileText) return;
    if (!password) {
      setError(t("backup.password.empty"));
      return;
    }
    setBusy(true);
    setError("");
    try {
      const decoded = await decryptBackup(password, fileText);
      setPayload(decoded);
      setStep("confirm");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const applyImport = async () => {
    if (!payload) return;
    setBusy(true);
    setError("");
    try {
      await api.replaceAccounts(payload.accounts);
      if (payload.settings) await api.setSettings(payload.settings).catch(() => undefined);
      if (payload.riotPath) await api.setRiotPath(payload.riotPath).catch(() => undefined);
      toast.show(t("backup.import.success"), "success");
      onImported();
      onClose();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const title = mode === "export" ? t("backup.export.title") : t("backup.import.title");

  return (
    <div className="modal-overlay" onMouseDown={busy ? undefined : onClose}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{title}</h2>

        {mode === "export" && (
          <>
            <p className="modal-subtext">{t("backup.export.desc")}</p>
            <div className="form-grid">
              <label>{t("backup.password")}</label>
              <input
                type="password"
                autoFocus
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <label>{t("backup.password.confirm")}</label>
              <input
                type="password"
                value={confirmPwd}
                onChange={(e) => setConfirmPwd(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && runExport()}
              />
            </div>
            {error && <div className="form-error">{error}</div>}
            <div className="modal-actions">
              <button className="btn btn-gray" disabled={busy} onClick={onClose}>
                {t("account.cancel")}
              </button>
              <button className="btn btn-primary" disabled={busy} onClick={runExport}>
                {t("backup.export.button")}
              </button>
            </div>
          </>
        )}

        {mode === "import" && step === "pick" && (
          <>
            <p className="modal-subtext">{t("backup.import.desc")}</p>
            {error && <div className="form-error">{error}</div>}
            <div className="modal-actions">
              <button className="btn btn-gray" disabled={busy} onClick={onClose}>
                {t("account.cancel")}
              </button>
              <button className="btn btn-primary" disabled={busy} onClick={chooseFile}>
                {t("backup.choose.file")}
              </button>
            </div>
          </>
        )}

        {mode === "import" && step === "password" && (
          <>
            <p className="modal-subtext">{t("backup.import.selected")}</p>
            <div className="form-grid">
              <label>{t("backup.password.enter")}</label>
              <input
                type="password"
                autoFocus
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && runDecrypt()}
              />
            </div>
            {error && <div className="form-error">{error}</div>}
            <div className="modal-actions">
              <button className="btn btn-gray" disabled={busy} onClick={onClose}>
                {t("account.cancel")}
              </button>
              <button className="btn btn-primary" disabled={busy} onClick={runDecrypt}>
                {t("backup.import.decrypt")}
              </button>
            </div>
          </>
        )}

        {mode === "import" && step === "confirm" && payload && (
          <>
            <p className="modal-message">{t("backup.import.confirm")}</p>
            <p className="modal-subtext">
              {t("backup.import.summary").replace("{n}", String(payload.accounts.length))}
            </p>
            <p className="modal-subtext">{t("backup.import.confirm.detail")}</p>
            {error && <div className="form-error">{error}</div>}
            <div className="modal-actions">
              <button className="btn btn-gray" disabled={busy} onClick={onClose}>
                {t("account.cancel")}
              </button>
              <button className="btn btn-danger" disabled={busy} onClick={applyImport}>
                {t("backup.import.confirm.button")}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
