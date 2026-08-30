import React from 'react';
import { WorkflowStage } from '../../types';

interface ExecutionRailProps {
  stages: WorkflowStage[];
  className?: string;
}

export const ExecutionRail: React.FC<ExecutionRailProps> = ({ stages, className = '' }) => {
  return (
    <div className={`flex items-center w-full justify-between py-2 ${className}`}>
      {stages.map((stage, idx) => {
        const isCompleted = stage.status === 'COMPLETED';
        const isRunning = stage.status === 'RUNNING';
        const isQueued = stage.status === 'QUEUED';
        const isLast = idx === stages.length - 1;

        return (
          <React.Fragment key={stage.id || idx}>
            <div className="flex flex-col items-center group relative">
              {/* Node Circle */}
              <div
                className={`w-4 h-4 rounded-full flex items-center justify-center transition-all duration-300 ${
                  isCompleted
                    ? 'bg-white border border-white'
                    : isRunning
                    ? 'bg-oled border-2 border-white animate-soft-pulse'
                    : 'bg-oled border border-white20'
                }`}
              >
                {isCompleted && <div className="w-1.5 h-1.5 rounded-full bg-oled" />}
                {isRunning && <div className="w-1.5 h-1.5 rounded-full bg-white animate-ping" />}
              </div>

              {/* Stage Title & Agent Label */}
              <div className="absolute top-6 flex flex-col items-center w-28 text-center pointer-events-none">
                <span
                  className={`text-[11px] font-medium tracking-tight truncate ${
                    isRunning ? 'text-white font-semibold' : isCompleted ? 'text-white80' : 'text-white48'
                  }`}
                >
                  {stage.stageName.split('&')[0].trim()}
                </span>
                <span className="text-[9px] text-white48 tracking-wide uppercase">
                  {stage.agentName}
                </span>
              </div>
            </div>

            {/* Connecting Rail Line */}
            {!isLast && (
              <div
                className={`flex-1 h-[1.5px] mx-2 transition-all duration-500 ${
                  isCompleted ? 'bg-white' : isRunning ? 'bg-gradient-to-r from-white to-white20' : 'bg-white14'
                }`}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};
