# PROJECT_SIMPLIFICATION_REPORT.md

Báo cáo tối ưu môi trường phát triển — Riot Account Manager (Tauri v2).

---

## 1. Đã tối ưu gì

### Workflow một lệnh từ thư mục gốc
- Thêm `package.json` ở root: `npm install` → tự cài `app-tauri/`.
- `npm run dev` = web preview (Vite), không cần Rust/Tauri.

### Web Preview Mode (test UI nhanh)
- `npm run dev` / `npm run web` mở giao diện tại **http://localhost:1420**.
- Tạo `app-tauri/src/lib/api.ts` — lớp IPC thống nhất với **mock fallback** khi chạy browser.
- `mock-store.ts` lưu account/config trong `localStorage` — không crash, không lỗi import Tauri.
- `ConfigPanel` không còn import trực tiếp `@tauri-apps/plugin-dialog` (dynamic import qua `api`).

### Script tự động hóa
| Script | Chức năng |
|--------|-----------|
| `npm run check` | TypeScript + `cargo test/check` ram-core |
| `npm run lint` | `tsc --noEmit` |
| `npm run test` | `cargo test -p ram-core` (8 tests, gồm tương thích Java) |
| `npm run doctor` | Kiểm tra Node/Rust/Tauri/deps/config + gợi ý sửa |

### Docker
- `Dockerfile` + `docker-compose.yml`: `docker compose up` → web preview, không cần cài Node/Rust local.

### CI (GitHub Actions)
- `.github/workflows/ci.yml`: mỗi PR/push `main` chạy TypeScript check + `cargo test -p ram-core` + `doctor`.
- Release Windows giữ nguyên `tauri-release.yml`.

### Rust toolchain
- `rust-toolchain.toml` pin `stable` ≥ 1.85 (dependency Tauri cần edition2024).

### Tài liệu
- `README_DEV.md` — 3 cách dev (web / Tauri / release), tối đa vài lệnh mỗi cách.

---

## 2. Đã xoá gì

### Hệ Java cũ (toàn bộ)
| Thành phần | Lý do |
|------------|-------|
| `src/main/java/` | Đã migrate sang Tauri — không còn dùng |
| `runtime/` (~21 MB JRE bundled) | Không cần cho Tauri |
| `pom.xml`, `MANIFEST.MF` | Build Maven Java |
| `scripts/build.sh`, `build.ps1`, `package.ps1` | Script build Java/jpackage |
| `dist/launch4j-config.xml` | Launch4j legacy |
| `.github/workflows/release.yml` | CI build Java |
| `ANALYSIS.md`, `README_UPDATE.md`, `README_FOR_DEV_*.md` | Tài liệu Java cũ |

### Tauri dead code
| Thành phần | Lý do |
|------------|-------|
| `@tauri-apps/plugin-notification` (npm) | Đăng ký nhưng **không dùng** |
| `tauri-plugin-notification` (Rust) | Tương tự |
| Permission `notification:default` | Không còn plugin |

**Giữ lại:** `docs/` (mô tả tương thích dữ liệu Java cũ cho migration), `README_MIGRATION.md`, `KNOWN_ISSUES.md`.

---

## 3. Đã thay đổi gì

| File / khu vực | Thay đổi |
|----------------|----------|
| `package.json` (root) | Workspace wrapper + scripts delegate |
| `app-tauri/package.json` | Thêm `web`, `check`, `lint`, `test`, `doctor`, `tauri:dev/build` |
| `app-tauri/src/lib/` | **Mới** — `api.ts`, `platform.ts`, `mock-store.ts` (file `api.ts` thiếu trên nhánh migration) |
| `app-tauri/vite.config.ts` | `host: true` cho Docker/LAN |
| `.gitignore` | Thêm `node_modules`, `dist`, `target` Tauri |
| `app-tauri/Cargo.toml` | `rust-version` 1.85 |

**Không đổi:** giao diện CSS/JSX, luồng nghiệp vụ, commands Rust, mã hóa/migration dữ liệu.

---

## 4. Cách test nhanh nhất

```bash
git pull
npm install
npm run dev
```

Mở http://localhost:1420 — test UI + CRUD account (mock). Không cần Rust.

**Hoặc Docker:**

```bash
git pull
docker compose up
```

**Kiểm tra logic backend (không cần UI):**

```bash
npm run test
```

---

## 5. Cách build nhanh nhất

### `npm run build` — chỉ frontend (mọi OS)

```bash
npm install
npm run build
```

**Output:** `app-tauri/dist/` (`index.html` + `assets/`).

**Xem thử:** `cd app-tauri && npm run preview` → http://localhost:4173 (mock API, không phải app desktop).

**Lưu ý:** Đây là file tĩnh cho Vite/Tauri bundle — **không** có `.exe`. Tauri tự dùng thư mục này khi `tauri:build`.

### `npm run tauri:build` — app desktop (Windows)

```bash
npm install
npm run tauri:build
```

**Output:**
- `app-tauri/src-tauri/target/release/RiotAccountManager.exe` (portable)
- `app-tauri/src-tauri/target/release/bundle/nsis/*.exe` (installer)
- `app-tauri/src-tauri/target/release/bundle/msi/*.msi` (installer)

Hoặc push tag `vX.Y.Z` → CI build NSIS/MSI/portable tự động.

---

## 6. Những phần vẫn bắt buộc phải cài

### Web preview (`npm run dev`)
- **Node.js 18+** (duy nhất)

### Tauri dev/build đầy đủ (Windows)
- Node.js 18+
- **Rust stable ≥ 1.85** (`rustup default stable`)
- **WebView2** (có sẵn Win10/11)
- MSVC Build Tools (khi build release lần đầu)

### Chức năng Windows-only (không test được trên web/Linux)
- Auto-login (`enigo`), focus/launch Riot Client, DPAPI mã hóa, startup registry — cần máy Windows thật.

---

## Kiểm tra sau tối ưu

| Kiểm tra | Kết quả |
|----------|---------|
| `npm run check:web` | PASS |
| `npm run build` | PASS |
| `npm run test` (8 tests ram-core) | PASS |
| `npm run doctor` | PASS |
| `npm run dev` (HTTP 200) | PASS |
| Full `cargo check` Tauri trên Linux | Cần libgtk/webkit (chỉ cần trên Windows cho release) |
