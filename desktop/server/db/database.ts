import fs from 'fs';
import path from 'path';
import initSqlJs, { Database as SqlJsDatabase } from 'sql.js';

export interface ProjectRecord {
  id: string;
  name: string;
  description: string;
  local_path: string;
  git_repo_url: string;
  active_branch: string;
  status: string;
  progress: number;
  created_at: string;
  updated_at: string;
}

export interface AgentRecord {
  id: string;
  name: string;
  provider: string;
  role: string;
  capabilities: string; // JSON array
  status: string;
  cli_path?: string;
  is_installed: number;
  is_authenticated: number;
  current_task_id?: string;
  tasks_today_count: number;
}

export interface TaskRecord {
  id: string;
  objective_id?: string;
  project_id: string;
  title: string;
  description?: string;
  status: string;
  priority: string;
  assigned_agent_id?: string;
  reviewer_agent_id?: string;
  worktree_path?: string;
  branch_name?: string;
  created_at: string;
  started_at?: string;
  completed_at?: string;
}

export interface AgentRunRecord {
  id: string;
  task_id: string;
  agent_id: string;
  stage_name: string;
  stage_order: number;
  status: string;
  started_at?: string;
  completed_at?: string;
  duration_seconds: number;
  exit_code?: number;
  summary?: string;
}

export interface DecisionRecord {
  id: string;
  project_id: string;
  code: string;
  title: string;
  status: string;
  made_by_agent_id: string;
  approved_by: string;
  reason: string;
  implications?: string;
  created_at: string;
}

export interface ProjectMemoryRecord {
  id: string;
  project_id: string;
  category: string;
  title: string;
  content: string;
  updated_at: string;
}

export interface ActivityEventRecord {
  id: string;
  project_id?: string;
  actor_name: string;
  actor_type: string;
  title: string;
  detail?: string;
  event_type: string;
  created_at: string;
}

class DmsDatabaseManager {
  private db: SqlJsDatabase | null = null;
  private dbPath: string;

  constructor() {
    const dataDir = path.resolve(process.cwd(), 'data');
    if (!fs.existsSync(dataDir)) {
      fs.mkdirSync(dataDir, { recursive: true });
    }
    this.dbPath = path.join(dataDir, 'dms_desktop.sqlite');
  }

  async init(): Promise<void> {
    const SQL = await initSqlJs();
    if (fs.existsSync(this.dbPath)) {
      const buffer = fs.readFileSync(this.dbPath);
      this.db = new SQL.Database(buffer);
    } else {
      this.db = new SQL.Database();
    }

    this.initSchema();
    this.seedDefaults();
    this.save();
  }

  private save(): void {
    if (!this.db) return;
    const data = this.db.export();
    const buffer = Buffer.from(data);
    fs.writeFileSync(this.dbPath, buffer);
  }

  private initSchema(): void {
    if (!this.db) return;

    this.db.run(`
      CREATE TABLE IF NOT EXISTS projects (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        local_path TEXT NOT NULL,
        git_repo_url TEXT,
        active_branch TEXT DEFAULT 'main',
        status TEXT DEFAULT 'ACTIVE',
        progress REAL DEFAULT 0.0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS agents (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        provider TEXT NOT NULL,
        role TEXT NOT NULL,
        capabilities TEXT NOT NULL,
        status TEXT DEFAULT 'IDLE',
        cli_path TEXT,
        is_installed INTEGER DEFAULT 0,
        is_authenticated INTEGER DEFAULT 0,
        current_task_id TEXT,
        tasks_today_count INTEGER DEFAULT 0
      );

      CREATE TABLE IF NOT EXISTS objectives (
        id TEXT PRIMARY KEY,
        project_id TEXT NOT NULL,
        raw_prompt TEXT NOT NULL,
        strategy TEXT DEFAULT 'AUTO',
        status TEXT DEFAULT 'RUNNING',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS tasks (
        id TEXT PRIMARY KEY,
        objective_id TEXT,
        project_id TEXT NOT NULL,
        title TEXT NOT NULL,
        description TEXT,
        status TEXT DEFAULT 'READY',
        priority TEXT DEFAULT 'HIGH',
        assigned_agent_id TEXT,
        reviewer_agent_id TEXT,
        worktree_path TEXT,
        branch_name TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        started_at TIMESTAMP,
        completed_at TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS agent_runs (
        id TEXT PRIMARY KEY,
        task_id TEXT NOT NULL,
        agent_id TEXT NOT NULL,
        stage_name TEXT NOT NULL,
        stage_order INTEGER NOT NULL,
        status TEXT DEFAULT 'QUEUED',
        started_at TIMESTAMP,
        completed_at TIMESTAMP,
        duration_seconds INTEGER DEFAULT 0,
        exit_code INTEGER,
        summary TEXT
      );

      CREATE TABLE IF NOT EXISTS run_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        run_id TEXT NOT NULL,
        stream_type TEXT NOT NULL,
        chunk_text TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS decisions (
        id TEXT PRIMARY KEY,
        project_id TEXT NOT NULL,
        code TEXT NOT NULL UNIQUE,
        title TEXT NOT NULL,
        status TEXT DEFAULT 'ACCEPTED',
        made_by_agent_id TEXT,
        approved_by TEXT DEFAULT 'User',
        reason TEXT NOT NULL,
        implications TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS project_memory (
        id TEXT PRIMARY KEY,
        project_id TEXT NOT NULL,
        category TEXT NOT NULL,
        title TEXT NOT NULL,
        content TEXT NOT NULL,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS activity_events (
        id TEXT PRIMARY KEY,
        project_id TEXT,
        actor_name TEXT NOT NULL,
        actor_type TEXT NOT NULL,
        title TEXT NOT NULL,
        detail TEXT,
        event_type TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);
  }

  private seedDefaults(): void {
    if (!this.db) return;

    // Check if projects exist
    const res = this.db.exec("SELECT COUNT(*) as count FROM projects");
    const count = res[0]?.values[0]?.[0] as number;
    if (count > 0) return;

    // Seed Real Projects
    this.db.run(`
      INSERT INTO projects (id, name, description, local_path, git_repo_url, active_branch, status, progress) VALUES
      ('darkmodestudio', 'DarkModeStudio', 'Unified AI Development Command Center & Multi-Agent Orchestrator', 'e:/DMSCC', 'https://github.com/Maazkhan88/DarkModeStudio', 'main', 'ACTIVE', 0.94),
      ('secondme', 'SecondMe', 'Second You digital twin & autonomous memory clone engine', 'e:/SecondMe', 'https://github.com/Maazkhan88/SecondMe', 'main', 'ACTIVE', 0.68),
      ('ghostcart', 'GhostCart', 'Stealth headless checkout & edge shopping engine', 'e:/Ghostcart', 'https://github.com/Maazkhan88/Ghostcart', 'main', 'ACTIVE', 0.81),
      ('agstudio', 'AGStudio', 'Agentic developer IDE and code generation runtime', 'e:/AGStudio', 'https://github.com/Maazkhan88/AGStudio', 'main', 'ACTIVE', 0.74),
      ('tonecast', 'ToneCast', 'Adaptive audio AI synthesis & spatial audio pipeline', 'e:/ToneCast', 'https://github.com/Maazkhan88/ToneCast', 'main', 'ACTIVE', 0.52);
    `);

    // Seed Primary AI Agents
    this.db.run(`
      INSERT INTO agents (id, name, provider, role, capabilities, status, is_installed, is_authenticated, tasks_today_count) VALUES
      ('codex', 'Codex', 'codex', 'Lead Architect & Code Reviewer', '["architecture", "planning", "code_review", "refactor", "git", "tests"]', 'IDLE', 1, 1, 6),
      ('claude', 'Claude Code', 'claude', 'Primary Developer & Implementation Agent', '["implementation", "code_modifications", "frontend", "backend", "debugging", "documentation"]', 'EXECUTING', 1, 1, 8),
      ('antigravity', 'Google Antigravity', 'antigravity', 'QA & Browser Verification Agent', '["browser_work", "visual_testing", "android_workflows", "independent_validation", "ui_verification"]', 'THINKING', 1, 1, 4);
    `);

    // Seed Tasks
    this.db.run(`
      INSERT INTO tasks (id, project_id, title, description, status, priority, assigned_agent_id, reviewer_agent_id) VALUES
      ('task-101', 'secondme', 'Finish the SecondMe Focus Screen', 'Implement OLED pure black visual controls, biometric authentication, and stream live timer telemetry', 'RUNNING', 'HIGH', 'claude', 'codex'),
      ('task-102', 'darkmodestudio', 'Build Desktop Agent Orchestration Engine', 'Multi-agent coordination layer with isolated Git worktrees and handoff packages', 'RUNNING', 'HIGH', 'codex', 'antigravity'),
      ('task-103', 'ghostcart', 'Verify headless checkout edge response', 'Ensure sub-50ms latency across Cloudflare workers and validate wishlist sync', 'READY_TO_MERGE', 'MEDIUM', 'claude', 'codex'),
      ('task-104', 'tonecast', 'Spatial audio filter pipeline benchmark', 'Benchmark latency across 32 concurrent multichannel audio tracks', 'READY', 'MEDIUM', 'codex', 'antigravity');
    `);

    // Seed Runs
    this.db.run(`
      INSERT INTO agent_runs (id, task_id, agent_id, stage_name, stage_order, status, duration_seconds, summary) VALUES
      ('run-101-1', 'task-101', 'codex', 'Architecture & Plan', 1, 'COMPLETED', 194, 'Architecture plan approved. Defined Room DAO schema and Compose theme.'),
      ('run-101-2', 'task-101', 'claude', 'Implementation', 2, 'RUNNING', 762, 'Implementing FocusScreen.kt with orbital canvas and circular node rail.'),
      ('run-101-3', 'task-101', 'antigravity', 'Visual QA & Verification', 3, 'QUEUED', 0, 'Pending completion of implementation stage.'),
      ('run-101-4', 'task-101', 'codex', 'Final Review', 4, 'QUEUED', 0, 'Awaiting QA pass.');
    `);

    // Seed Decisions
    this.db.run(`
      INSERT INTO decisions (id, project_id, code, title, status, made_by_agent_id, approved_by, reason, implications) VALUES
      ('dec-001', 'darkmodestudio', 'DEC-001', 'Use Isolated Git Worktrees for Agent Tasks', 'ACCEPTED', 'codex', 'Maaz Khan', 'Prevents concurrent agent code collisions and file corruptions.', 'Every agent gets an isolated directory branch dms/task-<id>-<agent>.'),
      ('dec-002', 'secondme', 'DEC-034', 'Room SQLite as Single Source of Truth', 'ACCEPTED', 'codex', 'Maaz Khan', 'Guarantees reactive UI updates without direct API mutation leaks.', 'All network synchronization writes directly to Room DAO Flows.');
    `);

    // Seed Project Memory
    this.db.run(`
      INSERT INTO project_memory (id, project_id, category, title, content) VALUES
      ('mem-001', 'darkmodestudio', 'DESIGN_SYSTEM', 'OLED Monochrome Baseline', 'OLED pure black (#000000), Surface01 (#050505), Surface02 (#090909), white typography, circular status nodes, execution rails.'),
      ('mem-002', 'darkmodestudio', 'ARCHITECTURE', 'Multi-Agent Handoff Grammar', 'Structured handoff packages containing task objective, diffs, test pass/fail metrics, and receiving agent instructions.');
    `);

    // Seed Activity Events
    this.db.run(`
      INSERT INTO activity_events (id, project_id, actor_name, actor_type, title, detail, event_type) VALUES
      ('evt-001', 'secondme', 'Codex', 'AGENT', 'Approved architecture plan for SecondMe Focus Screen', 'Plan verified against DEC-034 single source of truth guidelines', 'AGENT_REVIEW'),
      ('evt-002', 'secondme', 'Claude Code', 'AGENT', 'Started implementation in isolated worktree dms/task-101-claude', 'Working on FocusScreen.kt and TimerEngine.kt', 'AGENT_WORK'),
      ('evt-003', 'darkmodestudio', 'Maaz Khan', 'USER', 'Instructed command center: Finish Focus Screen and prepare PR', 'Automated orchestration workflow triggered with 4 stages', 'USER_COMMAND');
    `);
  }

  // Projects CRUD
  getProjects(): ProjectRecord[] {
    if (!this.db) return [];
    const res = this.db.exec("SELECT * FROM projects ORDER BY updated_at DESC");
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as ProjectRecord;
    });
  }

  getProject(id: string): ProjectRecord | null {
    if (!this.db) return null;
    const stmt = this.db.prepare("SELECT * FROM projects WHERE id = :id");
    stmt.bind({ ':id': id });
    if (stmt.step()) {
      const row = stmt.getAsObject() as unknown as ProjectRecord;
      stmt.free();
      return row;
    }
    stmt.free();
    return null;
  }

  // Agents CRUD
  getAgents(): AgentRecord[] {
    if (!this.db) return [];
    const res = this.db.exec("SELECT * FROM agents ORDER BY name ASC");
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as AgentRecord;
    });
  }

  updateAgentStatus(id: string, status: string, currentTaskId?: string): void {
    if (!this.db) return;
    this.db.run("UPDATE agents SET status = ?, current_task_id = ? WHERE id = ?", [status, currentTaskId || null, id]);
    this.save();
  }

  // Tasks CRUD
  getTasks(projectId?: string): TaskRecord[] {
    if (!this.db) return [];
    const query = projectId ? "SELECT * FROM tasks WHERE project_id = ? ORDER BY created_at DESC" : "SELECT * FROM tasks ORDER BY created_at DESC";
    const res = projectId ? this.db.exec(query, [projectId]) : this.db.exec(query);
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as TaskRecord;
    });
  }

  createTask(task: {
    id: string;
    projectId: string;
    title: string;
    description?: string;
    assignedAgentId?: string;
    reviewerAgentId?: string;
    priority?: string;
    status?: string;
  }): void {
    if (!this.db) return;
    this.db.run(
      "INSERT INTO tasks (id, project_id, title, description, assigned_agent_id, reviewer_agent_id, priority, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
      [
        task.id,
        task.projectId,
        task.title,
        task.description || '',
        task.assignedAgentId || null,
        task.reviewerAgentId || null,
        task.priority || 'HIGH',
        task.status || 'READY'
      ]
    );
    this.save();
  }

  updateTaskStatus(id: string, status: string): void {
    if (!this.db) return;
    this.db.run("UPDATE tasks SET status = ? WHERE id = ?", [status, id]);
    this.save();
  }

  // Runs CRUD
  getRuns(taskId?: string): AgentRunRecord[] {
    if (!this.db) return [];
    const query = taskId ? "SELECT * FROM agent_runs WHERE task_id = ? ORDER BY stage_order ASC" : "SELECT * FROM agent_runs ORDER BY started_at DESC";
    const res = taskId ? this.db.exec(query, [taskId]) : this.db.exec(query);
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as AgentRunRecord;
    });
  }

  createRun(run: {
    id: string;
    taskId: string;
    agentId: string;
    stageName: string;
    stageOrder: number;
    status: string;
  }): void {
    if (!this.db) return;
    this.db.run(
      "INSERT INTO agent_runs (id, task_id, agent_id, stage_name, stage_order, status) VALUES (?, ?, ?, ?, ?, ?)",
      [run.id, run.taskId, run.agentId, run.stageName, run.stageOrder, run.status]
    );
    this.save();
  }

  updateRunStatus(id: string, status: string, summary?: string, durationSeconds?: number): void {
    if (!this.db) return;
    this.db.run(
      "UPDATE agent_runs SET status = ?, summary = COALESCE(?, summary), duration_seconds = COALESCE(?, duration_seconds) WHERE id = ?",
      [status, summary || null, durationSeconds || null, id]
    );
    this.save();
  }

  // Activity Events
  getActivityEvents(projectId?: string): ActivityEventRecord[] {
    if (!this.db) return [];
    const query = projectId ? "SELECT * FROM activity_events WHERE project_id = ? ORDER BY created_at DESC" : "SELECT * FROM activity_events ORDER BY created_at DESC LIMIT 50";
    const res = projectId ? this.db.exec(query, [projectId]) : this.db.exec(query);
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as ActivityEventRecord;
    });
  }

  logActivity(evt: {
    id: string;
    projectId?: string;
    actorName: string;
    actorType: string;
    title: string;
    detail?: string;
    eventType: string;
  }): void {
    if (!this.db) return;
    this.db.run(
      "INSERT INTO activity_events (id, project_id, actor_name, actor_type, title, detail, event_type) VALUES (?, ?, ?, ?, ?, ?, ?)",
      [evt.id, evt.projectId || null, evt.actorName, evt.actorType, evt.title, evt.detail || null, evt.eventType]
    );
    this.save();
  }

  // Decisions
  getDecisions(projectId?: string): DecisionRecord[] {
    if (!this.db) return [];
    const query = projectId ? "SELECT * FROM decisions WHERE project_id = ? ORDER BY created_at DESC" : "SELECT * FROM decisions ORDER BY created_at DESC";
    const res = projectId ? this.db.exec(query, [projectId]) : this.db.exec(query);
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as DecisionRecord;
    });
  }

  // Memory
  getProjectMemory(projectId: string): ProjectMemoryRecord[] {
    if (!this.db) return [];
    const res = this.db.exec("SELECT * FROM project_memory WHERE project_id = ? ORDER BY category ASC", [projectId]);
    if (!res[0]) return [];
    const columns = res[0].columns;
    return res[0].values.map((row) => {
      const obj: any = {};
      columns.forEach((col, i) => (obj[col] = row[i]));
      return obj as ProjectMemoryRecord;
    });
  }
}

export const dbManager = new DmsDatabaseManager();
