import { Elysia } from 'elysia';
import { db } from '@/infrastructure/database/client';
import { users, sessions } from '@/database/schema';
import { eq, and, gt } from 'drizzle-orm';
import { AppError } from './error-handler';
import { APP_CONSTANTS } from '@/config/constants';
import jwt from '@elysiajs/jwt';
import { env } from '@/config/env';

export async function hashToken(token: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(token);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
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

export const authPlugin = new Elysia({ name: 'plugin:auth' })
  .use(
    jwt({
      name: 'jwt',
      secret: env.AUTH_SECRET,
    })
  )
  .derive({ as: 'scoped' }, async ({ cookie, headers, jwt, request }) => {
    // 1. Try Web HttpOnly Session Cookie
    let sessionCookie: string | undefined = cookie[APP_CONSTANTS.SESSION_COOKIE_NAME]?.value;
    if (!sessionCookie) {
      const cookieHeader = request.headers.get('cookie') || headers['cookie'];
      if (cookieHeader) {
        const match = cookieHeader.match(new RegExp(`${APP_CONSTANTS.SESSION_COOKIE_NAME}=([^;]+)`));
        if (match) sessionCookie = match[1].trim();
      }
    }

    let userId: string | null = null;
    let sessionId: string | null = null;

    if (sessionCookie) {
      const hashed = await hashToken(sessionCookie);
      const session = await db.query.sessions.findFirst({
        where: and(eq(sessions.tokenHash, hashed), gt(sessions.expiresAt, new Date())),
      });

      if (session) {
        userId = session.userId;
        sessionId = session.id;
        db.update(sessions)
          .set({ lastUsedAt: new Date() })
          .where(eq(sessions.id, session.id))
          .execute()
          .catch(() => {});
      }
    }

    // 2. Try Mobile / API Bearer Token
    if (!userId) {
      const authHeader = headers['authorization'];
      if (authHeader?.startsWith('Bearer ')) {
        const token = authHeader.substring(7).trim();
        const payload = (await jwt.verify(token)) as { sub?: string } | false;
        if (payload && payload.sub) {
          userId = payload.sub;
        }
      }
    }

    let user: AuthenticatedUser | null = null;
    if (userId) {
      const found = await db.query.users.findFirst({
        where: eq(users.id, userId),
      });

      if (found && found.status !== 'suspended') {
        user = {
          id: found.id,
          email: found.email,
          username: found.username,
          status: found.status,
          emailVerifiedAt: found.emailVerifiedAt,
          storageQuota: found.storageQuota,
          storageUsed: found.storageUsed,
        };
      }
    }

    return {
      currentUser: user,
      currentSessionId: sessionId,
    };
  });

export const requireAuth = (app: Elysia) =>
  app.onBeforeHandle(({ currentUser }: any) => {
    if (!currentUser) {
      throw new AppError(401, 'UNAUTHORIZED', 'Authentication required to access this resource');
    }
  });
