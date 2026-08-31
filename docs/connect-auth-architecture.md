# Connect Auth Architecture Specification — v1.7.0

## 1. Architectural Overview

Dark Mode Studio v1.7.0 introduces **Connect Auth**, a unified, secure authentication and integration architecture designed around official OAuth 2.0 (with PKCE), desktop runtime session bridging, and on-device Android Keystore hardware-backed encryption.

```
+-----------------------------------------------------------------------------------+
|                           Dark Mode Studio Mobile UI                              |
|   (ConnectStackScreen, ConnectServiceSheet, ManageAgentsSheet, AgentsScreen)      |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                                 ProviderRegistry                                  |
|   - Central catalog of providers, categories, auth methods, and capabilities      |
|   - Dynamic search, capability badges, and recommended action resolution          |
+-------------------+---------------------------------------+-----------------------+
                    |                                       |
                    v                                       v
+-----------------------------------+   +-------------------------------------------+
|         OAuthPkceManager          |   |            DesktopHostBridge              |
| - Cryptographic Verifier / S256   |   | - Authenticated pairing with host machine |
| - State generation & matching     |   | - Health / runtime discovery              |
| - Authorization URL builder       |   | - Session proxy to local agent CLIs       |
| - Android Keystore token storage  |   |   (Codex, Claude Code, Antigravity)       |
+-------------------+---------------+   +-------------------+-----------------------+
                    |                                       |
                    v                                       v
+-----------------------------------+   +-------------------------------------------+
|      Room SQLite (v6 SSOT)        |   |         Android Keystore / TEE            |
| - ProviderConnectionEntity        |   | - AES-256-GCM Master Key                  |
| - DesktopHostEntity               |   | - Access & Refresh Tokens                 |
| - Non-secret metadata ONLY        |   | - Zero secrets in plain SQLite            |
+-----------------------------------+   +-------------------------------------------+
```

---

## 2. Core Principles & Security Invariants

1. **Zero Service Passwords**: Dark Mode Studio NEVER collects or stores username/password pairs for third-party cloud services or agent accounts.
2. **Zero Plaintext Secrets in Database**: Access tokens, refresh tokens, and API keys are strictly encrypted in Android Keystore. Room SQLite stores only non-secret connection metadata (`accountDisplayName`, `accountId`, `workspaceName`, `connectionState`, `expiresAt`, `lastVerifiedAt`).
3. **Official Authentication Workflows**:
   - OAuth 2.0 with PKCE (Proof Key for Code Exchange, RFC 7636) for supported cloud platforms (GitHub, Cloudflare, Supabase).
   - OAuth Backend broker for platforms requiring client secret mediation (Vercel).
   - Desktop Runtime Session bridging for local agent CLIs (OpenAI Codex, Claude Code, Antigravity) via paired DMS Desktop Host.
   - Secure API token / key fallback mode for developers who explicitly choose manual entry.
4. **Data Integrity & Non-Destructive Updates**: All integration and project metadata ingestion uses non-destructive Room strategies (`OnConflictStrategy.ABORT` + explicit update) to guarantee child metrics, incidents, activities, and tasks are never wiped by SQLite cascading deletes.

---

## 3. Component Details

### A. `ProviderRegistry` (`com.darkmodestudio.commandcenter.core.auth`)
A centralized single source of truth for all supported services and agents. Eliminates hardcoded UI chip lists.

- **Categories**:
  - `SOURCE_CONTROL`: GitHub
  - `CLOUD_HOSTING`: Cloudflare, Vercel, Firebase / Google Cloud
  - `DATABASE_BACKEND`: Supabase
  - `AI_PROVIDERS`: OpenAI API, Anthropic API
  - `AI_AGENTS`: Codex, Claude Code, Antigravity
  - `CUSTOM`: Custom Service
- **Authentication Capabilities**:
  - `OAuthPkce`, `OAuthBackend`, `LocalRuntimeSession`, `ApiToken`, `ApiKey`, `ServiceAccount`, `CustomCredential`
- **Capabilities Matrix**:
  - `READ_TELEMETRY`, `CODE_SYNC`, `DEPLOYMENTS`, `DATABASE_MGMT`, `AGENT_ORCHESTRATION`, `CI_WORKFLOWS`

### B. `OAuthPkceManager` (`com.darkmodestudio.commandcenter.core.auth`)
Implements RFC 7636 PKCE state machine:
- **Code Verifier**: 48-byte cryptographically secure random entropy Base64URL-encoded (64 chars).
- **Code Challenge**: SHA-256 hash of verifier Base64URL-encoded without padding.
- **State Verification**: Constant-time comparison between initiation state and callback state.
- **Session Lifecycle**: Automatically persists access and refresh tokens into Android Keystore and records connection status in Room `ProviderConnectionEntity`.

### C. `DesktopHostBridge` & `AgentRuntimeAdapter` (`com.darkmodestudio.commandcenter.core.agent`)
Enables mobile Dark Mode Studio to orchestrate local coding agents executing on developer workstations without running foreign binaries on Android:
- **Codex**: Connects via ChatGPT account session on paired desktop host.
- **Claude Code**: Connects via official Claude subscription session on paired desktop host.
- **Antigravity**: Connects via Google account and `agy` system keyring on paired desktop host.
- **Truthful Status Reporting**: When host is unconfigured or offline, UI shows `"Desktop host required — [Pair Computer]"`.

---

## 4. Room Database Schema v6

### `provider_connections` Table:
```sql
CREATE TABLE IF NOT EXISTS `provider_connections` (
    `providerId` TEXT NOT NULL,
    `authMethod` TEXT NOT NULL,
    `connectionState` TEXT NOT NULL,
    `accountDisplayName` TEXT,
    `accountId` TEXT,
    `workspaceName` TEXT,
    `grantedScopes` TEXT,
    `expiresAt` INTEGER,
    `lastVerifiedAt` TEXT,
    `lastError` TEXT,
    `runtimeHostId` TEXT,
    PRIMARY KEY(`providerId`)
);
```

### `desktop_hosts` Table:
```sql
CREATE TABLE IF NOT EXISTS `desktop_hosts` (
    `hostId` TEXT NOT NULL,
    `hostName` TEXT NOT NULL,
    `hostAddress` TEXT NOT NULL,
    `isOnline` INTEGER NOT NULL,
    `lastSeen` TEXT NOT NULL,
    `authToken` TEXT,
    `availableAgents` TEXT NOT NULL,
    PRIMARY KEY(`hostId`)
);
```
