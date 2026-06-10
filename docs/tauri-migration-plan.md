# Tauri Migration Plan (Phase 4)

> Kế hoạch chuyển Riot Account Manager (Java Swing) → **Tauri v2 + React + TypeScript**.
> **Không code trước khi tài liệu này hoàn thành.** Không redesign UI, không đổi workflow.

---

## 1. Kiến trúc đích

```
riot-account-manager (Tauri v2)
├── src/                         # Frontend: React + TypeScript + Vite
│   ├── main.tsx, App.tsx
│   ├── components/              # AccountTable, Toolbar, ConfigPanel, dialogs, Toast
│   ├── dialogs/                 # AccountDialog, SettingsDialog, AboutDialog, WelcomeDialog
│   ├── i18n/                    # vi.json, en.json + hook
│   ├── lib/                     # tauri command bindings (typed)
│   ├── types.ts
│   └── styles.css
├── src-tauri/                   # Backend: Rust
│   ├── Cargo.toml
│   ├── tauri.conf.json          # bundler: nsis (exe), wix (msi); portable
│   ├── build.rs
│   ├── icons/
│   └── src/
│       ├── main.rs / lib.rs     # tauri::Builder, register commands
│       ├── model.rs             # Account, Settings structs (serde)
│       ├── paths.rs             # AppPaths (LOCALAPPDATA\RiotAccountManager)
│       ├── crypto.rs            # legacy AES-ECB decrypt + DPAPI encrypt/decrypt
│       ├── store.rs             # AccountStore: load/save/atomic/backup/rollback/migrate
│       ├── config.rs            # config.json
│       ├── settings.rs          # typed settings + startup sync
│       ├── startup.rs           # HKCU Run key
│       ├── riot.rs              # detect/focus/measure/launch/autologin
│       └── commands.rs          # #[tauri::command] wrappers
├── package.json, tsconfig.json, vite.config.ts
└── .github/workflows/tauri-release.yml
```

Tauri (Rust) chứa toàn bộ business logic + OS access; React chỉ là view + gọi `invoke`.

---

## 2. Phân loại module

### 2.1. Giữ nguyên LOGIC (port 1:1 sang Rust)

| Java | Rust | Ghi chú |
|---|---|---|
| `AppPaths` | `paths.rs` | Cùng đường dẫn/file |
| `AccountStore` | `store.rs` | Atomic + backup + rollback + migration giữ nguyên ý tưởng |
| `AppConfig` | `config.rs` | Cùng file/key config.json |
| `AppSettings` | `settings.rs` | runAtStartup, autoClickLogin |
| `StartupManager` | `startup.rs` | Cùng Run key + value name |
| `Account` | `model.rs` | Cùng field names (serde) |

### 2.2. Viết lại / nâng cấp (Phase 7 security)

| Java | Rust | Thay đổi |
|---|---|---|
| `CryptoService` (AES/ECB) | `crypto.rs` | **Đọc** legacy ECB (giữ tương thích) + **ghi** DPAPI (`RAM2:` prefix). Không plaintext, không hard-code key |

### 2.3. Chuyển sang Rust (OS integration)

| Java | Rust | Cách làm |
|---|---|---|
| `RiotClientService` | `riot.rs` | Detect (tasklist / `windows` Process), focus/measure (giữ **PowerShell + Win32** để bảo toàn hành vi đã kiểm chứng), input qua `enigo` (DPI-aware) |
| Startup registry | `startup.rs` | `windows`/`winreg` hoặc giữ `reg.exe` |

### 2.4. Chuyển sang React/TS (UI)

| Java Swing | React |
|---|---|
| `MainForm` | `App.tsx` + components |
| `AccountDialog` | `dialogs/AccountDialog.tsx` |
| `SettingsDialog` | `dialogs/SettingsDialog.tsx` |
| `AboutDialog` | `dialogs/AboutDialog.tsx` |
| `WelcomeDialog` | `dialogs/WelcomeDialog.tsx` |
| `Toast` | `components/Toast.tsx` (hoặc tauri notification) |
| `LanguageManager` | `i18n/` (context + json) |
| `UITheme`/`UIHelper` | `styles.css` (CSS variables) |

---

## 3. Mapping tính năng → command (IPC)

| Feature (inventory) | Tauri command |
|---|---|
| A1–A4 CRUD/list | `list_accounts`, `add_account`, `update_account`, `delete_account` |
| B1–B8 login | `auto_login(index, autoSubmit)` |
| C1–C2 status | `riot_status` → {running, windowVisible} |
| C3 focus | `focus_riot` |
| C5 launch | `launch_riot` |
| D1 default path | `default_riot_path` |
| D3 validate | `validate_riot_path(path)` |
| D4 persist | `set_riot_path(path)` / `get_riot_path` |
| E1/E3 settings | `get_settings`, `set_settings` |
| F* storage | nội bộ trong store.rs (gọi qua các command trên) |
| H1/H2 language | `get_language`, `set_language` (lưu config/store) |
| G10 welcome flag | `get_welcome_seen`, `set_welcome_seen` |

Mọi command trả `Result<T, String>`; lỗi map sang toast ở UI.

---

## 4. Dependencies (Rust)

- `tauri` v2, `serde`, `serde_json`
- `sha2`, `aes`, `ecb`, `cbc`? → dùng `aes` + `ecb` + `base64` cho legacy
- `base64`
- Windows: `windows` crate (DPAPI `CryptProtectData/Unprotect`, Win32 window APIs) — `#[cfg(windows)]`
- `enigo` (mô phỏng phím/chuột) — `#[cfg(windows)]` cho phần login
- Startup: `windows` (registry) hoặc `tauri-plugin-autostart`

Core logic (paths/crypto-legacy/store/model/config) **không phụ thuộc Windows** để `cargo test` chạy được trên Linux. Phần Windows bọc trong `#[cfg(windows)]` + fallback.

---

## 5. Dependencies (Frontend)

- `react`, `react-dom`, `typescript`, `vite`, `@vitejs/plugin-react`
- `@tauri-apps/api` (invoke), `@tauri-apps/cli` (dev/build)
- Không thêm UI framework nặng (ưu tiên nhẹ); CSS thuần + biến màu khớp `UITheme`.

---

## 6. Rủi ro & giảm thiểu

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Rust không giải mã đúng dữ liệu Java | 🔴 | Unit test với test vector cố định (data-compat §3.1) + test trên Windows thật |
| Mất account khi migrate sang DPAPI | 🔴 | Backup `.bak` trước, ghi atomic, rollback khi lỗi |
| Không build được .exe/.msi trên Linux | 🟠 | Build qua CI `windows-latest`; trên Linux chỉ check/test |
| Hành vi auto-login lệch (enigo vs Robot, DPI) | 🟠 | Giữ PowerShell cho focus/measure; bù DPI; test thủ công Windows |
| Tauri Linux build cần webkit libs | 🟡 | Tách core crate để `cargo test` không cần webkit; build full ở CI |
| Language/welcome prefs reset | 🟡 | Ghi rõ KNOWN_ISSUES; chỉ ảnh hưởng UX nhỏ, không mất account |
| Downgrade về Java sau migrate | 🟡 | Giữ `.bak`; tài liệu hướng dẫn |

---

## 7. Checklist migration

**Chuẩn bị**
- [x] Audit (Phase 1)
- [x] Feature inventory (Phase 2)
- [x] Data compat (Phase 3)
- [x] Plan (Phase 4)

**Backend (Rust)**
- [ ] paths.rs, model.rs, config.rs
- [ ] crypto.rs (legacy decrypt + encrypt) + DPAPI (cfg windows)
- [ ] store.rs (load/save/atomic/backup/rollback/migrate)
- [ ] settings.rs, startup.rs
- [ ] riot.rs (detect/focus/measure/launch/autologin)
- [ ] commands.rs + register trong lib.rs
- [ ] unit tests crypto/store (chạy trên Linux)

**Frontend (React/TS)**
- [ ] Scaffold Vite + React + TS + Tauri
- [ ] types + command bindings
- [ ] AccountTable + Toolbar (add/edit/delete/login)
- [ ] AccountDialog, SettingsDialog, AboutDialog, WelcomeDialog
- [ ] ConfigPanel (path + status + launch/focus)
- [ ] Toast non-blocking
- [ ] i18n vi/en + switch
- [ ] Styling khớp theme cũ

**Packaging/CI (Phase 9)**
- [ ] tauri.conf.json: nsis (exe), wix (msi)
- [ ] portable build
- [ ] CI workflow windows-latest

**Kiểm thử/Audit (Phase 10/11)**
- [ ] cargo test, frontend build
- [ ] ma trận test thủ công (fresh/upgrade/existing)
- [ ] bug audit → README_MIGRATION/KNOWN_ISSUES

**Tài liệu**
- [ ] README_MIGRATION.md, MIGRATION_CHANGELOG.md, KNOWN_ISSUES.md

---

## 8. Nguyên tắc thực thi

- Không xóa code Java trong cùng PR migration (giữ để đối chiếu & rollback); đặt app Tauri ở
  thư mục riêng (ví dụ `app-tauri/` hoặc root mới), tài liệu hóa rõ.
- Giữ workflow người dùng & layout tương đương; chỉ thay nền tảng.
- Mọi tính năng không migrate được hoàn toàn → ghi `KNOWN_ISSUES.md` + đề xuất thay thế.
