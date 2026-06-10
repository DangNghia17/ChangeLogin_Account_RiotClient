package com.riotaccountmanager.storage;

import com.riotaccountmanager.model.Account;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the list of {@link Account}s to an encrypted file with strong data-safety
 * guarantees (the #1 requirement of this project).
 *
 * <p><b>On-disk compatibility:</b> the decrypted payload is a top-level JSON array with
 * the exact same per-account field names as previous versions, so:
 * <ul>
 *   <li>old data loads transparently (no manual migration, no re-entry of accounts);</li>
 *   <li>data written by this version is still readable by older versions (safe rollback).</li>
 * </ul>
 *
 * <p><b>Write safety:</b> every save is performed atomically (write to a temp file then
 * atomic move) and a rolling backup ({@code accounts.dat.bak}) is created before each
 * overwrite. If anything fails mid-write, the previous file is left intact / restored.
 */
public class AccountStore {

    private static final Logger LOG = Logger.getLogger(AccountStore.class.getName());

    /** Current logical schema version (tracked in a side file, not inside accounts.dat). */
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final List<Account> accounts = new ArrayList<>();

    public AccountStore() {
        AppPaths.ensureDataDir();
        runMigrationsIfNeeded();
        load();
    }

    /**
     * Lightweight migration framework. Today the schema is at version 1 and no structural
     * transform is required, but the hook (with backup + version tracking) is in place so
     * future format changes can migrate safely.
     */
    private void runMigrationsIfNeeded() {
        try {
            File accountsFile = AppPaths.accountsFile();
            if (!accountsFile.exists()) {
                writeSchemaVersion(CURRENT_SCHEMA_VERSION);
                return;
            }
            int onDisk = readSchemaVersion();
            if (onDisk >= CURRENT_SCHEMA_VERSION) {
                return;
            }
            // A migration is required: back up first so we can always roll back.
            backupAccountsFile();
            // (No structural transform needed for v0 -> v1; the format is unchanged.)
            writeSchemaVersion(CURRENT_SCHEMA_VERSION);
            LOG.info("Migrated account data schema to version " + CURRENT_SCHEMA_VERSION);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Schema migration skipped due to error", e);
        }
    }

    private int readSchemaVersion() {
        File versionFile = AppPaths.dataVersionFile();
        if (!versionFile.exists()) {
            return 0;
        }
        try {
            String content = new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? 0 : Integer.parseInt(content);
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeSchemaVersion(int version) {
        try {
            Files.write(AppPaths.dataVersionFile().toPath(),
                    String.valueOf(version).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not write schema version", e);
        }
    }

    private void backupAccountsFile() {
        try {
            File src = AppPaths.accountsFile();
            if (src.exists()) {
                Files.copy(src.toPath(), AppPaths.accountsBackupFile().toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not create backup of accounts file", e);
        }
    }

    /** Loads accounts from disk. On any failure, accounts are left empty and the file untouched. */
    public final void load() {
        accounts.clear();

        File file = AppPaths.accountsFile();
        if (!file.exists()) {
            return;
        }

        try {
            String encryptedData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (encryptedData.trim().isEmpty()) {
                return;
            }

            String decrypted = CryptoService.decrypt(encryptedData);
            JSONArray jsonArray = new JSONArray(decrypted);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Account account = new Account();
                account.setUsername(json.optString("username", ""));
                account.setPassword(json.optString("password", ""));
                account.setRegion(json.optString("region", ""));
                account.setNote(json.optString("note", ""));
                accounts.add(account);
            }
        } catch (Exception e) {
            // Never destroy the original file on a read error; surface for diagnostics only.
            LOG.log(Level.SEVERE, "Failed to load accounts (file left intact)", e);
        }
    }

    /**
     * Saves all accounts atomically. Returns {@code true} on success.
     *
     * <p>Steps: serialize -> encrypt -> write temp file -> backup current -> atomic move.
     * If the move fails, the temp file is cleaned up and the original remains intact.
     */
    public synchronized boolean save() {
        File target = AppPaths.accountsFile();
        File temp = AppPaths.accountsTempFile();
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

            String encrypted = CryptoService.encrypt(jsonArray.toString());

            AppPaths.ensureDataDir();
            // 1) write to temp (explicit UTF-8 instead of platform-default FileWriter).
            Files.write(temp.toPath(), encrypted.getBytes(StandardCharsets.UTF_8));

            // 2) keep a backup of the previous good file before replacing it.
            backupAccountsFile();

            // 3) atomic replace.
            try {
                Files.move(temp.toPath(), target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception atomicFailed) {
                // Some filesystems don't support ATOMIC_MOVE; fall back to replace.
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to save accounts; attempting rollback", e);
            rollbackFromBackup();
            return false;
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    private void rollbackFromBackup() {
        try {
            File backup = AppPaths.accountsBackupFile();
            File target = AppPaths.accountsFile();
            if (backup.exists() && !target.exists()) {
                Files.copy(backup.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Restored accounts file from backup after a failed save");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Rollback from backup failed", e);
        }
    }

    public void addAccount(Account account) {
        accounts.add(account);
        save();
    }

    public void updateAccount(int index, Account account) {
        if (index >= 0 && index < accounts.size()) {
            accounts.set(index, account);
            save();
        }
    }

    public void removeAccount(int index) {
        if (index >= 0 && index < accounts.size()) {
            accounts.remove(index);
            save();
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
