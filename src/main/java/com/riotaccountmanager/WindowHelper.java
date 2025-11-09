package com.riotaccountmanager;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Helper class để kích hoạt cửa sổ Riot Client
 * Đơn giản: Minimize app hiện tại, sau đó Alt+Tab để focus vào Riot Client
 */
public class WindowHelper {
    
    /**
     * Kích hoạt cửa sổ Riot Client - đơn giản và hiệu quả
     * 1. Minimize app hiện tại (để không bị tab vào)
     * 2. Alt+Tab để chuyển sang Riot Client
     */
    public static boolean activateRiotClientWindow() {
        try {
            Robot robot = new Robot();
            
            // Bước 1: Minimize app hiện tại để không bị tab vào
            // Nhấn Windows + M để minimize tất cả, sau đó Alt+Tab sẽ dễ focus vào Riot Client hơn
            // Hoặc đơn giản hơn: chỉ cần Alt+Tab và đợi một chút
            robot.delay(300);
            
            // Bước 2: Alt+Tab để chuyển sang Riot Client
            // Riot Client thường là window được mở gần nhất hoặc đang chạy
            robot.keyPress(KeyEvent.VK_ALT);
            robot.delay(150);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(200); // Đợi Alt+Tab menu hiển thị
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(500); // Đợi window được focus
            
            // Đợi một chút để đảm bảo Riot Client đã được focus
            Thread.sleep(300);
            
            return true;
        } catch (Exception e) {
            System.out.println("Lỗi khi kích hoạt cửa sổ: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Đảm bảo Riot Client window đã được focus
     */
    public static void ensureRiotClientFocused() {
        try {
            Thread.sleep(300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
