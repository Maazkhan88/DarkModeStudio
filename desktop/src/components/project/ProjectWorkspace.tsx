import React, { useState } from 'react';
import {
  FolderGit2,
  GitBranch,
  ListTodo,
  Users,
  Code2,
  PlayCircle,
  CheckSquare,
  BookOpen,
  FileCode,
  ShieldCheck,
  Activity,
  Settings,
  Plus,
  Play,
  CheckCircle2,
  Clock,
  ArrowRight
} from 'lucide-react';
import { useDms } from '../../context/DmsContext';
import { CommandBar } from '../common/CommandBar';
import { AgentNode } from '../common/AgentNode';

type TabKey =
  | 'OVERVIEW'
  | 'TASKS'
  | 'AGENTS'
  | 'CODE'
  | 'GIT'
  | 'RUNS'
  | 'TESTS'
  | 'MEMORY'
  | 'FILES'
  | 'DECISIONS'
  | 'ACTIVITY'
  | 'SETTINGS';

export const ProjectWorkspace: React.FC = () => {
  const {
    projects,
    selectedProjectId,
    tasks,
    agents,
    decisions,
    activityEvents,
    setIsLiveRunOpen
  } = useDms();

  const [activeTab, setActiveTab] = useState<TabKey>('OVERVIEW');

  const project = projects.find((p) => p.id === selectedProjectId) || projects[0];
  const projectTasks = tasks.filter((t) => t.project_id === project?.id);
  const projectDecisions = decisions.filter((d) => d.project_id === project?.id);
  const projectActivities = activityEvents.filter((a) => a.project_id === project?.id);

  const tabs: { key: TabKey; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { key: 'OVERVIEW', label: 'Overview', icon: FolderGit2 },
    { key: 'TASKS', label: 'Tasks', icon: ListTodo },
    { key: 'AGENTS', label: 'Agents', icon: Users },
    { key: 'CODE', label: 'Code', icon: Code2 },
    { key: 'GIT', label: 'Git & Worktrees', icon: GitBranch },
    { key: 'RUNS', label: 'Runs', icon: PlayCircle },
    { key: 'TESTS', label: 'Tests', icon: CheckSquare },
    { key: 'MEMORY', label: 'Memory', icon: BookOpen },
    { key: 'FILES', label: 'Files', icon: FileCode },
    { key: 'DECISIONS', label: 'Decisions', icon: ShieldCheck },
    { key: 'ACTIVITY', label: 'Activity', icon: Activity },
    { key: 'SETTINGS', label: 'Settings', icon: Settings }
  ];

  return (
    <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-7xl mx-auto">
      {/* Project Header */}
      <div className="flex items-center justify-between border-b border-white10 pb-6">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-r14 bg-surface01 border border-white14 flex items-center justify-center font-bold text-base text-white">
            {project?.name.substring(0, 2).toUpperCase()}
          </div>
          <div>
            <div className="flex items-center space-x-3">
              <h2 className="text-2xl font-bold text-white tracking-tight">{project?.name}</h2>
              <span className="text-[11px] font-mono bg-surface02 border border-white14 px-2 py-0.5 rounded-full text-accentGreen uppercase">
                {project?.status}
              </span>
            </div>
            <p className="text-xs text-white64 max-w-xl mt-1">{project?.description}</p>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          <div className="text-right font-mono">
            <div className="text-xs text-white48">Branch</div>
            <div className="text-xs font-semibold text-white">{project?.active_branch || 'main'}</div>
          </div>
          <div className="h-8 w-[1px] bg-white10" />
          <div className="text-right font-mono">
            <div className="text-xs text-white48">Progress</div>
            <div className="text-xs font-semibold text-white">
              {((project?.progress || 0) * 100).toFixed(0)}%
            </div>
          </div>
        </div>
      </div>

      {/* Global Command Bar in Project Context */}
      <CommandBar />

      {/* Tabs Horizontal Rail */}
      <div className="flex items-center space-x-1 border-b border-white10 overflow-x-auto pb-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-r10 text-xs font-medium transition-all shrink-0 ${
                isActive
                  ? 'bg-surfaceSelected text-white border border-white14'
                  : 'text-white48 hover:text-white hover:bg-surface01'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab Contents */}
      {activeTab === 'OVERVIEW' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Progress Breakdown */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-surface01 border border-white10 rounded-r18 p-5 space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-white">
                Feature Progress Breakdown
              </h3>
              <div className="space-y-3">
                {[
                  { name: 'Focus Screen & OLED Themes', progress: 0.86 },
                  { name: 'Memory Import & Context Retrieval', progress: 0.48 },
                  { name: 'Multi-Agent Command Center Orchestrator', progress: 0.71 },
                  { name: 'Push Notifications & Background Watchdogs', progress: 0.22 }
                ].map((item) => (
                  <div key={item.name} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-white92 font-medium">{item.name}</span>
                      <span className="font-mono text-white48">{(item.progress * 100).toFixed(0)}%</span>
                    </div>
                    <div className="w-full h-1.5 bg-white10 rounded-full overflow-hidden">
                      <div className="h-full bg-white" style={{ width: `${item.progress * 100}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Active Tasks in Project */}
            <div className="bg-surface01 border border-white10 rounded-r18 p-5 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold uppercase tracking-wider text-white">
                  Active Tasks ({projectTasks.length})
                </h3>
                <button
                  onClick={() => setActiveTab('TASKS')}
                  className="text-xs text-white64 hover:text-white flex items-center space-x-1"
                >
                  <span>View task board</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>

              <div className="space-y-2">
                {projectTasks.map((t) => (
                  <div
                    key={t.id}
                    onClick={() => setIsLiveRunOpen(true)}
                    className="flex items-center justify-between bg-surface02 border border-white08 hover:border-white20 p-3.5 rounded-r12 cursor-pointer transition-all"
                  >
                    <div>
                      <div className="text-xs font-semibold text-white">{t.title}</div>
                      <div className="text-[11px] text-white48 truncate max-w-md">{t.description}</div>
                    </div>
                    <span className="text-[10px] font-mono bg-surface03 px-2 py-1 rounded text-white80 border border-white08">
                      {t.status}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Assigned Agents & Project Decisions */}
          <div className="space-y-6">
            <div className="bg-surface01 border border-white10 rounded-r18 p-5 space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-white">
                Assigned Agents
              </h3>
              <div className="space-y-3">
                {agents.map((agent) => (
                  <div key={agent.id} className="flex items-center justify-between border-b border-white08 pb-2.5 last:border-b-0 last:pb-0">
                    <div>
                      <div className="text-xs font-bold text-white">{agent.name}</div>
                      <div className="text-[10px] text-white48">{agent.role}</div>
                    </div>
                    <AgentNode status={agent.status} size={6} label={agent.status} />
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-surface01 border border-white10 rounded-r18 p-5 space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-white">
                Key Decisions ({projectDecisions.length})
              </h3>
              <div className="space-y-2.5">
                {projectDecisions.map((d) => (
                  <div key={d.id} className="text-xs border-b border-white08 pb-2.5 last:border-b-0 last:pb-0">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-white48 text-[10px]">{d.code}</span>
                      <span className="font-semibold text-white">{d.title}</span>
                    </div>
                    <p className="text-[11px] text-white48 mt-1">{d.reason}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'TASKS' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              Project Tasks & Subtasks
            </h3>
            <button className="flex items-center space-x-1.5 bg-white text-oled text-xs font-semibold px-3 py-1.5 rounded-r10 hover:bg-white92">
              <Plus className="w-3.5 h-3.5" />
              <span>Create Task</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {projectTasks.map((t) => (
              <div key={t.id} className="bg-surface01 border border-white10 rounded-r16 p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-mono bg-surface02 px-2 py-0.5 rounded text-white48">
                    {t.id}
                  </span>
                  <span className="text-[10px] font-mono uppercase text-accentGreen">
                    {t.status}
                  </span>
                </div>
                <div>
                  <h4 className="text-sm font-bold text-white">{t.title}</h4>
                  <p className="text-xs text-white48 mt-1">{t.description}</p>
                </div>
                <div className="flex items-center justify-between pt-2 border-t border-white08 text-[11px] text-white64">
                  <span>Assigned: {t.assigned_agent_id ? agents.find((a) => a.id === t.assigned_agent_id)?.name : 'Unassigned'}</span>
                  <button
                    onClick={() => setIsLiveRunOpen(true)}
                    className="text-white hover:underline flex items-center space-x-1"
                  >
                    <Play className="w-3 h-3" />
                    <span>Run in Worktree</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'GIT' && (
        <div className="bg-surface01 border border-white10 rounded-r18 p-6 space-y-6">
          <div>
            <h3 className="text-sm font-bold text-white">Git Worktree Sandboxes</h3>
            <p className="text-xs text-white48">
              Agents operate inside isolated worktrees under `.dms-worktrees/` to prevent concurrent file overwrite collisions.
            </p>
          </div>

          <div className="space-y-3">
            {[
              { branch: 'main', path: project?.local_path, agent: 'Master Repo', status: 'Clean' },
              { branch: 'dms/task-101-claude', path: '.dms-worktrees/task-101-claude', agent: 'Claude Code', status: 'Active Workspace' },
              { branch: 'dms/task-101-antigravity', path: '.dms-worktrees/task-101-antigravity', agent: 'Antigravity QA', status: 'Ready for Test' }
            ].map((wt) => (
              <div key={wt.branch} className="flex items-center justify-between bg-surface02 border border-white08 p-3.5 rounded-r12 font-mono text-xs">
                <div className="space-y-0.5">
                  <div className="text-white font-semibold flex items-center space-x-2">
                    <GitBranch className="w-3.5 h-3.5 text-white80" />
                    <span>{wt.branch}</span>
                  </div>
                  <div className="text-[11px] text-white48">{wt.path}</div>
                </div>
                <div className="text-right">
                  <div className="text-white80">{wt.agent}</div>
                  <div className="text-[10px] text-accentGreen">{wt.status}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'DECISIONS' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              Architectural Decision Log
            </h3>
          </div>

          <div className="space-y-3">
            {projectDecisions.map((dec) => (
              <div key={dec.id} className="bg-surface01 border border-white10 rounded-r18 p-5 space-y-2.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <span className="text-xs font-mono font-bold text-white bg-surface02 px-2 py-0.5 rounded border border-white14">
                      {dec.code}
                    </span>
                    <h4 className="text-sm font-bold text-white">{dec.title}</h4>
                  </div>
                  <span className="text-[10px] font-mono bg-surface02 text-accentGreen px-2 py-0.5 rounded uppercase">
                    {dec.status}
                  </span>
                </div>
                <p className="text-xs text-white80">{dec.reason}</p>
                {dec.implications && (
                  <div className="text-[11px] text-white48 bg-surface02 p-2.5 rounded-r10 border border-white08">
                    <span className="font-semibold text-white64">Implications: </span>
                    {dec.implications}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
