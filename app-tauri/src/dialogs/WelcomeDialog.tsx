import { useState } from "react";
import { useI18n } from "../i18n/I18nContext";

interface Props {
  onClose: (dontShowAgain: boolean) => void;
}

export function WelcomeDialog({ onClose }: Props) {
  const { t } = useI18n();
  const [dontShow, setDontShow] = useState(false);

  const instructions = [
    t("app.welcome.instruction1"),
    t("app.welcome.instruction2"),
    t("app.welcome.instruction3"),
    t("app.welcome.instruction4"),
    t("app.welcome.instruction5"),
  ];

  return (
    <div className="modal-overlay" onMouseDown={() => onClose(dontShow)}>
      <div className="modal modal-wide" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title center">{t("app.welcome.title")}</h2>
        <ul className="welcome-list">
          {instructions.map((line, i) => (
            <li key={i}>{line}</li>
          ))}
        </ul>
        <div className="modal-actions space-between">
          <label className="checkbox-inline">
            <input type="checkbox" checked={dontShow} onChange={(e) => setDontShow(e.target.checked)} />
            {t("app.welcome.dontShowAgain")}
          </label>
          <button className="btn btn-primary" onClick={() => onClose(dontShow)}>
            {t("app.welcome.close")}
          </button>
        </div>
      </div>
    </div>
  );
}
