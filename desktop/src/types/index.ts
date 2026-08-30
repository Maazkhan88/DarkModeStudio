export type AgentStatus = 'OFFLINE' | 'IDLE' | 'THINKING' | 'EXECUTING' | 'BLOCKED' | 'COMPLETED';

export interface Project {
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

export interface Agent {
  id: string;
  name: string;
  provider: string;
  role: string;
  capabilities: string[];
  status: AgentStatus;
  is_installed: boolean;
  is_authenticated: boolean;
  current_task_id?: string;
  tasks_today_count: number;
}

export interface Task {
  id: string;
  objective_id?: string;
  project_id: string;
  title: string;
  description?: string;
  status: 'BACKLOG' | 'PLANNED' | 'READY' | 'RUNNING' | 'REVIEW' | 'TESTING' | 'BLOCKED' | 'WAITING_FOR_USER' | 'READY_TO_MERGE' | 'DONE' | 'FAILED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  assigned_agent_id?: string;
  reviewer_agent_id?: string;
  worktree_path?: string;
  branch_name?: string;
  created_at: string;
  started_at?: string;
  completed_at?: string;
}

export interface WorkflowStage {
  id: string;
  stageName: string;
  stageOrder: number;
  agentId: string;
  agentName: string;
  role: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  durationSeconds: number;
  summary?: string;
  filesModified?: string[];
}

export interface WorkflowExecutionPlan {
  objectiveId: string;
  projectId: string;
  rawPrompt: string;
  strategy: string;
  stages: WorkflowStage[];
}

export interface Decision {
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

export interface ProjectMemory {
  id: string;
  project_id: string;
  category: string;
  title: string;
  content: string;
  updated_at: string;
}

export interface ActivityEvent {
  id: string;
  project_id?: string;
  actor_name: string;
  actor_type: string;
  title: string;
  detail?: string;
  event_type: string;
  created_at: string;
}

export interface GitCommit {
  hash: string;
  author: string;
  date: string;
  message: string;
}
