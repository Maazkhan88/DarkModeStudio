# Dark Mode Studio — System Architecture

## Architecture Overview

Dark Mode Studio Desktop consists of two tightly integrated layers:

1. **Desktop UI Layer (React 19 + TypeScript + Vite + Tailwind CSS)**:
   - Command Center Dashboard with live agent status, execution metrics, and activity stream.
   - Global Command Input Bar with instant task decomposition preview.
   - Project Workspace with 12 distinct functional tabs.
   - Live Run View with real-time log streaming over WebSockets.
   - Universal Command Palette (`⌘K`).
   - Signature components: `ExecutionRail` and `AgentNode`.

2. **Core Desktop Engine (Node.js + Express + WebSocket + SQLite + Git Worktrees)**:
   - **Orchestrator**: Multi-agent task decomposition, execution sequencing, and structured handoff packaging.
   - **Database**: SQLite database initialized with schema migrations and seed records (`dms_desktop.sqlite`).
   - **Provider Adapters**: Modular bridge for Codex, Claude Code, and Google Antigravity.
   - **Git & Worktree Manager**: Automated creation of isolated sandbox directories under `.dms-worktrees/`.
   - **Terminal Manager**: PTY process manager supporting interactive shell execution.
