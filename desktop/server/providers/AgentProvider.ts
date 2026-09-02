export type AgentStatus = 'OFFLINE' | 'IDLE' | 'THINKING' | 'EXECUTING' | 'BLOCKED' | 'COMPLETED';

export interface AgentCapabilities {
  supportsPlanning: boolean;
  supportsCodeEditing: boolean;
  supportsBrowserTesting: boolean;
  supportsStreamingOutput: boolean;
  supportsWorktrees: boolean;
  supportedTools: string[];
}

export interface ProjectHandoffContext {
  projectId: string;
  projectName: string;
  taskTitle: string;
  taskDescription?: string;
  filesChanged?: string[];
  gitDiffSummary?: string;
  testsStatus?: { passed: number; failed: number; summary: string };
  instructionsForReceiver: string;
}

export interface AgentExecutionOptions {
  sessionId: string;
  workingDirectory: string;
  worktreeBranch?: string;
  env?: Record<string, string>;
  timeoutMs?: number;
  onLogChunk: (type: 'stdout' | 'stderr' | 'thought', text: string) => void;
  onStatusChange: (status: AgentStatus) => void;
}

export interface AgentRunResult {
  isSuccess: boolean;
  exitCode: number;
  summary: string;
  filesModified: string[];
  logs: string[];
  suggestedNextStep?: string;
}

export interface AgentSession {
  sessionId: string;
  agentId: string;
  workingDirectory: string;
  status: AgentStatus;
  startedAt: number;
}

export interface AgentAuthDetectionResult {
  isAuthenticated: boolean;
  accountLabel?: string;
  authType: string;
  errorMessage?: string;
}

export interface AgentLoginActionResult {
  isSuccess: boolean;
  loginInstructions: string;
  errorMessage?: string;
}

export interface AgentVerificationResult {
  isVerified: boolean;
  account?: string;
  capabilities: string[];
  errorMessage?: string;
}

export interface AgentProvider {
  id: string;
  name: string;
  role: string;
  detectInstallation(): Promise<{ isInstalled: boolean; version?: string; isAuthenticated: boolean; instructions?: string }>;
  detectAuth(): Promise<AgentAuthDetectionResult>;
  startLogin(): Promise<AgentLoginActionResult>;
  verifyAuth(): Promise<AgentVerificationResult>;
  startSession(options: AgentExecutionOptions): Promise<AgentSession>;
  sendPrompt(sessionId: string, prompt: string, context?: ProjectHandoffContext): Promise<AgentRunResult>;
  cancelSession(sessionId: string): Promise<void>;
  getStatus(sessionId: string): AgentStatus;
  getCapabilities(): AgentCapabilities;
}
