# Web preview only — no Rust/Tauri required.
# Full Tauri build must run on Windows with Rust installed locally or via CI.
FROM node:20-bookworm-slim

WORKDIR /app

COPY package.json ./
COPY app-tauri/package.json app-tauri/package-lock.json ./app-tauri/

RUN npm install --prefix app-tauri

COPY app-tauri ./app-tauri

WORKDIR /app/app-tauri

EXPOSE 1420

CMD ["npm", "run", "web"]
