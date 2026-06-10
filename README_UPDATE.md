# README_UPDATE.md — Bản nâng cấp Riot Account Manager (v2.0.0)

Tài liệu này tổng hợp toàn bộ thay đổi của bản nâng cấp. Xem thêm `ANALYSIS.md` để biết
phân tích kiến trúc cũ và đánh giá rủi ro dữ liệu được thực hiện **trước khi** sửa code.

---

## Tổng quan

### Kiến trúc cũ

- Toàn bộ class nằm phẳng trong 1 package `com.riotaccountmanager` (15 file).
- Trộn lẫn trách nhiệm: UI, lưu trữ, mã hóa, tự động hóa nằm cạnh nhau.
- Có **dead code làm gãy build**: `MainForm_temp.java` khai báo trùng `public class MainForm`;
  `WindowHelper.java` không được dùng.
- Lưu account: `AccountManager` ghi thẳng bằng `FileWriter` (không atomic, không backup).
- Thông báo sau đăng nhập: `JOptionPane` **chặn** — phải bấm OK mới đóng được app.
- Đăng nhập: click theo tỉ lệ pixel, **không bù DPI scaling**.
- Không có: chạy cùng Windows, tự bấm Login, cài đặt, installer gọn nhẹ.

### Kiến trúc mới

Tách thành các package theo trách nhiệm rõ ràng:

```
com.riotaccountmanager
├── App.java                  # Entry point (thin)
├── model/Account.java
├── i18n/LanguageManager.java
├── storage/
│   ├── AppPaths.java         # Nguồn chân lý duy nhất về đường dẫn dữ liệu
│   ├── CryptoService.java    # AES (tương thích ngược tuyệt đối)
│   ├── AccountStore.java     # Atomic write + backup + rollback + migration
│   ├── AppConfig.java        # config.json (path + settings)
│   └── AppSettings.java      # Settings có kiểu (startup, auto-login)
├── riot/RiotClientService.java   # Phát hiện/focus/đo cửa sổ + auto login (DPI-aware)
├── system/StartupManager.java    # Run-at-Windows-startup (HKCU Run key)
└── ui/
    ├── MainForm.java, AccountDialog.java, SettingsDialog.java,
    ├── AboutDialog.java, WelcomeDialog.java, Toast.java,
    └── UITheme.java, UIHelper.java, GradientPanel.java
```

---

## Các nâng cấp đã thực hiện

1. **Refactor toàn bộ sang kiến trúc module** (model / i18n / storage / riot / system / ui).
2. **An toàn dữ liệu (`AccountStore`)**:
   - Ghi **atomic** (ghi file tạm → `move` đè), thay `FileWriter` charset mặc định bằng UTF-8.
   - **Backup tự động** (`accounts.dat.bak`) trước mỗi lần ghi đè.
   - **Rollback** khi ghi lỗi.
   - **Migration framework** + file `data.version` riêng (không đụng vào format `accounts.dat`).
3. **Mã hóa ổn định hơn (`CryptoService`)**: cache machine id trong vòng đời tiến trình để
   tránh đổi khóa giữa chừng; giữ **nguyên** thuật toán/khóa để tương thích dữ liệu cũ.
4. **Đăng nhập ổn định hơn (`RiotClientService`)**:
   - **Bù DPI scaling**: chuyển toạ độ pixel vật lý (Win32) sang toạ độ logic của `Robot`,
     giúp click đúng ở mọi mức scaling 100/125/150%.
   - Bám theo **client area** (không phụ thuộc vị trí cửa sổ trên màn hình).
   - **Tự động gửi form** (Enter) sau khi điền — bật/tắt trong Settings.
5. **Run at Windows Startup** (`StartupManager` + Settings): đăng ký/gỡ qua HKCU Run key,
   không cần quyền admin, hoạt động trên Windows 10/11. Tự đồng bộ lại khi khởi động.
6. **Thông báo hiện đại (`Toast`)**: non-blocking, tự biến mất, **không khóa app**, không
   cần bấm OK, không cướp focus của Riot Client.
7. **Settings dialog**: bật/tắt "Khởi động cùng Windows" và "Tự động bấm Đăng nhập".
8. **UI hiện đại hơn nhưng quen thuộc**: thêm nút Settings (⚙) ở header, double-click một
   dòng để đăng nhập nhanh, toast thay dialog — giữ nguyên bố cục/luồng cũ.
9. **Đóng gói gọn (jpackage)**: tạo bản **portable** (kèm runtime) và **installer Setup.exe**,
   người dùng **không cần cài Java**.
10. **CI release**: `.github/workflows/release.yml` build & đính kèm artifact khi tạo tag.

---

## Các lỗi đã sửa

- 🔴 **Build bị gãy**: `MainForm_temp.java` khai báo trùng `public class MainForm`
  (`javac` báo "class MainForm is public, should be declared in a file named MainForm.java").
  → Đã xóa.
- 🟠 **Ghi file không an toàn**: `FileWriter` không atomic, không backup → nguy cơ hỏng/mất
  dữ liệu nếu crash khi ghi. → Atomic write + backup + rollback.
- 🟠 **Charset khi ghi** phụ thuộc OS (`new FileWriter`). → Ghi UTF-8 tường minh.
- 🟠 **Khóa mã hóa có thể đổi giữa chừng** do `getMachineId()` được gọi nhiều lần và có
  fallback khác nhau. → Cache machine id, ổn định trong toàn phiên.
- 🟡 **Dead code** `WindowHelper` (alt-tab) không dùng → xóa.
- 🟡 **Đọc account thiếu chịu lỗi**: dùng `getString` (ném lỗi nếu thiếu field). → `optString`.
- 🟡 **Rò rỉ tài nguyên**: nhiều `BufferedReader`/process stream không đóng. → try-with-resources.
- 🟡 **UX chặn**: dialog OK sau đăng nhập. → Toast non-blocking.
- 🟡 **Build artifacts bị commit** (`build/`, `RiotAccountManager.exe`) → untrack + gitignore.

---

## Các vấn đề phát hiện được

- **ECB mode** trong mã hóa là điểm yếu (lộ pattern). Đã **giữ nguyên có chủ đích** để đảm
  bảo tương thích ngược/xuôi (không mất dữ liệu, rollback an toàn). Khuyến nghị tương lai:
  thêm tầng AES/GCM tự mô tả (prefix) + migration, đọc được cả format cũ.
- **Phụ thuộc PowerShell/Win32** cho focus/đo cửa sổ: chấp nhận được vì là app Windows-only;
  đã giảm rủi ro bằng retry + DPI-aware.
- **Phụ thuộc toạ độ form login**: vẫn cần ước lượng vị trí ô username (client là web/CEF).
  Đã giảm phụ thuộc bằng client-area + DPI + auto-submit bằng Enter.
- Repo trước đây commit cả binary/build output (thực hành không tốt) → đã dọn.

---

## Thay đổi dữ liệu

- **Có thay đổi không?** Về **format `accounts.dat`: KHÔNG**. Vẫn là
  `Base64(AES/ECB(JSON array))` với đúng các field `username/password/region/note`.
- **Bổ sung**: file phụ `data.version` (theo dõi schema, **không** nằm trong `accounts.dat`),
  file `accounts.dat.bak` (backup), `config.json` có thêm khóa tùy chọn (`runAtStartup`,
  `autoClickLogin`) đọc bằng default nên không phá vỡ config cũ.
- **Cách migrate**: tự động, im lặng. Khi mở bản mới: nếu `data.version` cũ/thiếu → backup
  `accounts.dat` rồi cập nhật version (không biến đổi dữ liệu ở v1). Không cần thao tác tay.

---

## Khả năng tương thích

- **Tương thích dữ liệu cũ?** ✅ Có. Bản mới đọc trực tiếp `accounts.dat` của bản cũ.
- **Có mất dữ liệu không?** ❌ Không. Đã kiểm chứng bằng smoke test (9/9 PASS):
  - tạo/lưu/đọc lại, ký tự đặc biệt round-trip,
  - file giải mã ra **JSON array** (bản cũ vẫn đọc được → rollback an toàn),
  - backup được tạo khi ghi đè,
  - **dữ liệu định dạng cũ (legacy) load nguyên vẹn**.
- **Hệ điều hành:** Windows 10/11 (runtime). Code biên dịch/chạy kiểm thử trên mọi nền
  (đường dẫn dữ liệu fallback về `user.home` khi không có `%LOCALAPPDATA%`).

---

## Cách build

### Development

Bất kỳ nền tảng nào (chỉ để biên dịch/kiểm tra):

```bash
bash scripts/build.sh
```

Windows (tạo JAR chạy được):

```powershell
./scripts/build.ps1
# => dist/RiotAccountManager.jar
java -jar dist/RiotAccountManager.jar
```

Hoặc dùng Maven:

```bash
mvn -DskipTests clean package
# => target/riot-account-manager-2.0.0.jar (đã shade org.json)
```

### Production

```powershell
./scripts/package.ps1 -AppVersion 2.0.0
```

---

## Cách đóng gói phát hành

`scripts/package.ps1` (yêu cầu JDK 17+ có `jpackage`) tạo:

- **Portable** (khuyến nghị end-user): `dist/RiotAccountManager-<ver>-portable.zip`
  - app-image kèm **runtime bundled** (jlink). Giải nén → chạy `RiotAccountManager.exe`.
  - **Không cần cài Java.**
- **Installer (Setup.exe)**: `dist/RiotAccountManager-<ver>.exe`
  - Yêu cầu **WiX Toolset**. Có shortcut Start Menu/Desktop, chọn thư mục cài.
  - Nếu không có WiX, script vẫn tạo bản portable và bỏ qua installer (cảnh báo).
- **EXE (legacy)**: cấu hình Launch4j cũ vẫn còn ở `dist/launch4j-config.xml` (wrap JAR +
  `runtime/`). Khuyến nghị chuyển hẳn sang jpackage.

CI: đẩy tag `vX.Y.Z` → workflow build và đính kèm `.zip`/`.exe`/`.msi` vào Release.

---

## Ưu tiên đã tuân thủ

1. ✅ Không mất dữ liệu người dùng (kiểm chứng bằng test).
2. ✅ Không phá vỡ chức năng hiện có (giữ luồng & bố cục quen thuộc).
3. ✅ Nâng cấp trải nghiệm (toast, auto-submit, double-click login, startup).
4. ✅ Nâng cấp độ ổn định (atomic IO, DPI-aware, đóng tài nguyên, retry).
5. ✅ Nâng cấp giao diện (Settings, toast, gọn gàng).
6. ✅ Tối ưu/hợp lý hóa (bỏ dead code, gộp logic trùng).
7. ✅ Dễ bảo trì & mở rộng (module hóa, migration framework, build/CI script).
