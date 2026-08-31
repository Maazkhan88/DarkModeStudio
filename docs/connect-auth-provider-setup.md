# Connect Auth Provider Setup & Verification Guide — v1.7.0

This document outlines the official setup workflows, required scopes, desktop bridge configurations, and fallback token setups for all supported Dark Mode Studio integrations.

---

## 1. Source Control

### GitHub
- **Primary Auth**: Official Sign-in with GitHub (OAuth 2.0 PKCE / Web Intent).
- **Required Scopes**: `repo`, `read:org`, `workflow`, `user:email`.
- **Telemetry Ingested**:
  - Repositories, default branches, recent commit history.
  - Pull request count and status.
  - GitHub Actions CI workflow run outcomes (success / failure detection).
- **Advanced / PAT Mode**:
  - Go to `https://github.com/settings/tokens`.
  - Generate a fine-grained or classic token with `repo` and `workflow` permissions.
  - Paste into Dark Mode Studio -> Connect Service -> Advanced Mode.

---

## 2. Cloud & Hosting

### Cloudflare
- **Primary Auth**: Sign in with Cloudflare (OAuth 2.0 PKCE).
- **Required Scopes**: `zone:read`, `workers:read`, `dns:read`.
- **Advanced / API Token Mode**:
  - Go to `https://dash.cloudflare.com/profile/api-tokens`.
  - Create token using the **Read all resources** or custom **Zone / Workers Read** template.

### Vercel
- **Primary Auth**: Sign in with Vercel (OAuth Backend Broker).
- **Required Permissions**: Read-only deployments and project inspection.
- **Advanced / Token Mode**:
  - Go to `https://vercel.com/account/tokens`.
  - Generate an API token and save into Android Keystore.

### Firebase / Google Cloud
- **Primary Auth**: Sign in with Google (OAuth 2.0 PKCE).
- **Required Scopes**: `cloud-platform.read-only`, `firebase.readonly`.
- **Advanced Mode**: Service Account JSON Key stored in Keystore.

---

## 3. Database & Backend

### Supabase
- **Primary Auth**: Sign in with Supabase (OAuth 2.0 PKCE).
- **Advanced Mode**:
  - Open Supabase Project Settings -> API.
  - Provide Project URL (`https://xyz.supabase.co`) and API anon / service key.
  - Verified via real ping health without fabricated telemetry numbers.

---

## 4. AI Providers (Direct API Access)

### OpenAI API
- **Auth**: Direct API Key (`sk-proj-...`).
- **Storage**: Android Keystore AES-256-GCM encrypted.

### Anthropic API
- **Auth**: Direct API Key (`sk-ant-...`).
- **Storage**: Android Keystore AES-256-GCM encrypted.

---

## 5. AI Agents (Desktop Runtime Sessions)

### Overview
AI agents run as autonomous developer tools on your primary workstation. Dark Mode Studio Mobile pairs with your workstation over an authenticated local network bridge.

### Codex (Lead Architect & Developer)
- **Runtime**: OpenAI Codex CLI.
- **Authentication**: Official ChatGPT Plus / Team / Enterprise account session on desktop.
- **Status in App**: Truthfully reports runtime status and desktop host availability.

### Claude Code (Terminal Orchestration Agent)
- **Runtime**: Anthropic Claude Code (`claude-code`).
- **Authentication**: Official Anthropic Claude Pro / Team subscription session on desktop.

### Antigravity (Google DeepMind Agentic Assistant)
- **Runtime**: Antigravity 2.0 CLI (`agy`).
- **Authentication**: Google account authenticated via system keyring on desktop.

---

## 6. Desktop Host Pairing Instructions

1. Start Dark Mode Studio Desktop on your workstation:
   ```bash
   dms-desktop start --port 8998
   ```
2. Note the 6-digit pairing code displayed in your terminal (e.g. `DMS-994821`).
3. In Dark Mode Studio Mobile:
   - Navigate to **Manage Coding Agents** -> **Pair Desktop Host**.
   - Enter your workstation hostname and pairing code.
4. Mobile pairs instantly and verifies available local CLI runtimes.
