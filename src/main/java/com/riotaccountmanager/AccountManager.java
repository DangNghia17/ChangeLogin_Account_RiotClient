package com.riotaccountmanager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class để quản lý danh sách tài khoản
 * Lưu trữ tài khoản đã mã hóa vào file accounts.dat
 */
public class AccountManager {
    private static final String DATA_DIR = System.getenv("LOCALAPPDATA") + "\\RiotAccountManager";
    private static final String ACCOUNTS_FILE = DATA_DIR + "\\accounts.dat";
    
    private List<Account> accounts;
    
    public AccountManager() {
        this.accounts = new ArrayList<>();
        ensureDataDir();
        loadAccounts();
    }
    
    /**
     * Đảm bảo thư mục dữ liệu tồn tại
     */
    private void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Tải danh sách tài khoản từ file
     */
    public void loadAccounts() {
        accounts.clear();
        
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) {
            return;
        }
        
        try {
            // Đọc dữ liệu đã mã hóa
            String encryptedData = new String(Files.readAllBytes(Paths.get(ACCOUNTS_FILE)), "UTF-8");
            
            if (encryptedData.isEmpty()) {
                return;
            }
            
            // Giải mã dữ liệu
            String decryptedData = EncryptionHelper.decrypt(encryptedData);
            
            // Parse JSON
            JSONArray jsonArray = new JSONArray(decryptedData);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Account account = new Account();
                account.setUsername(json.getString("username"));
                account.setPassword(json.getString("password"));
                account.setRegion(json.getString("region"));
                // Load note nếu có, nếu không có thì để trống
                account.setNote(json.optString("note", ""));
                accounts.add(account);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải tài khoản: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Lưu danh sách tài khoản vào file
     */
    public void saveAccounts() {
        try {
            // Tạo JSON array
            JSONArray jsonArray = new JSONArray();
            
            for (Account account : accounts) {
                JSONObject json = new JSONObject();
                json.put("username", account.getUsername());
                json.put("password", account.getPassword());
                json.put("region", account.getRegion());
                json.put("note", account.getNote() != null ? account.getNote() : "");
                jsonArray.put(json);
            }
            
            // Mã hóa dữ liệu
            String encryptedData = EncryptionHelper.encrypt(jsonArray.toString());
            
            // Lưu vào file
            try (FileWriter writer = new FileWriter(ACCOUNTS_FILE)) {
                writer.write(encryptedData);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu tài khoản: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Thêm tài khoản mới
     */
    public void addAccount(Account account) {
        accounts.add(account);
        saveAccounts();
    }
    
    /**
     * Cập nhật tài khoản
     */
    public void updateAccount(int index, Account account) {
        if (index >= 0 && index < accounts.size()) {
            accounts.set(index, account);
            saveAccounts();
        }
    }
    
    /**
     * Xóa tài khoản
     */
    public void removeAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            accounts.remove(index);
            saveAccounts();
        }
    }
    
    /**
     * Lấy tài khoản theo index
     */
    public Account getAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            return accounts.get(index);
        }
        return null;
    }
    
    /**
     * Lấy danh sách tất cả tài khoản
     */
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts);
    }
    
    /**
     * Lấy số lượng tài khoản
     */
    public int getAccountCount() {
        return accounts.size();
    }
}

