import crypto from 'crypto';
import os from 'os';
import { Request, Response, NextFunction } from 'express';

interface PairingCodeEntry {
  code: string;
  createdAt: number;
  expiresAt: number;
  used: boolean;
}

export class HostPairingManager {
  private activeCodes = new Map<string, PairingCodeEntry>();
  private pairedTokens = new Set<string>();
  private failedAttempts = new Map<string, { count: number; lockedUntil: number }>();

  private readonly CODE_TTL_MS = 5 * 60 * 1000; // 5 minutes
  private readonly MAX_FAILED_ATTEMPTS = 5;
  private readonly LOCKOUT_MS = 60 * 1000; // 1 minute

  generatePairingCode(): { code: string; expiresAt: number; hostName: string } {
    // Generate a 6-digit cryptographic uppercase alphanumeric code e.g. DMS-849201
    const randomDigits = crypto.randomInt(100000, 999999).toString();
    const code = `DMS-${randomDigits}`;
    const now = Date.now();
    const expiresAt = now + this.CODE_TTL_MS;

    this.activeCodes.set(code, {
      code,
      createdAt: now,
      expiresAt,
      used: false
    });

    return {
      code,
      expiresAt,
      hostName: os.hostname()
    };
  }

  verifyPairingCode(
    code: string,
    clientName: string,
    clientIp: string
  ): { success: boolean; hostId?: string; hostName?: string; pairingSecret?: string; availableAgents?: string; error?: string } {
    const now = Date.now();

    // Check rate limit / lockout
    const attempt = this.failedAttempts.get(clientIp);
    if (attempt && attempt.lockedUntil > now) {
      const waitSec = Math.ceil((attempt.lockedUntil - now) / 1000);
      return { success: false, error: `Too many failed pairing attempts. Try again in ${waitSec}s.` };
    }

    const entry = this.activeCodes.get(code.trim().toUpperCase());
    if (!entry) {
      this.recordFailedAttempt(clientIp);
      return { success: false, error: 'Invalid pairing code.' };
    }

    if (entry.used) {
      this.recordFailedAttempt(clientIp);
      return { success: false, error: 'Pairing code has already been used.' };
    }

    if (entry.expiresAt < now) {
      this.activeCodes.delete(code.trim().toUpperCase());
      this.recordFailedAttempt(clientIp);
      return { success: false, error: 'Pairing code has expired. Generate a new code on your desktop.' };
    }

    // Mark code as used (single-use)
    entry.used = true;
    this.activeCodes.delete(code.trim().toUpperCase());
    this.failedAttempts.delete(clientIp);

    // Generate a cryptographically secure 256-bit long-term pairing secret
    const pairingSecret = `dms_host_sec_${crypto.randomBytes(32).toString('hex')}`;
    this.pairedTokens.add(pairingSecret);

    return {
      success: true,
      hostId: 'primary_desktop',
      hostName: os.hostname(),
      pairingSecret,
      availableAgents: 'codex,claude,antigravity'
    };
  }

  isValidToken(token: string | undefined): boolean {
    if (!token || typeof token !== 'string') return false;
    const cleanToken = token.startsWith('Bearer ') ? token.slice(7).trim() : token.trim();
    return this.pairedTokens.has(cleanToken);
  }

  revokeToken(token: string): boolean {
    const cleanToken = token.startsWith('Bearer ') ? token.slice(7).trim() : token.trim();
    return this.pairedTokens.delete(cleanToken);
  }

  authMiddleware() {
    return (req: Request, res: Response, next: NextFunction) => {
      // Public endpoints
      const publicPaths = ['/api/host/pair/generate', '/api/host/pair/verify', '/api/health'];
      if (publicPaths.includes(req.path)) {
        return next();
      }

      const authHeader = req.headers.authorization;
      if (!authHeader || !this.isValidToken(authHeader)) {
        return res.status(401).json({
          error: 'Unauthorized: Valid DMS pairing credential required. Pair your device via Desktop Host.'
        });
      }

      next();
    };
  }

  private recordFailedAttempt(clientIp: string) {
    const now = Date.now();
    const entry = this.failedAttempts.get(clientIp) || { count: 0, lockedUntil: 0 };
    entry.count += 1;
    if (entry.count >= this.MAX_FAILED_ATTEMPTS) {
      entry.lockedUntil = now + this.LOCKOUT_MS;
      entry.count = 0;
    }
    this.failedAttempts.set(clientIp, entry);
  }
}

export const hostPairingManager = new HostPairingManager();
