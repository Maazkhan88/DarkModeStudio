import fs from 'fs';
import path from 'path';
import { exec } from 'child_process';
import util from 'util';

const execPromise = util.promisify(exec);

export interface WorktreeInfo {
  id: string;
  path: string;
  branch: string;
  agentId: string;
  taskId: string;
  createdAt: number;
}

export class WorktreeManager {
  private activeWorktrees = new Map<string, WorktreeInfo>();

  async allocateWorktree(options: {
    repoPath: string;
    taskId: string;
    agentId: string;
    baseBranch?: string;
  }): Promise<WorktreeInfo> {
    const baseBranch = options.baseBranch || 'main';
    const branchName = `dms/task-${options.taskId}-${options.agentId}`;
    const worktreeDir = path.resolve(options.repoPath, '..', '.dms-worktrees', `${options.taskId}-${options.agentId}`);

    if (!fs.existsSync(path.dirname(worktreeDir))) {
      fs.mkdirSync(path.dirname(worktreeDir), { recursive: true });
    }

    try {
      // Check if branch already exists, or create new branch
      await execPromise(`git worktree add -B ${branchName} "${worktreeDir}" ${baseBranch}`, { cwd: options.repoPath });
    } catch {
      // Fallback: If worktree cannot be created via git CLI (e.g. nested sandbox or detached state), fallback to safe shadow sandbox directory
      if (!fs.existsSync(worktreeDir)) {
        fs.mkdirSync(worktreeDir, { recursive: true });
      }
    }

    const info: WorktreeInfo = {
      id: `${options.taskId}-${options.agentId}`,
      path: worktreeDir,
      branch: branchName,
      agentId: options.agentId,
      taskId: options.taskId,
      createdAt: Date.now()
    };

    this.activeWorktrees.set(info.id, info);
    return info;
  }

  async cleanupWorktree(worktreeId: string, repoPath: string): Promise<void> {
    const info = this.activeWorktrees.get(worktreeId);
    if (!info) return;

    try {
      await execPromise(`git worktree remove "${info.path}" --force`, { cwd: repoPath });
    } catch {
      if (fs.existsSync(info.path)) {
        fs.rmSync(info.path, { recursive: true, force: true });
      }
    }

    this.activeWorktrees.delete(worktreeId);
  }

  getWorktree(worktreeId: string): WorktreeInfo | undefined {
    return this.activeWorktrees.get(worktreeId);
  }

  listWorktrees(): WorktreeInfo[] {
    return Array.from(this.activeWorktrees.values());
  }
}

export const worktreeManager = new WorktreeManager();
