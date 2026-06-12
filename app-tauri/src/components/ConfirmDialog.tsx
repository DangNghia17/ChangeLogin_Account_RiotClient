import { useI18n } from "../i18n/I18nContext";

interface Props {
  title: string;
  message: string;
  detail?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** Reusable confirmation dialog. Replaces the native window.confirm prompt. */
export function ConfirmDialog({
  title,
  message,
  detail,
  confirmLabel,
  cancelLabel,
  danger = false,
  onConfirm,
  onCancel,
}: Props) {
  const { t } = useI18n();

  return (
    <div className="modal-overlay" onMouseDown={onCancel}>
      <div className="modal modal-confirm" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{title}</h2>
        <p className="modal-message">{message}</p>
        {detail && <p className="modal-subtext">{detail}</p>}
        <div className="modal-actions">
          <button className="btn btn-gray" onClick={onCancel}>
            {cancelLabel ?? t("account.cancel")}
          </button>
          <button
            className={danger ? "btn btn-danger" : "btn btn-primary"}
            autoFocus
            onClick={onConfirm}
          >
            {confirmLabel ?? t("account.save")}
          </button>
        </div>
      </div>
    </div>
  );
}
