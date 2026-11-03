import { Elysia, t } from 'elysia';
import { db } from '@/infrastructure/database/client';
import { notifications } from '@/database/schema';
import { eq, and, isNull, sql } from 'drizzle-orm';
import { AppError } from '@/middleware/error-handler';
import { authPlugin } from '@/middleware/authentication';

export const notificationRoutes = new Elysia({ prefix: '/notifications' })
  .use(authPlugin)
  // 1. List Notifications
  .get(
    '/',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const items = await db.query.notifications.findMany({
        where: eq(notifications.userId, currentUser.id),
        orderBy: (n, { desc }) => [desc(n.createdAt)],
        limit: 50,
      });

      const unreadCountRes = await db
        .select({ count: sql<number>`count(*)::int` })
        .from(notifications)
        .where(and(eq(notifications.userId, currentUser.id), isNull(notifications.readAt)));

      return {
        success: true,
        data: {
          notifications: items,
          unreadCount: unreadCountRes[0]?.count || 0,
        },
      };
    },
    {
      detail: { tags: ['Notifications'], summary: 'List user notifications and unread count' },
    }
  )

  // 2. Mark single notification as read
  .patch(
    '/:notificationId/read',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      await db
        .update(notifications)
        .set({ readAt: new Date() })
        .where(and(eq(notifications.id, params.notificationId), eq(notifications.userId, currentUser.id)));

      return {
        success: true,
        message: 'Notification marked as read',
      };
    },
    {
      params: t.Object({ notificationId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Notifications'], summary: 'Mark single notification as read' },
    }
  )

  // 3. Mark all as read
  .post(
    '/read-all',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      await db
        .update(notifications)
        .set({ readAt: new Date() })
        .where(and(eq(notifications.userId, currentUser.id), isNull(notifications.readAt)));

      return {
        success: true,
        message: 'All notifications marked as read',
      };
    },
    {
      detail: { tags: ['Notifications'], summary: 'Mark all user notifications as read' },
    }
  );

