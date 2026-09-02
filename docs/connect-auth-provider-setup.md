# Connect Auth Provider Setup & Verification Guide — v1.7.0

This document outlines the official setup workflows, required scopes, desktop bridge configurations, and fallback token setups for all supported Dark Mode Studio integrations.

---

## 1. Source Control

### GitHub (Production-Ready)
- **Primary Auth**: Official Sign-in with GitHub (OAuth 2.0 PKCE / Web Intent).
- **Client Configuration**: Pre-configured in `ProviderRegistry` with standard public OAuth Client ID (`Ov23liauTz93Q0f3v9g5`).
- **Redirect URI**: `darkmodestudio://oauth/callback` (registered in `AndroidManifest.xml`).
- **Required Scopes**: `repo`, `read:org`, `workflow`, `user:email`.
- **Telemetry Ingested**:
  - Repositories, default branches, recent commit history.
  - Pull request count and status.
  - GitHub Actions CI workflow run outcomes (success / failure detection).
- **Advanced / PAT Mode**:
  - Go to `https://github.com/settings/tokens`.
  - Generate a fine-grained or classic token with `repo` and `workflow` permissions.
  - Paste into Dark Mode Studio -> Connect Service -> Advanced: Use API Key / Token.
  - Token is stored in Android Keystore with AES-256-GCM encryption.

---

## 2. Cloud & Hosting

### Cloudflare
- **Primary Auth**: Sign in with Cloudflare (OAuth 2.0 PKCE).
- **Status**: OAuth Setup Required (configure client ID in `ProviderRegistry` or use Advanced mode).
- **Required Scopes**: `zone:read`, `workers:read`, `dns:read`.
- **Advanced / API Token Mode**:
  - Go to `https://dash.cloudflare.com/profile/api-tokens`.
  - Create token using the **Read all resources** or custom **Zone / Workers Read** template.
  - Paste into Advanced mode.

### Vercel
- **Primary Auth**: Sign in with Vercel (OAuth Backend Broker).
- **Status**: OAuth Setup Required (use Advanced mode).
- **Required Permissions**: Read-only deployments and project inspection.
- **Advanced / Token Mode**:
  - Go to `https://vercel.com/account/tokens`.
  - Generate an API token and save into Android Keystore via Advanced mode.

### Firebase / Google Cloud
- **Primary Auth**: Sign in with Google (OAuth 2.0 PKCE).
- **Status**: OAuth Setup Required (use Advanced mode).
- **Required Scopes**: `cloud-platform.read-only`, `firebase.readonly`.
- **Advanced Mode**: Service Account JSON Key stored in Keystore.

---

## 3. Database & Backend

### Supabase
- **Primary Auth**: Sign in with Supabase (OAuth 2.0 PKCE).
- **Status**: OAuth Setup Required (use Advanced mode).
- **Advanced Mode**:
  - Open Supabase Project Settings -> API.
  - Provide Project URL (`https://xyz.supabase.co`) and API anon / service key.
  - Verified via real live HTTP endpoint response measurements without fabricated telemetry numbers.

---

## 4. AI Providers (Direct API Access)

### OpenAI API
- **Auth**: Direct API Key (`sk-proj-...`).
- **Storage**: Encrypted using Android Keystore-backed AES-256-GCM keys. Zero secrets in SQLite.

### Anthropic API
- **Auth**: Direct API Key (`sk-ant-...`).
- **Storage**: Encrypted using Android Keystore-backed AES-256-GCM keys. Zero secrets in SQLite.

---

## 5. AI Agents (Desktop Runtime Sessions)

### Overview
AI agents run as autonomous developer tools on your primary workstation. Dark Mode Studio Mobile pairs with your workstation over an authenticated local network bridge.

### Codex (Lead Architect & Developer)
- **Runtime**: OpenAI Codex CLI (`codex`).
- **Authentication**: Official ChatGPT Plus / Team / Enterprise account session on desktop.
- **Status in App**: Truthfully reports runtime version and desktop host availability via live CLI execution.

### Claude Code (Terminal Orchestration Agent)
- **Runtime**: Anthropic Claude Code CLI (`claude`).
- **Authentication**: Official Anthropic Claude Pro / Team subscription session on desktop.

### Antigravity (Google DeepMind Agentic Assistant)
- **Runtime**: Google Antigravity CLI (`agy`).
- **Authentication**: Google account authenticated via system keyring on desktop.

---

## 6. Desktop Host Pairing Instructions

1. Start Dark Mode Studio Desktop on your workstation:
   ```bash
   cd desktop
   npm start -- --port 8998
   ```
2. Generate a pairing code:
   - On Desktop UI: Click **Generate Pairing Code** in settings.
   - Or run `POST http://localhost:8998/api/host/pair/generate`.
   - Note the 6-digit code (e.g. `DMS-994821`), valid for 5 minutes.
3. In Dark Mode Studio Mobile:
   - Navigate to **Manage AI Agents** -> **Pair Desktop Host** (or select Codex/Claude/Antigravity in Connect Stack).
   - Enter your computer LAN IP / hostname (e.g. `192.168.1.50:8998` or `10.0.2.2:8998` on Android Emulator).
   - Enter the 6-digit pairing code.
   - Tap **Pair Computer**.
4. Mobile verifies with desktop server, receives a 256-bit cryptographically secure pairing secret, stores it in Android Keystore, and registers the host in Room DB v7.
5. All agent CLI detection, authentication, and execution requests are now securely proxied to your workstation with `Authorization: Bearer <secret>`.
