import React, { useState } from 'react';
import { X, Play, Pause, Square, Terminal as TerminalIcon, CheckCircle2, Clock, ChevronDown, ChevronUp } from 'lucide-react';
import { useDms } from '../../context/DmsContext';
import { ExecutionRail } from '../common/ExecutionRail';

export const LiveRunView: React.FC = () => {
  const { isLiveRunOpen, setIsLiveRunOpen, activePlan, terminalLogs } = useDms();
  const [isLogsExpanded, setIsLogsExpanded] = useState(true);

  if (!isLiveRunOpen) return null;

  const defaultStages = [
    {
      id: 'stg-1',
      stageName: 'Architecture & Room DAO Schema',
      stageOrder: 1,
      agentId: 'codex',
      agentName: 'Codex',
      role: 'Lead Architect',
      status: 'COMPLETED' as const,
      durationSeconds: 194,
      summary: 'Verified DEC-034 single source of truth guidelines and generated room entity schema.'
    },
    {
      id: 'stg-2',
      stageName: 'Compose UI Implementation',
      stageOrder: 2,
      agentId: 'claude',
      agentName: 'Claude Code',
      role: 'Primary Developer',
      status: 'RUNNING' as const,
      durationSeconds: 762,
      summary: 'Writing FocusScreen.kt and TimerEngine.kt inside isolated worktree dms/task-101-claude.'
    },
    {
      id: 'stg-3',
      stageName: 'Automated QA & Visual Verification',
      stageOrder: 3,
      agentId: 'antigravity',
      agentName: 'Google Antigravity',
      role: 'QA Engineer',
      status: 'QUEUED' as const,
      durationSeconds: 0
    },
    {
      id: 'stg-4',
      stageName: 'Final Code Review & Merge Approval',
      stageOrder: 4,
      agentId: 'codex',
      agentName: 'Codex',
      role: 'Lead Architect',
      status: 'QUEUED' as const,
      durationSeconds: 0
    }
  ];

  const stages = activePlan?.stages || defaultStages;

  return (
    <div className="fixed inset-0 bg-oled/85 backdrop-blur-md z-50 flex items-center justify-center p-6 select-none animate-in fade-in duration-200">
      <div className="bg-surface01 border border-white20 rounded-r24 w-full max-w-5xl h-[88vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-white10 flex items-center justify-between bg-surface02">
          <div className="flex items-center space-x-3">
            <div className="w-3 h-3 rounded-full bg-white animate-ping" />
            <div>
              <h3 className="text-sm font-bold text-white tracking-tight">
                Live Orchestration: SecondMe — Focus Screen
              </h3>
              <p className="text-[11px] text-white48 font-mono">
                Isolated Git Worktree: `dms/task-101-claude` • Multi-Agent Pipeline
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setIsLiveRunOpen(false)}
              className="p-1.5 rounded-r8 hover:bg-surface03 text-white64 hover:text-white transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Execution Rail Component */}
        <div className="px-10 py-6 border-b border-white08 bg-surface01">
          <ExecutionRail stages={stages} />
        </div>

        {/* Main Content Area */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Stage Cards Grid */}
          <div className="p-6 grid grid-cols-1 md:grid-cols-4 gap-3 border-b border-white08 shrink-0">
            {stages.map((stage) => {
              const isDone = stage.status === 'COMPLETED';
              const isCurrent = stage.status === 'RUNNING';

              return (
                <div
                  key={stage.id}
                  className={`border rounded-r14 p-3.5 transition-all ${
                    isCurrent
                      ? 'bg-surface02 border-white48 shadow-lg'
                      : isDone
                      ? 'bg-surface01 border-white20'
                      : 'bg-surface01 border-white08 opacity-60'
                  }`}
                >
                  <div className="flex items-center justify-between text-[10px] font-mono text-white48 mb-1.5">
                    <span>0{stage.stageOrder}</span>
                    <span className={isCurrent ? 'text-white font-semibold uppercase' : ''}>
                      {stage.status}
                    </span>
                  </div>
                  <div className="text-xs font-bold text-white truncate">{stage.agentName}</div>
                  <div className="text-[11px] text-white64 truncate">{stage.stageName}</div>

                  {stage.durationSeconds > 0 && (
                    <div className="flex items-center space-x-1 text-[10px] text-white48 font-mono mt-2">
                      <Clock className="w-3 h-3" />
                      <span>{Math.floor(stage.durationSeconds / 60)}m {stage.durationSeconds % 60}s</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Streamed Output Console */}
          <div className="flex-1 flex flex-col bg-oled p-4 overflow-hidden font-mono text-xs">
            <div className="flex items-center justify-between pb-2 border-b border-white08 text-white48 text-[11px]">
              <div className="flex items-center space-x-2">
                <TerminalIcon className="w-3.5 h-3.5" />
                <span>Real-Time Multi-Agent Stream Log</span>
              </div>
              <span>UTF-8 • Child Process IPC</span>
            </div>

            <div className="flex-1 overflow-y-auto pt-3 space-y-1.5 text-white80 selection:bg-white selection:text-black">
              {terminalLogs.map((log, i) => (
                <div key={i} className="leading-relaxed whitespace-pre-wrap">
                  {log.startsWith('>>') ? (
                    <span className="text-white font-bold">{log}</span>
                  ) : log.startsWith('✓') ? (
                    <span className="text-accentGreen font-semibold">{log}</span>
                  ) : (
                    log
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="px-6 py-3.5 border-t border-white10 bg-surface02 flex items-center justify-between text-xs">
          <span className="text-white48">
            Press <kbd className="bg-surface03 px-1.5 py-0.5 rounded border border-white14 text-white80 font-mono">ESC</kbd> to minimize run window
          </span>

          <div className="flex items-center space-x-2.5">
            <button
              onClick={() => setIsLiveRunOpen(false)}
              className="px-4 py-1.5 rounded-r10 bg-white text-oled font-semibold hover:bg-white92 transition-all"
            >
              Close Live Run
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
