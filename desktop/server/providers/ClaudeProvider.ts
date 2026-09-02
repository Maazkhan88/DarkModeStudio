import { AgentProvider, AgentCapabilities, AgentExecutionOptions, AgentRunResult, AgentSession, AgentStatus, ProjectHandoffContext, AgentAuthDetectionResult, AgentLoginActionResult, AgentVerificationResult } from './AgentProvider.ts';
import { spawn, ChildProcess } from 'child_process';

export class ClaudeProvider implements AgentProvider {
  id = 'claude';
  name = 'Claude Code';
  role = 'Terminal Orchestration & Code Generation';

  private activeSessions = new Map<string, { session: AgentSession; options: AgentExecutionOptions; isCancelled: boolean; process?: ChildProcess }>();

  async detectInstallation(): Promise<{ isInstalled: boolean; version?: string; isAuthenticated: boolean; instructions?: string }> {
    return new Promise((resolve) => {
      const child = spawn('claude', ['--version'], { shell: true });
      let output = '';
      child.stdout?.on('data', (d) => (output += d.toString()));
      child.stderr?.on('data', (d) => (output += d.toString()));
      child.on('error', () => {
        resolve({
          isInstalled: false,
          version: undefined,
          isAuthenticated: false,
          instructions: 'Install Claude Code CLI via winget or npm install -g @anthropic-ai/claude-code.'
        });
      });
      child.on('close', (code) => {
        if (code === 0 && output.trim()) {
          resolve({ isInstalled: true, version: output.trim(), isAuthenticated: true });
        } else {
          resolve({
            isInstalled: false,
            version: undefined,
            isAuthenticated: false,
            instructions: 'Claude Code CLI not found on host path.'
          });
        }
      });
    });
  }

  async detectAuth(): Promise<AgentAuthDetectionResult> {
    const install = await this.detectInstallation();
    if (!install.isInstalled) {
      return {
        isAuthenticated: false,
        authType: 'Claude Subscription',
        errorMessage: 'Claude Code CLI is not installed on host machine'
      };
    }

    return new Promise((resolve) => {
      const child = spawn('claude', ['auth', 'status'], { shell: true });
      let output = '';
      child.stdout?.on('data', (d) => (output += d.toString()));
      child.stderr?.on('data', (d) => (output += d.toString()));
      child.on('close', () => {
        try {
          const parsed = JSON.parse(output.trim());
          if (parsed.loggedIn) {
            resolve({
              isAuthenticated: true,
              accountLabel: `Claude Account (${parsed.authMethod || 'Session'})`,
              authType: 'Claude Subscription'
            });
          } else {
            resolve({
              isAuthenticated: false,
              authType: 'Claude Subscription',
              errorMessage: 'Claude Code OAuth session expired or unauthenticated. Run login on desktop.'
            });
          }
        } catch {
          resolve({
            isAuthenticated: false,
            authType: 'Claude Subscription',
            errorMessage: 'Claude Code session unauthenticated'
          });
        }
      });
    });
  }

  async startLogin(): Promise<AgentLoginActionResult> {
    const install = await this.detectInstallation();
    if (!install.isInstalled) {
      return {
        isSuccess: false,
        loginInstructions: '',
        errorMessage: 'Cannot start login: Claude Code CLI is not installed on desktop host.'
      };
    }
    try {
      spawn('claude', ['auth', 'login'], { shell: true, detached: true });
      return {
        isSuccess: true,
        loginInstructions: 'Claude Code official authentication process launched on desktop host browser.'
      };
    } catch (e: any) {
      return {
        isSuccess: false,
        loginInstructions: '',
        errorMessage: e.message || 'Failed to launch desktop Claude login process'
      };
    }
  }

  async verifyAuth(): Promise<AgentVerificationResult> {
    const auth = await this.detectAuth();
    if (!auth.isAuthenticated) {
      return {
        isVerified: false,
        capabilities: [],
        errorMessage: auth.errorMessage || 'Claude Code is not authenticated'
      };
    }
    return {
      isVerified: true,
      account: auth.accountLabel || 'Claude Pro / Team Session',
      capabilities: ['Terminal Orchestration', 'Multi-file Edits', 'Test Execution']
    };
  }

  async startSession(options: AgentExecutionOptions): Promise<AgentSession> {
    const session: AgentSession = {
      sessionId: options.sessionId,
      agentId: this.id,
      workingDirectory: options.workingDirectory,
      status: 'IDLE',
      startedAt: Date.now()
    };
    this.activeSessions.set(options.sessionId, { session, options, isCancelled: false });
    return session;
  }

  async sendPrompt(sessionId: string, prompt: string, context?: ProjectHandoffContext): Promise<AgentRunResult> {
    const entry = this.activeSessions.get(sessionId);
    if (!entry) throw new Error(`Session ${sessionId} not found`);

    entry.session.status = 'THINKING';
    entry.options.onStatusChange('THINKING');
    entry.options.onLogChunk('thought', `[Claude Code] Preparing prompt execution: "${prompt}"...`);

    if (entry.isCancelled) {
      entry.session.status = 'IDLE';
      entry.options.onStatusChange('IDLE');
      return { isSuccess: false, exitCode: 130, summary: 'Cancelled by user', filesModified: [], logs: ['Cancelled'] };
    }

    entry.session.status = 'EXECUTING';
    entry.options.onStatusChange('EXECUTING');

    const logs: string[] = [];
    const pushLog = (type: 'stdout' | 'stderr' | 'thought', text: string) => {
      logs.push(text);
      entry.options.onLogChunk(type, text);
    };

    return new Promise((resolve) => {
      const child = spawn('claude', ['-p', `"${prompt.replace(/"/g, '\\"')}"`], {
        shell: true,
        cwd: entry.options.workingDirectory || process.cwd()
      });

      entry.process = child;

      child.stdout?.on('data', (chunk) => {
        const text = chunk.toString();
        pushLog('stdout', text);
      });

      child.stderr?.on('data', (chunk) => {
        const text = chunk.toString();
        pushLog('stderr', text);
      });

      child.on('error', (err) => {
        entry.session.status = 'IDLE';
        entry.options.onStatusChange('IDLE');
        pushLog('stderr', `[Claude Error] ${err.message}`);
        resolve({
          isSuccess: false,
          exitCode: 1,
          summary: `Execution failed: ${err.message}`,
          filesModified: [],
          logs
        });
      });

      child.on('close', (code) => {
        entry.process = undefined;
        const isSuccess = code === 0;
        entry.session.status = isSuccess ? 'COMPLETED' : 'IDLE';
        entry.options.onStatusChange(entry.session.status);

        const summary = logs.filter((l) => l.trim().length > 0).pop() || (isSuccess ? 'Completed' : 'Execution ended with exit code ' + code);

        resolve({
          isSuccess,
          exitCode: code || 0,
          summary: summary.trim(),
          filesModified: [],
          logs,
          suggestedNextStep: isSuccess ? 'Review changes' : undefined
        });
      });
    });
  }

  async cancelSession(sessionId: string): Promise<void> {
    const entry = this.activeSessions.get(sessionId);
    if (entry) {
      entry.isCancelled = true;
      if (entry.process) {
        try {
          entry.process.kill('SIGTERM');
        } catch {}
      }
      entry.session.status = 'IDLE';
      entry.options.onStatusChange('IDLE');
      entry.options.onLogChunk('stderr', '[Claude Code] Session cancelled by user.');
    }
  }

  getStatus(sessionId: string): AgentStatus {
    return this.activeSessions.get(sessionId)?.session.status || 'IDLE';
  }

  getCapabilities(): AgentCapabilities {
    return {
      supportsPlanning: false,
      supportsCodeEditing: true,
      supportsBrowserTesting: false,
      supportsStreamingOutput: true,
      supportsWorktrees: true,
      supportedTools: ['git', 'read_file', 'write_file', 'bash_execution', 'diff_analysis']
    };
  }
}
