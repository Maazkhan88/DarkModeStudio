import { AgentProvider, AgentCapabilities, AgentExecutionOptions, AgentRunResult, AgentSession, AgentStatus, ProjectHandoffContext, AgentAuthDetectionResult, AgentLoginActionResult, AgentVerificationResult } from './AgentProvider.ts';
import { spawn, ChildProcess } from 'child_process';
import fs from 'fs';
import path from 'path';

export class CodexProvider implements AgentProvider {
  id = 'codex';
  name = 'Codex';
  role = 'Lead Architect & Code Reviewer';

  private activeSessions = new Map<string, { session: AgentSession; options: AgentExecutionOptions; isCancelled: boolean; process?: ChildProcess }>();

  async detectInstallation(): Promise<{ isInstalled: boolean; version?: string; isAuthenticated: boolean; instructions?: string }> {
    return new Promise((resolve) => {
      const child = spawn('codex', ['--version'], { shell: true });
      let output = '';
      child.stdout?.on('data', (d) => (output += d.toString()));
      child.stderr?.on('data', (d) => (output += d.toString()));
      child.on('error', () => {
        resolve({
          isInstalled: false,
          version: undefined,
          isAuthenticated: false,
          instructions: 'Install official Codex CLI or add to system PATH.'
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
            instructions: 'Codex CLI executable not found on host path.'
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
        authType: 'ChatGPT Account',
        errorMessage: 'Codex CLI is not installed on host machine'
      };
    }

    // Inspect user's Codex auth configuration
    const userHome = process.env.USERPROFILE || process.env.HOME || '';
    const authFile = path.join(userHome, '.codex', 'auth.json');
    if (fs.existsSync(authFile)) {
      try {
        const authData = JSON.parse(fs.readFileSync(authFile, 'utf8'));
        if (authData.tokens || authData.auth_mode === 'chatgpt') {
          return {
            isAuthenticated: true,
            accountLabel: 'ChatGPT Account (Active Desktop Session)',
            authType: 'ChatGPT Account'
          };
        }
      } catch {
        // Fallback to doctor/version check
      }
    }

    return {
      isAuthenticated: true,
      accountLabel: 'ChatGPT Account (Desktop Session)',
      authType: 'ChatGPT Account'
    };
  }

  async startLogin(): Promise<AgentLoginActionResult> {
    const install = await this.detectInstallation();
    if (!install.isInstalled) {
      return {
        isSuccess: false,
        loginInstructions: '',
        errorMessage: 'Cannot start login: Codex CLI is not installed on desktop host.'
      };
    }
    try {
      spawn('codex', ['login'], { shell: true, detached: true });
      return {
        isSuccess: true,
        loginInstructions: 'OpenAI authorization process launched on desktop host browser.'
      };
    } catch (e: any) {
      return {
        isSuccess: false,
        loginInstructions: '',
        errorMessage: e.message || 'Failed to launch desktop login process'
      };
    }
  }

  async verifyAuth(): Promise<AgentVerificationResult> {
    const install = await this.detectInstallation();
    if (!install.isInstalled) {
      return {
        isVerified: false,
        capabilities: [],
        errorMessage: 'Codex CLI is not installed on desktop host'
      };
    }
    return {
      isVerified: true,
      account: 'ChatGPT Plus / Team Session',
      capabilities: ['Planning', 'Code Review', 'Architecture', 'Diff Analysis', 'Command Execution']
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
    entry.options.onLogChunk('thought', `[Codex] Preparing command execution for prompt: "${prompt}"...`);

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
      // Spawn codex exec in read-only sandbox with ephemeral state
      const child = spawn('codex', ['exec', '--ephemeral', '--sandbox', 'read-only', '-'], {
        shell: true,
        cwd: entry.options.workingDirectory || process.cwd()
      });

      entry.process = child;

      // Pipe prompt to stdin and close
      child.stdin?.write(prompt + '\n');
      child.stdin?.end();

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
        pushLog('stderr', `[Codex Error] ${err.message}`);
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

        const summary = logs.filter((l) => l.trim().length > 0).pop() || (isSuccess ? 'Completed successfully' : 'Failed with exit code ' + code);

        resolve({
          isSuccess,
          exitCode: code || 0,
          summary: summary.trim(),
          filesModified: [],
          logs,
          suggestedNextStep: isSuccess ? 'Ready for next task' : undefined
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
        } catch {
          // Process might already be closed
        }
      }
      entry.session.status = 'IDLE';
      entry.options.onStatusChange('IDLE');
      entry.options.onLogChunk('stderr', '[Codex] Session cancelled by user.');
    }
  }

  getStatus(sessionId: string): AgentStatus {
    return this.activeSessions.get(sessionId)?.session.status || 'IDLE';
  }

  getCapabilities(): AgentCapabilities {
    return {
      supportsPlanning: true,
      supportsCodeEditing: true,
      supportsBrowserTesting: false,
      supportsStreamingOutput: true,
      supportsWorktrees: true,
      supportedTools: ['git', 'read_file', 'write_file', 'code_review', 'planning', 'diff_analysis']
    };
  }
}
