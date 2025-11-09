package com.riotaccountmanager;

public class Account {
    private String username;
    private String password;
    private String region;
    private String note;
    
    public Account() {
        this.note = "";
    }
    
    public Account(String username, String password, String region) {
        this.username = username;
        this.password = password;
        this.region = region;
        this.note = "";
    }
    
    public Account(String username, String password, String region, String note) {
        this.username = username;
        this.password = password;
        this.region = region;
        this.note = note != null ? note : "";
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getNote() {
        return note != null ? note : "";
    }
    
    public void setNote(String note) {
        this.note = note != null ? note : "";
    }
    
    @Override
    public String toString() {
        return "Account{username='" + username + "', region='" + region + "', note='" + note + "'}";
    }
}
