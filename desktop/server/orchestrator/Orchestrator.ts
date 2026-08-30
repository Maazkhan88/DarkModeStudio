import { dbManager } from '../db/database.ts';
import { providerRegistry } from '../providers/ProviderRegistry.ts';
import { worktreeManager } from '../git/WorktreeManager.ts';
import { ProjectHandoffContext } from '../providers/AgentProvider.ts';

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

export class Orchestrator {
  private activeWorkflows = new Map<string, { plan: WorkflowExecutionPlan; isCancelled: boolean }>();
  private logListeners = new Set<(event: { runId: string; taskId: string; type: string; chunk: string }) => void>();

  addLogListener(listener: (event: { runId: string; taskId: string; type: string; chunk: string }) => void): () => void {
    this.logListeners.add(listener);
    return () => this.logListeners.delete(listener);
  }

  private broadcastLog(runId: string, taskId: string, type: string, chunk: string): void {
    this.logListeners.forEach((l) => l({ runId, taskId, type, chunk }));
  }

  decomposePrompt(projectId: string, rawPrompt: string): WorkflowExecutionPlan {
    const objectiveId = 'obj-' + Date.now();
    const lower = rawPrompt.toLowerCase().trim();
    const isFullPipeline = lower.includes('finish') || lower.includes('build') || lower.includes('pr') || lower.includes('feature');
    const isReviewOnly = !isFullPipeline && (lower.startsWith('review') || lower === 'code review');
    const isTestOnly = !isFullPipeline && (lower.startsWith('test') || lower.startsWith('run tests') || lower === 'qa check');

    let stages: WorkflowStage[] = [];

    if (isReviewOnly) {
      stages = [
        {
          id: `stage-${objectiveId}-1`,
          stageName: 'Code Review & Architecture Audit',
          stageOrder: 1,
          agentId: 'codex',
          agentName: 'Codex',
          role: 'Lead Architect',
          status: 'QUEUED',
          durationSeconds: 0
        }
      ];
    } else if (isTestOnly) {
      stages = [
        {
          id: `stage-${objectiveId}-1`,
          stageName: 'Automated QA & Visual Verification',
          stageOrder: 1,
          agentId: 'antigravity',
          agentName: 'Google Antigravity',
          role: 'QA Engineer',
          status: 'QUEUED',
          durationSeconds: 0
        }
      ];
    } else {
      stages = [
        {
          id: `stage-${objectiveId}-1`,
          stageName: 'Architecture & Technical Plan',
          stageOrder: 1,
          agentId: 'codex',
          agentName: 'Codex',
          role: 'Lead Architect',
          status: 'QUEUED',
          durationSeconds: 0
        },
        {
          id: `stage-${objectiveId}-2`,
          stageName: 'Implementation & Code Generation',
          stageOrder: 2,
          agentId: 'claude',
          agentName: 'Claude Code',
          role: 'Primary Developer',
          status: 'QUEUED',
          durationSeconds: 0
        },
        {
          id: `stage-${objectiveId}-3`,
          stageName: 'Automated QA & UI Verification',
          stageOrder: 3,
          agentId: 'antigravity',
          agentName: 'Google Antigravity',
          role: 'QA Engineer',
          status: 'QUEUED',
          durationSeconds: 0
        },
        {
          id: `stage-${objectiveId}-4`,
          stageName: 'Final Code Review & PR Approval',
          stageOrder: 4,
          agentId: 'codex',
          agentName: 'Codex',
          role: 'Lead Architect',
          status: 'QUEUED',
          durationSeconds: 0
        }
      ];
    }

    return {
      objectiveId,
      projectId,
      rawPrompt,
      strategy: 'AUTO_ORCHESTRATED',
      stages
    };
  }

  async executeWorkflow(plan: WorkflowExecutionPlan): Promise<void> {
    this.activeWorkflows.set(plan.objectiveId, { plan, isCancelled: false });

    // 1. Create main task in database
    const taskId = 'task-' + Date.now().toString().slice(-6);
    dbManager.createTask({
      id: taskId,
      projectId: plan.projectId,
      title: plan.rawPrompt,
      description: `Orchestrated multi-agent workflow: ${plan.stages.map((s) => s.agentName).join(' → ')}`,
      assignedAgentId: 'claude',
      reviewerAgentId: 'codex',
      priority: 'HIGH',
      status: 'RUNNING'
    });

    dbManager.logActivity({
      id: 'act-' + Date.now(),
      projectId: plan.projectId,
      actorName: 'Dark Mode Studio',
      actorType: 'SYSTEM',
      title: `Started workflow: "${plan.rawPrompt}"`,
      detail: `Allocating isolated worktree and coordinating 3 agents across ${plan.stages.length} stages`,
      eventType: 'WORKFLOW_START'
    });

    let previousHandoff: ProjectHandoffContext | undefined;
    const project = dbManager.getProject(plan.projectId);
    const repoPath = project?.local_path || process.cwd();

    // 2. Iterate through stages sequentially with structured handoffs
    for (const stage of plan.stages) {
      const entry = this.activeWorkflows.get(plan.objectiveId);
      if (entry?.isCancelled) {
        stage.status = 'CANCELLED';
        break;
      }

      stage.status = 'RUNNING';
      const runId = `run-${taskId}-${stage.stageOrder}`;
      dbManager.createRun({
        id: runId,
        taskId,
        agentId: stage.agentId,
        stageName: stage.stageName,
        stageOrder: stage.stageOrder,
        status: 'RUNNING'
      });

      dbManager.updateAgentStatus(stage.agentId, 'EXECUTING', taskId);

      // Allocate worktree for agent if code editing or testing
      const worktree = await worktreeManager.allocateWorktree({
        repoPath,
        taskId,
        agentId: stage.agentId
      });

      const provider = providerRegistry.get(stage.agentId);
      if (!provider) {
        stage.status = 'FAILED';
        dbManager.updateRunStatus(runId, 'FAILED', 'Provider adapter not found');
        continue;
      }

      const session = await provider.startSession({
        sessionId: `sess-${runId}`,
        workingDirectory: worktree.path,
        worktreeBranch: worktree.branch,
        onLogChunk: (type, text) => {
          this.broadcastLog(runId, taskId, type, text);
        },
        onStatusChange: (status) => {
          dbManager.updateAgentStatus(stage.agentId, status, taskId);
        }
      });

      const promptForAgent = `Task: ${plan.rawPrompt}\nStage: ${stage.stageName}\nProject: ${project?.name || plan.projectId}`;
      const result = await provider.sendPrompt(session.sessionId, promptForAgent, previousHandoff);

      stage.status = result.isSuccess ? 'COMPLETED' : 'FAILED';
      stage.summary = result.summary;
      stage.filesModified = result.filesModified;
      stage.durationSeconds = Math.round((Date.now() - session.startedAt) / 1000);

      dbManager.updateRunStatus(runId, stage.status, stage.summary, stage.durationSeconds);
      dbManager.updateAgentStatus(stage.agentId, 'IDLE');

      // Build structured handoff for next agent in chain
      previousHandoff = {
        projectId: plan.projectId,
        projectName: project?.name || plan.projectId,
        taskTitle: plan.rawPrompt,
        filesChanged: result.filesModified,
        instructionsForReceiver: result.suggestedNextStep || 'Proceed with next phase'
      };

      dbManager.logActivity({
        id: 'act-' + Date.now(),
        projectId: plan.projectId,
        actorName: stage.agentName,
        actorType: 'AGENT',
        title: `${stage.stageName} completed by ${stage.agentName}`,
        detail: stage.summary,
        eventType: 'STAGE_COMPLETE'
      });
    }

    // 3. Mark final task status
    const allPassed = plan.stages.every((s) => s.status === 'COMPLETED');
    const finalStatus = allPassed ? 'READY_TO_MERGE' : 'FAILED';
    dbManager.updateTaskStatus(taskId, finalStatus);

    dbManager.logActivity({
      id: 'act-' + Date.now(),
      projectId: plan.projectId,
      actorName: 'Codex',
      actorType: 'AGENT',
      title: allPassed ? `Task READY TO MERGE: "${plan.rawPrompt}"` : `Task Failed: "${plan.rawPrompt}"`,
      detail: allPassed ? 'All architectural checks, implementations, and QA tests passed.' : 'Check run logs for details.',
      eventType: 'WORKFLOW_COMPLETE'
    });

    this.activeWorkflows.delete(plan.objectiveId);
  }

  cancelWorkflow(objectiveId: string): void {
    const entry = this.activeWorkflows.get(objectiveId);
    if (entry) {
      entry.isCancelled = true;
    }
  }
}

export const orchestrator = new Orchestrator();
