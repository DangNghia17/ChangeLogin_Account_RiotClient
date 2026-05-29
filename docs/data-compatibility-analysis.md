# Data Compatibility Analysis (Phase 3)

> Phân tích chi tiết dữ liệu của bản Java để bản Tauri **đọc được 100% dữ liệu cũ** và migrate
> an toàn. Ưu tiên #1: **không mất tài khoản / settings / dữ liệu mã hóa**.

---

## 1. Vị trí lưu dữ liệu

| Mục | Vị trí | Ghi chú |
|---|---|---|
| Account file | `%LOCALAPPDATA%\RiotAccountManager\accounts.dat` | Mã hóa |
| Config | `%LOCALAPPDATA%\RiotAccountManager\config.json` | Plaintext |
| Backup | `…\accounts.dat.bak` | Tạo trước mỗi ghi đè |
| Temp | `…\accounts.dat.tmp` | Atomic write |
| Schema version | `…\data.version` | Số nguyên |
| Language + welcome flag | Registry `HKCU\Software\JavaSoft\Prefs\com\riotaccountmanager` | java.util.prefs |
| Startup entry | Registry `HKCU\…\CurrentVersion\Run` value `RiotAccountManager` | reg.exe |

**Bản Tauri giữ nguyên thư mục và tên file** `accounts.dat` / `config.json` để tự nhận dữ liệu cũ.

---

## 2. Cấu trúc dữ liệu

### 2.1. `accounts.dat`
- Trên đĩa: **một chuỗi Base64** (không xuống dòng) = `Base64( AES_ECB( UTF8(json) ) )`.
- Sau giải mã: **JSON array** (top-level), mỗi phần tử:

```json
{ "username": "...", "password": "...", "region": "...", "note": "..." }
```

- Field bắt buộc: `username`, `password`, `region`; `note` mặc định `""`.

### 2.2. `config.json` (plaintext)
```json
{
  "riotClientPath": "C:\\Riot Games\\Riot Client\\RiotClientServices.exe",
  "runAtStartup": false,
  "autoClickLogin": true,
  "language": "vi"
}
```
> Bản Java cũ có thể chỉ chứa `riotClientPath`. Các key boolean đọc bằng default.

---

## 3. Cơ chế mã hóa (để Rust tái hiện chính xác)

```
machineId  = Windows MachineGuid (HKLM\SOFTWARE\Microsoft\Cryptography /v MachineGuid)
             fallback: user.name + os.name + os.version  → fallback: "default-machine-id"
key (32B)  = SHA-256( UTF8(machineId) )
cipher     = AES-256 / ECB / PKCS5(=PKCS7) padding
ondisk     = Base64-standard( cipher )   # Java Base64.getEncoder(): có padding '=', không newline
```

- **AES/ECB**: không IV; mỗi block 16B độc lập.
- **PKCS5Padding** trong JCE == **PKCS7** cho block 16B (giống nhau).
- **Base64**: standard alphabet, có `=` padding, **không** chèn `\n`.

### 3.1. Test vector xác định (dùng cho unit test Rust)

Để chứng minh Rust giải mã đúng dữ liệu Java, dùng vector cố định (machineId cố định thay vì MachineGuid thật):

```
machineId  = "TEST-MACHINE-GUID-1234"
plaintext  = [{"username":"u@example.com","password":"p@ss_w0rd-1","region":"VN","note":"main"}]
cipher_b64 = MRIryDr0wxwARmbPntThUZI8YfKYLdjEBf2vEZcENUVnmes37AyQE4dv6xOGbhK5Z4CQOBzW5TwayetBaI/7tSGPW96bfVXEZcXR1NiloS/UH5eqrKQyqiKUfKZhpvRj
```

> Rust test: `decrypt_legacy(cipher_b64, key=SHA256("TEST-MACHINE-GUID-1234"))` phải ra đúng `plaintext`.
> Và `encrypt_legacy(plaintext, key)` phải ra đúng `cipher_b64` (chứng minh round-trip byte-for-byte).

---

## 4. Cơ chế giải mã (đọc)

1. Đọc toàn bộ file `accounts.dat` (UTF-8 / ASCII Base64).
2. `base64_decode` → ciphertext bytes.
3. AES-256-ECB decrypt với key = SHA-256(machineId).
4. Bỏ PKCS7 padding → UTF-8 string.
5. Parse JSON array → danh sách account.
6. Lỗi bất kỳ ở bước nào: **không sửa/không xóa file**, trả danh sách rỗng + log (giống Java).

---

## 5. Cơ chế migrate (Java hiện tại → Tauri)

### 5.1. Chiến lược (Phase 6 + Phase 7)

Bản Tauri sẽ:

1. **Đọc legacy**: nếu `accounts.dat` tồn tại và **chưa** ở format mới → giải mã bằng thuật toán
   AES/ECB + SHA-256(MachineGuid) như Java (tự nhận dữ liệu cũ).
2. **Backup**: copy `accounts.dat` → `accounts.dat.bak` **trước khi** migrate.
3. **Re-encrypt an toàn (Phase 7)**: mã hóa lại payload bằng **Windows DPAPI**
   (`CryptProtectData`, scope CurrentUser) — không hard-code key, không plaintext.
   - Định dạng mới tự mô tả bằng **prefix** để phân biệt:
     - Legacy (Java): chuỗi Base64 thuần (không prefix).
     - Mới (DPAPI): `RAM2:` + Base64(DPAPI blob).
   - Nhờ prefix, hàm đọc tự nhận diện và giải mã đúng cả 2 định dạng.
4. **Ghi atomic**: temp → move; cập nhật `data.version`.
5. **Rollback**: nếu bước migrate/ghi lỗi → khôi phục từ `accounts.dat.bak`, giữ nguyên dữ liệu gốc.

### 5.2. Vì sao chuyển sang DPAPI

- Phase 7 cấm plaintext + cấm cơ chế yếu (ECB) + cấm hard-code key.
- DPAPI: key do OS quản lý theo user account, không cần lưu/hard-code, vẫn machine/user-bound.
- Vẫn giữ khả năng **đọc** legacy ECB để không mất dữ liệu.

### 5.3. Tương thích ngược / rollback

- Nếu người dùng cần quay lại bản Java: bản Java **không** đọc được format `RAM2:` (DPAPI).
  → Vì vậy **giữ `accounts.dat.bak`** (bản legacy) sau migrate để có thể khôi phục thủ công.
  Ghi rõ trong `KNOWN_ISSUES.md` (downgrade sau migrate cần dùng file `.bak`).

---

## 6. Dữ liệu KHÔNG migrate tự động (ghi rõ)

| Dữ liệu | Lý do | Ảnh hưởng | Giải pháp |
|---|---|---|---|
| `language` (java.util.prefs) | Lưu trong registry path riêng của JavaSoft, khó/không nên đọc chéo | Ngôn ngữ về mặc định (vi) lần đầu | Người dùng chọn lại 1 lần; lưu vào store mới |
| `welcome_shown` (java.util.prefs) | Như trên | Welcome có thể hiện lại 1 lần | Chấp nhận; lưu store mới sau đó |
| `riotClientPath`, `runAtStartup`, `autoClickLogin` | Nằm trong `config.json` (file) | **Migrate được** (đọc file) | Đọc trực tiếp config.json |
| Startup registry entry | Value name giữ nguyên `RiotAccountManager` nhưng command trỏ tới exe mới | Cần ghi lại khi bật | `syncStartup` lúc khởi động |

> **Account & mật khẩu (quan trọng nhất): migrate được 100%** vì nằm trong `accounts.dat` (file),
> và Rust giải mã được bằng cùng thuật toán.

---

## 7. Checklist tương thích (phải pass)

- [ ] Rust decrypt đúng test vector mục 3.1 (unit test).
- [ ] Rust encrypt round-trip ra đúng `cipher_b64` (unit test).
- [ ] Đọc `accounts.dat` thật do Java tạo trên Windows → đúng danh sách account (manual, Windows).
- [ ] Đọc `config.json` cũ (chỉ có `riotClientPath`) → không lỗi, có default settings.
- [ ] Migrate tạo `accounts.dat.bak` trước khi ghi format mới.
- [ ] Ghi DPAPI format `RAM2:` và đọc lại đúng.
- [ ] Lỗi migrate → rollback, dữ liệu gốc còn nguyên.
