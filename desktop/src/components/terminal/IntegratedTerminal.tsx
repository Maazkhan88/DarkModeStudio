import React, { useState, useRef, useEffect } from 'react';
import { Terminal as TerminalIcon, Play, Trash2, Shield } from 'lucide-react';
import { useDms } from '../../context/DmsContext';

export const IntegratedTerminal: React.FC = () => {
  const { terminalLogs, sendTerminalCommand, selectedProjectId, projects } = useDms();
  const [cmdInput, setCmdInput] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  const selectedProject = projects.find((p) => p.id === selectedProjectId) || projects[0];

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [terminalLogs]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && cmdInput.trim()) {
      e.preventDefault();
      sendTerminalCommand(cmdInput.trim());
      setCmdInput('');
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-oled p-6 max-w-7xl mx-auto w-full select-text">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white10 pb-4 mb-4 select-none">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-r8 bg-surface01 border border-white14 flex items-center justify-center">
            <TerminalIcon className="w-4 h-4 text-white" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white tracking-tight">
              Integrated PTY Terminal
            </h3>
            <p className="text-[11px] text-white48 font-mono">
              Working Directory: {selectedProject?.local_path || 'e:/DMSCC'}
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-[10px] font-mono text-white48 bg-surface01 border border-white10 px-2 py-1 rounded">
            PowerShell / Bash Bridge
          </span>
        </div>
      </div>

      {/* Terminal Output Window */}
      <div className="flex-1 bg-surface01 border border-white10 rounded-r18 p-5 font-mono text-xs overflow-y-auto space-y-1.5 selection:bg-white selection:text-black">
        {terminalLogs.map((log, index) => (
          <div key={index} className="leading-relaxed whitespace-pre-wrap text-white80">
            {log}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Command Input Rail */}
      <div className="mt-4 flex items-center bg-surface01 border border-white14 rounded-r14 px-4 py-2.5 space-x-3">
        <span className="text-xs font-mono font-bold text-white64">$</span>
        <input
          type="text"
          value={cmdInput}
          onChange={(e) => setCmdInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Execute shell command or CLI agent prompt in project context..."
          className="w-full bg-transparent text-xs font-mono text-white placeholder-white48 outline-none"
        />
        <button
          onClick={() => {
            if (cmdInput.trim()) {
              sendTerminalCommand(cmdInput.trim());
              setCmdInput('');
            }
          }}
          className="p-1.5 rounded-r8 bg-white text-oled hover:bg-white92 transition-all"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
        </button>
      </div>
    </div>
  );
};
