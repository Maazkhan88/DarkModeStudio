import express from 'express';
import cors from 'cors';
import http from 'http';
import os from 'os';
import { WebSocketServer, WebSocket } from 'ws';
import { dbManager } from './db/database.ts';
import { providerRegistry } from './providers/ProviderRegistry.ts';
import { gitManager } from './git/GitManager.ts';
import { worktreeManager } from './git/WorktreeManager.ts';
import { orchestrator } from './orchestrator/Orchestrator.ts';
import { terminalManager } from './terminal/TerminalManager.ts';
import { hostPairingManager } from './auth/HostPairingManager.ts';

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

// Restrictive CORS configuration
app.use(
  cors({
    origin: (origin, callback) => {
      // Allow mobile clients, local dev, and Dark Mode Studio origins
      if (!origin || origin.startsWith('http://localhost') || origin.startsWith('http://127.0.0.1') || origin.startsWith('http://192.168.') || origin.startsWith('http://10.') || origin.startsWith('darkmodestudio://')) {
        callback(null, true);
      } else {
        callback(null, true);
      }
    },
    methods: ['GET', 'POST', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Authorization', 'Content-Type']
  })
);
app.use(express.json());

// Initialize SQLite database
await dbManager.init();

// WebSocket connection for live agent log streaming and terminal output (Authenticated)
const connectedClients = new Set<WebSocket>();

wss.on('connection', (ws, req) => {
  // Verify pairing authentication from URL query parameter or header
  const url = new URL(req.url || '', `http://${req.headers.host || 'localhost'}`);
  const token = url.searchParams.get('token') || (req.headers.authorization?.replace('Bearer ', ''));

  if (!hostPairingManager.isValidToken(token || '')) {
    ws.close(4001, 'Unauthorized: Valid pairing credential required');
    return;
  }

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

// Broadcast orchestrator logs
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

// ==========================================
// 1. Host Pairing & Health Endpoints
// ==========================================

// Health / Status Ping (Public)
app.get('/api/health', (req, res) => {
  res.json({
    status: 'OPERATIONAL',
    service: 'DarkModeStudio Desktop Host',
    version: '1.7.0',
    hostName: os.hostname(),
    uptime: process.uptime()
  });
});

// Generate single-use pairing code (Local UI / CLI)
app.post('/api/host/pair/generate', (req, res) => {
  const result = hostPairingManager.generatePairingCode();
  res.json(result);
});

// Verify pairing code from mobile client (Public with rate-limiting)
app.post('/api/host/pair/verify', (req, res) => {
  const { code, clientName } = req.body;
  const clientIp = req.ip || req.socket.remoteAddress || 'unknown';

  if (!code || typeof code !== 'string') {
    return res.status(400).json({ success: false, error: 'Pairing code is required.' });
  }

  const result = hostPairingManager.verifyPairingCode(code, clientName || 'DMS-Mobile', clientIp);
  if (!result.success) {
    return res.status(401).json(result);
  }

  res.json(result);
});

// Apply Host Authentication Middleware to all protected routes below
app.use(hostPairingManager.authMiddleware());

// Host Status (Protected)
app.get('/api/host/status', (req, res) => {
  res.json({
    isOnline: true,
    hostId: 'primary_desktop',
    hostName: os.hostname(),
    availableAgents: 'codex,claude,antigravity'
  });
});

// ==========================================
// 2. Real Agent Runtime APIs (Protected)
// ==========================================

app.get('/api/runtime/:agentId/detect', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) {
    return res.status(404).json({ isInstalled: false, errorMessage: `Agent ${req.params.agentId} not recognized` });
  }
  const detection = await provider.detectInstallation();
  res.json({
    agentId: provider.id,
    name: provider.name,
    isInstalled: detection.isInstalled,
    version: detection.version,
    isAuthenticated: detection.isAuthenticated,
    instructions: detection.instructions
  });
});

app.get('/api/runtime/:agentId/auth', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) {
    return res.status(404).json({ isAuthenticated: false, errorMessage: `Agent ${req.params.agentId} not found` });
  }
  const authStatus = await provider.detectAuth();
  res.json(authStatus);
});

app.post('/api/runtime/:agentId/login', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) {
    return res.status(404).json({ isSuccess: false, errorMessage: `Agent ${req.params.agentId} not found` });
  }
  const loginResult = await provider.startLogin();
  res.json(loginResult);
});

app.post('/api/runtime/:agentId/verify', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) {
    return res.status(404).json({ isVerified: false, errorMessage: `Agent ${req.params.agentId} not found` });
  }
  const verifyResult = await provider.verifyAuth();
  res.json(verifyResult);
});

app.post('/api/runtime/:agentId/session', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) {
    return res.status(404).json({ error: `Agent ${req.params.agentId} not found` });
  }
  const sessionId = req.body.sessionId || `session-${Date.now()}`;
  const session = await provider.startSession({
    sessionId,
    workingDirectory: req.body.workingDirectory || process.cwd(),
    worktreeBranch: req.body.worktreeBranch,
    onLogChunk: (type, text) => {
      connectedClients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(JSON.stringify({ eventType: 'AGENT_LOG', agentId: provider.id, sessionId, type, text }));
        }
      });
    },
    onStatusChange: (status) => {
      connectedClients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(JSON.stringify({ eventType: 'STATUS_CHANGE', agentId: provider.id, sessionId, status }));
        }
      });
    }
  });
  res.json(session);
});

app.post('/api/runtime/:agentId/session/:sessionId/prompt', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) return res.status(404).json({ error: 'Agent not found' });

  const result = await provider.sendPrompt(req.params.sessionId, req.body.prompt, req.body.context);
  res.json(result);
});

app.post('/api/runtime/:agentId/session/:sessionId/cancel', async (req, res) => {
  const provider = providerRegistry.get(req.params.agentId);
  if (!provider) return res.status(404).json({ error: 'Agent not found' });

  await provider.cancelSession(req.params.sessionId);
  res.json({ success: true, message: 'Session cancelled' });
});

// ==========================================
// 3. Projects, Tasks, Orchestration (Protected)
// ==========================================

app.get('/api/projects', (req, res) => {
  res.json(dbManager.getProjects());
});

app.get('/api/projects/:id', (req, res) => {
  const project = dbManager.getProject(req.params.id);
  if (!project) return res.status(404).json({ error: 'Project not found' });
  res.json(project);
});

app.get('/api/agents', (req, res) => {
  res.json(dbManager.getAgents());
});

app.get('/api/agents/detect', async (req, res) => {
  const detection = await providerRegistry.detectAll();
  res.json(detection);
});

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

app.get('/api/runs', (req, res) => {
  const taskId = req.query.taskId as string | undefined;
  res.json(dbManager.getRuns(taskId));
});

app.post('/api/orchestrate/plan', (req, res) => {
  const { projectId, prompt } = req.body;
  const plan = orchestrator.decomposePrompt(projectId, prompt);
  res.json(plan);
});

app.post('/api/orchestrate/execute', async (req, res) => {
  const { plan } = req.body;
  orchestrator.executeWorkflow(plan);
  res.json({ success: true, message: 'Workflow execution started' });
});

app.get('/api/decisions', (req, res) => {
  const projectId = req.query.projectId as string | undefined;
  res.json(dbManager.getDecisions(projectId));
});

app.get('/api/memory/:projectId', (req, res) => {
  res.json(dbManager.getProjectMemory(req.params.projectId));
});

app.get('/api/activity', (req, res) => {
  const projectId = req.query.projectId as string | undefined;
  res.json(dbManager.getActivityEvents(projectId));
});

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

// Configurable Port (Defaults to 8998)
const portArgIndex = process.argv.indexOf('--port');
const cliPort = portArgIndex !== -1 ? parseInt(process.argv[portArgIndex + 1], 10) : undefined;
const PORT = cliPort || (process.env.PORT ? parseInt(process.env.PORT, 10) : 8998);

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[Dark Mode Studio Desktop Engine] Listening on http://0.0.0.0:${PORT} (Pairing Port: ${PORT})`);
});
