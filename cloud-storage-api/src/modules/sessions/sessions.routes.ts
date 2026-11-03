import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { sessions } from '../../database/schema';
import { eq, and, ne } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';

export const sessionRoutes = new Elysia({ prefix: '/sessions' })
  .use(authPlugin)
  // 1. List all active sessions for current user
  .get(
    '/',
    async ({ currentUser, currentSessionId }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const userSessions = await db.query.sessions.findMany({
        where: eq(sessions.userId, currentUser.id),
        orderBy: (s, { desc }) => [desc(s.lastUsedAt)],
      });

      return {
        success: true,
        data: {
          sessions: userSessions.map(s => ({
            id: s.id,
            ipAddress: s.ipAddress,
            userAgent: s.userAgent,
            lastUsedAt: s.lastUsedAt,
            createdAt: s.createdAt,
            isCurrent: s.id === currentSessionId,
          })),
        },
      };
    },
    {
      detail: { tags: ['Sessions'], summary: 'List all active user sessions and devices' },
    }
  )

  // 2. Revoke specific session
  .delete(
    '/:sessionId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const deleted = await db
        .delete(sessions)
        .where(and(eq(sessions.id, params.sessionId), eq(sessions.userId, currentUser.id)))
        .returning();

      if (!deleted.length) {
        throw new AppError(404, 'NOT_FOUND', 'Session not found or already revoked');
      }

      return {
        success: true,
        message: 'Session revoked successfully',
      };
    },
    {
      params: t.Object({
        sessionId: t.String({ format: 'uuid' }),
      }),
      detail: { tags: ['Sessions'], summary: 'Revoke a specific active session' },
    }
  )

  // 3. Revoke all other sessions except current
  .post(
    '/revoke-others',
    async ({ currentUser, currentSessionId }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      if (currentSessionId) {
        await db
          .delete(sessions)
          .where(and(eq(sessions.userId, currentUser.id), ne(sessions.id, currentSessionId)));
      }

      return {
        success: true,
        message: 'All other sessions have been revoked',
      };
    },
    {
      detail: { tags: ['Sessions'], summary: 'Revoke all sessions except the current active session' },
    }
  );

