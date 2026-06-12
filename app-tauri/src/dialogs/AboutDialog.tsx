import { useI18n } from "../i18n/I18nContext";

interface Props {
  onClose: () => void;
  onShowWelcome: () => void;
}

export function AboutDialog({ onClose, onShowWelcome }: Props) {
  const { t, lang } = useI18n();

  const sections = lang === "vi"
    ? [
        ["Giới thiệu", "Riot Account Manager giúp quản lý và đăng nhập nhanh nhiều tài khoản Riot Games (League of Legends, VALORANT, ...)."],
        ["An toàn với game", "Chỉ mô phỏng thao tác bàn phím/chuột như người dùng thực. KHÔNG hook, KHÔNG inject, tương thích Vanguard."],
        ["Bảo mật dữ liệu", "Mật khẩu được mã hóa và lưu cục bộ bằng Windows DPAPI. KHÔNG gửi dữ liệu lên server."],
        ["Tương thích", "Tự động nhận dữ liệu từ bản Java cũ, không phải nhập lại tài khoản."],
      ]
    : [
        ["Introduction", "Riot Account Manager helps you manage and quickly log into multiple Riot Games accounts (League of Legends, VALORANT, ...)."],
        ["Game-safe", "Only simulates keyboard/mouse input like a real user. NO hooking, NO injection, Vanguard-compatible."],
        ["Data security", "Passwords are encrypted and stored locally using Windows DPAPI. NO data is sent to any server."],
        ["Compatibility", "Automatically imports data from the old Java version — no need to re-enter accounts."],
      ];

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <div className="modal modal-wide" onMouseDown={(e) => e.stopPropagation()}>
        <h2 className="modal-title">{t("app.title")}</h2>
        <div className="about-content">
          {sections.map(([title, body], i) => (
            <div key={i} className="about-section">
              <div className="about-section-title">{i + 1}. {title}</div>
              <div className="about-section-body">{body}</div>
            </div>
          ))}
        </div>
        <div className="about-version">Version 3.0.0 (Tauri) | MIT License</div>
        <div className="modal-actions">
          <button className="btn btn-gray" onClick={onShowWelcome}>{t("about.showWelcome")}</button>
          <button className="btn btn-primary" onClick={onClose}>{t("about.close")}</button>
        </div>
      </div>
    </div>
  );
}
