# MIGRATION_CHANGELOG.md

Thay đổi file trong quá trình migration Java Swing → Tauri + React + TypeScript.
App Tauri nằm ở `app-tauri/`. Mã nguồn Java đã được gỡ khỏi repo sau khi migration hoàn tất.

## File THÊM

### Tài liệu (Phase 1–4 + final)
- `docs/current-system-audit.md`
- `docs/feature-inventory.md`
- `docs/data-compatibility-analysis.md`
- `docs/tauri-migration-plan.md`
- `README_MIGRATION.md`
- `MIGRATION_CHANGELOG.md`
- `KNOWN_ISSUES.md`

### App Tauri — cấu hình
- `app-tauri/package.json`, `app-tauri/package-lock.json`
- `app-tauri/tsconfig.json`, `app-tauri/tsconfig.node.json`
- `app-tauri/vite.config.ts`, `app-tauri/index.html`
- `app-tauri/.gitignore`
- `app-tauri/Cargo.toml` (workspace), `app-tauri/Cargo.lock`

### App Tauri — Rust core (`crates/core`)
- `app-tauri/crates/core/Cargo.toml`
- `app-tauri/crates/core/src/lib.rs`
- `app-tauri/crates/core/src/model.rs`
- `app-tauri/crates/core/src/paths.rs`
- `app-tauri/crates/core/src/crypto.rs`  (legacy AES-ECB + DPAPI; unit tests tương thích Java)
- `app-tauri/crates/core/src/config.rs`
- `app-tauri/crates/core/src/store.rs`   (atomic + backup + rollback + migration; unit tests)
- `app-tauri/crates/core/src/settings.rs`
- `app-tauri/crates/core/src/startup.rs`
- `app-tauri/crates/core/src/riot.rs`

### App Tauri — vỏ Tauri (`src-tauri`)
- `app-tauri/src-tauri/Cargo.toml`
- `app-tauri/src-tauri/build.rs`
- `app-tauri/src-tauri/tauri.conf.json`
- `app-tauri/src-tauri/capabilities/default.json`
- `app-tauri/src-tauri/src/main.rs`
- `app-tauri/src-tauri/src/lib.rs`
- `app-tauri/src-tauri/src/commands.rs`
- `app-tauri/src-tauri/icons/{32x32.png,128x128.png,128x128@2x.png,icon.png,icon.ico}`

### App Tauri — Frontend (`src`)
- `app-tauri/src/main.tsx`, `app-tauri/src/App.tsx`
- `app-tauri/src/types.ts`, `app-tauri/src/vite-env.d.ts`, `app-tauri/src/styles.css`
- `app-tauri/src/lib/api.ts`
- `app-tauri/src/i18n/strings.ts`, `app-tauri/src/i18n/I18nContext.tsx`
- `app-tauri/src/components/{AccountTable,ConfigPanel,Toast}.tsx`
- `app-tauri/src/dialogs/{AccountDialog,SettingsDialog,AboutDialog,WelcomeDialog}.tsx`
- `app-tauri/src/assets/*` (icon dùng lại từ bản Java để giữ giao diện)

### CI
- `.github/workflows/tauri-release.yml` (build NSIS exe + MSI + portable zip trên windows-latest)

## File XÓA (sau migration hoàn tất)

- `src/main/java/` — mã nguồn Java Swing
- `runtime/` — JRE bundled (~21 MB)
- `pom.xml`, `MANIFEST.MF`
- `scripts/build.sh`, `scripts/build.ps1`, `scripts/package.ps1`
- `dist/launch4j-config.xml`
- `.github/workflows/release.yml` — CI build Java
- `ANALYSIS.md`, `README_UPDATE.md`, `README_FOR_DEV_EN.md`, `README_FOR_DEV_VI.md`

> Ghi chú: thư mục build tạm (`app-tauri/node_modules`, `app-tauri/dist`,
> `app-tauri/src-tauri/target`, `app-tauri/src-tauri/gen`) được gitignore, không commit.
