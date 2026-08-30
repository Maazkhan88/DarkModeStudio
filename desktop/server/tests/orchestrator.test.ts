import { describe, it, expect, beforeEach } from 'vitest';
import { dbManager } from '../db/database.ts';
import { providerRegistry } from '../providers/ProviderRegistry.ts';
import { orchestrator } from '../orchestrator/Orchestrator.ts';
import { gitManager } from '../git/GitManager.ts';

describe('Dark Mode Studio Desktop Engine Tests', () => {
  beforeEach(async () => {
    await dbManager.init();
  });

  it('should initialize SQLite database with seeded projects and agents', () => {
    const projects = dbManager.getProjects();
    expect(projects.length).toBeGreaterThan(0);
    expect(projects.some((p) => p.name === 'SecondMe')).toBe(true);
    expect(projects.some((p) => p.name === 'DarkModeStudio')).toBe(true);

    const agents = dbManager.getAgents();
    expect(agents.length).toBeGreaterThanOrEqual(3);
    expect(agents.some((a) => a.id === 'codex')).toBe(true);
    expect(agents.some((a) => a.id === 'claude')).toBe(true);
    expect(agents.some((a) => a.id === 'antigravity')).toBe(true);
  });

  it('should register and detect all 3 primary agent adapters', async () => {
    const allProviders = providerRegistry.getAll();
    expect(allProviders.length).toBe(3);

    const codex = providerRegistry.get('codex');
    expect(codex).toBeDefined();
    expect(codex?.getCapabilities().supportsPlanning).toBe(true);

    const claude = providerRegistry.get('claude');
    expect(claude).toBeDefined();
    expect(claude?.getCapabilities().supportsCodeEditing).toBe(true);

    const antigravity = providerRegistry.get('antigravity');
    expect(antigravity).toBeDefined();
    expect(antigravity?.getCapabilities().supportsBrowserTesting).toBe(true);
  });

  it('should decompose instructions into structured multi-agent workflow plan', () => {
    const plan = orchestrator.decomposePrompt('secondme', 'Finish the SecondMe Focus Screen, test everything, fix any problems and prepare a PR.');
    expect(plan.stages.length).toBe(4);
    expect(plan.stages[0].agentId).toBe('codex');
    expect(plan.stages[1].agentId).toBe('claude');
    expect(plan.stages[2].agentId).toBe('antigravity');
    expect(plan.stages[3].agentId).toBe('codex');
  });

  it('should retrieve Git status and recent commits', async () => {
    const status = await gitManager.getStatus(process.cwd());
    expect(status).toBeDefined();
    expect(status.currentBranch).toBeDefined();

    const commits = await gitManager.getRecentCommits(process.cwd(), 5);
    expect(commits.length).toBeGreaterThan(0);
  });
});
