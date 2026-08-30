import React, { useState } from 'react';
import { Sparkles, ArrowRight, X, Play, ShieldAlert, Cpu } from 'lucide-react';
import { useDms } from '../../context/DmsContext';
import { WorkflowExecutionPlan } from '../../types';
import { ExecutionRail } from './ExecutionRail';

export const CommandBar: React.FC = () => {
  const { createExecutionPlan, executePlan, selectedProjectId, projects } = useDms();
  const [input, setInput] = useState('');
  const [isPlanning, setIsPlanning] = useState(false);
  const [planPreview, setPlanPreview] = useState<WorkflowExecutionPlan | null>(null);

  const selectedProject = projects.find((p) => p.id === selectedProjectId) || projects[0];

  const handleKeyDown = async (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && input.trim()) {
      e.preventDefault();
      setIsPlanning(true);
      try {
        const plan = await createExecutionPlan(input.trim());
        setPlanPreview(plan);
      } finally {
        setIsPlanning(false);
      }
    }
  };

  const handleExecute = async () => {
    if (planPreview) {
      await executePlan(planPreview);
      setPlanPreview(null);
      setInput('');
    }
  };

  return (
    <div className="relative w-full max-w-4xl mx-auto z-30">
      {/* Main Input Box */}
      <div className="relative flex items-center bg-surface01 border border-white14 hover:border-white32 focus-within:border-white rounded-r24 px-5 py-3.5 shadow-2xl transition-all duration-200">
        <Sparkles className="w-5 h-5 text-white64 mr-3 shrink-0" />
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={`Ask your development team on ${selectedProject?.name || 'project'}… (e.g. "Finish Focus Screen and prepare PR")`}
          className="w-full bg-transparent text-sm text-white placeholder-white48 outline-none font-medium"
        />

        {input.trim() && !planPreview && (
          <button
            onClick={async () => {
              setIsPlanning(true);
              const plan = await createExecutionPlan(input.trim());
              setPlanPreview(plan);
              setIsPlanning(false);
            }}
            disabled={isPlanning}
            className="flex items-center space-x-1.5 bg-white text-oled text-xs font-semibold px-3 py-1.5 rounded-r12 hover:bg-white92 transition-all ml-2 shrink-0"
          >
            <span>{isPlanning ? 'Planning...' : 'Plan Workflow'}</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Structured Execution Plan Preview Modal / Drawer */}
      {planPreview && (
        <div className="absolute top-16 left-0 right-0 bg-surface01 border border-white20 rounded-r22 p-5 shadow-2xl backdrop-blur-xl animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="flex items-center justify-between border-b border-white10 pb-3 mb-4">
            <div className="flex items-center space-x-2.5">
              <Cpu className="w-4 h-4 text-white" />
              <h4 className="text-xs font-semibold uppercase tracking-wider text-white">
                Multi-Agent Execution Plan
              </h4>
              <span className="text-[10px] bg-surface03 text-white80 px-2 py-0.5 rounded-full border border-white14">
                {planPreview.strategy}
              </span>
            </div>
            <button
              onClick={() => setPlanPreview(null)}
              className="text-white48 hover:text-white transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <p className="text-sm text-white92 font-medium mb-6">
            "{planPreview.rawPrompt}"
          </p>

          {/* Execution Rail Preview */}
          <div className="mb-10 px-4">
            <ExecutionRail stages={planPreview.stages} />
          </div>

          {/* Workflow Stage List */}
          <div className="space-y-2 mb-5">
            {planPreview.stages.map((stage) => (
              <div
                key={stage.id}
                className="flex items-center justify-between bg-surface02 border border-white08 rounded-r12 px-3.5 py-2.5"
              >
                <div className="flex items-center space-x-3">
                  <span className="text-xs font-mono text-white48">0{stage.stageOrder}</span>
                  <div>
                    <div className="text-xs font-medium text-white">{stage.stageName}</div>
                    <div className="text-[10px] text-white48">{stage.role}</div>
                  </div>
                </div>
                <span className="text-xs font-mono text-white80 bg-surface03 px-2 py-1 rounded-r8 border border-white08">
                  {stage.agentName}
                </span>
              </div>
            ))}
          </div>

          {/* Action CTAs */}
          <div className="flex items-center justify-between pt-3 border-t border-white10">
            <div className="flex items-center space-x-2 text-xs text-white48">
              <ShieldAlert className="w-3.5 h-3.5 text-white64" />
              <span>Agents work in isolated Git worktrees (`.dms-worktrees/`)</span>
            </div>
            <div className="flex items-center space-x-2.5">
              <button
                onClick={() => setPlanPreview(null)}
                className="px-3.5 py-1.5 rounded-r10 text-xs font-medium text-white64 hover:text-white hover:bg-surface02 transition-all"
              >
                Cancel
              </button>
              <button
                onClick={handleExecute}
                className="flex items-center space-x-2 bg-white text-oled text-xs font-semibold px-4 py-2 rounded-r12 hover:bg-white92 transition-all shadow-lg"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Launch Orchestrator</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
