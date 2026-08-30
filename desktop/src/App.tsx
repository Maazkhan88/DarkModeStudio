import React from 'react';
import { useDms } from './context/DmsContext';
import { Sidebar } from './components/layout/Sidebar';
import { TopBar } from './components/layout/TopBar';
import { CommandCenterHome } from './components/dashboard/CommandCenterHome';
import { ProjectWorkspace } from './components/project/ProjectWorkspace';
import { IntegratedTerminal } from './components/terminal/IntegratedTerminal';
import { LiveRunView } from './components/runs/LiveRunView';
import { CommandPalette } from './components/modals/CommandPalette';
import { Users, ListTodo, GitBranch, Settings } from 'lucide-react';
import { AgentNode } from './components/common/AgentNode';

export const App: React.FC = () => {
  const { activeView, agents, tasks, projects } = useDms();

  return (
    <div className="flex h-screen w-screen bg-oled text-white overflow-hidden font-sans">
      {/* Sidebar Navigation */}
      <Sidebar />

      {/* Main App Content Area */}
      <div className="flex-1 flex flex-col h-screen overflow-hidden">
        <TopBar />

        <main className="flex-1 flex overflow-hidden">
          {activeView === 'home' && <CommandCenterHome />}
          {activeView === 'project' && <ProjectWorkspace />}
          {activeView === 'terminal' && <IntegratedTerminal />}

          {/* Team / Agents View */}
          {activeView === 'team' && (
            <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-7xl mx-auto">
              <div className="space-y-2">
                <h2 className="text-2xl font-bold text-white tracking-tight">AI Development Team</h2>
                <p className="text-xs text-white64">
                  Multi-agent development team powered by official locally authenticated CLI runtimes.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                {agents.map((agent) => (
                  <div key={agent.id} className="bg-surface01 border border-white10 rounded-r18 p-6 space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-base font-bold text-white">{agent.name}</h3>
                        <p className="text-xs text-white48">{agent.role}</p>
                      </div>
                      <AgentNode status={agent.status} size={8} label={agent.status} />
                    </div>

                    <div className="space-y-2 text-xs text-white80 pt-2 border-t border-white08">
                      <div className="text-[11px] text-white48 uppercase font-semibold">Capabilities</div>
                      <div className="flex flex-wrap gap-1.5">
                        {agent.capabilities.map((cap) => (
                          <span key={cap} className="bg-surface02 border border-white08 px-2 py-0.5 rounded text-[10px] text-white80 font-mono">
                            {cap}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div className="pt-2 border-t border-white08 flex items-center justify-between text-xs text-white48 font-mono">
                      <span>Tasks today: {agent.tasks_today_count}</span>
                      <span className="text-accentGreen">Installed & Authenticated ✓</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tasks Board View */}
          {activeView === 'tasks' && (
            <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-7xl mx-auto">
              <div className="space-y-2">
                <h2 className="text-2xl font-bold text-white tracking-tight">All Development Tasks</h2>
                <p className="text-xs text-white64">
                  Unified task lifecycle management across all projects and isolated worktrees.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                {['RUNNING', 'READY', 'DONE'].map((colStatus) => {
                  const colTasks = tasks.filter((t) =>
                    colStatus === 'RUNNING' ? t.status === 'RUNNING' : colStatus === 'READY' ? (t.status === 'READY' || t.status === 'READY_TO_MERGE') : t.status === 'DONE'
                  );
                  return (
                    <div key={colStatus} className="bg-surface01 border border-white10 rounded-r18 p-4 space-y-3">
                      <div className="flex items-center justify-between border-b border-white08 pb-2">
                        <span className="text-xs font-bold uppercase tracking-wider text-white">
                          {colStatus}
                        </span>
                        <span className="text-xs font-mono text-white48">{colTasks.length}</span>
                      </div>

                      <div className="space-y-2.5">
                        {colTasks.map((t) => (
                          <div key={t.id} className="bg-surface02 border border-white08 rounded-r12 p-3.5 space-y-2">
                            <div className="flex items-center justify-between text-[10px] font-mono text-white48">
                              <span>{t.id}</span>
                              <span className="text-accentGreen uppercase">{t.status}</span>
                            </div>
                            <h4 className="text-xs font-bold text-white">{t.title}</h4>
                            <div className="text-[10px] text-white48 truncate">{t.description}</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Git View */}
          {activeView === 'git' && (
            <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-7xl mx-auto">
              <div className="space-y-2">
                <h2 className="text-2xl font-bold text-white tracking-tight">Git & Worktree Isolation</h2>
                <p className="text-xs text-white64">
                  Automated worktree sandbox provisioning under `.dms-worktrees/` ensuring safe parallel agent execution.
                </p>
              </div>

              <div className="bg-surface01 border border-white10 rounded-r18 p-6 space-y-4 font-mono text-xs">
                <div className="text-white font-bold text-sm">Active Agent Sandboxes</div>
                <div className="space-y-2">
                  <div className="p-3 bg-surface02 border border-white08 rounded-r10 flex items-center justify-between">
                    <div>
                      <div className="text-white font-semibold">dms/task-101-claude</div>
                      <div className="text-white48 text-[11px]">.dms-worktrees/task-101-claude</div>
                    </div>
                    <span className="text-accentGreen text-[10px]">Claude Code Workspace</span>
                  </div>
                  <div className="p-3 bg-surface02 border border-white08 rounded-r10 flex items-center justify-between">
                    <div>
                      <div className="text-white font-semibold">dms/task-101-antigravity</div>
                      <div className="text-white48 text-[11px]">.dms-worktrees/task-101-antigravity</div>
                    </div>
                    <span className="text-accentGreen text-[10px]">Antigravity QA Sandbox</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Settings View */}
          {activeView === 'settings' && (
            <div className="flex-1 overflow-y-auto p-8 space-y-6 max-w-4xl mx-auto">
              <div className="space-y-2">
                <h2 className="text-2xl font-bold text-white tracking-tight">Command Center Settings</h2>
                <p className="text-xs text-white64">Configure local CLI agent paths, permissions, and security enclave.</p>
              </div>

              <div className="bg-surface01 border border-white10 rounded-r18 p-6 space-y-4 text-xs">
                <div className="flex items-center justify-between border-b border-white08 pb-3">
                  <div>
                    <div className="text-white font-semibold">OLED Pure Black Mode</div>
                    <div className="text-white48 text-[11px]">Enforce strict #000000 true black contrast palette</div>
                  </div>
                  <span className="text-accentGreen font-mono font-semibold">ENABLED</span>
                </div>

                <div className="flex items-center justify-between border-b border-white08 pb-3">
                  <div>
                    <div className="text-white font-semibold">Local Agent CLI Privileges</div>
                    <div className="text-white48 text-[11px]">Use existing local CLI subscriptions for Codex, Claude Code, and Antigravity</div>
                  </div>
                  <span className="text-accentGreen font-mono font-semibold">LOCAL ONLY</span>
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-white font-semibold">Hardware Enclave Key Storage</div>
                    <div className="text-white48 text-[11px]">Store API secrets and credentials in local encrypted keystore</div>
                  </div>
                  <span className="text-accentGreen font-mono font-semibold">ENCRYPTED</span>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Global Modals */}
      <LiveRunView />
      <CommandPalette />
    </div>
  );
};
