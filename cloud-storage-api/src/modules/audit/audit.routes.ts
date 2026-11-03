import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { auditLogs } from '../../database/schema';
import { eq, and, sql } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';

export const auditRoutes = new Elysia({ prefix: '/audit' })
  .use(authPlugin)
  .get(
    '/',
    async ({ currentUser, query }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const limit = Math.min(Number(query.limit) || 20, 100);
      const offset = Number(query.offset) || 0;

      const logs = await db.query.auditLogs.findMany({
        where: eq(auditLogs.actorId, currentUser.id),
        orderBy: (a, { desc }) => [desc(a.createdAt)],
        limit,
        offset,
      });

      const totalRes = await db
        .select({ count: sql<number>`count(*)::int` })
        .from(auditLogs)
        .where(eq(auditLogs.actorId, currentUser.id));

      return {
        success: true,
        data: {
          logs,
          total: totalRes[0]?.count || 0,
          limit,
          offset,
        },
      };
    },
    {
      query: t.Object({
        limit: t.Optional(t.String()),
        offset: t.Optional(t.String()),
      }),
      detail: { tags: ['Audit'], summary: 'Get current user operational and security audit logs' },
    }
  );

