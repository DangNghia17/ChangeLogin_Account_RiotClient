# README_MIGRATION.md — Java Swing → Tauri + React + TypeScript

Tài liệu tổng hợp quá trình migration Riot Account Manager. Tài liệu phân tích/kế hoạch chi
tiết nằm trong `docs/` (Phase 1–4). Ưu tiên #1 xuyên suốt: **không mất dữ liệu người dùng**.

---

## Kiến trúc cũ (Java)

- **Java 11 + Swing** (GUI), `org.json`, `javax.crypto` (AES-256/ECB), `java.awt.Robot`,
  PowerShell + Win32 cho điều khiển cửa sổ.
- Đóng gói: JAR → Launch4j/jpackage (kèm JRE). Người dùng không cần cài Java (runtime bundled).
- Dữ liệu: `%LOCALAPPDATA%\RiotAccountManager\{accounts.dat, config.json}`.
  `accounts.dat` = `Base64(AES-256/ECB(JSON array))`, key = `SHA-256(MachineGuid)`.

## Kiến trúc mới (Tauri)

```
app-tauri/
├── src/                     # Frontend: React 18 + TypeScript + Vite
│   ├── App.tsx, main.tsx, styles.css, types.ts
│   ├── components/ (AccountTable, ConfigPanel, Toast)
│   ├── dialogs/ (Account, Settings, About, Welcome)
│   ├── i18n/ (strings vi/en + context)
│   └── lib/api.ts (typed IPC bindings)
├── crates/core/             # Rust core (KHÔNG phụ thuộc GUI/WebKit → unit-test mọi nền)
│   └── src/ model, paths, crypto, config, store, settings, startup, riot
└── src-tauri/               # Vỏ Tauri: commands.rs, lib.rs, main.rs, tauri.conf.json
```

- **Backend Rust** giữ toàn bộ business logic + truy cập OS; **React** chỉ là view + `invoke`.
- Tách `ram-core` (không webkit) để test logic dữ liệu/mã hóa trên Linux/CI.
- Native, nhẹ, không cần Java runtime; dùng WebView2 sẵn có trên Windows 10/11.

---

## Các tính năng đã migrate (đối chiếu `docs/feature-inventory.md`)

| Nhóm | Tính năng | Trạng thái |
|---|---|---|
| A | Add/Edit/Delete/List/Select account, Region combo, Note, hover | ✅ |
| B | Quick Login (nút + double-click), auto-fill, clear field, **auto-submit**, special chars | ✅ (xem ghi chú DPI ở KNOWN_ISSUES) |
| C | Detect running/visible/tray, focus/restore, measure window, launch, status 3s, Open/Focus | ✅ |
| D | Auto-detect path, browse (dialog plugin), validate, persist, hiển thị path | ✅ |
| E | Run at Windows Startup, sync on launch, Auto-click Login, persist settings | ✅ |
| F | Encryption, machine-bound, atomic write, backup, rollback, migration, load, error-tolerant | ✅ (nâng cấp sang DPAPI) |
| G | Main window 3 vùng, gradient header, buttons, **Toast non-blocking**, icon, top-right, resizable, About, Welcome, re-show welcome | ✅ |
| H | VI/EN switch, persist language, full localization, default = VI | ✅ (xem ghi chú prefs) |
| I | Portable, EXE installer (NSIS), MSI (WiX), no-Java, CI, Win10/11 | ✅ (build qua CI) |

> **62/62** điểm tính năng được migrate. Các mục có thay đổi/cần lưu ý liệt kê bên dưới và trong `KNOWN_ISSUES.md`.

## Các tính năng thay đổi (có chủ đích)

1. **Mã hóa**: ECB (Java) → **DPAPI** (Windows) cho dữ liệu mới (Phase 7). Vẫn **đọc** được
   dữ liệu ECB cũ để migrate. Định dạng mới có prefix `RAM2:`.
2. **Thông báo**: Swing `Toast` (JWindow) → React Toast non-blocking (giữ nguyên trải nghiệm).
3. **Window state**: Tauri lưu vị trí mặc định góc trên-phải qua config window; (xem KNOWN_ISSUES
   về "remember window state" nếu cần nâng cao).
4. **Language/Welcome prefs**: trước ở Java `Preferences` (registry JavaSoft) → nay ở
   `localStorage` + `config.json`. Có thể reset 1 lần khi nâng cấp (không ảnh hưởng account).

## Các tính năng chưa migrate hoàn toàn

- Không có tính năng nào bị bỏ. Các điểm phụ thuộc OS sâu (auto-login bằng `enigo`, focus cửa sổ,
  DPAPI, startup registry) **chỉ chạy trên Windows** và **chưa thể chạy GUI test tự động** trong
  môi trường CI Linux — cần kiểm thử thủ công trên Windows (xem `KNOWN_ISSUES.md`, mục test matrix).

---

## Các lỗi đã sửa / cải thiện trong quá trình migrate

- Loại bỏ phụ thuộc JRE: app native, nhẹ hơn nhiều.
- Mã hóa mạnh hơn: thay ECB (lộ pattern) bằng DPAPI (không hard-code key, không plaintext).
- `enigo.text()` gõ Unicode trực tiếp → chính xác hơn `Robot` typeChar thủ công (nhiều ký tự hơn).
- Tách core khỏi GUI → testable, dễ bảo trì.
- Toàn bộ command trả `Result<_, String>` → error handling nhất quán; logging qua `log` crate.
- Migration có backup + rollback + atomic write (giữ an toàn dữ liệu).

## Các rủi ro còn tồn tại

Xem `KNOWN_ISSUES.md` (đầy đủ). Tóm tắt:
- Hành vi auto-login/DPI của `enigo` cần xác minh trên Windows thật.
- Downgrade về bản Java sau khi đã migrate sang DPAPI cần dùng `accounts.dat.bak`.
- Fallback machine-id (khi không đọc được MachineGuid) có thể lệch giữa Java và Rust.

---

## Cách build

### Yêu cầu
- Node 18+ và Rust stable (>= 1.85 do dependency `edition2024`).
- Windows: WebView2 (mặc định có trên Win10/11). Linux dev: `libwebkit2gtk-4.1-dev`, `libgtk-3-dev`,
  `libxdo-dev`, `libayatana-appindicator3-dev`, `librsvg2-dev`.

### Development
```bash
cd app-tauri
npm install
npm run tauri dev      # mở app dev (hot reload)
```

### Chỉ kiểm tra logic (mọi nền, không cần WebKit)
```bash
cd app-tauri
cargo test -p ram-core   # gồm test tương thích dữ liệu Java
npm run build            # type-check + build frontend
```

### Production build
```bash
cd app-tauri
npm install
npm run tauri build
```

## Cách release

- Tạo tag `vX.Y.Z` và push → workflow `.github/workflows/tauri-release.yml` (windows-latest)
  build NSIS `.exe`, WiX `.msi`, và zip portable, rồi đính kèm vào GitHub Release.
- Hoặc chạy thủ công `workflow_dispatch` với input version.

## Cách đóng gói

`npm run tauri build` (trên Windows) tạo trong `app-tauri/target/release/`:
- `bundle/nsis/*.exe` — **EXE Installer** (NSIS, per-user, có shortcut).
- `bundle/msi/*.msi` — **MSI Installer** (WiX).
- `RiotAccountManager.exe` (release) — **Portable** (self-contained, chỉ cần WebView2 hệ thống).
- Không yêu cầu cài Java / runtime thủ công.

---

## Ưu tiên đã tuân thủ

1. ✅ Không mất dữ liệu (test chứng minh đọc/migrate dữ liệu Java).
2. ✅ Không mất chức năng (đối chiếu 62/62 trong feature-inventory).
3. ✅ Tương thích dữ liệu cũ (đọc accounts.dat/config.json của Java).
4. ✅ Ổn định (atomic IO, backup, rollback, error handling).
5. ✅ Bảo mật (DPAPI, không plaintext, không hard-code key).
6. ✅ Hiệu năng (native, nhẹ).
7. ✅ Giao diện (giữ layout/workflow, theme tương đương).
8. ✅ Mở rộng (core tách module, IPC rõ ràng).
