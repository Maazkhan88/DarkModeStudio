import React from 'react';
import {
  LayoutDashboard,
  FolderGit2,
  Users,
  Activity,
  CheckSquare,
  ListTodo,
  PlayCircle,
  GitBranch,
  GitPullRequest,
  Tag,
  Blocks,
  Workflow,
  Bell,
  Settings,
  Terminal,
  ChevronRight
} from 'lucide-react';
import { useDms } from '../../context/DmsContext';
import { AgentNode } from '../common/AgentNode';

export const Sidebar: React.FC = () => {
  const {
    projects,
    selectedProjectId,
    setSelectedProjectId,
    agents,
    activeView,
    setActiveView
  } = useDms();

  return (
    <aside className="w-64 h-screen bg-oled border-r border-white08 flex flex-col justify-between shrink-0 select-none overflow-y-auto">
      <div className="p-4 space-y-6">
        {/* Brand / Logo */}
        <div className="flex items-center space-x-3 px-2">
          <div className="w-7 h-7 rounded-r8 bg-white flex items-center justify-center font-bold text-oled text-xs tracking-tighter">
            DM
          </div>
          <div>
            <h1 className="text-xs font-bold tracking-widest uppercase text-white">
              Dark Mode Studio
            </h1>
            <p className="text-[10px] tracking-wider text-white48 uppercase font-medium">
              Command Center
            </p>
          </div>
        </div>

        {/* Global Navigation */}
        <div className="space-y-1">
          <button
            onClick={() => setActiveView('home')}
            className={`w-full flex items-center justify-between px-3 py-2 rounded-r10 text-xs font-medium transition-all ${
              activeView === 'home'
                ? 'bg-surfaceSelected text-white border border-white14'
                : 'text-white64 hover:text-white hover:bg-surface01'
            }`}
          >
            <div className="flex items-center space-x-2.5">
              <LayoutDashboard className="w-4 h-4" />
              <span>Command Center</span>
            </div>
          </button>
        </div>

        {/* Dynamic Projects */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between px-3 text-[10px] font-semibold tracking-wider text-white48 uppercase">
            <span>Projects</span>
            <span className="text-[9px] bg-surface02 px-1.5 py-0.5 rounded text-white48">
              {projects.length}
            </span>
          </div>

          <div className="space-y-0.5">
            {projects.map((proj) => {
              const isSelected = activeView === 'project' && selectedProjectId === proj.id;
              return (
                <button
                  key={proj.id}
                  onClick={() => {
                    setSelectedProjectId(proj.id);
                    setActiveView('project');
                  }}
                  className={`w-full flex items-center justify-between px-3 py-1.5 rounded-r8 text-xs transition-all ${
                    isSelected
                      ? 'bg-surfaceSelected text-white font-semibold border border-white14'
                      : 'text-white64 hover:text-white hover:bg-surface01 font-normal'
                  }`}
                >
                  <div className="flex items-center space-x-2 truncate">
                    <FolderGit2 className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">{proj.name}</span>
                  </div>
                  <span className="text-[10px] font-mono text-white32">
                    {(proj.progress * 100).toFixed(0)}%
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Team Section */}
        <div className="space-y-1.5">
          <div className="px-3 text-[10px] font-semibold tracking-wider text-white48 uppercase">
            Team
          </div>
          <div className="space-y-0.5">
            <button
              onClick={() => setActiveView('team')}
              className={`w-full flex items-center justify-between px-3 py-1.5 rounded-r8 text-xs ${
                activeView === 'team'
                  ? 'bg-surfaceSelected text-white font-semibold border border-white14'
                  : 'text-white64 hover:text-white hover:bg-surface01'
              }`}
            >
              <div className="flex items-center space-x-2.5">
                <Users className="w-3.5 h-3.5" />
                <span>Agents</span>
              </div>
              <span className="text-[10px] text-white48 font-mono">{agents.length}</span>
            </button>

            <button
              onClick={() => setActiveView('home')}
              className="w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs text-white64 hover:text-white hover:bg-surface01"
            >
              <Activity className="w-3.5 h-3.5" />
              <span>Activity</span>
            </button>

            <button
              onClick={() => setActiveView('tasks')}
              className="w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs text-white64 hover:text-white hover:bg-surface01"
            >
              <CheckSquare className="w-3.5 h-3.5" />
              <span>Reviews</span>
            </button>
          </div>
        </div>

        {/* Development Section */}
        <div className="space-y-1.5">
          <div className="px-3 text-[10px] font-semibold tracking-wider text-white48 uppercase">
            Development
          </div>
          <div className="space-y-0.5">
            <button
              onClick={() => setActiveView('tasks')}
              className={`w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs ${
                activeView === 'tasks'
                  ? 'bg-surfaceSelected text-white font-semibold border border-white14'
                  : 'text-white64 hover:text-white hover:bg-surface01'
              }`}
            >
              <ListTodo className="w-3.5 h-3.5" />
              <span>Tasks</span>
            </button>

            <button
              onClick={() => setActiveView('git')}
              className={`w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs ${
                activeView === 'git'
                  ? 'bg-surfaceSelected text-white font-semibold border border-white14'
                  : 'text-white64 hover:text-white hover:bg-surface01'
              }`}
            >
              <GitBranch className="w-3.5 h-3.5" />
              <span>Git & Worktrees</span>
            </button>

            <button
              onClick={() => setActiveView('terminal')}
              className={`w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs ${
                activeView === 'terminal'
                  ? 'bg-surfaceSelected text-white font-semibold border border-white14'
                  : 'text-white64 hover:text-white hover:bg-surface01'
              }`}
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>Integrated Terminal</span>
            </button>
          </div>
        </div>

        {/* System Section */}
        <div className="space-y-1.5">
          <div className="px-3 text-[10px] font-semibold tracking-wider text-white48 uppercase">
            System
          </div>
          <div className="space-y-0.5">
            <button
              onClick={() => setActiveView('settings')}
              className={`w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs ${
                activeView === 'settings'
                  ? 'bg-surfaceSelected text-white font-semibold border border-white14'
                  : 'text-white64 hover:text-white hover:bg-surface01'
              }`}
            >
              <Blocks className="w-3.5 h-3.5" />
              <span>Integrations</span>
            </button>

            <button
              onClick={() => setActiveView('settings')}
              className="w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs text-white64 hover:text-white hover:bg-surface01"
            >
              <Workflow className="w-3.5 h-3.5" />
              <span>Automations</span>
            </button>

            <button
              onClick={() => setActiveView('settings')}
              className="w-full flex items-center space-x-2.5 px-3 py-1.5 rounded-r8 text-xs text-white64 hover:text-white hover:bg-surface01"
            >
              <Settings className="w-3.5 h-3.5" />
              <span>Settings</span>
            </button>
          </div>
        </div>
      </div>

      {/* Footer Agent Status Pill */}
      <div className="p-3 border-t border-white08 bg-surface01">
        <div className="space-y-2">
          {agents.map((agent) => (
            <div key={agent.id} className="flex items-center justify-between px-2 py-1 text-xs">
              <span className="text-white80 font-medium">{agent.name}</span>
              <AgentNode status={agent.status} size={6} label={agent.status} />
            </div>
          ))}
        </div>
      </div>
    </aside>
  );
};
