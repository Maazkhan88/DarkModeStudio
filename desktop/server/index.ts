import express from 'express';
import cors from 'cors';
import http from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import { dbManager } from './db/database.ts';
import { providerRegistry } from './providers/ProviderRegistry.ts';
import { gitManager } from './git/GitManager.ts';
import { worktreeManager } from './git/WorktreeManager.ts';
import { orchestrator } from './orchestrator/Orchestrator.ts';
import { terminalManager } from './terminal/TerminalManager.ts';

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

app.use(cors());
app.use(express.json());

// Initialize SQLite database
await dbManager.init();

// WebSocket connection for live agent log streaming and terminal output
const connectedClients = new Set<WebSocket>();

wss.on('connection', (ws) => {
  connectedClients.add(ws);

  ws.on('message', async (message) => {
    try {
      const data = JSON.parse(message.toString());
      if (data.type === 'TERMINAL_COMMAND') {
        await terminalManager.executeCommand(data.sessionId || 'default', data.command);
      }
    } catch (e) {
      console.error('WS Error:', e);
    }
  });

  ws.on('close', () => connectedClients.delete(ws));
});

// Broadcast orchestrator logs to all connected desktop UI clients
orchestrator.addLogListener((evt) => {
  const payload = JSON.stringify({ eventType: 'AGENT_LOG', ...evt });
  connectedClients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(payload);
    }
  });
});

// Broadcast terminal output
terminalManager.addOutputListener((evt) => {
  const payload = JSON.stringify({ type: 'TERMINAL_OUTPUT', ...evt });
  connectedClients.forEach((client) => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(payload);
    }
  });
});

// REST API Endpoints

// 1. Projects
app.get('/api/projects', (req, res) => {
  res.json(dbManager.getProjects());
});

app.get('/api/projects/:id', (req, res) => {
  const project = dbManager.getProject(req.params.id);
  if (!project) return res.status(404).json({ error: 'Project not found' });
  res.json(project);
});

// 2. Agents & Detection
app.get('/api/agents', (req, res) => {
  res.json(dbManager.getAgents());
});

app.get('/api/agents/detect', async (req, res) => {
  const detection = await providerRegistry.detectAll();
  res.json(detection);
});

// 3. Tasks
app.get('/api/tasks', (req, res) => {
  const projectId = req.query.projectId as string | undefined;
  res.json(dbManager.getTasks(projectId));
});

app.post('/api/tasks', (req, res) => {
  const id = 'task-' + Date.now();
  dbManager.createTask({ id, ...req.body });
  res.json({ success: true, id });
});

app.patch('/api/tasks/:id/status', (req, res) => {
  dbManager.updateTaskStatus(req.params.id, req.body.status);
  res.json({ success: true });
});

// 4. Runs & Stages
app.get('/api/runs', (req, res) => {
  const taskId = req.query.taskId as string | undefined;
  res.json(dbManager.getRuns(taskId));
});

// 5. Orchestrator: Plan & Execute
app.post('/api/orchestrate/plan', (req, res) => {
  const { projectId, prompt } = req.body;
  const plan = orchestrator.decomposePrompt(projectId, prompt);
  res.json(plan);
});

app.post('/api/orchestrate/execute', async (req, res) => {
  const { plan } = req.body;
  // Trigger in background without blocking request
  orchestrator.executeWorkflow(plan);
  res.json({ success: true, message: 'Workflow execution started' });
});

// 6. Decisions & Memory
app.get('/api/decisions', (req, res) => {
  const projectId = req.query.projectId as string | undefined;
  res.json(dbManager.getDecisions(projectId));
});

app.get('/api/memory/:projectId', (req, res) => {
  res.json(dbManager.getProjectMemory(req.params.projectId));
});

// 7. Activity Events
app.get('/api/activity', (req, res) => {
  const projectId = req.query.projectId as string | undefined;
  res.json(dbManager.getActivityEvents(projectId));
});

// 8. Git & Worktrees
app.get('/api/git/status', async (req, res) => {
  const projectPath = (req.query.path as string) || process.cwd();
  const status = await gitManager.getStatus(projectPath);
  res.json(status);
});

app.get('/api/git/commits', async (req, res) => {
  const projectPath = (req.query.path as string) || process.cwd();
  const commits = await gitManager.getRecentCommits(projectPath);
  res.json(commits);
});

app.get('/api/git/worktrees', (req, res) => {
  res.json(worktreeManager.listWorktrees());
});

// Start Server on port 4000
const PORT = 4000;
server.listen(PORT, () => {
  console.log(`[Dark Mode Studio Desktop Engine] Listening on http://localhost:${PORT}`);
});
