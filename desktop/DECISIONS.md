# DARK MODE STUDIO — ARCHITECTURAL DECISIONS

## DEC-001: Desktop-First React + Vite + Node Engine Architecture
- **Status**: ACCEPTED
- **Made By**: Codex (Lead Architect)
- **Approved By**: User (Maaz Khan)
- **Reason**: Provides superior desktop command center performance, low-latency WebSocket log streaming, direct local PTY terminal access, and native Git worktree process isolation.
- **Implications**: The desktop application operates in `/desktop` with clean separation from the companion Android app in `/app`.

## DEC-002: Use Locally Authenticated Agent CLIs (No Forced Paid API Dependency)
- **Status**: ACCEPTED
- **Made By**: Codex (Lead Architect)
- **Approved By**: User (Maaz Khan)
- **Reason**: Uses existing official subscriptions for Codex, Claude Code, and Google Antigravity without requiring redundant paid API meters.
- **Implications**: Agent adapters invoke official local CLIs and session bridges with structured handoff contexts.

## DEC-003: Isolated Git Worktrees for Multi-Agent Task Execution
- **Status**: ACCEPTED
- **Made By**: Codex (Lead Architect)
- **Approved By**: User (Maaz Khan)
- **Reason**: Prevents concurrent agents (e.g. Claude implementing while Antigravity tests) from overwriting or corrupting each other's working copies.
- **Implications**: Worktrees are allocated in `.dms-worktrees/<task-id>-<agent>` on branch `dms/task-<id>-<agent>`.

## DEC-004: Structured Handoff Packages (Not Raw Chat Transcripts)
- **Status**: ACCEPTED
- **Made By**: Codex (Lead Architect)
- **Approved By**: User (Maaz Khan)
- **Reason**: Reduces token bloat and context loss by packaging exact task objectives, file diffs, test results, and targeted instructions for receiving agents.
- **Implications**: Every stage transition generates a `ProjectHandoffContext` payload.
