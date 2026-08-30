import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Project, Agent, Task, WorkflowExecutionPlan, Decision, ActivityEvent } from '../types';

interface DmsContextType {
  projects: Project[];
  selectedProjectId: string | null;
  setSelectedProjectId: (id: string | null) => void;
  agents: Agent[];
  tasks: Task[];
  decisions: Decision[];
  activityEvents: ActivityEvent[];
  activePlan: WorkflowExecutionPlan | null;
  activeTaskId: string | null;
  setActiveTaskId: (id: string | null) => void;
  isCommandPaletteOpen: boolean;
  setIsCommandPaletteOpen: (open: boolean) => void;
  isLiveRunOpen: boolean;
  setIsLiveRunOpen: (open: boolean) => void;
  terminalLogs: string[];
  activeView: 'home' | 'project' | 'team' | 'tasks' | 'git' | 'terminal' | 'settings';
  setActiveView: (view: 'home' | 'project' | 'team' | 'tasks' | 'git' | 'terminal' | 'settings') => void;
  createExecutionPlan: (prompt: string, projectId?: string) => Promise<WorkflowExecutionPlan>;
  executePlan: (plan: WorkflowExecutionPlan) => Promise<void>;
  sendTerminalCommand: (command: string) => void;
  refreshData: () => Promise<void>;
}

const DmsContext = createContext<DmsContextType | null>(null);

export const DmsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [agents, setAgents] = useState<Agent[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [decisions, setDecisions] = useState<Decision[]>([]);
  const [activityEvents, setActivityEvents] = useState<ActivityEvent[]>([]);
  const [activePlan, setActivePlan] = useState<WorkflowExecutionPlan | null>(null);
  const [activeTaskId, setActiveTaskId] = useState<string | null>(null);
  const [isCommandPaletteOpen, setIsCommandPaletteOpen] = useState(false);
  const [isLiveRunOpen, setIsLiveRunOpen] = useState(false);
  const [terminalLogs, setTerminalLogs] = useState<string[]>([
    'Dark Mode Studio Desktop Terminal Initialized.',
    'Connected to local PTY agent environment.'
  ]);
  const [activeView, setActiveView] = useState<'home' | 'project' | 'team' | 'tasks' | 'git' | 'terminal' | 'settings'>('home');
  const [ws, setWs] = useState<WebSocket | null>(null);

  const refreshData = useCallback(async () => {
    try {
      const [projRes, agentRes, taskRes, decRes, actRes] = await Promise.all([
        fetch('http://localhost:4000/api/projects').then((r) => r.json()),
        fetch('http://localhost:4000/api/agents').then((r) => r.json()),
        fetch('http://localhost:4000/api/tasks').then((r) => r.json()),
        fetch('http://localhost:4000/api/decisions').then((r) => r.json()),
        fetch('http://localhost:4000/api/activity').then((r) => r.json())
      ]);

      setProjects(projRes);
      if (!selectedProjectId && projRes.length > 0) {
        setSelectedProjectId(projRes[0].id);
      }
      setAgents(
        agentRes.map((a: any) => ({
          ...a,
          capabilities: typeof a.capabilities === 'string' ? JSON.parse(a.capabilities) : a.capabilities,
          is_installed: Boolean(a.is_installed),
          is_authenticated: Boolean(a.is_authenticated)
        }))
      );
      setTasks(taskRes);
      setDecisions(decRes);
      setActivityEvents(actRes);
    } catch {
      // Fallback mock seeds if server is bootstrapping
      const defaultProjects: Project[] = [
        {
          id: 'secondme',
          name: 'SecondMe',
          description: 'Second You digital twin & autonomous memory clone engine',
          local_path: 'e:/SecondMe',
          git_repo_url: 'https://github.com/Maazkhan88/SecondMe',
          active_branch: 'main',
          status: 'ACTIVE',
          progress: 0.68,
          created_at: '2026-08-30',
          updated_at: '2026-08-30 • 05:00 PM'
        },
        {
          id: 'ghostcart',
          name: 'GhostCart',
          description: 'Stealth headless checkout & edge shopping engine',
          local_path: 'e:/Ghostcart',
          git_repo_url: 'https://github.com/Maazkhan88/Ghostcart',
          active_branch: 'main',
          status: 'ACTIVE',
          progress: 0.81,
          created_at: '2026-08-30',
          updated_at: '2026-08-30 • 04:30 PM'
        },
        {
          id: 'darkmodestudio',
          name: 'DarkModeStudio',
          description: 'Unified AI Development Command Center & Multi-Agent Orchestrator',
          local_path: 'e:/DMSCC',
          git_repo_url: 'https://github.com/Maazkhan88/DarkModeStudio',
          active_branch: 'main',
          status: 'ACTIVE',
          progress: 0.94,
          created_at: '2026-08-30',
          updated_at: '2026-08-30 • 05:24 PM'
        },
        {
          id: 'agstudio',
          name: 'AGStudio',
          description: 'Agentic developer IDE and code generation runtime',
          local_path: 'e:/AGStudio',
          git_repo_url: 'https://github.com/Maazkhan88/AGStudio',
          active_branch: 'main',
          status: 'ACTIVE',
          progress: 0.74,
          created_at: '2026-08-30',
          updated_at: '2026-08-30 • 02:00 PM'
        },
        {
          id: 'tonecast',
          name: 'ToneCast',
          description: 'Adaptive audio AI synthesis & spatial audio pipeline',
          local_path: 'e:/ToneCast',
          git_repo_url: 'https://github.com/Maazkhan88/ToneCast',
          active_branch: 'main',
          status: 'ACTIVE',
          progress: 0.52,
          created_at: '2026-08-30',
          updated_at: '2026-08-30 • 01:15 PM'
        }
      ];
      setProjects(defaultProjects);
      if (!selectedProjectId) setSelectedProjectId('secondme');

      setAgents([
        {
          id: 'codex',
          name: 'Codex',
          provider: 'codex',
          role: 'Lead Architect & Code Reviewer',
          capabilities: ['architecture', 'planning', 'code_review', 'refactors', 'git'],
          status: 'IDLE',
          is_installed: true,
          is_authenticated: true,
          tasks_today_count: 6
        },
        {
          id: 'claude',
          name: 'Claude Code',
          provider: 'claude',
          role: 'Primary Developer & Implementation Agent',
          capabilities: ['implementation', 'frontend', 'backend', 'debugging'],
          status: 'EXECUTING',
          is_installed: true,
          is_authenticated: true,
          current_task_id: 'task-101',
          tasks_today_count: 8
        },
        {
          id: 'antigravity',
          name: 'Google Antigravity',
          provider: 'antigravity',
          role: 'QA & Verification Agent',
          capabilities: ['browser_work', 'visual_testing', 'android_workflows', 'ui_verification'],
          status: 'THINKING',
          is_installed: true,
          is_authenticated: true,
          tasks_today_count: 4
        }
      ]);
    }
  }, [selectedProjectId]);

  useEffect(() => {
    refreshData();
    const interval = setInterval(refreshData, 3000);
    return () => clearInterval(interval);
  }, [refreshData]);

  // Connect WebSocket
  useEffect(() => {
    const socket = new WebSocket('ws://localhost:4000/ws');
    socket.onopen = () => setWs(socket);
    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'AGENT_LOG') {
          setTerminalLogs((prev) => [...prev, `[${data.type}] ${data.chunk}`]);
        } else if (data.type === 'TERMINAL_OUTPUT') {
          setTerminalLogs((prev) => [...prev, data.data]);
        }
      } catch {
        // Raw string
      }
    };
    return () => socket.close();
  }, []);

  // Keyboard Shortcuts (⌘K for command palette)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setIsCommandPaletteOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const createExecutionPlan = async (prompt: string, projId?: string): Promise<WorkflowExecutionPlan> => {
    const targetProject = projId || selectedProjectId || 'secondme';
    try {
      const res = await fetch('http://localhost:4000/api/orchestrate/plan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ projectId: targetProject, prompt })
      });
      const plan = await res.json();
      setActivePlan(plan);
      return plan;
    } catch {
      // Local fallback planner
      const plan: WorkflowExecutionPlan = {
        objectiveId: 'obj-' + Date.now(),
        projectId: targetProject,
        rawPrompt: prompt,
        strategy: 'AUTO_ORCHESTRATED',
        stages: [
          {
            id: 's-1',
            stageName: 'Architecture & Technical Plan',
            stageOrder: 1,
            agentId: 'codex',
            agentName: 'Codex',
            role: 'Lead Architect',
            status: 'QUEUED',
            durationSeconds: 0
          },
          {
            id: 's-2',
            stageName: 'Implementation & Code Generation',
            stageOrder: 2,
            agentId: 'claude',
            agentName: 'Claude Code',
            role: 'Primary Developer',
            status: 'QUEUED',
            durationSeconds: 0
          },
          {
            id: 's-3',
            stageName: 'Automated QA & UI Verification',
            stageOrder: 3,
            agentId: 'antigravity',
            agentName: 'Google Antigravity',
            role: 'QA Engineer',
            status: 'QUEUED',
            durationSeconds: 0
          },
          {
            id: 's-4',
            stageName: 'Final Code Review & PR Approval',
            stageOrder: 4,
            agentId: 'codex',
            agentName: 'Codex',
            role: 'Lead Architect',
            status: 'QUEUED',
            durationSeconds: 0
          }
        ]
      };
      setActivePlan(plan);
      return plan;
    }
  };

  const executePlan = async (plan: WorkflowExecutionPlan): Promise<void> => {
    setIsLiveRunOpen(true);
    try {
      await fetch('http://localhost:4000/api/orchestrate/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ plan })
      });
    } catch (e) {
      console.error('Execute error:', e);
    }
  };

  const sendTerminalCommand = (command: string) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'TERMINAL_COMMAND', command, sessionId: 'default' }));
    } else {
      setTerminalLogs((prev) => [...prev, `$ ${command}`, 'Command executed locally in shadow terminal session.']);
    }
  };

  return (
    <DmsContext.Provider
      value={{
        projects,
        selectedProjectId,
        setSelectedProjectId,
        agents,
        tasks,
        decisions,
        activityEvents,
        activePlan,
        activeTaskId,
        setActiveTaskId,
        isCommandPaletteOpen,
        setIsCommandPaletteOpen,
        isLiveRunOpen,
        setIsLiveRunOpen,
        terminalLogs,
        activeView,
        setActiveView,
        createExecutionPlan,
        executePlan,
        sendTerminalCommand,
        refreshData
      }}
    >
      {children}
    </DmsContext.Provider>
  );
};

export const useDms = () => {
  const context = useContext(DmsContext);
  if (!context) throw new Error('useDms must be used within DmsProvider');
  return context;
};
