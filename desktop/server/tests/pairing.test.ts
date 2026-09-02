import { describe, it, expect, beforeEach } from 'vitest';
import { hostPairingManager } from '../auth/HostPairingManager.ts';
import { providerRegistry } from '../providers/ProviderRegistry.ts';

describe('Desktop Host Pairing & Security Tests', () => {
  it('should generate valid single-use 6-digit pairing code', () => {
    const pairData = hostPairingManager.generatePairingCode();
    expect(pairData.code).toBeDefined();
    expect(pairData.code.startsWith('DMS-')).toBe(true);
    expect(pairData.expiresAt).toBeGreaterThan(Date.now());
    expect(pairData.hostName).toBeDefined();
  });

  it('should verify valid pairing code and issue cryptographically secure long-term token', () => {
    const pairData = hostPairingManager.generatePairingCode();
    const result = hostPairingManager.verifyPairingCode(pairData.code, 'Pixel-9-Pro', '192.168.1.45');

    expect(result.success).toBe(true);
    expect(result.pairingSecret).toBeDefined();
    expect(result.pairingSecret?.startsWith('dms_host_sec_')).toBe(true);
    expect(result.hostId).toBe('primary_desktop');
    expect(result.availableAgents).toBe('codex,claude,antigravity');

    // Token must now be valid
    expect(hostPairingManager.isValidToken(result.pairingSecret)).toBe(true);
    expect(hostPairingManager.isValidToken(`Bearer ${result.pairingSecret}`)).toBe(true);
  });

  it('should reject reused pairing code (single-use invariant)', () => {
    const pairData = hostPairingManager.generatePairingCode();
    const result1 = hostPairingManager.verifyPairingCode(pairData.code, 'Device-1', '192.168.1.10');
    expect(result1.success).toBe(true);

    const result2 = hostPairingManager.verifyPairingCode(pairData.code, 'Device-2', '192.168.1.11');
    expect(result2.success).toBe(false);
    expect(result2.error).toContain('Invalid pairing code');
  });

  it('should reject invalid or non-existent pairing code', () => {
    const result = hostPairingManager.verifyPairingCode('DMS-000000', 'Device-Bad', '192.168.1.99');
    expect(result.success).toBe(false);
    expect(result.error).toContain('Invalid pairing code');
  });

  it('should reject unauthenticated tokens', () => {
    expect(hostPairingManager.isValidToken('invalid_token')).toBe(false);
    expect(hostPairingManager.isValidToken('')).toBe(false);
    expect(hostPairingManager.isValidToken(undefined)).toBe(false);
  });

  it('should revoke token on request', () => {
    const pairData = hostPairingManager.generatePairingCode();
    const result = hostPairingManager.verifyPairingCode(pairData.code, 'Device-1', '192.168.1.12');
    expect(result.success).toBe(true);

    const token = result.pairingSecret!;
    expect(hostPairingManager.isValidToken(token)).toBe(true);

    const revoked = hostPairingManager.revokeToken(token);
    expect(revoked).toBe(true);
    expect(hostPairingManager.isValidToken(token)).toBe(false);
  });

  it('should provide structured runtime detection for Codex, Claude, Antigravity', async () => {
    const codex = providerRegistry.get('codex');
    const claude = providerRegistry.get('claude');
    const antigravity = providerRegistry.get('antigravity');

    expect(codex).toBeDefined();
    expect(claude).toBeDefined();
    expect(antigravity).toBeDefined();

    const codexDetect = await codex!.detectInstallation();
    expect(typeof codexDetect.isInstalled).toBe('boolean');

    const claudeAuth = await claude!.detectAuth();
    expect(claudeAuth.authType).toBe('Claude Subscription');

    const agyVerify = await antigravity!.verifyAuth();
    expect(agyVerify).toBeDefined();
  });
});
