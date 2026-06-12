# 🎉 v3.0.0 - UX & Interface Upgrade

Phiên bản này tập trung **nâng cấp giao diện và trải nghiệm người dùng** của Riot Account Manager, đồng thời giữ nguyên sự ổn định và toàn bộ luồng nghiệp vụ hiện có (quản lý tài khoản + tự động đăng nhập Riot Client).

## ✨ Điểm nổi bật

- 🎨 Thiết kế giao diện hiện đại hơn — nút bo góc, gradient, shadow nhẹ, animation mượt.
- 🧭 Nút thao tác trực quan hơn — cụm Thêm / Sửa / Xóa chuyển sang **icon-only** kèm tooltip.
- ⚠️ Xác nhận trước khi xóa dữ liệu — hộp thoại "Hủy / Xác nhận xóa", tránh xóa nhầm.
- 👋 Hướng dẫn người dùng khi mở ứng dụng lần đầu — màn hình chào mừng thân thiện.
- 🔐 Kiểm soát trạng thái đăng nhập tốt hơn — không chạy lại quy trình khi đã đăng nhập.
- 🚪 Thêm khả năng đăng xuất nhanh — chuyển tài khoản dễ dàng.
- ✨ Cải thiện trải nghiệm sử dụng tổng thể — badge trạng thái, hiệu ứng hover/active.
- 🧹 Refactor mã nguồn để dễ bảo trì — tách component tái sử dụng, dọn CSS thừa.

## 🚀 Dành cho người dùng

- **Thao tác gọn gàng hơn:** ba nút Thêm / Sửa / Xóa giờ chỉ còn biểu tượng, vuông vắn và cân đối với nút Đăng nhập. Rê chuột vào mỗi nút sẽ hiện tooltip giải thích.
- **An toàn hơn khi xóa:** mỗi lần xóa tài khoản, ứng dụng sẽ hỏi xác nhận kèm cảnh báo *"Hành động này không thể hoàn tác."*. Chỉ khi bạn bấm **Xác nhận xóa** thì tài khoản mới bị xóa.
- **Khởi đầu dễ dàng:** lần đầu mở ứng dụng, một màn hình chào mừng sẽ hướng dẫn từng bước. Bạn có thể tích **"Không hiển thị lại"** để bỏ qua ở các lần sau.
- **Rõ ràng về trạng thái đăng nhập:** tài khoản đang đăng nhập được đánh dấu bằng badge **"Đang đăng nhập"**. Nếu bấm Đăng nhập lại đúng tài khoản đó, ứng dụng sẽ báo bạn đang đăng nhập và cho phép **Đăng xuất** thay vì làm lại từ đầu.
- **Đổi tài khoản nhanh:** sau khi Đăng xuất, bạn có thể đăng nhập tài khoản khác ngay.

## 👨‍💻 Dành cho nhà phát triển

- **Component hóa & tái sử dụng:** thêm `ConfirmDialog` (hộp thoại xác nhận dùng chung) và `LoginStatusDialog`, giúp loại bỏ phụ thuộc vào `window.confirm` của trình duyệt.
- **Quản lý trạng thái phía client:** phiên đăng nhập được theo dõi trong state của React (`session`), tách biệt và không ảnh hưởng tới logic nghiệp vụ ở backend Rust.
- **CSS sạch hơn:** dọn các CSS variable không còn dùng, gom style nút toolbar theo một hệ thống thống nhất (base `.toolbar-btn` + biến thể).
- **Đồng bộ phiên bản:** bump `2.0.0 → 3.0.0` ở tất cả nơi liên quan (root `package.json`, `app-tauri/package.json`, Cargo workspace, `Cargo.lock`, `tauri.conf.json`, About dialog).
- **Không phá vỡ build:** đã xác minh `npm run build`, `cargo check -p ram-core` và `cargo test -p ram-core` đều thành công.

## 🌟 Điểm mạnh của dự án

- Mã nguồn rõ ràng, dễ mở rộng (frontend React/TS tách bạch khỏi lõi Rust).
- Giao diện thân thiện, hiện đại, đa ngôn ngữ (vi/en).
- Trải nghiệm người dùng được cải thiện rõ rệt.
- Dễ triển khai và phát triển tiếp (Tauri đóng gói gọn, native).
- Có thể dùng làm nền tảng cho các tính năng mới (tìm kiếm, sao lưu, theme…).

## 📌 Ghi chú phát hành

Đây là bản phát hành **v3.0.0** tập trung vào nâng cao trải nghiệm người dùng và cải thiện giao diện, đồng thời duy trì tính ổn định của ứng dụng. Không có thay đổi phá vỡ (breaking change) đối với dữ liệu hay luồng đăng nhập hiện có; dữ liệu tài khoản từ các phiên bản trước vẫn được giữ nguyên.

**Kiểm thử trước phát hành**

| Kiểm tra | Lệnh | Kết quả |
|----------|------|---------|
| Build web (TypeScript + Vite) | `npm run build` | ✅ |
| Kiểm tra lõi Rust | `cargo check -p ram-core` | ✅ |
| Unit test lõi Rust | `cargo test -p ram-core` | ✅ 8 passed |

---

**Full Changelog:** xem mục Changelog trong [README.md](README.md).
