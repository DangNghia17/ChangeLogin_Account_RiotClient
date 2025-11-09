package com.riotaccountmanager;

import java.awt.*;
import java.awt.event.KeyEvent;

public class WindowHelper {
    
    public static boolean activateRiotClientWindow() {
        try {
            Robot robot = new Robot();
            
            robot.delay(300);
            
            robot.keyPress(KeyEvent.VK_ALT);
            robot.delay(150);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(200);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(500);
            
            Thread.sleep(300);
            
            return true;
        } catch (Exception e) {
            System.out.println("Lỗi khi kích hoạt cửa sổ: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static void ensureRiotClientFocused() {
        try {
            Thread.sleep(300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
