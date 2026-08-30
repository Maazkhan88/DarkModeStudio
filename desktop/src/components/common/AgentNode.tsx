import React from 'react';
import { AgentStatus } from '../../types';

interface AgentNodeProps {
  status: AgentStatus;
  size?: number;
  label?: string;
  showLabel?: boolean;
}

export const AgentNode: React.FC<AgentNodeProps> = ({
  status,
  size = 8,
  label,
  showLabel = true
}) => {
  const getStatusStyle = () => {
    switch (status) {
      case 'EXECUTING':
        return {
          bg: 'bg-white',
          border: 'border-white',
          pulse: 'animate-spin border-t-transparent',
          text: 'Executing'
        };
      case 'THINKING':
        return {
          bg: 'bg-white',
          border: 'border-white',
          pulse: 'animate-pulse-slow',
          text: 'Thinking'
        };
      case 'COMPLETED':
        return {
          bg: 'bg-white',
          border: 'border-white',
          pulse: '',
          text: 'Completed'
        };
      case 'BLOCKED':
        return {
          bg: 'bg-accentRed',
          border: 'border-accentRed',
          pulse: '',
          text: 'Blocked'
        };
      case 'IDLE':
        return {
          bg: 'bg-white',
          border: 'border-white',
          pulse: '',
          text: 'Idle'
        };
      case 'OFFLINE':
      default:
        return {
          bg: 'bg-transparent',
          border: 'border-white48',
          pulse: '',
          text: 'Offline'
        };
    }
  };

  const style = getStatusStyle();

  return (
    <div className="flex items-center space-x-2">
      <div
        className={`relative flex items-center justify-center rounded-full border ${style.border} ${style.pulse}`}
        style={{ width: `${size + 4}px`, height: `${size + 4}px` }}
      >
        <div
          className={`rounded-full ${style.bg}`}
          style={{ width: `${size}px`, height: `${size}px` }}
        />
      </div>
      {showLabel && label && (
        <span className="text-xs font-medium tracking-wide text-white80 uppercase">
          {label}
        </span>
      )}
    </div>
  );
};
