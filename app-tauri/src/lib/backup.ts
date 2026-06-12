import type { Account, Settings } from "../types";

/**
 * Encrypted backup format.
 *
 * The file is a small JSON envelope holding metadata in clear text (so future
 * versions can detect and migrate it) plus the AES-256-GCM encrypted payload.
 * The key is derived from the user's password with PBKDF2 (SHA-256). The
 * password itself is never stored anywhere.
 */
export const APP_ID = "RiotAccountManager";
export const BACKUP_TYPE = "backup";
/** Envelope structure version — bump if the envelope shape changes. */
export const FORMAT_VERSION = 1;
/** Payload schema version — bump if the decrypted data shape changes. */
export const DATA_VERSION = 1;
const PBKDF2_ITERATIONS = 210_000;
const SALT_BYTES = 16;
const IV_BYTES = 12;

export type BackupErrorCode =
  | "wrong_password"
  | "corrupt"
  | "format"
  | "version"
  | "unsupported"
  | "unknown";

export class BackupError extends Error {
  code: BackupErrorCode;
  constructor(code: BackupErrorCode, message?: string) {
    super(message ?? code);
    this.name = "BackupError";
    this.code = code;
  }
}

export interface BackupPayload {
  accounts: Account[];
  settings?: Settings;
  riotPath?: string | null;
}

interface BackupEnvelope {
  app: string;
  type: string;
  formatVersion: number;
  dataVersion: number;
  createdAt: string;
  kdf: { name: "PBKDF2"; hash: "SHA-256"; iterations: number; salt: string };
  cipher: { name: "AES-GCM"; iv: string };
  data: string;
}

function getCrypto(): Crypto {
  const c = globalThis.crypto;
  if (!c?.subtle) {
    throw new BackupError("unsupported", "Web Crypto API is not available");
  }
  return c;
}

function toBase64(bytes: ArrayBuffer | Uint8Array): string {
  const view = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
  let binary = "";
  for (let i = 0; i < view.length; i++) binary += String.fromCharCode(view[i]);
  return btoa(binary);
}

function fromBase64(value: string): Uint8Array<ArrayBuffer> {
  const binary = atob(value);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

async function deriveKey(
  password: string,
  salt: BufferSource,
  iterations: number,
): Promise<CryptoKey> {
  const subtle = getCrypto().subtle;
  const baseKey = await subtle.importKey(
    "raw",
    new TextEncoder().encode(password),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  return subtle.deriveKey(
    { name: "PBKDF2", hash: "SHA-256", salt, iterations },
    baseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

/** Encrypts a payload with the password and returns the JSON envelope as text. */
export async function encryptBackup(password: string, payload: BackupPayload): Promise<string> {
  const cryptoObj = getCrypto();
  const salt = cryptoObj.getRandomValues(new Uint8Array(SALT_BYTES));
  const iv = cryptoObj.getRandomValues(new Uint8Array(IV_BYTES));
  const key = await deriveKey(password, salt, PBKDF2_ITERATIONS);

  const plaintext = new TextEncoder().encode(JSON.stringify(payload));
  const ciphertext = await cryptoObj.subtle.encrypt({ name: "AES-GCM", iv }, key, plaintext);

  const envelope: BackupEnvelope = {
    app: APP_ID,
    type: BACKUP_TYPE,
    formatVersion: FORMAT_VERSION,
    dataVersion: DATA_VERSION,
    createdAt: new Date().toISOString(),
    kdf: { name: "PBKDF2", hash: "SHA-256", iterations: PBKDF2_ITERATIONS, salt: toBase64(salt) },
    cipher: { name: "AES-GCM", iv: toBase64(iv) },
    data: toBase64(ciphertext),
  };
  return JSON.stringify(envelope, null, 2);
}

function parseEnvelope(fileText: string): BackupEnvelope {
  let parsed: unknown;
  try {
    parsed = JSON.parse(fileText);
  } catch {
    throw new BackupError("corrupt", "Backup file is not valid JSON");
  }
  if (!parsed || typeof parsed !== "object") {
    throw new BackupError("format", "Unexpected backup structure");
  }
  const env = parsed as Partial<BackupEnvelope>;
  if (env.app !== APP_ID || env.type !== BACKUP_TYPE) {
    throw new BackupError("format", "Not a Riot Account Manager backup file");
  }
  if (typeof env.formatVersion !== "number" || env.formatVersion > FORMAT_VERSION) {
    throw new BackupError("version", "Backup was created by a newer version");
  }
  if (
    !env.kdf ||
    typeof env.kdf.salt !== "string" ||
    typeof env.kdf.iterations !== "number" ||
    !env.cipher ||
    typeof env.cipher.iv !== "string" ||
    typeof env.data !== "string"
  ) {
    throw new BackupError("corrupt", "Backup file is missing required fields");
  }
  return env as BackupEnvelope;
}

function validatePayload(value: unknown): BackupPayload {
  if (!value || typeof value !== "object" || !Array.isArray((value as BackupPayload).accounts)) {
    throw new BackupError("format", "Decrypted data has an unexpected shape");
  }
  const raw = value as BackupPayload;
  const accounts: Account[] = raw.accounts.map((a) => ({
    username: String(a?.username ?? ""),
    password: String(a?.password ?? ""),
    region: String(a?.region ?? "VN"),
    note: String(a?.note ?? ""),
  }));
  return { accounts, settings: raw.settings, riotPath: raw.riotPath ?? null };
}

/** Decrypts an envelope text with the password. Throws a typed BackupError. */
export async function decryptBackup(password: string, fileText: string): Promise<BackupPayload> {
  const env = parseEnvelope(fileText);
  const key = await deriveKey(password, fromBase64(env.kdf.salt), env.kdf.iterations);

  let plaintext: ArrayBuffer;
  try {
    plaintext = await getCrypto().subtle.decrypt(
      { name: "AES-GCM", iv: fromBase64(env.cipher.iv) },
      key,
      fromBase64(env.data),
    );
  } catch {
    // AES-GCM authentication failure almost always means a wrong password
    // (or a tampered/corrupt payload).
    throw new BackupError("wrong_password", "Could not decrypt — wrong password or corrupt file");
  }

  let payload: unknown;
  try {
    payload = JSON.parse(new TextDecoder().decode(plaintext));
  } catch {
    throw new BackupError("corrupt", "Decrypted data is not valid JSON");
  }
  return validatePayload(payload);
}

/** Suggested file name for a new backup, e.g. riot-accounts-backup-2026-06-12.backup */
export function defaultBackupFileName(): string {
  const stamp = new Date().toISOString().slice(0, 10);
  return `riot-accounts-backup-${stamp}.backup`;
}
