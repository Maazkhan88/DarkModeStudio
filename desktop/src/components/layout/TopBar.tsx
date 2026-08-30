import React from 'react';
import { Search, Bell, Terminal, Cpu, Play } from 'lucide-react';
import { useDms } from '../../context/DmsContext';

export const TopBar: React.FC = () => {
  const {
    projects,
    selectedProjectId,
    setSelectedProjectId,
    agents,
    tasks,
    setIsCommandPaletteOpen,
    setIsLiveRunOpen
  } = useDms();

  const selectedProject = projects.find((p) => p.id === selectedProjectId) || projects[0];
  const runningTasksCount = tasks.filter((t) => t.status === 'RUNNING').length;
  const onlineAgentsCount = agents.filter((a) => a.status !== 'OFFLINE').length;

  return (
    <header className="h-14 border-b border-white08 bg-oled px-6 flex items-center justify-between shrink-0 select-none z-20">
      {/* Left: Project Selector & Breadcrumb */}
      <div className="flex items-center space-x-3">
        <div className="flex items-center space-x-2 bg-surface01 border border-white14 rounded-r10 px-3 py-1.5 text-xs font-semibold text-white">
          <span className="w-2 h-2 rounded-full bg-white animate-pulse-slow" />
          <select
            value={selectedProjectId || ''}
            onChange={(e) => setSelectedProjectId(e.target.value)}
            className="bg-transparent text-white outline-none cursor-pointer font-medium"
          >
            {projects.map((p) => (
              <option key={p.id} value={p.id} className="bg-surface02 text-white">
                {p.name} ({p.active_branch})
              </option>
            ))}
          </select>
        </div>

        <span className="text-white20">/</span>

        <span className="text-xs text-white64 font-mono">
          {selectedProject?.local_path || 'e:/DMSCC'}
        </span>
      </div>

      {/* Center: Global Status Pill */}
      <div className="hidden md:flex items-center space-x-4 bg-surface01 border border-white10 rounded-full px-4 py-1 text-xs">
        <div className="flex items-center space-x-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-accentGreen" />
          <span className="text-white80 font-medium">{onlineAgentsCount} Agents Online</span>
        </div>
        <span className="text-white20">|</span>
        <div className="flex items-center space-x-1.5">
          <Cpu className="w-3 h-3 text-white80" />
          <span className="text-white80 font-medium">{projects.length} Projects Active</span>
        </div>
        <span className="text-white20">|</span>
        <div className="flex items-center space-x-1.5">
          <Play className="w-3 h-3 text-white80" />
          <span className="text-white80 font-medium">{runningTasksCount} Tasks Running</span>
        </div>
      </div>

      {/* Right: Search Hotkey & Live Run Toggle */}
      <div className="flex items-center space-x-2.5">
        <button
          onClick={() => setIsCommandPaletteOpen(true)}
          className="flex items-center space-x-2 bg-surface01 hover:bg-surface02 border border-white14 rounded-r10 px-3 py-1.5 text-xs text-white64 hover:text-white transition-all"
        >
          <Search className="w-3.5 h-3.5" />
          <span>Search</span>
          <kbd className="text-[10px] bg-surface03 border border-white14 px-1.5 py-0.5 rounded font-mono text-white80">
            ⌘K
          </kbd>
        </button>

        <button
          onClick={() => setIsLiveRunOpen(true)}
          className="flex items-center space-x-1.5 bg-surface01 hover:bg-surface02 border border-white14 rounded-r10 px-3 py-1.5 text-xs text-white transition-all font-medium"
        >
          <Terminal className="w-3.5 h-3.5 text-white" />
          <span>Live Run</span>
        </button>

        <button className="p-2 rounded-r10 text-white64 hover:text-white hover:bg-surface01 transition-all">
          <Bell className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};
