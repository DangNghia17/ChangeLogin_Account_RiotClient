#!/usr/bin/env node
/**
 * Environment health check — run: npm run doctor
 * Reports missing tools/deps and how to fix them.
 */

import { execSync, spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const appDir = join(root, "app-tauri");

const issues = [];
const warnings = [];
const ok = [];

function run(cmd, opts = {}) {
  try {
    return execSync(cmd, { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"], ...opts }).trim();
  } catch {
    return null;
  }
}

function section(title) {
  console.log(`\n── ${title} ${"─".repeat(Math.max(0, 50 - title.length))}`);
}

function checkNode() {
  const version = process.version;
  const major = Number(version.slice(1).split(".")[0]);
  if (major >= 18) {
    ok.push(`Node.js ${version}`);
  } else {
    issues.push(`Node.js ${version} — cần >= 18. Cài từ https://nodejs.org/`);
  }
}

function checkNpm() {
  const v = run("npm --version");
  if (v) ok.push(`npm ${v}`);
  else issues.push("npm không tìm thấy — cài cùng Node.js");
}

function checkRust() {
  const rustc = run("rustc --version");
  const cargo = run("cargo --version");
  if (rustc && cargo) {
    ok.push(rustc);
    ok.push(cargo);
    const minor = Number((rustc.match(/(\d+)\.(\d+)/) || [])[2] ?? 0);
    const major = Number((rustc.match(/(\d+)\.(\d+)/) || [])[1] ?? 0);
    if (major < 1 || (major === 1 && minor < 85)) {
      warnings.push(
        `Rust ${major}.${minor} — Tauri build cần >= 1.85. Chạy: rustup update stable && rustup default stable`,
      );
    }
  } else {
    warnings.push(
      "Rust chưa cài — KHÔNG cần cho web preview (npm run dev).\n" +
        "    Cần cho Tauri build: https://rustup.rs/ → rustup default stable",
    );
  }
}

function checkTauriCli() {
  const local = join(appDir, "node_modules", ".bin", "tauri");
  const hasLocal = existsSync(local) || existsSync(`${local}.cmd`);
  if (hasLocal) {
    ok.push("Tauri CLI (local via npm)");
    return;
  }
  const global = run("tauri --version");
  if (global) {
    ok.push(`Tauri CLI ${global}`);
  } else if (existsSync(join(appDir, "node_modules"))) {
    warnings.push("Tauri CLI chưa sẵn sàng — chạy: npm install");
  } else {
    warnings.push("Chưa npm install — chạy: npm install ở thư mục gốc project");
  }
}

function checkDeps() {
  const nm = join(appDir, "node_modules");
  if (existsSync(nm)) {
    ok.push("app-tauri/node_modules");
  } else {
    issues.push("Thiếu dependencies — chạy: npm install");
  }
}

function checkConfig() {
  const confPath = join(appDir, "src-tauri", "tauri.conf.json");
  if (!existsSync(confPath)) {
    issues.push(`Thiếu ${confPath}`);
    return;
  }
  try {
    JSON.parse(readFileSync(confPath, "utf8"));
    ok.push("tauri.conf.json hợp lệ");
  } catch (e) {
    issues.push(`tauri.conf.json lỗi JSON: ${e.message}`);
  }

  const pkgPath = join(appDir, "package.json");
  if (!existsSync(pkgPath)) {
    issues.push("Thiếu app-tauri/package.json");
  } else {
    ok.push("app-tauri/package.json");
  }
}

function checkWebBuild() {
  const tsc = join(appDir, "node_modules", ".bin", "tsc");
  if (!existsSync(tsc) && !existsSync(`${tsc}.cmd`)) return;

  const result = spawnSync("npm", ["run", "check:web"], {
    cwd: appDir,
    encoding: "utf8",
    shell: true,
  });
  if (result.status === 0) {
    ok.push("TypeScript check (frontend) passed");
  } else {
    issues.push(`TypeScript lỗi:\n${(result.stdout || "") + (result.stderr || "")}`);
  }
}

function checkRustTests() {
  if (!run("cargo --version")) return;

  const result = spawnSync("cargo", ["test", "-p", "ram-core", "--quiet"], {
    cwd: appDir,
    encoding: "utf8",
  });
  if (result.status === 0) {
    ok.push("cargo test -p ram-core passed");
  } else {
    warnings.push(
      `cargo test thất bại (cần Rust cho logic backend):\n${(result.stdout || "") + (result.stderr || "")}`,
    );
  }
}

function printResults() {
  console.log("Riot Account Manager — Doctor");
  console.log("=".repeat(40));

  if (ok.length) {
    section("OK");
    ok.forEach((m) => console.log(`  ✓ ${m}`));
  }
  if (warnings.length) {
    section("Cảnh báo (web preview vẫn chạy được)");
    warnings.forEach((m) => console.log(`  ⚠ ${m}`));
  }
  if (issues.length) {
    section("Cần sửa");
    issues.forEach((m) => console.log(`  ✗ ${m}`));
  }

  section("Workflow nhanh");
  console.log("  Web UI (không cần Rust):  npm install && npm run dev");
  console.log("  Tauri đầy đủ (Windows):   npm install && npm run tauri:dev");
  console.log("  Docker web preview:        docker compose up");

  const exitCode = issues.length > 0 ? 1 : 0;
  console.log(`\n${exitCode === 0 ? "Doctor: PASS" : "Doctor: FAIL"}\n`);
  process.exit(exitCode);
}

checkNode();
checkNpm();
checkRust();
checkTauriCli();
checkDeps();
checkConfig();
checkWebBuild();
checkRustTests();
printResults();
