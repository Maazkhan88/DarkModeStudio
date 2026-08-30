import React from 'react';
import {
  FolderGit2,
  CheckCircle2,
  Clock,
  PlayCircle,
  ArrowRight,
  ShieldCheck,
  AlertCircle,
  Cpu
} from 'lucide-react';
import { useDms } from '../../context/DmsContext';
import { CommandBar } from '../common/CommandBar';
import { AgentNode } from '../common/AgentNode';

export const CommandCenterHome: React.FC = () => {
  const {
    projects,
    agents,
    tasks,
    activityEvents,
    setSelectedProjectId,
    setActiveView
  } = useDms();

  const runningTasks = tasks.filter((t) => t.status === 'RUNNING');
  const readyTasks = tasks.filter((t) => t.status === 'READY_TO_MERGE' || t.status === 'READY');
  const waitingApprovalCount = tasks.filter((t) => t.status === 'WAITING_FOR_USER' || t.status === 'READY_TO_MERGE').length;

  return (
    <div className="flex-1 overflow-y-auto p-8 space-y-8 max-w-7xl mx-auto">
      {/* Hero Section */}
      <div className="space-y-3">
        <div className="flex items-center space-x-2">
          <span className="text-[11px] font-bold tracking-widest uppercase text-white48">
            DARK MODE STUDIO
          </span>
          <span className="text-white20">•</span>
          <span className="text-[11px] font-mono text-white64">
            AI AGENT DEVELOPMENT COMMAND CENTER
          </span>
        </div>
        <h2 className="text-3xl font-bold tracking-tight text-white">
          Development Command Center
        </h2>
        <p className="text-sm text-white64 max-w-2xl">
          Coordinate Codex, Claude Code, and Google Antigravity across isolated Git worktrees. One prompt turns into an orchestrated multi-agent development pipeline.
        </p>
      </div>

      {/* Global Command Bar */}
      <div className="pt-2">
        <CommandBar />
      </div>

      {/* Live Agent Activity Cards (Section 5) */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-bold uppercase tracking-wider text-white">
            Live Agent Activity
          </h3>
          <span className="text-xs text-white48 font-mono">
            {agents.filter((a) => a.status === 'EXECUTING').length} active execution streams
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {agents.map((agent) => {
            const isExecuting = agent.status === 'EXECUTING';
            const isThinking = agent.status === 'THINKING';

            return (
              <div
                key={agent.id}
                className={`bg-surface01 border rounded-r18 p-5 transition-all duration-300 ${
                  isExecuting
                    ? 'border-white48 shadow-xl bg-surface02'
                    : 'border-white10 hover:border-white20'
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <h4 className="text-sm font-bold text-white tracking-tight">{agent.name}</h4>
                    <p className="text-[11px] text-white48">{agent.role}</p>
                  </div>
                  <AgentNode status={agent.status} size={8} label={agent.status} />
                </div>

                <div className="space-y-2 mt-4 pt-3 border-t border-white08">
                  <div className="text-xs text-white92 font-medium">
                    {agent.id === 'codex' && '● Analysing SecondMe authentication architecture'}
                    {agent.id === 'claude' && '● Implementing GhostCart wishlist sync & Focus Screen'}
                    {agent.id === 'antigravity' && '● Running SecondMe Android UI & visual regression tests'}
                  </div>

                  <div className="flex items-center justify-between text-[10px] text-white48 font-mono pt-1">
                    <span>
                      {agent.id === 'codex' && '12m active'}
                      {agent.id === 'claude' && '8m active'}
                      {agent.id === 'antigravity' && '3m active'}
                    </span>
                    <span>{agent.tasks_today_count} tasks completed today</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Active Projects & Recent Task Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Active Projects (2 Cols) */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              Active Project Workspaces
            </h3>
            <button
              onClick={() => setActiveView('project')}
              className="text-xs text-white64 hover:text-white flex items-center space-x-1"
            >
              <span>View all projects</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="space-y-3">
            {projects.map((project) => (
              <div
                key={project.id}
                onClick={() => {
                  setSelectedProjectId(project.id);
                  setActiveView('project');
                }}
                className="group bg-surface01 border border-white10 hover:border-white32 rounded-r18 p-4.5 cursor-pointer transition-all duration-200"
              >
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center space-x-3">
                    <div className="w-9 h-9 rounded-r10 bg-surfaceSelected flex items-center justify-center font-bold text-xs text-white border border-white14">
                      {project.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-white group-hover:text-white transition-colors">
                        {project.name}
                      </h4>
                      <p className="text-xs text-white48 truncate max-w-md">
                        {project.description}
                      </p>
                    </div>
                  </div>

                  <div className="text-right">
                    <div className="text-xs font-mono font-semibold text-white">
                      {(project.progress * 100).toFixed(0)}%
                    </div>
                    <span className="text-[10px] text-accentGreen font-mono uppercase">
                      {project.status}
                    </span>
                  </div>
                </div>

                {/* Progress Rail */}
                <div className="w-full h-1 bg-white10 rounded-full overflow-hidden mt-3">
                  <div
                    className="h-full bg-white transition-all duration-500"
                    style={{ width: `${project.progress * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Activity Feed (1 Col) */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              Activity Audit Trail
            </h3>
            <span className="text-[10px] text-white48 font-mono">Live Stream</span>
          </div>

          <div className="bg-surface01 border border-white10 rounded-r18 p-4 space-y-3.5">
            {activityEvents.slice(0, 6).map((evt) => (
              <div key={evt.id} className="text-xs border-b border-white08 pb-3 last:border-b-0 last:pb-0">
                <div className="flex items-center justify-between text-[10px] text-white48 font-mono mb-1">
                  <span>{evt.actor_name}</span>
                  <span>{evt.created_at.substring(11, 16) || 'Just now'}</span>
                </div>
                <div className="text-white92 font-medium">{evt.title}</div>
                {evt.detail && (
                  <div className="text-[11px] text-white48 mt-0.5">{evt.detail}</div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
