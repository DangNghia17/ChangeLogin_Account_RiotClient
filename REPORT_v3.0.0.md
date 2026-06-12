# Báo cáo nâng cấp — v3.0.0 (UX & Interface Upgrade)

Tài liệu này tổng hợp các thay đổi đã thực hiện trong lần nâng cấp lên **v3.0.0**, các vấn đề phát hiện trong quá trình rà soát, và đề xuất cải tiến trong tương lai.

> **Ghi chú về phạm vi:** Mô tả nhiệm vụ ban đầu mang tính khuôn mẫu (có nhắc đến "đăng nhập Google"). Dự án thực tế là **Riot Account Manager** — đăng nhập ở đây là **tự động điền thông tin vào Riot Client**, không phải đăng nhập Google. Các yêu cầu đã được diễn giải và áp dụng phù hợp với ngữ cảnh thật của ứng dụng, đồng thời tôn trọng nguyên tắc *không phá vỡ luồng nghiệp vụ hiện có*.

---

## 1. Các file đã sửa / thêm mới

### Thêm mới
| File | Mục đích |
|------|----------|
| `app-tauri/src/components/ConfirmDialog.tsx` | Hộp thoại xác nhận in-app dùng chung (thay `window.confirm`). |
| `app-tauri/src/dialogs/LoginStatusDialog.tsx` | Hộp thoại "Đang đăng nhập" + nút Đăng xuất. |
| `RELEASE_v3.0.0.md` | Ghi chú phát hành cho GitHub Releases. |
| `REPORT_v3.0.0.md` | Báo cáo này. |

### Đã chỉnh sửa
| File | Thay đổi chính |
|------|----------------|
| `app-tauri/src/App.tsx` | Toolbar icon-only; state `session`, `pendingDelete`, `showLoginStatus`; logic xác nhận xóa, kiểm tra đã đăng nhập, đăng xuất; render dialog mới. |
| `app-tauri/src/components/AccountTable.tsx` | Thêm prop `loggedInUsername` + badge "Đang đăng nhập". |
| `app-tauri/src/dialogs/WelcomeDialog.tsx` | Nội dung chào mừng thân thiện (tiêu đề, các bước, mục Lưu ý). |
| `app-tauri/src/dialogs/AboutDialog.tsx` | Cập nhật version hiển thị 2.0.0 → 3.0.0. |
| `app-tauri/src/i18n/strings.ts` | Bổ sung key cho xác nhận xóa, trạng thái đăng nhập/đăng xuất, badge, nội dung welcome mới (vi + en). |
| `app-tauri/src/styles.css` | Thiết kế lại nút toolbar (icon-only, gradient, hover/active/animation, shadow), tooltip tùy biến, style cho ConfirmDialog/LoginStatusDialog/badge/welcome-notes; dọn CSS variable thừa. |
| `app-tauri/package.json`, `app-tauri/package-lock.json` | Bump version 3.0.0. |
| `app-tauri/Cargo.toml`, `app-tauri/Cargo.lock` | Bump version workspace/crate 3.0.0. |
| `app-tauri/src-tauri/tauri.conf.json` | Bump version 3.0.0. |
| `package.json`, `package-lock.json` (root) | Bump version 3.0.0. |
| `README.md` | Viết lại chuẩn dự án mã nguồn mở + Changelog v3.0.0. |

---

## 2. Các thay đổi chính (theo yêu cầu)

### ✅ 1. Thiết kế lại cụm nút Thêm / Sửa / Xóa
- Chuyển sang **icon-only** (bỏ chữ trên nút).
- **Tooltip** tùy biến khi hover (`data-tooltip`) + `aria-label` cho accessibility.
- Nút vuông `46×46`, bo góc `14px`, đồng đều chiều cao với nút Đăng nhập; khoảng cách `gap: 10px`, padding cân đối.
- Hiệu ứng **hover** (nâng nhẹ + tăng shadow), **active** (scale 0.97), **animation** transition mượt, icon căn giữa, gradient màu (Thêm: xanh lá, Sửa: cam, Xóa: đỏ), shadow nhẹ.

### ✅ 2. Xác nhận trước khi xóa
- Thay `window.confirm` bằng `ConfirmDialog` với 2 nút **Hủy** / **Xác nhận xóa** và dòng cảnh báo *"Hành động này không thể hoàn tác."*. Chỉ xóa khi người dùng xác nhận.

### ✅ 3. Màn hình chào mừng lần đầu
- `WelcomeDialog` hiển thị ở lần chạy đầu (first-run), nội dung gồm tiêu đề 🎉, các bước hướng dẫn, và mục **Lưu ý** (sao lưu định kỳ, không chia sẻ tài khoản).
- Có checkbox **"Không hiển thị lại"**; trạng thái lưu qua `localStorage` (`ram.welcome_shown`).

### ✅ 4. Xử lý nút Đăng nhập khi đã đăng nhập
- Theo dõi phiên đăng nhập phía client (state `session` = username của tài khoản vừa đăng nhập).
- Khi bấm Đăng nhập đúng tài khoản đang trong phiên → hiện `LoginStatusDialog` thông báo *"Bạn đang đăng nhập bằng tài khoản: …"* + *"Không cần đăng nhập lại."* kèm nút **Đăng xuất**, thay vì chạy lại quy trình.
- **Đăng xuất** xóa phiên, cập nhật UI (badge biến mất), hiển thị toast; sau đó có thể đăng nhập tài khoản khác. Việc đăng nhập sang tài khoản khác vẫn hoạt động bình thường (đặc thù của ứng dụng đổi tài khoản).

### ✅ 5. README v3.0.0
- Viết lại đầy đủ: Giới thiệu, Tính năng nổi bật, Công nghệ, Cài đặt, Chạy dự án, Build, Cấu trúc project, Roadmap, License, Changelog (kèm mục v3.0.0).

### ✅ 6. RELEASE_v3.0.0.md
- Tạo tài liệu phát hành đầy đủ để copy lên GitHub Releases.

---

## 3. Các vấn đề phát hiện

1. **Mô tả nhiệm vụ vs. ứng dụng thực tế:** yêu cầu nhắc "đăng nhập Google", nhưng ứng dụng là Riot Account Manager (auto-login Riot Client). Đã diễn giải lại các yêu cầu cho đúng ngữ cảnh.
2. **Khái niệm "đã đăng nhập":** ứng dụng vốn không lưu phiên đăng nhập (mỗi lần Đăng nhập là một thao tác điền form vào Riot Client). Đã bổ sung theo dõi phiên **phía client** (in-memory) thuần UI — không can thiệp backend, không phá vỡ luồng cũ.
3. **CSS dead code:** phát hiện một số CSS variable không còn sử dụng (`--edit`, `--toolbar-action-min-width`, `--toolbar-login-min-width`, `--toolbar-action-border`, `--toolbar-action-shadow`) → đã xóa.
4. **Trùng tooltip:** nếu vừa dùng `title` (native) vừa dùng tooltip tùy biến sẽ hiện 2 tooltip → đã bỏ `title` ở nút icon-only, chỉ giữ tooltip tùy biến + `aria-label`.

## 4. Kiểm tra cuối cùng

| Hạng mục | Lệnh | Kết quả |
|----------|------|---------|
| TypeScript (no emit) | `npx tsc --noEmit` | ✅ Không lỗi |
| Build web | `npm run build` (tsc + vite) | ✅ Thành công |
| Rust core check | `cargo check -p ram-core` | ✅ Thành công |
| Rust core test | `cargo test -p ram-core` | ✅ 8 passed |
| Import/CSS thừa | Rà soát thủ công | ✅ Đã dọn |

> Lưu ý: `cargo build` cho lớp Taul shell (`src-tauri`) cần các thư viện hệ thống của môi trường desktop và không thuộc phạm vi kiểm thử ở môi trường CI headless này; phần lõi nghiệp vụ `ram-core` đã được kiểm tra đầy đủ. Không có thay đổi mã Rust ngoài việc bump version.

## 5. Đề xuất cải tiến trong tương lai

- Tìm kiếm / lọc / sắp xếp tài khoản trong danh sách.
- Import/Export & sao lưu dữ liệu (có mã hóa).
- Tùy biến theme sáng/tối.
- Phím tắt toàn cục để đăng nhập nhanh.
- Lưu (tùy chọn) phiên đăng nhập gần nhất để gợi ý khi mở lại app.
- Viết test cho tầng UI (ví dụ React Testing Library) để bảo vệ các luồng vừa thêm.

---

*Báo cáo cho phiên bản v3.0.0 — UX & Interface Upgrade.*
