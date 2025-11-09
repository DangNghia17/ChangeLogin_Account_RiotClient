package com.riotaccountmanager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AccountManager {
    private static final String DATA_DIR = System.getenv("LOCALAPPDATA") + "\\RiotAccountManager";
    private static final String ACCOUNTS_FILE = DATA_DIR + "\\accounts.dat";
    
    private List<Account> accounts;
    
    public AccountManager() {
        this.accounts = new ArrayList<>();
        ensureDataDir();
        loadAccounts();
    }
    
    private void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void loadAccounts() {
        accounts.clear();
        
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) {
            return;
        }
        
        try {
            String encryptedData = new String(Files.readAllBytes(Paths.get(ACCOUNTS_FILE)), "UTF-8");
            
            if (encryptedData.isEmpty()) {
                return;
            }
            
            String decryptedData = EncryptionHelper.decrypt(encryptedData);
            
            JSONArray jsonArray = new JSONArray(decryptedData);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Account account = new Account();
                account.setUsername(json.getString("username"));
                account.setPassword(json.getString("password"));
                account.setRegion(json.getString("region"));
                account.setNote(json.optString("note", ""));
                accounts.add(account);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải tài khoản: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void saveAccounts() {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Account account : accounts) {
                JSONObject json = new JSONObject();
                json.put("username", account.getUsername());
                json.put("password", account.getPassword());
                json.put("region", account.getRegion());
                json.put("note", account.getNote() != null ? account.getNote() : "");
                jsonArray.put(json);
            }
            
            String encryptedData = EncryptionHelper.encrypt(jsonArray.toString());
            
            try (FileWriter writer = new FileWriter(ACCOUNTS_FILE)) {
                writer.write(encryptedData);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu tài khoản: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void addAccount(Account account) {
        accounts.add(account);
        saveAccounts();
    }
    
    public void updateAccount(int index, Account account) {
        if (index >= 0 && index < accounts.size()) {
            accounts.set(index, account);
            saveAccounts();
        }
    }
    
    public void removeAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            accounts.remove(index);
            saveAccounts();
        }
    }
    
    public Account getAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            return accounts.get(index);
        }
        return null;
    }
    
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts);
    }
    
    public int getAccountCount() {
        return accounts.size();
    }
}
