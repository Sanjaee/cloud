import { Elysia } from 'elysia';
import { db } from '../infrastructure/database/client';
import { users, sessions } from '../database/schema';
import { eq, and, gt } from 'drizzle-orm';
import { AppError } from './error-handler';
import { APP_CONSTANTS } from '../config/constants';
import jwt from '@elysiajs/jwt';
import { env } from '../config/env';

export async function hashToken(token: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(token);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Robustly matches an input backup code against hashed backup codes stored in the database.
 * Tests multiple normalized formats (hyphenated, unhyphenated, trimmed, uppercase)
 * to guarantee backwards compatibility and forgiving user input.
 */
export async function matchBackupCode(
  inputCode: string,
  backupCodesHash?: string[] | null
): Promise<number> {
  if (!inputCode || !backupCodesHash || backupCodesHash.length === 0) return -1;

  const raw = String(inputCode).trim();
  const clean = raw.replace(/[\s-]+/g, '').toUpperCase();
  const hyphenated = clean.length === 8 ? `${clean.slice(0, 4)}-${clean.slice(4)}` : clean;

  const candidates = Array.from(new Set([
    raw.toUpperCase(),
    clean,
    hyphenated,
    raw,
  ]));

  for (const candidate of candidates) {
    const hash = await hashToken(candidate);
    const idx = backupCodesHash.indexOf(hash);
    if (idx !== -1) {
      return idx;
    }
  }

  return -1;
}

export interface AuthenticatedUser {
  id: string;
  email: string;
  username: string;
  status: string;
  emailVerifiedAt: Date | null;
  storageQuota: number;
  storageUsed: number;
}

// 60-second in-memory session cache to eliminate redundant database auth queries
interface CachedSession {
  user: AuthenticatedUser;
  sessionId: string;
  cachedAt: number;
}
const sessionCache = new Map<string, CachedSession>();

export function invalidateSessionCache(tokenHash?: string) {
  if (tokenHash) {
    sessionCache.delete(tokenHash);
  } else {
    sessionCache.clear();
  }
}

export const authPlugin = new Elysia({ name: 'plugin:auth' })
  .use(
    jwt({
      name: 'jwt',
      secret: env.AUTH_SECRET,
    })
  )
  .derive({ as: 'scoped' }, async ({ cookie, headers, jwt, request }) => {
    // 1. Try Web HttpOnly Session Cookie
    let sessionCookie: string | undefined = (cookie[APP_CONSTANTS.SESSION_COOKIE_NAME]?.value as string) || undefined;
    if (!sessionCookie) {
      const cookieHeader = request.headers.get('cookie') || headers['cookie'];
      if (cookieHeader) {
        const match = cookieHeader.match(new RegExp(`${APP_CONSTANTS.SESSION_COOKIE_NAME}=([^;]+)`));
        if (match) sessionCookie = match[1].trim();
      }
    }

    if (sessionCookie) {
      const hashed = await hashToken(sessionCookie);

      // Check fast in-memory cache first (avoids duplicate DB query on concurrent requests)
      const cached = sessionCache.get(hashed);
      if (cached && Date.now() - cached.cachedAt < 60_000) {
        return {
          currentUser: cached.user,
          currentSessionId: cached.sessionId,
        };
      }

      const session = await db.query.sessions.findFirst({
        where: and(eq(sessions.tokenHash, hashed), gt(sessions.expiresAt, new Date())),
      });

      if (session) {
        // Throttle lastUsedAt updates: only update DB if more than 5 minutes have elapsed
        const now = Date.now();
        if (!session.lastUsedAt || now - session.lastUsedAt.getTime() > 5 * 60 * 1000) {
          db.update(sessions)
            .set({ lastUsedAt: new Date() })
            .where(eq(sessions.id, session.id))
            .execute()
            .catch(() => {});
        }

        const found = await db.query.users.findFirst({
          where: eq(users.id, session.userId),
        });

        if (found && found.status !== 'suspended') {
          const authUser: AuthenticatedUser = {
            id: found.id,
            email: found.email,
            username: found.username,
            status: found.status,
            emailVerifiedAt: found.emailVerifiedAt,
            storageQuota: found.storageQuota,
            storageUsed: found.storageUsed,
          };

          // Store in cache
          sessionCache.set(hashed, {
            user: authUser,
            sessionId: session.id,
            cachedAt: now,
          });

          return {
            currentUser: authUser,
            currentSessionId: session.id,
          };
        }
      }
    }

    // 2. Try Mobile / API Bearer Token
    const authHeader = headers['authorization'];
    if (authHeader?.startsWith('Bearer ')) {
      const token = authHeader.substring(7).trim();
      const payload = (await jwt.verify(token)) as { sub?: string } | false;
      if (payload && payload.sub) {
        const found = await db.query.users.findFirst({
          where: eq(users.id, payload.sub),
        });

        if (found && found.status !== 'suspended') {
          return {
            currentUser: {
              id: found.id,
              email: found.email,
              username: found.username,
              status: found.status,
              emailVerifiedAt: found.emailVerifiedAt,
              storageQuota: found.storageQuota,
              storageUsed: found.storageUsed,
            },
            currentSessionId: null,
          };
        }
      }
    }

    return {
      currentUser: null,
      currentSessionId: null,
    };
  });

export const requireAuth = (app: Elysia) =>
  app.onBeforeHandle(({ currentUser }: any) => {
    if (!currentUser) {
      throw new AppError(401, 'UNAUTHORIZED', 'Authentication required to access this resource');
    }
  });
