# KNOWN_ISSUES.md

Liệt kê trung thực bug còn tồn tại, hạn chế hiện tại và technical debt sau migration.
**Không che giấu lỗi.**

---

## 1. Cần kiểm thử thủ công trên Windows (chưa test tự động được)

Các tính năng phụ thuộc OS sâu chỉ chạy trên Windows và **chưa được kiểm thử end-to-end** trong
môi trường phát triển (CI/agent chạy Linux, không có Riot Client + không có GUI/đầu vào thật):

| Hạng mục | Trạng thái | Cần làm |
|---|---|---|
| `auto_login` (enigo gõ phím/chuột) | Code xong, **chưa test Windows** | Chạy thử với Riot Client thật |
| Tọa độ click + **DPI scaling** với enigo | Dùng pixel vật lý từ Win32 | Xác minh ở 100/125/150% scaling |
| `focus_riot_client_window` / measure (PowerShell) | Port từ Java (đã chạy ở bản Java) | Xác minh lại trên Windows |
| DPAPI protect/unprotect | Code xong, build OK | Test trên Windows (ghi/đọc accounts.dat) |
| Startup registry (HKCU Run) | Code xong | Bật/tắt + kiểm tra khởi động lại |
| NSIS `.exe` / WiX `.msi` build | Qua CI windows-latest | Chạy CI tạo artifact thật |

> Lý do: môi trường build là Linux. Đã verify được: `cargo check` toàn workspace (gồm crate
> Tauri), `cargo test -p ram-core` (8/8, gồm tương thích dữ liệu Java byte-for-byte), và
> `npm run build` (TypeScript strict + Vite). Phần Windows-only chỉ verify được ở mức biên dịch.

## 2. DPI / enigo có thể lệch so với Java Robot

- Java `Robot` dùng tọa độ **logic** (đã chia theo scale) → bản Java bù DPI bằng
  `getDefaultTransform`. `enigo` trên Windows dùng tọa độ **pixel vật lý**.
- Hiện tại `riot.rs` đưa thẳng pixel vật lý từ `GetClientRect/ClientToScreen` vào enigo
  (đúng về lý thuyết cho enigo). **Nhưng chưa kiểm chứng** ở các mức scaling khác nhau.
- **Giải pháp dự phòng nếu lệch**: thêm bù tỉ lệ theo `GetDpiForWindow`/`GetScaleFactorForMonitor`
  hoặc cho phép người dùng tinh chỉnh offset trong Settings.

## 3. Reset preferences khi nâng cấp từ bản Java

- `language` và cờ `welcome_shown` ở bản Java lưu trong **Java Preferences** (registry path
  `HKCU\Software\JavaSoft\Prefs\...`), bản Tauri **không đọc** path đó.
- Hệ quả: lần đầu chạy bản Tauri có thể **hiện lại Welcome** và **ngôn ngữ về mặc định (VI)**.
- **Không ảnh hưởng tài khoản** (account nằm trong `accounts.dat`, migrate đầy đủ).
- Giải pháp: người dùng chọn lại ngôn ngữ 1 lần (đã lưu vào `config.json` + `localStorage`).

## 4. Downgrade về bản Java sau khi đã migrate sang DPAPI

- Sau khi bản Tauri migrate `accounts.dat` sang định dạng DPAPI (`RAM2:`), **bản Java cũ không
  đọc được** định dạng này.
- Giảm thiểu: trước khi migrate, bản Tauri **tạo `accounts.dat.bak`** (định dạng ECB cũ).
  Nếu cần quay lại bản Java: khôi phục `accounts.dat` từ `accounts.dat.bak`.
- Technical debt: chưa có UI "export/downgrade" tự động; hiện là thao tác file thủ công.

## 5. Fallback machine-id có thể không khớp giữa Java và Rust

- Khi đọc được Windows **MachineGuid** (trường hợp thường gặp): Java và Rust tạo **cùng key** →
  giải mã dữ liệu cũ thành công.
- Khi **không** đọc được MachineGuid (hiếm): Java fallback = `user.name + os.name + os.version`;
  Rust fallback = `USERNAME + os` (không thể tái tạo `os.name/os.version` của Java chính xác).
  → Trong trường hợp fallback này, key có thể lệch và **không giải mã được dữ liệu cũ**.
- Đánh giá: xác suất thấp (MachineGuid gần như luôn đọc được trên Windows). Đã ghi nhận để
  theo dõi; nếu cần, có thể thêm tùy chọn nhập/khôi phục.

## 6. WebView2 runtime

- App Tauri cần **WebView2** runtime. Windows 10 (bản mới) và Windows 11 có sẵn.
- Trên Windows 10 rất cũ thiếu WebView2: NSIS installer mặc định kèm bootstrapper tải WebView2.
  Bản **portable** (.exe đơn lẻ) giả định WebView2 đã có; nếu thiếu cần cài WebView2 Evergreen.

## 7. Technical debt / hạng mục tương lai

- Chưa có "remember window state" (lưu kích thước/vị trí cửa sổ giữa các phiên) — bản Java cũng
  chỉ đặt vị trí cố định góc trên-phải. Có thể thêm bằng `tauri-plugin-window-state`.
- Toàn bộ danh sách account vẫn ghi lại mỗi lần thay đổi (như bản Java) — chấp nhận với số lượng nhỏ.
- Chưa có test tự động cho frontend (React) — hiện chỉ có type-check + build. Có thể thêm Vitest.
- Notification hiện dùng Toast trong app; có thể bổ sung native OS notification qua
  `tauri-plugin-notification` cho thông báo nền.
- Bản Java cũ vẫn nằm trong repo — cần quyết định thời điểm gỡ sau khi bản Tauri được kiểm thử trên Windows.

## 8. Bug đã biết: chưa phát hiện lỗi logic trong phạm vi test hiện tại

- `cargo test -p ram-core`: 8/8 PASS (crypto tương thích Java, store atomic/backup/migration).
- `cargo check` toàn workspace: PASS (không lỗi, không warning).
- `npm run build`: PASS (TypeScript strict, không lỗi).
- Chưa có lỗi runtime nào được quan sát; tuy nhiên runtime Windows chưa được test (mục 1).
