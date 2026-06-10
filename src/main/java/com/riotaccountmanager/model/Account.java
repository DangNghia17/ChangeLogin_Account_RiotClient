package com.riotaccountmanager.model;

import java.util.Objects;

/**
 * Immutable-ish data model for a single Riot account.
 *
 * <p>The on-disk JSON schema (inside {@code accounts.dat}) uses exactly the field
 * names {@code username}, {@code password}, {@code region}, {@code note}. These names
 * MUST NOT change to preserve backward/forward compatibility with previously saved data.
 */
public class Account {
    private String username;
    private String password;
    private String region;
    private String note;

    public Account() {
        this.note = "";
    }

    public Account(String username, String password, String region) {
        this(username, password, region, "");
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return Objects.equals(username, account.username)
                && Objects.equals(region, account.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, region);
    }

    @Override
    public String toString() {
        return "Account{username='" + username + "', region='" + region + "', note='" + note + "'}";
    }
}
