#!/usr/bin/env node
/**
 * Regenerate src-tauri/icons/icon.ico from icon.png (valid Windows ICO format).
 * Requires: pip install pillow  OR  run on machine with Python + Pillow.
 *
 * Usage: node scripts/generate-icon.mjs
 */
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const icons = join(root, "app-tauri", "src-tauri", "icons");

const py = `
from PIL import Image
from pathlib import Path
icons_dir = Path(${JSON.stringify(icons)})
src = Image.open(icons_dir / "icon.png").convert("RGBA")
sizes = [16, 24, 32, 48, 64, 128, 256]
imgs = [src.resize((s, s), Image.Resampling.LANCZOS) for s in sizes]
out = icons_dir / "icon.ico"
imgs[-1].save(out, format="ICO", sizes=[(im.width, im.height) for im in imgs], append_images=imgs[:-1][::-1])
print("OK:", out)
`;

const r = spawnSync("python3", ["-c", py], { encoding: "utf8" });
if (r.status !== 0) {
  console.error(r.stderr || r.stdout);
  process.exit(1);
}
console.log(r.stdout.trim());
