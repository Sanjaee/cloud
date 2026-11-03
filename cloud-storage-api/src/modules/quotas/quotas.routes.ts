import { Elysia } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { users, files, storageObjects, filePermissions } from '../../database/schema';
import { eq, and, sql, isNull } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';
import { QuotaEngine } from '../../services/quota-engine';

export const quotaRoutes = new Elysia({ prefix: '/quotas' })
  .use(authPlugin)
  .get(
    '/',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const [user, fileCountRes, objCountRes, sharedCountRes] = await Promise.all([
        db.query.users.findFirst({
          where: eq(users.id, currentUser.id),
        }),
        db
          .select({ count: sql<number>`count(*)::int` })
          .from(files)
          .where(and(eq(files.ownerId, currentUser.id), isNull(files.deletedAt))),
        db
          .select({ count: sql<number>`count(*)::int` })
          .from(storageObjects)
          .where(and(eq(storageObjects.ownerId, currentUser.id), eq(storageObjects.status, 'ready'))),
        db
          .select({ count: sql<number>`count(*)::int` })
          .from(filePermissions)
          .where(eq(filePermissions.grantedTo, currentUser.id)),
      ]);

      if (!user) throw new AppError(404, 'NOT_FOUND', 'User not found');

      const fileCount = fileCountRes[0]?.count || 0;
      const physicalObjectCount = objCountRes[0]?.count || 0;
      const sharedFileCount = sharedCountRes[0]?.count || 0;

      const quotaBytes = user.storageQuota;
      const usedBytes = user.storageUsed;
      const availableBytes = Math.max(0, quotaBytes - usedBytes);
      const percentage = quotaBytes > 0 ? Math.min(100, Math.round((usedBytes / quotaBytes) * 100)) : 0;

      const formatBytes = (bytes: number) => {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
      };

      return {
        success: true,
        data: {
          quotaBytes,
          usedBytes,
          availableBytes,
          percentage,
          fileCount,
          physicalObjectCount,
          sharedFileCount,
          formattedUsed: formatBytes(usedBytes),
          formattedQuota: formatBytes(quotaBytes),
          formattedAvailable: formatBytes(availableBytes),
        },
      };
    },
    {
      detail: { tags: ['Quotas'], summary: 'Get current user storage quota and object statistics' },
    }
  );

