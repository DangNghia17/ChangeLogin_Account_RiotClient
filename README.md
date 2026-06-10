# Riot Account Manager

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue?style=flat-square&logo=github)](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)
[![Release](https://img.shields.io/badge/Release-v2.0.0-green?style=flat-square)](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/releases)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

## Giới thiệu

Riot Account Manager là công cụ hỗ trợ quản lý và đăng nhập nhanh vào nhiều tài khoản Riot Games (League of Legends, VALORANT, etc.). Ứng dụng giúp bạn chuyển đổi giữa các tài khoản một cách nhanh chóng và tiện lợi.

## Tính năng

- ✅ Quản lý nhiều tài khoản Riot Games trong một nơi
- ✅ Đăng nhập tự động - chỉ cần chọn tài khoản và bấm nút
- ✅ Mở và focus vào Riot Client từ ứng dụng
- ✅ Hiển thị trạng thái Riot Client (đang chạy/chưa chạy)
- ✅ Lưu trữ an toàn thông tin đăng nhập (mã hóa AES-256)
- ✅ Chuyển đổi tài khoản nhanh chóng, tiết kiệm thời gian
- ✅ Hỗ trợ đa ngôn ngữ (Tiếng Việt và Tiếng Anh)

## Lợi ích

So với đăng nhập thủ công:
- ⏱️ **Tiết kiệm thời gian**: Từ 30-60 giây → 2-3 giây
- 🎯 **Chính xác**: Không lo nhập sai username/password
- 🎨 **Tiện lợi**: Quản lý nhiều tài khoản dễ dàng
- 🔒 **An toàn**: Mã hóa và lưu trữ cục bộ
- ⚡ **Hiệu quả**: Tự động hóa quy trình đăng nhập

## An toàn và Bảo mật

### Có ảnh hưởng đến game không?

**KHÔNG.** Ứng dụng hoàn toàn an toàn:
- Chỉ sử dụng Windows API chuẩn (Robot, Process)
- KHÔNG hook process, KHÔNG inject DLL, KHÔNG can thiệp vào bộ nhớ
- Hoàn toàn tương thích với Vanguard (anti-cheat của Riot)
- Chỉ mô phỏng thao tác bàn phím/chuột như người dùng thực
- Không can thiệp vào game client hay game logic

### Có bị lộ thông tin không?

**KHÔNG.** Thông tin được bảo vệ tối đa:
- Mã hóa AES-256 cho tất cả dữ liệu nhạy cảm
- Lưu trữ cục bộ trong `%LOCALAPPDATA%\RiotAccountManager\`
- KHÔNG gửi dữ liệu lên server, KHÔNG kết nối internet
- Mã nguồn mở (MIT License) - bạn có thể kiểm tra
- Chỉ bạn mới có quyền truy cập dữ liệu trên máy tính của mình

## 📦 Tải xuống

### Phiên bản Portable (Khuyến nghị)
- **File:** `RiotAccountManager-portable.zip` hoặc `RiotAccountManager.exe`
- **Nội dung:** Ứng dụng Tauri native, nhẹ, sẵn sàng sử dụng
- **Đối tượng:** Người dùng cuối

### Installer
- **NSIS `.exe`** hoặc **WiX `.msi`** — cài đặt có shortcut

### Source code
- Clone repo hoặc tải từ GitHub — xem `README_DEV.md` để build

📥 **Tải xuống:** [Releases](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/releases)

## Cài đặt

1. Tải file **Portable Version** từ [Releases](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/releases)
2. Giải nén file ZIP
3. Chạy file `RiotAccountManager.exe` (double-click)

**Lưu ý:** KHÔNG cần cài Java. Cần Windows 10/11 với WebView2 (có sẵn trên hầu hết máy).

## Hướng dẫn sử dụng

### Bước 1: Cấu hình Riot Client
1. Mở ứng dụng
2. Bấm nút "Chọn" để chọn đường dẫn đến file `RiotClientServices.exe`
3. Đường dẫn thường là: `C:\Riot Games\Riot Client\RiotClientServices.exe`

### Bước 2: Thêm tài khoản
1. Bấm nút "Thêm tài khoản" (icon người với dấu +)
2. Nhập Username, Password, và chọn Region
3. (Tùy chọn) Thêm Ghi chú để phân biệt các tài khoản
4. Bấm "Lưu"

### Bước 3: Đăng nhập
1. Đảm bảo Riot Client đã được mở (bấm nút "Mở Riot Client" nếu chưa)
2. Chọn tài khoản từ danh sách
3. Bấm nút "Đăng nhập" (icon khóa)
4. Ứng dụng sẽ tự động điền thông tin đăng nhập
5. Kiểm tra thông tin và bấm Enter để đăng nhập

### Quản lý tài khoản
- **Sửa**: Chọn tài khoản và bấm nút "Sửa" (icon bút chì)
- **Xóa**: Chọn tài khoản và bấm nút "Xóa" (icon thùng rác)

## Yêu cầu hệ thống

- Windows 10/11
- Riot Client đã được cài đặt
- Không cần cài Java (ứng dụng Tauri native)
- WebView2 (có sẵn trên Windows 10/11)

## Hỗ trợ

Nếu gặp vấn đề, vui lòng:
1. Kiểm tra Riot Client đã được mở chưa
2. Kiểm tra đường dẫn Riot Client có đúng không
3. Đảm bảo Riot Client đang hiển thị màn hình đăng nhập
4. Tạo [issue trên GitHub](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/issues) nếu vẫn gặp vấn đề

## License

MIT License - Xem file LICENSE để biết thêm chi tiết.

## Tác giả

Riot Account Manager - Mã nguồn mở, miễn phí sử dụng.

**GitHub:** [https://github.com/DangNghia17/ChangeLogin_Account_RiotClient](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

