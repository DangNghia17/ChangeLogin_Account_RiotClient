# Báo cáo nâng cấp — v3.1.0 (Encrypted Backup & Theme)

Tài liệu này tổng hợp các thay đổi của bản **v3.1.0**: bổ sung **Import/Export & sao lưu dữ liệu có mã hóa** và **hệ thống Theme Sáng/Tối**. Bản nâng cấp giữ nguyên toàn bộ chức năng hiện có và tương thích ngược với dữ liệu cũ.

> Bản này được xây dựng tiếp nối trên v3.0.0 (UX & Interface Upgrade).

---

## 1. Danh sách file đã sửa / thêm mới

### Thêm mới
| File | Mục đích |
|------|----------|
| `app-tauri/src/lib/backup.ts` | Lõi mã hóa/giải mã backup (AES-256-GCM + PBKDF2) + định dạng envelope, lỗi có phân loại. |
| `app-tauri/src/dialogs/BackupDialog.tsx` | Luồng UI Export/Import nhiều bước (mật khẩu, chọn file, xác nhận ghi đè, báo lỗi). |
| `app-tauri/src/theme/ThemeContext.tsx` | Context quản lý Theme (light/dark/system), lưu lựa chọn, theo dõi thay đổi OS. |
| `REPORT_v3.1.0.md` | Báo cáo này. |

### Đã chỉnh sửa
| File | Thay đổi chính |
|------|----------------|
| `app-tauri/src-tauri/src/commands.rs` | Thêm command `replace_accounts`, `read_text_file`, `write_text_file`. |
| `app-tauri/src-tauri/src/lib.rs` | Đăng ký 3 command mới vào `invoke_handler`. |
| `app-tauri/src-tauri/capabilities/default.json` | Bổ sung quyền `dialog:allow-save` (lưu file backup). |
| `app-tauri/src/lib/api.ts` | Thêm `replaceAccounts`, `saveTextFile`, `pickAndReadTextFile` (đa nền tảng Tauri/web). |
| `app-tauri/src/lib/mock-store.ts` | Thêm `replaceAccounts` cho chế độ web preview. |
| `app-tauri/src/dialogs/SettingsDialog.tsx` | Thêm khu vực **Giao diện** (segmented Light/Dark/System) và **Sao lưu & Khôi phục** (Export/Import). |
| `app-tauri/src/App.tsx` | Nút chuyển nhanh Sáng/Tối ở header; state `backupMode`; render `BackupDialog`; refresh sau import. |
| `app-tauri/src/main.tsx` | Bọc ứng dụng trong `ThemeProvider`. |
| `app-tauri/index.html` | Script áp dụng theme trước first paint (tránh nhấp nháy/FOUC). |
| `app-tauri/src/styles.css` | Bảng biến cho dark theme, chuyển màu hardcode sang biến, transition mượt, style cho theme/backup. |
| `app-tauri/src/i18n/strings.ts` | Bổ sung chuỗi cho Theme & Backup (vi + en). |
| `app-tauri/src/dialogs/AboutDialog.tsx` | Cập nhật version 3.0.0 → 3.1.0. |
| `package.json`, `app-tauri/package.json`, `Cargo.toml`, `Cargo.lock`, lock files, `tauri.conf.json` | Bump version 3.1.0. |
| `README.md` | Bổ sung tính năng, cập nhật Roadmap (đánh dấu hoàn thành) và Changelog v3.1.0. |

---

## 2. Các thay đổi chính

- **Export có mã hóa:** xuất toàn bộ dữ liệu (accounts + settings + đường dẫn Riot) ra file `.backup`, mã hóa bằng mật khẩu người dùng. Bắt buộc nhập **mật khẩu + xác nhận**; nếu không khớp sẽ không cho xuất.
- **Import có kiểm tra:** chọn file → nhập mật khẩu → giải mã → kiểm tra hợp lệ → **xác nhận ghi đè** rồi mới áp dụng. Báo lỗi rõ ràng cho từng trường hợp (sai mật khẩu, file hỏng, sai định dạng, phiên bản mới hơn).
- **Tương thích & versioning:** envelope chứa `formatVersion` + `dataVersion` + `createdAt`, cho phép phát hiện và từ chối an toàn file từ phiên bản mới hơn; không làm crash ứng dụng.
- **Theme đầy đủ:** Light / Dark / System; chuyển đổi tức thì không reload; ghi nhớ qua `localStorage`; áp dụng sớm để tránh nhấp nháy.
- **Không phá vỡ dữ liệu cũ:** không thay đổi cấu trúc lưu trữ tài khoản hiện có; chỉ thêm command ghi-đè-toàn-bộ phục vụ khôi phục.

---

## 3. Kiến trúc giải pháp

```
        ┌────────────────────────── Frontend (React/TS) ──────────────────────────┐
        │  SettingsDialog ──(Export/Import)──► App.backupMode ──► BackupDialog       │
        │                                                          │                 │
        │                          lib/backup.ts (Web Crypto)      │                 │
        │              encryptBackup() / decryptBackup()  ◄─────────┘                 │
        │                          │                                                  │
        │             lib/api.ts (platform abstraction)                               │
        │   saveTextFile / pickAndReadTextFile / replaceAccounts / get/set...         │
        └───────────────┬───────────────────────────────────┬─────────────────────┘
                        │ isTauri()                          │ web preview
                        ▼                                    ▼
        Rust commands (read/write_text_file,          mock-store + Blob download /
        replace_accounts) + plugin-dialog             <input type=file> + localStorage
                        │
                        ▼
                ram-core::store (lưu trữ mã hóa cục bộ - giữ nguyên)
```

- **Mã hóa nằm hoàn toàn ở frontend** bằng **Web Crypto API**, đúng định hướng của stack; backend chỉ đọc/ghi bytes thô.
- **`ThemeProvider`** đặt ngoài cùng cây React, gán `data-theme` lên `<html>`; CSS dùng biến `--*` để đổi màu tức thì.

## 4. Cách hoạt động của Import/Export

### Định dạng file `.backup`
```jsonc
{
  "app": "RiotAccountManager",
  "type": "backup",
  "formatVersion": 1,            // phiên bản cấu trúc envelope
  "dataVersion": 1,             // phiên bản schema payload
  "createdAt": "2026-06-12T...Z",
  "kdf":   { "name": "PBKDF2", "hash": "SHA-256", "iterations": 210000, "salt": "<base64>" },
  "cipher":{ "name": "AES-GCM", "iv": "<base64>" },
  "data":  "<base64 ciphertext>"  // JSON {accounts, settings, riotPath} đã mã hóa
}
```

### Export
1. Người dùng nhập mật khẩu + xác nhận (không khớp ⇒ dừng).
2. Sinh `salt` (16 byte) + `iv` (12 byte) ngẫu nhiên; PBKDF2-SHA256 (210k vòng) ⇒ khóa AES-256.
3. AES-GCM mã hóa payload ⇒ ghi envelope ra file (`.backup`). Mật khẩu **không** được lưu.

### Import
1. Chọn file ⇒ đọc text ⇒ parse envelope (kiểm tra `app/type/formatVersion`).
2. Nhập mật khẩu ⇒ derive khóa từ `salt` lưu trong file ⇒ AES-GCM decrypt.
3. AES-GCM tự xác thực: sai mật khẩu / dữ liệu bị sửa ⇒ ném lỗi ⇒ thông báo "sai mật khẩu hoặc file hỏng".
4. Kiểm tra payload hợp lệ ⇒ hiển thị xác nhận ghi đè ⇒ áp dụng (`replaceAccounts`, `setSettings`, `setRiotPath`) ⇒ refresh UI.

### Các luồng đã kiểm thử
| Luồng | Kết quả |
|-------|---------|
| Export thành công | ✅ (roundtrip verify qua Web Crypto) |
| Import thành công | ✅ |
| Import sai mật khẩu | ✅ bị từ chối, báo lỗi rõ ràng |
| Import file lỗi/hỏng | ✅ bị từ chối |

## 5. Cách hoạt động của Theme

- `ThemeContext` đọc `localStorage["ram.theme"]` (`light`/`dark`/`system`, mặc định `system`).
- Theme được "resolve" thành `light|dark` và gán `data-theme` lên `<html>`. Toàn bộ màu dùng biến CSS nên đổi **ngay lập tức, không reload**.
- Khi chọn `system`, lắng nghe `matchMedia("(prefers-color-scheme: dark)")` để tự đổi theo OS.
- `index.html` áp dụng theme trước khi React mount ⇒ không nhấp nháy lúc khởi động.
- Hai điểm điều khiển: nút ☀/🌙 ở header (đổi nhanh) và bộ chọn 3 lựa chọn trong Cài đặt.

| Luồng | Kết quả |
|-------|---------|
| Chuyển Light Mode | ✅ |
| Chuyển Dark Mode | ✅ |
| Theo Hệ thống (System) | ✅ |
| Ghi nhớ theme sau khởi động lại | ✅ (localStorage + apply sớm) |

## 6. Những điểm cần lưu ý

- **Mật khẩu backup không thể khôi phục:** nếu quên mật khẩu sẽ không giải mã được file (đây là chủ đích bảo mật).
- **Import là ghi đè toàn bộ** danh sách tài khoản hiện tại (sau xác nhận). Backend `ram-core` vẫn tự tạo bản sao lưu nội bộ khi ghi (`accounts.bak`).
- **Web preview** dùng tải/đọc file qua trình duyệt và `localStorage`; bản desktop dùng dialog gốc + lệnh Rust.
- **Phụ thuộc hệ thống khi build desktop:** cần GTK/WebKit (`libgtk-3-dev`, `libwebkit2gtk-4.1-dev`, …) để `cargo check`/`tauri build` chạy trên Linux.

## 7. Kiểm tra cuối cùng

| Hạng mục | Lệnh | Kết quả |
|----------|------|---------|
| TypeScript + build web | `npm run build` | ✅ |
| Kiểm tra lõi Rust | `cargo check -p ram-core` | ✅ |
| Unit test lõi Rust | `cargo test -p ram-core` | ✅ 8 passed |
| Build crate Tauri (sau khi cài GTK/WebKit) | `cargo check -p riot-account-manager` | ✅ |
| Mã hóa backup (roundtrip / sai mật khẩu / file hỏng) | Web Crypto verify | ✅ |

## 8. Đề xuất cải tiến trong tương lai

- Tự động sao lưu định kỳ (scheduled backup) ra thư mục do người dùng chọn.
- Cho phép **merge** khi import thay vì chỉ ghi đè.
- Thêm các theme/màu nhấn tùy biến và độ tương phản cao (accessibility).
- Thêm thanh đo độ mạnh mật khẩu khi đặt mật khẩu backup.
- Đồng bộ ngôn ngữ khi import (hiện chỉ lưu vào config, không đổi UI tức thì).

---

*Báo cáo cho phiên bản v3.1.0 — Encrypted Backup & Theme.*
