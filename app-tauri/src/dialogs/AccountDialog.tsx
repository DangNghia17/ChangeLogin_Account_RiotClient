import { useState } from "react";
import { REGIONS, type Account } from "../types";
import { useI18n } from "../i18n/I18nContext";

interface Props {
  initial: Account | null;
  onSave: (account: Account) => void;
  onCancel: () => void;
}

export function AccountDialog({ initial, onSave, onCancel }: Props) {
  const { t } = useI18n();
  const [username, setUsername] = useState(initial?.username ?? "");
  const [password, setPassword] = useState(initial?.password ?? "");
  const [region, setRegion] = useState(initial?.region ?? "VN");
  const [note, setNote] = useState(initial?.note ?? "");
  const [error, setError] = useState("");

  const submit = () => {
    if (!username.trim() || !password) {
      setError(t("account.fill.required"));
      return;
    }
    onSave({ username: username.trim(), password, region, note: note.trim() });
  };

  return (
    <div className="modal-overlay" onMouseDown={onCancel}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title">
          {initial ? t("account.edit.title") : t("account.add.title")}
        </h2>
        <div className="form-grid">
          <label>{t("account.username")}</label>
          <input value={username} autoFocus onChange={(e) => setUsername(e.target.value)} />

          <label>{t("account.password")}</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />

          <label>{t("account.region")}</label>
          <select value={region} onChange={(e) => setRegion(e.target.value)}>
            {REGIONS.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>

          <label>{t("account.note")}</label>
          <input value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        {error && <div className="form-error">{error}</div>}
        <div className="modal-actions">
          <button className="btn btn-gray" onClick={onCancel}>{t("account.cancel")}</button>
          <button className="btn btn-success" onClick={submit}>{t("account.save")}</button>
        </div>
      </div>
    </div>
  );
}
