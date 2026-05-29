# Current System Audit (Phase 1)

> Audit của ứng dụng **Riot Account Manager v2.0.0** (Java Swing) — cơ sở để migration sang
> **Tauri + React + TypeScript**. Tài liệu này được tạo **trước khi** viết bất kỳ code migration nào.

---

## 1. Tổng quan hệ thống

- **Loại:** Desktop app, **Windows-only** (10/11).
- **Stack hiện tại:** Java 11, Swing (GUI), `org.json` (serialize), AES-256 (mã hóa), Windows
  API qua `java.awt.Robot` + PowerShell/Win32 interop.
- **Mục đích:** Quản lý nhiều tài khoản Riot Games và tự động điền/đăng nhập vào Riot Client.
- **Đóng gói:** JAR → Launch4j `.exe` + JRE tùy biến (`runtime/`), hoặc jpackage (portable + installer).
- **Entry point:** `com.riotaccountmanager.App`.

### 1.1. Cấu trúc package (sau refactor v2)

```
com.riotaccountmanager
├── App.java                  # Entry point
├── model/Account.java        # {username, password, region, note}
├── i18n/LanguageManager.java # VI/EN, ResourceBundle + java.util.prefs
├── storage/
│   ├── AppPaths.java         # %LOCALAPPDATA%\RiotAccountManager\
│   ├── CryptoService.java    # AES/ECB/PKCS5, key = SHA-256(MachineGuid)
│   ├── AccountStore.java     # atomic write + backup + rollback + migration
│   ├── AppConfig.java        # config.json: riotClientPath + settings
│   └── AppSettings.java      # runAtStartup, autoClickLogin
├── riot/RiotClientService.java  # detect/focus/measure window + auto login
├── system/StartupManager.java   # HKCU Run key
└── ui/                       # MainForm, dialogs, Toast, theme, helpers
```

---

## 2. Luồng hoạt động

### 2.1. Khởi động
1. `App.main` → set System L&F → `AppSettings.syncStartupRegistration()` (đồng bộ Run key
   với setting đã lưu) → tạo `MainForm`, đặt ở góc trên-phải, hiện cửa sổ.
2. `MainForm` tạo `AccountStore` (tự migrate + load), dựng UI (header / bảng account / config panel).
3. Lần đầu chạy: hiện `WelcomeDialog` (cờ lưu ở `java.util.prefs`).
4. `javax.swing.Timer` 3s: cập nhật trạng thái Riot Client (`tasklist` + powershell).

### 2.2. CRUD account
- Add/Edit: `AccountDialog` (username, password, region combo, note) → `AccountStore.add/update` → `save()`.
- Delete: confirm dialog → `AccountStore.removeAccount` → `save()`.
- Mỗi thao tác ghi lại toàn bộ file (atomic + backup).

### 2.3. Login (Quick Login / Auto Login)
1. Chọn dòng (hoặc double-click) → `performLogin()`.
2. Hiện progress dialog (non-modal).
3. Thread nền:
   - `isRiotClientRunning()` (tasklist) — nếu chưa chạy → toast cảnh báo.
   - Nếu cửa sổ ẩn → `focusRiotClientWindow()` (PowerShell + Win32 SetForegroundWindow…).
   - `autoLogin(account, autoSubmit)`:
     - `getRiotClientWindowBounds()` (PowerShell GetClientRect/ClientToScreen → physical px).
     - Tính điểm click (≈13.3% X, 25% Y của client area), **bù DPI** (physical→logical), clamp.
     - `Robot`: ESC → click username → Ctrl+A/Delete → gõ username → Tab → clear → gõ password.
     - Nếu `autoSubmit`: nhấn Enter (auto-click Login).
4. Kết thúc: **Toast** non-blocking (không chặn, không cần OK).

### 2.4. Riot Client config
- `AppConfig.getRiotClientPath()`: đọc `config.json`; nếu trống dò 3 path mặc định; validate file là `RiotClientServices.exe`.
- Nút "Mở/Focus Riot Client": nếu đang chạy+hiện → focus; nếu chưa → launch rồi poll ≤30s.

### 2.5. Settings
- `SettingsDialog`: 2 toggle → "Run at Windows startup" (HKCU Run key) + "Auto-click Login".
- Lưu vào `config.json`; startup đồng thời ghi/xóa registry.

---

## 3. Business Logic (chi tiết theo module)

| Module | Trách nhiệm | Phụ thuộc Windows | Ghi chú migration |
|---|---|---|---|
| `Account` | Model dữ liệu | Không | → struct Rust + type TS |
| `CryptoService` | AES/ECB, key SHA-256(MachineGuid) | `reg query MachineGuid` | **Phải đọc được dữ liệu Java cũ** |
| `AccountStore` | Load/save, atomic, backup, rollback, migration | Không (chỉ FS) | → Rust (fs + crypto) |
| `AppConfig` | `config.json` (path + settings) | `%LOCALAPPDATA%` | Giữ nguyên file & key |
| `AppSettings` | Settings có kiểu | Qua StartupManager | → Rust |
| `StartupManager` | HKCU Run key (add/delete/query) | reg.exe | → Rust (`windows`/`winreg`) hoặc tauri plugin |
| `RiotClientService` | Detect/focus/measure window + auto login | tasklist, powershell, Win32, Robot | → Rust (windows crate + enigo) hoặc giữ powershell |
| `LanguageManager` | i18n VI/EN | java.util.prefs | → React i18n + tauri store |
| `ui/*` | Swing UI | Không | → React/TS |

---

## 4. Data Storage

- **Thư mục:** `%LOCALAPPDATA%\RiotAccountManager\` (fallback `user.home` khi không có biến môi trường).
- **Files:**
  - `accounts.dat` — danh sách account, **mã hóa** (Base64 của AES ciphertext).
  - `config.json` — plaintext: `{ riotClientPath, runAtStartup?, autoClickLogin?, language? }`.
  - `accounts.dat.bak` — backup tự động trước mỗi lần ghi đè.
  - `accounts.dat.tmp` — file tạm cho atomic write.
  - `data.version` — số schema version (tách riêng, không nằm trong accounts.dat).
- **Preferences (Registry, qua `java.util.prefs`):** `HKCU\Software\JavaSoft\Prefs\com\riotaccountmanager`
  lưu `language` và `welcome_shown`.
- **Startup (Registry):** `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` value `RiotAccountManager`.

Chi tiết format & mã hóa: xem `docs/data-compatibility-analysis.md`.

---

## 5. Encryption

- **Thuật toán:** `AES/ECB/PKCS5Padding`, key 256-bit = `SHA-256(MachineGuid)`.
- **MachineGuid:** đọc từ `HKLM\SOFTWARE\Microsoft\Cryptography /v MachineGuid` (fallback
  `user.name + os.name + os.version`, rồi `"default-machine-id"`).
- **Encode:** Base64.
- **Đặc tính:** machine-bound (copy sang máy khác không giải mã được).
- **Điểm yếu:** ECB (lộ pattern); key tự derive, không dùng OS keystore. Đây là lý do Phase 7
  yêu cầu chuyển sang DPAPI/OS credential storage.

---

## 6. Riot Integration

- **Phát hiện process:** `tasklist` lọc `riotclientservices.exe` / "riot client".
- **Phát hiện cửa sổ:** powershell `Get-Process | MainWindowTitle -like '*Riot*'`.
- **Focus/restore:** powershell sinh tạm, `Add-Type` C# gọi `SetForegroundWindow`,
  `ShowWindow(SW_RESTORE)`, `BringWindowToTop`, `IsIconic`.
- **Đo cửa sổ:** powershell `GetClientRect` + `ClientToScreen` → `x,y,w,h` (physical px).
- **Nhập liệu:** `java.awt.Robot` (key/mouse), **bù DPI** bằng `GraphicsConfiguration.getDefaultTransform`.
- **An toàn anti-cheat:** chỉ mô phỏng input, không hook/inject/đọc bộ nhớ.

---

## 7. Settings

- `runAtStartup` (bool) → registry Run key.
- `autoClickLogin` (bool) → nhấn Enter sau khi điền.
- `language` (vi/en) → hiện lưu ở java.util.prefs (LanguageManager) **và** key `language` trong config.json (AppConfig, chưa dùng tới ở UI).
- `welcome_shown` (bool) → java.util.prefs.

---

## 8. Packaging

- **Maven shade** → fat JAR (`com.riotaccountmanager.App`).
- **Launch4j** (`dist/launch4j-config.xml`) → `.exe` wrap JAR + `runtime/` (JRE jlink, minVersion 11).
- **jpackage** (`scripts/package.ps1`) → portable app-image (kèm runtime) + Setup.exe (cần WiX).
- Yêu cầu hiện tại: người dùng **không cần cài Java** (runtime bundled).

---

## 9. Build Pipeline

- `scripts/build.sh` — dev compile (Linux/macOS).
- `scripts/build.ps1` — build JAR (Maven hoặc javac fat-jar).
- `scripts/package.ps1` — jpackage portable + installer.
- `.github/workflows/release.yml` — build trên `windows-latest`, đính kèm artifact khi tag `v*`.

---

## 10. Dependency quan trọng

| Dependency | Vai trò | Tương ứng phía Tauri |
|---|---|---|
| Java Swing | UI | React + TS + CSS |
| `org.json` | JSON | `serde` / `serde_json` (Rust), JSON tự nhiên (TS) |
| `javax.crypto` AES | Mã hóa | `aes`/`cbc`/`ecb` crate + DPAPI (`windows` crate) |
| `java.awt.Robot` | Mô phỏng input | `enigo` crate (hoặc powershell) |
| PowerShell + Win32 | Window control | `windows` crate (Win32 API) hoặc giữ powershell |
| `java.util.prefs` | Preferences | tauri store plugin / file JSON |
| Registry `reg.exe` | Startup | `winreg`/`windows` crate hoặc `tauri-plugin-autostart` |

---

## 11. Các điểm cần chú ý khi migration

1. **Tương thích dữ liệu (ưu tiên #1):** Rust **bắt buộc** giải mã được `accounts.dat` do Java
   ghi (AES/ECB + SHA-256(MachineGuid) + Base64). Cần unit test với vector dữ liệu thật.
2. **Đường dẫn & tên file giữ nguyên** để tự nhận dữ liệu cũ (no manual migration).
3. **MachineGuid** phải lấy giống Java (cùng giá trị) để key trùng — đọc từ HKLM Cryptography.
4. **Bảo mật (Phase 7):** sau khi đọc dữ liệu cũ → migrate sang DPAPI, backup trước, rollback nếu lỗi.
   Không hard-code key, không lưu plaintext.
5. **Auto login phụ thuộc OS sâu:** Robot→`enigo`; DPI scaling phải xử lý đúng; có thể giữ
   PowerShell cho focus/measure để bảo toàn hành vi đã kiểm chứng.
6. **Startup registry:** giữ đúng value name `RiotAccountManager` ở HKCU Run.
7. **i18n:** chuyển nội dung `messages_vi/en.properties` sang JSON cho React; giữ nguyên text.
8. **Không redesign UI / không đổi workflow:** layout & thao tác phải tương đương.
9. **Windows-only:** Tauri build Windows; CI dùng `windows-latest`. Trên Linux chỉ check/test logic.
10. **Welcome/Language preferences** đang ở Java prefs (registry path riêng của JavaSoft) — KHÔNG
    đọc lại được từ Rust dễ dàng; chấp nhận reset (chỉ ảnh hưởng hiện lại Welcome + ngôn ngữ mặc định),
    **không ảnh hưởng account** — ghi rõ trong KNOWN_ISSUES.
