# ANALYSIS.md — Phân tích kiến trúc hiện tại (Riot Account Manager)

> Tài liệu này được tạo **trước khi sửa bất kỳ dòng code nào**, theo yêu cầu.
> Mục tiêu: mô tả đầy đủ kiến trúc hiện tại, vị trí lưu dữ liệu người dùng,
> cơ chế mã hóa, luồng hoạt động, và **đánh giá nguy cơ mất dữ liệu khi update**.
>
> Trạng thái phân tích: tại commit `e143ad9` trên nhánh `main`.

---

## 1. Tổng quan

Riot Account Manager là ứng dụng **Java Swing** (desktop, Windows-only) giúp quản lý
nhiều tài khoản Riot Games và tự động điền thông tin đăng nhập vào cửa sổ Riot Client.

- **Ngôn ngữ:** Java (pom.xml đặt `source/target = 8`, README ghi yêu cầu Java 11).
- **UI:** Java Swing (không dùng FX, không dùng thư viện UI ngoài).
- **Build:** Maven (`pom.xml`) với `maven-shade-plugin` → fat JAR. Phát hành `.exe`
  bằng **Launch4j** (`dist/launch4j-config.xml`) + JRE tùy biến trong `runtime/`.
- **Phụ thuộc runtime ngoài:** `org.json:json:20231013` (JSON serialize).
- **Hệ điều hành đích:** Windows 10/11 (dùng `reg`, `tasklist`, `powershell`, `java.awt.Robot`).

### 1.1. Cây mã nguồn

```
src/main/java/com/riotaccountmanager/
├── MainForm.java          # JFrame chính — UI + điều phối sự kiện
├── MainForm_temp.java     # ⚠️ DEAD CODE: cũng khai báo "public class MainForm"
├── Account.java           # Model: username/password/region/note
├── AccountManager.java    # Đọc/ghi danh sách account (accounts.dat, mã hóa)
├── AccountDialog.java     # Dialog thêm/sửa account
├── ConfigManager.java     # Đọc/ghi config.json (đường dẫn Riot Client)
├── EncryptionHelper.java  # AES-256/ECB, key = SHA-256(MachineGuid)
├── AutoLoginHelper.java   # Tự động đăng nhập (Robot + PowerShell/Win32)
├── WindowHelper.java      # ⚠️ DEAD CODE: không nơi nào tham chiếu
├── LanguageManager.java   # Đa ngôn ngữ VI/EN (ResourceBundle + Preferences)
├── UIHelper.java          # Factory cho component Swing (button, field, icon)
├── UITheme.java           # Hằng số màu/font
├── GradientPanel.java     # Panel nền gradient (header)
├── WelcomeDialog.java     # Dialog hướng dẫn lần đầu
└── AboutDialog.java       # Dialog "Thông tin"
src/main/resources/
├── messages_vi.properties / messages_en.properties   # i18n
└── *.png / *.jpg                                       # icon
```

---

## 2. Luồng hoạt động của ứng dụng

1. `MainForm.main()` set System Look&Feel → tạo `MainForm` → đặt cửa sổ ở **góc trên-phải** màn hình.
2. `MainForm` khởi tạo `AccountManager` (tự load `accounts.dat`), dựng UI 3 phần:
   - **Header** (gradient): tiêu đề + combo ngôn ngữ + nút About.
   - **Center** (card): bảng account (`JTable`) + nút Thêm/Sửa/Xóa/Đăng nhập.
   - **South** (card): đường dẫn Riot Client + nút "Chọn"/"Mở Riot Client" + label trạng thái.
3. Một `javax.swing.Timer` 3 giây liên tục gọi `updateRiotClientStatus()` (spawn thread →
   `tasklist` + `powershell`) để cập nhật trạng thái Riot Client.
4. Lần chạy đầu tiên hiển thị `WelcomeDialog` (lưu cờ qua `Preferences`).

### 2.1. Luồng đăng nhập (`performLogin` → `AutoLoginHelper.autoLogin`)

1. Người dùng chọn 1 dòng account → bấm nút Login.
2. Hiện `JDialog` tiến trình (non-modal) "Đang xử lý".
3. Thread nền:
   - Kiểm tra Riot Client có chạy (`isRiotClientRunning` qua `tasklist`).
   - Nếu cửa sổ ẩn (system tray) → `focusRiotClientWindow()` (PowerShell + Win32 API).
   - `autoLogin(account, clientPath)`:
     1. Re-check running + window visible.
     2. `focusRiotClientWindow()` để đưa cửa sổ lên foreground.
     3. `getRiotClientWindowBounds()` — chạy script PowerShell sinh tạm, dùng
        `GetWindowRect`/`GetClientRect`/`ClientToScreen` để lấy **client area**.
     4. Tính toạ độ click **theo tỉ lệ %** của client area
        (`usernameXRatio ≈ 13.3%`, `usernameYRatio = 25%`), clamp vào trong cửa sổ.
     5. `Robot`: ESC → mouseMove → click → Ctrl+A/Delete (xóa username) → gõ username
        (`typeChar`) → Tab → Ctrl+A/Delete (xóa password) → gõ password.
4. Kết thúc: đóng dialog tiến trình → hiện **`JOptionPane` chặn** "Đã điền thành công,
   bấm Enter để đăng nhập" (người dùng **phải bấm OK** mới đóng được — UX kém).

### 2.2. Tương tác với Riot Client

- **Phát hiện tiến trình:** `tasklist` lọc `riotclientservices.exe` / "riot client".
- **Phát hiện cửa sổ:** PowerShell `Get-Process | MainWindowTitle -like '*Riot*'`.
- **Focus/restore:** script PowerShell lớn nhúng C# (`Add-Type`) gọi
  `SetForegroundWindow`, `ShowWindow`, `BringWindowToTop`, `EnumWindows`, `PostMessage`…
- **Đo cửa sổ:** script PowerShell khác trả `x,y,width,height` của client area.
- **Nhập liệu:** `java.awt.Robot` mô phỏng phím/chuột (không inject, tương thích Vanguard).

> Không có hook/inject/đọc bộ nhớ — chỉ mô phỏng input người dùng (an toàn anti-cheat).

---

## 3. Vị trí & format lưu dữ liệu người dùng

> **Đây là phần quan trọng nhất cho yêu cầu "không mất dữ liệu".**

### 3.1. Thư mục dữ liệu

```
%LOCALAPPDATA%\RiotAccountManager\
├── accounts.dat   # Danh sách account — ĐÃ MÃ HÓA (nhạy cảm)
└── config.json    # Đường dẫn Riot Client (plaintext JSON)
```

- Hằng số trong code: `System.getenv("LOCALAPPDATA") + "\\RiotAccountManager"`
  (lặp lại ở cả `AccountManager` và `ConfigManager`).
- Thư mục được tạo tự động (`mkdirs`) nếu chưa tồn tại.
- Ngoài ra, **language preference** và **cờ "đã xem Welcome"** lưu ở **Windows Registry**
  qua `java.util.prefs.Preferences` (node theo package `com.riotaccountmanager`,
  tức `HKCU\Software\JavaSoft\Prefs\com\riotaccountmanager`).

### 3.2. Format `accounts.dat`

- **Trên đĩa:** một chuỗi **Base64** (ciphertext AES). Toàn bộ file là 1 chuỗi, không xuống dòng.
- **Sau giải mã:** một **JSON array**:

```json
[
  { "username": "user1", "password": "pass1", "region": "VN", "note": "" },
  { "username": "user2", "password": "pass2", "region": "NA", "note": "smurf" }
]
```

- Ghi bằng `FileWriter` (encoding mặc định của hệ thống — ⚠️ rủi ro, xem mục 6),
  đọc bằng `Files.readAllBytes` + `new String(..., "UTF-8")`.
- Không có **trường version** trong dữ liệu → khó migrate có kiểm soát.

### 3.3. Format `config.json`

```json
{ "riotClientPath": "C:\\Riot Games\\Riot Client\\RiotClientServices.exe" }
```

- Plaintext, ghi bằng `json.toString(2)`.

---

## 4. Cơ chế mã hóa (`EncryptionHelper`)

| Thành phần | Giá trị hiện tại |
|---|---|
| Thuật toán | **AES** |
| Transformation | **`AES/ECB/PKCS5Padding`** |
| Độ dài khóa | 256-bit (SHA-256 digest) |
| Nguồn khóa | `SHA-256( MachineGuid )` |
| MachineGuid | đọc từ `reg query HKLM\SOFTWARE\Microsoft\Cryptography /v MachineGuid` |
| Encode ciphertext | Base64 |

### 4.1. Cơ chế đọc/giải mã

1. `getMachineId()` chạy `reg query …MachineGuid`, parse dòng chứa `MachineGuid`,
   lấy token cuối làm machine id.
   - **Fallback** (nếu lỗi): `user.name + os.name + os.version`.
   - **Fallback cuối:** chuỗi cố định `"default-machine-id"`.
2. `getSecretKey()` = `SecretKeySpec( SHA-256(machineId), "AES" )`.
3. `decrypt()` = AES/ECB decrypt (Base64 decode → doFinal → UTF-8 string).

### 4.2. Đặc tính bảo mật (đánh giá)

- ✅ Dữ liệu **gắn theo máy** (machine-bound): copy `accounts.dat` sang máy khác → không giải mã được
  (MachineGuid khác). Đây là tính năng người dùng đang dựa vào.
- ⚠️ **ECB mode là điểm yếu mật mã**: cùng plaintext block → cùng ciphertext block, lộ pattern.
  Nên thay bằng AES/GCM (hoặc CBC) + IV ngẫu nhiên — **nhưng đổi format = rủi ro tương thích** (mục 6).
- ⚠️ **Fallback machine id** rất mong manh: nếu `reg query` thất bại ở lần ghi đầu nhưng thành công ở lần đọc sau
  (hoặc ngược lại), key thay đổi → **không giải mã được dữ liệu cũ** → mất dữ liệu trên thực tế.
  Đây là một nguy cơ tiềm ẩn cần xử lý cẩn thận.

---

## 5. Quy trình build & phát hành hiện tại

- **Build JAR:** Maven `package` (shade) hoặc `javac` thủ công (xem README_FOR_DEV).
- **Đóng gói EXE:** Launch4j wrap (`dontWrapJar=true`) trỏ tới `RiotAccountManager.jar`
  + JRE trong `runtime/` (minVersion 11). Cần phát hành đủ 3 thành phần: `exe + jar + runtime/`.
- **Phát hành:** Portable ZIP (~26MB, kèm JRE) và Full Source ZIP (~46MB).
- ⚠️ **Người dùng phải tự cài Java 11** ở một số luồng (theo yêu cầu cần loại bỏ).
- ⚠️ **Build hiện tại có thể GÃY** vì `MainForm_temp.java` khai báo trùng `public class MainForm`
  (xem mục 6.1) — `javac *.java` sẽ lỗi "duplicate class".

---

## 6. ĐÁNH GIÁ NGUY CƠ MẤT DỮ LIỆU KHI UPDATE

> Yêu cầu ưu tiên #1: **người dùng cài bản mới, mọi tài khoản cũ vẫn hoạt động**.

### 6.1. Các nguy cơ đã xác định

| # | Nguy cơ | Mức độ | Mô tả |
|---|---|---|---|
| R1 | **Đổi thuật toán/khóa mã hóa** | 🔴 Cao | Nếu bản mới đổi transformation (ECB→GCM) hoặc cách derive key, `accounts.dat` cũ **không giải mã được** → mất toàn bộ account. |
| R2 | **Đổi vị trí/tên file dữ liệu** | 🔴 Cao | Nếu đổi `%LOCALAPPDATA%\RiotAccountManager\accounts.dat` sang chỗ khác mà không migrate → app mới không thấy dữ liệu cũ. |
| R3 | **Đổi schema JSON** (tên field) | 🟠 Trung bình | Đổi `username/password/region/note` → đọc account cũ ra rỗng. |
| R4 | **Ghi đè file không an toàn** | 🟠 Trung bình | `FileWriter` ghi trực tiếp; nếu crash giữa chừng → file hỏng, mất dữ liệu. Không có atomic write/backup. |
| R5 | **Encoding khi ghi** | 🟡 Thấp-TB | `new FileWriter(...)` dùng charset mặc định OS. Ciphertext là Base64 (ASCII) nên hiện tại an toàn, nhưng nếu sau này ghi text khác → rủi ro. |
| R6 | **MachineGuid không đọc được** | 🟠 Trung bình | Fallback đổi key (mục 4.2) → giải mã thất bại trên cùng một máy. |
| R7 | **Mất Preferences (Registry)** | 🟡 Thấp | Ngôn ngữ / cờ welcome lưu ở Registry; reset chỉ gây hiện lại welcome, không mất account. |
| R8 | **`MainForm_temp.java` làm gãy build** | 🟡 Thấp (gián tiếp) | Không mất dữ liệu trực tiếp, nhưng cản trở phát hành bản vá an toàn. |

### 6.2. Nguyên tắc bắt buộc cho bản nâng cấp (để KHÔNG mất dữ liệu)

1. **Giữ nguyên đường dẫn dữ liệu** `%LOCALAPPDATA%\RiotAccountManager\` và tên file `accounts.dat`, `config.json`.
2. **Giữ khả năng GIẢI MÃ format cũ** (AES/ECB + SHA-256(MachineGuid)) — đọc được mọi dữ liệu hiện hữu.
   - Nếu nâng cấp lên GCM: phải **tự nhận diện** format cũ (legacy) và giải mã được, chỉ ghi format mới sau khi đã đọc thành công.
   - Cân nhắc giữ tương thích 2 chiều (bản cũ vẫn đọc được) để tránh rủi ro khi người dùng rollback.
3. **Atomic write**: ghi ra file tạm rồi `move` đè (giảm rủi ro R4).
4. **Backup trước khi migrate/ghi đè format**: copy `accounts.dat` → `accounts.dat.bak` (và bản theo version).
5. **Rollback tự động**: nếu migrate/giải mã/ghi lỗi → khôi phục từ backup, không xóa dữ liệu gốc.
6. **Thêm trường `version`** vào file dữ liệu mới để các bản sau migrate có kiểm soát.
7. **Không yêu cầu người dùng nhập lại / migrate thủ công** — mọi thứ tự động, im lặng, an toàn.
8. **Giữ derive-key ổn định**: ưu tiên dùng đúng cách lấy MachineGuid như cũ để key không đổi.

### 6.3. Kết luận

Cấu trúc dữ liệu hiện tại **đơn giản và ổn định**; rủi ro mất dữ liệu chủ yếu đến từ việc
**vô tình thay đổi** khóa mã hóa, vị trí file, hoặc schema. Bản nâng cấp sẽ:

- Tách logic đường dẫn/đọc-ghi/mã hóa thành module rõ ràng nhưng **giữ nguyên hành vi đọc dữ liệu cũ**.
- Thêm lớp an toàn (atomic write + backup + rollback + version) **mà không phá vỡ tương thích ngược**.
- Mọi thay đổi format (nếu có) đều kèm migration tự động + backup + fallback đọc legacy.

Chỉ sau khi tài liệu phân tích này hoàn tất, công việc chỉnh sửa code mới bắt đầu.
