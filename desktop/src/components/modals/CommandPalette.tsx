import React, { useState } from 'react';
import { Search, FolderGit2, ListTodo, Users, Terminal, Play, X, ArrowRight } from 'lucide-react';
import { useDms } from '../../context/DmsContext';

export const CommandPalette: React.FC = () => {
  const {
    isCommandPaletteOpen,
    setIsCommandPaletteOpen,
    projects,
    tasks,
    agents,
    setSelectedProjectId,
    setActiveView,
    createExecutionPlan,
    executePlan
  } = useDms();

  const [query, setQuery] = useState('');

  if (!isCommandPaletteOpen) return null;

  const filteredProjects = projects.filter((p) => p.name.toLowerCase().includes(query.toLowerCase()));
  const filteredTasks = tasks.filter((t) => t.title.toLowerCase().includes(query.toLowerCase()));
  const filteredAgents = agents.filter((a) => a.name.toLowerCase().includes(query.toLowerCase()));

  const handleAction = async (action: string) => {
    setIsCommandPaletteOpen(false);
    if (action.startsWith('PROJ:')) {
      const id = action.replace('PROJ:', '');
      setSelectedProjectId(id);
      setActiveView('project');
    } else if (action === 'VIEW_TERMINAL') {
      setActiveView('terminal');
    } else if (action === 'VIEW_TEAM') {
      setActiveView('team');
    } else if (action.startsWith('EXEC:')) {
      const prompt = action.replace('EXEC:', '');
      const plan = await createExecutionPlan(prompt);
      await executePlan(plan);
    }
  };

  return (
    <div className="fixed inset-0 bg-oled/80 backdrop-blur-md z-50 flex items-start justify-center pt-24 p-4 select-none animate-in fade-in duration-150">
      <div className="bg-surface01 border border-white20 rounded-r22 w-full max-w-2xl shadow-2xl overflow-hidden">
        {/* Search Bar */}
        <div className="flex items-center px-4 py-3.5 border-b border-white10">
          <Search className="w-4 h-4 text-white48 mr-3 shrink-0" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && query.trim()) {
                handleAction(`EXEC:${query.trim()}`);
              }
            }}
            placeholder="Search projects, tasks, agents, decisions, or enter an agent prompt..."
            className="w-full bg-transparent text-sm text-white placeholder-white48 outline-none font-medium"
            autoFocus
          />
          <button
            onClick={() => setIsCommandPaletteOpen(false)}
            className="text-white48 hover:text-white p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Results List */}
        <div className="max-h-96 overflow-y-auto p-3 space-y-4">
          {/* Quick Actions */}
          {query.trim() && (
            <div>
              <div className="px-3 text-[10px] font-semibold text-white48 uppercase mb-1">
                Ask Multi-Agent Team
              </div>
              <div
                onClick={() => handleAction(`EXEC:${query.trim()}`)}
                className="flex items-center justify-between px-3 py-2.5 rounded-r10 bg-surface02 hover:bg-surfaceSelected cursor-pointer text-xs font-semibold text-white border border-white10"
              >
                <div className="flex items-center space-x-2">
                  <Play className="w-3.5 h-3.5 text-white" />
                  <span>Execute: "{query}"</span>
                </div>
                <ArrowRight className="w-3.5 h-3.5 text-white48" />
              </div>
            </div>
          )}

          {/* Projects */}
          {filteredProjects.length > 0 && (
            <div>
              <div className="px-3 text-[10px] font-semibold text-white48 uppercase mb-1">
                Projects
              </div>
              <div className="space-y-0.5">
                {filteredProjects.map((p) => (
                  <div
                    key={p.id}
                    onClick={() => handleAction(`PROJ:${p.id}`)}
                    className="flex items-center justify-between px-3 py-2 rounded-r8 hover:bg-surface02 cursor-pointer text-xs text-white64 hover:text-white"
                  >
                    <div className="flex items-center space-x-2.5">
                      <FolderGit2 className="w-3.5 h-3.5" />
                      <span>{p.name}</span>
                    </div>
                    <span className="text-[10px] font-mono text-white32">{p.active_branch}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tasks */}
          {filteredTasks.length > 0 && (
            <div>
              <div className="px-3 text-[10px] font-semibold text-white48 uppercase mb-1">
                Tasks
              </div>
              <div className="space-y-0.5">
                {filteredTasks.map((t) => (
                  <div
                    key={t.id}
                    onClick={() => handleAction(`PROJ:${t.project_id}`)}
                    className="flex items-center justify-between px-3 py-2 rounded-r8 hover:bg-surface02 cursor-pointer text-xs text-white64 hover:text-white"
                  >
                    <div className="flex items-center space-x-2.5">
                      <ListTodo className="w-3.5 h-3.5" />
                      <span className="truncate max-w-md">{t.title}</span>
                    </div>
                    <span className="text-[10px] font-mono uppercase text-white48">{t.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Agents */}
          {filteredAgents.length > 0 && (
            <div>
              <div className="px-3 text-[10px] font-semibold text-white48 uppercase mb-1">
                AI Agents
              </div>
              <div className="space-y-0.5">
                {filteredAgents.map((a) => (
                  <div
                    key={a.id}
                    onClick={() => handleAction('VIEW_TEAM')}
                    className="flex items-center justify-between px-3 py-2 rounded-r8 hover:bg-surface02 cursor-pointer text-xs text-white64 hover:text-white"
                  >
                    <div className="flex items-center space-x-2.5">
                      <Users className="w-3.5 h-3.5" />
                      <span>{a.name} ({a.role})</span>
                    </div>
                    <span className="text-[10px] font-mono uppercase text-accentGreen">{a.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-4 py-2.5 border-t border-white10 bg-surface02 text-[10px] text-white48 flex items-center justify-between">
          <span>Navigate with arrow keys • Press Enter to select</span>
          <kbd className="bg-surface03 px-1.5 py-0.5 rounded border border-white14 text-white80 font-mono">
            ESC to close
          </kbd>
        </div>
      </div>
    </div>
  );
};
