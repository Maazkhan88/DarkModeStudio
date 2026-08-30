import { AgentProvider } from './AgentProvider.ts';
import { CodexProvider } from './CodexProvider.ts';
import { ClaudeProvider } from './ClaudeProvider.ts';
import { AntigravityProvider } from './AntigravityProvider.ts';

export class ProviderRegistry {
  private providers = new Map<string, AgentProvider>();

  constructor() {
    this.register(new CodexProvider());
    this.register(new ClaudeProvider());
    this.register(new AntigravityProvider());
  }

  register(provider: AgentProvider): void {
    this.providers.set(provider.id, provider);
  }

  get(id: string): AgentProvider | undefined {
    return this.providers.get(id);
  }

  getAll(): AgentProvider[] {
    return Array.from(this.providers.values());
  }

  async detectAll(): Promise<Array<{ id: string; name: string; isInstalled: boolean; version?: string; isAuthenticated: boolean }>> {
    const results = [];
    for (const provider of this.providers.values()) {
      const detection = await provider.detectInstallation();
      results.push({
        id: provider.id,
        name: provider.name,
        isInstalled: detection.isInstalled,
        version: detection.version,
        isAuthenticated: detection.isAuthenticated
      });
    }
    return results;
  }
}

export const providerRegistry = new ProviderRegistry();
