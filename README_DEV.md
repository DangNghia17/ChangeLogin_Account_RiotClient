# Riot Account Manager — Hướng dẫn phát triển

Tài liệu ngắn gọn cho workflow hàng ngày. App chính nằm trong `app-tauri/` (Tauri v2 + React + Rust core).

---

## Cách 1: Test giao diện nhanh nhất (Web Preview)

**Chỉ cần Node.js 18+.** Không cần Rust, không cần build Tauri.

```bash
git pull
npm install
npm run dev
```

Mở trình duyệt: **http://localhost:1420**

- UI và logic React chạy đầy đủ.
- API native (lưu account, Riot Client, auto-login…) dùng **mock** trong `localStorage`.
- Không crash, không lỗi import Tauri.

**Docker (máy mới, không muốn cài Node local):**

```bash
git pull
docker compose up
```

---

## Cách 2: Test đầy đủ Tauri (Windows)

**Cần:** Node 18+, Rust stable, WebView2 (có sẵn trên Windows 10/11).

```bash
git pull
npm install
cd app-tauri && npm run tauri:dev
```

Hoặc từ thư mục gốc:

```bash
npm run tauri:dev
```

---

## Cách 3: Build frontend (`npm run build`)

**Chỉ build giao diện React** (HTML/JS/CSS tĩnh). Không tạo file `.exe`.

```bash
git pull
npm install
npm run build
```

### Output ở đâu?

```
app-tauri/dist/
├── index.html          # trang chính
└── assets/             # JS, CSS, ảnh (đã bundle + hash tên file)
    ├── index-xxxxx.js
    ├── index-xxxxx.css
    └── *.png / *.jpg
```

### Dùng thế nào?

| Mục đích | Lệnh |
|----------|------|
| Xem bản build trên browser (mock API) | `cd app-tauri && npm run preview` → mở **http://localhost:4173** |
| Đóng gói vào app desktop | Tự động dùng khi chạy `npm run tauri:build` (Tauri đọc `app-tauri/dist/`) |
| Deploy lên web server tĩnh | Copy cả thư mục `app-tauri/dist/` lên host (chỉ UI mock, **không** có auto-login Windows) |

> `npm run build` **không** tạo app cài đặt. Muốn file `.exe`/`.msi` → xem Cách 4.

---

## Cách 4: Build release desktop (Windows)

```bash
git pull
npm install
npm run tauri:build
```

Lệnh này tự chạy `npm run build` trước, rồi đóng gói Rust + WebView.

### Output ở đâu?

```
app-tauri/target/release/
├── RiotAccountManager.exe              # portable (chạy trực tiếp)
└── bundle/
    ├── nsis/RiotAccountManager_x.x.x_x64-setup.exe   # installer NSIS
    └── msi/RiotAccountManager_x.x.x_x64_en-US.msi    # installer MSI
```

Chạy file `.exe` portable hoặc cài qua installer — đây mới là bản dùng thật (auto-login, lưu account, v.v.).

Release tự động qua GitHub Actions khi push tag `v*`.

---

## Script tiện ích

| Lệnh | Mô tả |
|------|--------|
| `npm run dev` / `npm run web` | Web preview (browser, hot reload) |
| `npm run build` | Build frontend → `app-tauri/dist/` |
| `npm run preview` *(trong `app-tauri/`)* | Xem `dist/` trên browser |
| `npm run tauri:dev` | Tauri dev (Windows) |
| `npm run tauri:build` | Build installer/portable |
| `npm run check` | TypeScript + `cargo test/check` core |
| `npm run lint` | Kiểm tra TypeScript |
| `npm run test` | Unit test Rust core (`ram-core`) |
| `npm run doctor` | Kiểm tra môi trường + gợi ý sửa lỗi |

---

## Git workflow

```bash
git pull
# sửa code
git add .
git commit -m "mô tả thay đổi"
git push
```

CI (`.github/workflows/ci.yml`) tự chạy trên mỗi PR/push `main`: TypeScript check + `cargo test -p ram-core`.

---

## Cấu trúc quan trọng

```
app-tauri/
├── src/                 # React UI
│   └── lib/api.ts       # IPC Tauri + mock web fallback
├── crates/core/         # Logic Rust (test được trên Linux/CI)
└── src-tauri/           # Vỏ Tauri + commands
```

## Khi gặp lỗi

```bash
npm run doctor
```

Doctor báo thiếu gì và cách khắc phục.
