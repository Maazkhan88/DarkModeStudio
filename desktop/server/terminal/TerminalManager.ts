import { spawn, ChildProcessWithoutNullStreams } from 'child_process';

export interface TerminalSession {
  id: string;
  projectId?: string;
  shell: string;
  cwd: string;
  createdAt: number;
}

export class TerminalManager {
  private sessions = new Map<string, { info: TerminalSession; process?: ChildProcessWithoutNullStreams }>();
  private outputListeners = new Set<(event: { sessionId: string; data: string }) => void>();

  addOutputListener(listener: (event: { sessionId: string; data: string }) => void): () => void {
    this.outputListeners.add(listener);
    return () => this.outputListeners.delete(listener);
  }

  createSession(id: string, cwd: string = process.cwd(), shell: string = 'powershell.exe'): TerminalSession {
    const info: TerminalSession = {
      id,
      shell,
      cwd,
      createdAt: Date.now()
    };
    this.sessions.set(id, { info });
    return info;
  }

  async executeCommand(sessionId: string, command: string): Promise<void> {
    const entry = this.sessions.get(sessionId);
    if (!entry) throw new Error(`Terminal session ${sessionId} not found`);

    const isWindows = process.platform === 'win32';
    const shell = isWindows ? 'cmd.exe' : 'bash';
    const shellArgs = isWindows ? ['/d', '/c', command] : ['-c', command];

    return new Promise((resolve) => {
      this.outputListeners.forEach((l) => l({ sessionId, data: `\r\n$ ${command}\r\n` }));

      const child = spawn(shell, shellArgs, {
        cwd: entry.info.cwd,
        shell: true
      });

      entry.process = child;

      child.stdout?.on('data', (d) => {
        const text = d.toString();
        this.outputListeners.forEach((l) => l({ sessionId, data: text }));
      });

      child.stderr?.on('data', (d) => {
        const text = d.toString();
        this.outputListeners.forEach((l) => l({ sessionId, data: text }));
      });

      child.on('close', (code) => {
        this.outputListeners.forEach((l) => l({ sessionId, data: `\r\n[Process exited with code ${code}]\r\n` }));
        entry.process = undefined;
        resolve();
      });

      child.on('error', (err) => {
        this.outputListeners.forEach((l) => l({ sessionId, data: `\r\nError: ${err.message}\r\n` }));
        entry.process = undefined;
        resolve();
      });
    });
  }

  killSession(sessionId: string): void {
    const entry = this.sessions.get(sessionId);
    if (entry?.process) {
      entry.process.kill();
      entry.process = undefined;
    }
    this.sessions.delete(sessionId);
  }
}

export const terminalManager = new TerminalManager();
