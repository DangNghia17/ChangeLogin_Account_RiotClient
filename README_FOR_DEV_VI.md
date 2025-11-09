# Riot Account Manager - Tài liệu Developer

[![GitHub](https://img.shields.io/badge/GitHub-Repository-blue?style=flat-square&logo=github)](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

**Repository:** [https://github.com/DangNghia17/ChangeLogin_Account_RiotClient](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

## Tổng quan

Riot Account Manager là ứng dụng desktop Java Swing để quản lý và đăng nhập tự động vào nhiều tài khoản Riot Games. Ứng dụng sử dụng Java 11+, Swing UI, và mã hóa AES-256 để bảo vệ dữ liệu người dùng.

## Kiến trúc

### Cấu trúc Project

```
src/main/java/com/riotaccountmanager/
├── MainForm.java              # Form chính, quản lý UI và tương tác người dùng
├── Account.java               # Model class đại diện cho tài khoản
├── AccountManager.java        # Quản lý danh sách tài khoản, lưu/đọc từ file
├── AccountDialog.java         # Dialog thêm/sửa tài khoản
├── ConfigManager.java         # Quản lý cấu hình (đường dẫn Riot Client)
├── EncryptionHelper.java      # Mã hóa/giải mã dữ liệu bằng AES-256
├── AutoLoginHelper.java       # Tự động hóa đăng nhập vào Riot Client
├── WindowHelper.java          # Helper để tương tác với Windows API
├── LanguageManager.java       # Quản lý đa ngôn ngữ (VI/EN)
├── UIHelper.java              # Helper cho UI components
├── UITheme.java               # Theme và màu sắc
├── GradientPanel.java         # Panel với gradient background
├── WelcomeDialog.java         # Dialog chào mừng
└── AboutDialog.java           # Dialog thông tin ứng dụng
```

### Components chính

#### 1. MainForm
- Form chính của ứng dụng
- Quản lý danh sách tài khoản (JTable)
- Xử lý sự kiện đăng nhập, thêm/sửa/xóa tài khoản
- Quản lý cấu hình Riot Client path
- Hiển thị trạng thái Riot Client

#### 2. AccountManager
- Quản lý danh sách tài khoản
- Lưu/đọc từ file `accounts.dat` (đã mã hóa)
- Sử dụng JSON để serialize dữ liệu
- Tự động mã hóa/giải mã khi lưu/đọc

#### 3. EncryptionHelper
- Mã hóa AES-256 với key từ Machine ID
- Sử dụng Windows Registry để lấy Machine GUID
- Mã hóa/giải mã dữ liệu trước khi lưu/đọc file

#### 4. AutoLoginHelper
- Kiểm tra Riot Client có đang chạy không
- Tự động restore window từ system tray
- Tính toán vị trí click chính xác dựa trên window bounds
- Mô phỏng thao tác bàn phím/chuột để đăng nhập

#### 5. LanguageManager
- Quản lý đa ngôn ngữ (VI/EN)
- Load resource bundles từ `messages_vi.properties` và `messages_en.properties`
- Lưu preference ngôn ngữ vào Java Preferences API
- Hỗ trợ UTF-8 encoding

## Công nghệ sử dụng

- **Java 11+**: Ngôn ngữ lập trình chính
- **Java Swing**: GUI framework
- **JSON (org.json)**: Serialize dữ liệu tài khoản
- **AES-256**: Mã hóa dữ liệu nhạy cảm
- **Windows API**: Tương tác với Riot Client (Robot, Process)
- **Java Preferences API**: Lưu cấu hình người dùng
- **ResourceBundle**: Quản lý đa ngôn ngữ

## Cấu trúc dữ liệu

### Account
```java
public class Account {
    private String username;
    private String password;
    private String region;
    private String note;
}
```

### File lưu trữ
- **accounts.dat**: File chứa danh sách tài khoản đã mã hóa (JSON)
- **config.json**: File cấu hình (đường dẫn Riot Client)
- **Location**: `%LOCALAPPDATA%\RiotAccountManager\`

## Build và Chạy

### Yêu cầu
- Java JDK 11+
- Maven (tùy chọn) hoặc javac trực tiếp
- Windows 10/11

### Build từ source
```bash
# Compile
javac -encoding UTF-8 -d build/classes -cp "lib/json-20231013.jar" src/main/java/com/riotaccountmanager/*.java

# Copy resources
cp -r src/main/resources/* build/classes/

# Tạo JAR
jar cfm RiotAccountManager.jar MANIFEST.MF -C build/classes .
```

### Chạy ứng dụng
```bash
java -jar RiotAccountManager.jar
```

### Build file thực thi (.exe)

Ứng dụng sử dụng Launch4j để tạo file thực thi Windows. Sau khi build JAR file, sử dụng Launch4j để tạo `.exe`:

1. **Yêu cầu:**
   - Launch4j đã cài đặt
   - File JAR đã được biên dịch
   - Custom JRE runtime (tạo bằng `jlink`)

2. **Cấu trúc file khi phân phối:**
   ```
   RiotAccountManager/
   ├── RiotAccountManager.exe    # File thực thi chính (tạo bởi Launch4j)
   ├── RiotAccountManager.jar    # File JAR (bắt buộc cho exe)
   └── runtime/                  # Thư mục JRE tùy chỉnh (bắt buộc cho exe)
       ├── bin/
       ├── lib/
       └── ...
   ```

3. **Lưu ý quan trọng:**
   - File `.exe` yêu cầu cả file JAR và thư mục `runtime/` trong cùng một thư mục
   - Cả 3 thành phần (exe, jar, runtime) phải được đóng gói khi phân phối
   - Thư mục `runtime/` chứa JRE tùy chỉnh được tạo bằng `jlink` (nhỏ hơn JRE đầy đủ)
   - Cấu hình Launch4j nằm trong `dist/launch4j-config.xml`

## Bảo mật

### Mã hóa
- Sử dụng AES-256 với key từ Machine ID
- Key được tạo từ Windows Machine GUID (SHA-256 hash)
- Dữ liệu được mã hóa trước khi lưu vào file
- Chỉ có thể giải mã trên cùng một máy tính

### An toàn
- KHÔNG gửi dữ liệu lên server
- KHÔNG kết nối internet
- Dữ liệu chỉ lưu trữ cục bộ
- Mã nguồn mở, có thể kiểm tra

## Tương thích

### Riot Client
- Hỗ trợ Riot Client mới nhất
- Tương thích với Vanguard (anti-cheat)
- Chỉ sử dụng Windows API chuẩn
- Không can thiệp vào process hoặc memory

### Hệ điều hành
- Windows 10
- Windows 11
- Không hỗ trợ Linux/Mac (sử dụng Windows API)

## Phát triển

### Thêm tính năng mới
1. Tạo branch mới từ `main`
2. Phát triển tính năng
3. Test kỹ lưỡng
4. Tạo Pull Request

### Coding Standards
- Sử dụng Java naming conventions
- Code không có comments (đã xóa để clean code)
- Sử dụng UTF-8 encoding
- Format code theo chuẩn Java

## License

MIT License - Xem file LICENSE để biết thêm chi tiết.

## Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng tạo [Issue](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/issues) hoặc [Pull Request](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient/pulls) trên GitHub.

**Repository:** [https://github.com/DangNghia17/ChangeLogin_Account_RiotClient](https://github.com/DangNghia17/ChangeLogin_Account_RiotClient)

