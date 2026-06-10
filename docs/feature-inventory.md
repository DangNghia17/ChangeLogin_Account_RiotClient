# Feature Inventory (Phase 2)

> Liệt kê **100% tính năng** của Riot Account Manager v2.0.0 (Java). Đây là **tài liệu đối chiếu
> bắt buộc**: mọi tính năng phải được migrate hoặc ghi rõ lý do/giải pháp thay thế.
>
> Cột **Trạng thái migrate** được cập nhật trong `README_MIGRATION.md`.

## Quy ước
- ✅ Migrate đầy đủ · ⚠️ Migrate có thay đổi (ghi rõ) · ❌ Chưa/không migrate (ghi rõ + giải pháp)

---

## A. Quản lý tài khoản (Account Management)

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| A1 | **Add Account** | Dialog nhập username/password/region/note → lưu | `AccountDialog`, `AccountStore.addAccount` |
| A2 | **Edit Account** | Sửa account đang chọn | `AccountDialog`, `AccountStore.updateAccount` |
| A3 | **Delete Account** | Xóa account đang chọn (có confirm) | `MainForm`, `AccountStore.removeAccount` |
| A4 | **List Accounts (table)** | Bảng hiển thị username / region / note | `MainForm` JTable |
| A5 | **Select Account** | Chọn 1 dòng (single selection) | `MainForm` |
| A6 | **Region selection** | Combo: VN, NA, EUW, EUNE, KR, JP, BR, LAN, LAS, OCE, RU, TR | `AccountDialog.REGIONS` |
| A7 | **Note field** | Ghi chú phân biệt account | `Account.note` |
| A8 | **Row hover highlight** | Đổi màu dòng khi hover | `MainForm` renderer |

## B. Đăng nhập (Login)

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| B1 | **Quick Login (button)** | Bấm nút khóa để điền tài khoản đang chọn | `MainForm.performLogin` |
| B2 | **Quick Login (double-click)** | Double-click dòng để login | `MainForm` mouse listener |
| B3 | **Auto fill username/password** | Robot gõ username, Tab, gõ password | `RiotClientService.autoLogin` |
| B4 | **Clear fields trước khi gõ** | Ctrl+A + Delete để xóa nội dung cũ | `RiotClientService.clearField` |
| B5 | **Auto-submit (auto-click Login)** | Nhấn Enter sau khi điền (tùy chọn) | `RiotClientService` + `AppSettings.autoClickLogin` |
| B6 | **DPI-aware click** | Bù scaling physical→logical | `RiotClientService.toLogical` |
| B7 | **Special character typing** | Hỗ trợ @ . _ - và chữ/số | `RiotClientService.typeChar` |
| B8 | **Progress feedback** | Hiện trạng thái "checking/restoring/ready" | `MainForm.performLogin` |

## C. Riot Client Integration

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| C1 | **Detect Riot Client running** | tasklist tìm process | `RiotClientService.isRiotClientRunning` |
| C2 | **Detect window visible / in tray** | powershell MainWindowTitle | `isRiotClientWindowVisible` |
| C3 | **Focus / restore window** | SetForegroundWindow + restore tray | `focusRiotClientWindow` |
| C4 | **Measure window (client area)** | GetClientRect + ClientToScreen | `getRiotClientWindowBounds` |
| C5 | **Launch Riot Client** | Mở từ path, poll ≤30s | `launchRiotClient` |
| C6 | **Live status indicator** | Cập nhật mỗi 3s (running/tray/not running) | `MainForm.updateRiotClientStatus` |
| C7 | **Open vs Focus button** | Nút đổi label theo trạng thái | `MainForm.launchRiotClient` |

## D. Cấu hình Riot Path

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| D1 | **Auto-detect default path** | Dò 3 path mặc định | `AppConfig.findDefaultRiotClientPath` |
| D2 | **Browse path (file chooser)** | Chọn RiotClientServices.exe | `MainForm.browseRiotClientPath` |
| D3 | **Validate path** | Phải là file RiotClientServices.exe | `AppConfig.validateRiotClientPath` |
| D4 | **Persist path** | Lưu vào config.json | `AppConfig.setRiotClientPath` |
| D5 | **Display path (filename + full)** | Hiện tên file + path đầy đủ wrap | `MainForm.updatePathDisplay` |

## E. Settings

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| E1 | **Run at Windows Startup** | Toggle → HKCU Run key add/delete | `StartupManager`, `SettingsDialog` |
| E2 | **Sync startup on launch** | Đồng bộ registry với setting | `AppSettings.syncStartupRegistration` |
| E3 | **Auto-click Login toggle** | Bật/tắt auto-submit | `SettingsDialog` |
| E4 | **Persist settings** | Lưu config.json | `AppConfig.setBoolean` |

## F. Dữ liệu & Bảo mật

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| F1 | **Encryption (AES-256)** | Mã hóa account file | `CryptoService` |
| F2 | **Machine-bound key** | Key = SHA-256(MachineGuid) | `CryptoService.getMachineId` |
| F3 | **Atomic write** | temp → move | `AccountStore.save` |
| F4 | **Automatic backup** | accounts.dat.bak trước ghi đè | `AccountStore.backupAccountsFile` |
| F5 | **Rollback on error** | Khôi phục từ backup | `AccountStore.rollbackFromBackup` |
| F6 | **Migration framework** | data.version + migrate hook | `AccountStore.runMigrationsIfNeeded` |
| F7 | **Backward/forward compat** | Format JSON array giữ nguyên | `AccountStore` |
| F8 | **Load on startup** | Tự đọc dữ liệu cũ | `AccountStore.load` |
| F9 | **Error-tolerant read** | optString, không phá file khi lỗi | `AccountStore.load` |

## G. Giao diện & UX

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| G1 | **Main window (header/list/config)** | 3 vùng | `MainForm` |
| G2 | **Gradient header** | Nền gradient tím | `GradientPanel` |
| G3 | **Material-style buttons** | Bo góc, hover, click effect | `UIHelper.createMaterialButton` |
| G4 | **Icon buttons** | Add/Edit/Delete/Login icons | `UIHelper.createIconButton` |
| G5 | **Toast notification (non-blocking)** | Thay JOptionPane, tự ẩn | `Toast` |
| G6 | **Window icon** | Icon ứng dụng | `UIHelper.setWindowIcon` |
| G7 | **Place top-right on launch** | Vị trí cửa sổ ban đầu | `MainForm.placeTopRight` |
| G8 | **Resizable + min size** | 440x600, min 360x560 | `MainForm.initializeUI` |
| G9 | **About dialog** | 5 mục thông tin cuộn | `AboutDialog` |
| G10 | **Welcome dialog (first run)** | Hướng dẫn 5 bước + "đừng hiện lại" | `WelcomeDialog` |
| G11 | **Re-show Welcome** | Nút trong About | `AboutDialog` |

## H. Đa ngôn ngữ (i18n)

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| H1 | **VI/EN switch** | Combo VI/EN ở header | `MainForm`, `LanguageManager` |
| H2 | **Persist language** | java.util.prefs | `LanguageManager.setLanguage` |
| H3 | **All UI strings localized** | messages_vi/en.properties | `resources/` |
| H4 | **Default = Vietnamese** | DEFAULT_LANGUAGE=vi | `LanguageManager` |

## I. Packaging & Distribution

| # | Tính năng | Mô tả hành vi Java | Nguồn |
|---|---|---|---|
| I1 | **Runnable JAR** | fat jar | Maven shade / build.ps1 |
| I2 | **Portable (bundled runtime)** | app-image + runtime | jpackage / Launch4j |
| I3 | **EXE installer** | Setup.exe | jpackage (WiX) |
| I4 | **No Java required by user** | runtime bundled | packaging |
| I5 | **CI release on tag** | GitHub Actions | release.yml |
| I6 | **Win10/11 support** | — | — |

---

## Tổng kết phải đối chiếu khi migrate

- **A**: 8 · **B**: 8 · **C**: 7 · **D**: 5 · **E**: 4 · **F**: 9 · **G**: 11 · **H**: 4 · **I**: 6
- **Tổng: 62 tính năng/điểm hành vi** cần được migrate hoặc ghi rõ giải pháp thay thế.

> Các mục có rủi ro migrate (ghi chi tiết ở `tauri-migration-plan.md` và `KNOWN_ISSUES.md`):
> - H2 (language persisted ở Java prefs — sẽ chuyển store mới, có thể reset 1 lần).
> - G10/G11 (welcome_shown ở Java prefs — có thể reset 1 lần, hiện lại Welcome).
> - B3–B7, C3–C4 (phụ thuộc OS sâu — cần test thủ công trên Windows thật).
