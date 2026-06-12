import { useI18n } from "../i18n/I18nContext";

interface Props {
  username: string;
  onLogout: () => void;
  onClose: () => void;
}

/**
 * Shown when the user clicks "Login" for an account that is already the active
 * session. Instead of running the auto-login flow again, it tells the user which
 * account is signed in and offers a logout action.
 */
export function LoginStatusDialog({ username, onLogout, onClose }: Props) {
  const { t } = useI18n();

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <div className="modal modal-confirm" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{t("login.status.title")}</h2>
        <p className="modal-message">{t("login.status.message")}</p>
        <div className="login-status-account">{username}</div>
        <p className="modal-subtext">{t("login.status.hint")}</p>
        <div className="modal-actions">
          <button className="btn btn-gray" onClick={onClose}>
            {t("login.status.close")}
          </button>
          <button className="btn btn-danger" onClick={onLogout}>
            {t("login.logout")}
          </button>
        </div>
      </div>
    </div>
  );
}
