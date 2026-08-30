# Dark Mode Studio — Desktop AI Agent Command Center

Dark Mode Studio is a desktop-first unified AI development command center and multi-agent orchestrator. It allows you to coordinate **Codex (Lead Architect / Code Reviewer)**, **Claude Code (Primary Developer / Implementation Agent)**, and **Google Antigravity (QA & Verification Agent)** from a single interface.

---

## Key Features

1. **Multi-Agent Orchestration**:
   - Give one command (e.g. *"Finish the SecondMe Focus Screen, test everything, fix any problems and prepare a PR."*).
   - The Orchestrator automatically decomposes it into a 4-stage pipeline:
     1. **Codex**: Architecture & Technical Implementation Plan.
     2. **Claude Code**: Full code implementation in an isolated Git worktree.
     3. **Google Antigravity**: Automated unit, UI, and visual regression verification.
     4. **Codex**: Final code review, architectural audit, and merge approval.
2. **Local CLI Agent Integration**:
   - Integrates directly with official locally authenticated CLI runtimes (Codex, Claude Code, Google Antigravity).
   - No forced paid API usage when your existing subscriptions are active.
3. **Isolated Git Worktrees**:
   - Agents work strictly inside isolated sandboxes (`.dms-worktrees/task-<id>-<agent>`), ensuring no code collisions.
4. **Signature OLED Visual Language**:
   - Pure `#000000` OLED black canvas, `#050505` Surface01, soft editorial typography, circular status nodes, and signature **Execution Rails** (`●────●────◉────○`).
5. **Project Workspaces (12 Tabs)**:
   - `OVERVIEW`, `TASKS`, `AGENTS`, `CODE`, `GIT`, `RUNS`, `TESTS`, `MEMORY`, `FILES`, `DECISIONS`, `ACTIVITY`, `SETTINGS`.
6. **Integrated PTY Terminal**:
   - Multi-session terminal execution with project-aware working directories.
7. **Universal Command Palette**:
   - `CMD/CTRL + K` search and instant prompt dispatch across projects and agents.

---

## Quick Start

### 1. Install Dependencies
```bash
cd desktop
npm install
```

### 2. Run Tests
```bash
npm test
```

### 3. Build Production Desktop Bundle
```bash
npm run build
```

### 4. Start Desktop Server & Interface
```bash
npm run server
```
Or start Vite dev server:
```bash
npm run dev
```
Open `http://localhost:3000` to access the Command Center.
