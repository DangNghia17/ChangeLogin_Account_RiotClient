package com.riotaccountmanager;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Helper class để mã hóa và giải mã dữ liệu bằng AES-256
 * Sử dụng Machine ID để tạo key mã hóa
 */
public class EncryptionHelper {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    
    /**
     * Lấy Machine ID của Windows để tạo key mã hóa
     */
    private static String getMachineId() {
        try {
            // Lấy Machine GUID từ Windows Registry
            Process process = Runtime.getRuntime().exec(
                "reg query HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography /v MachineGuid"
            );
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("MachineGuid")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        return parts[parts.length - 1];
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: sử dụng tên máy và user name
            try {
                return System.getProperty("user.name") + System.getProperty("os.name") + 
                       System.getProperty("os.version");
            } catch (Exception ex) {
                return "default-machine-id";
            }
        }
        return "default-machine-id";
    }
    
    /**
     * Tạo secret key từ Machine ID
     */
    private static SecretKey getSecretKey() throws Exception {
        String machineId = getMachineId();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(machineId.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, ALGORITHM);
    }
    
    /**
     * Mã hóa dữ liệu
     */
    public static String encrypt(String data) throws Exception {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        SecretKey secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    
    /**
     * Giải mã dữ liệu
     */
    public static String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        SecretKey secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}

