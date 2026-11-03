import { Elysia, t } from 'elysia';
import { db } from '@/infrastructure/database/client';
import { files, folders } from '@/database/schema';
import { eq, and, isNotNull } from 'drizzle-orm';
import { AppError } from '@/middleware/error-handler';
import { authPlugin } from '@/middleware/authentication';
import { FileLifecycleService } from '@/services/file-lifecycle.service';

export const trashRoutes = new Elysia({ prefix: '/trash' })
  .use(authPlugin)
  // 1. List Trash Items
  .get(
    '/',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const trashedFiles = await db.query.files.findMany({
        where: and(eq(files.ownerId, currentUser.id), isNotNull(files.deletedAt)),
        orderBy: (f, { desc }) => [desc(f.deletedAt)],
      });

      const trashedFolders = await db.query.folders.findMany({
        where: and(eq(folders.ownerId, currentUser.id), isNotNull(folders.deletedAt)),
        orderBy: (f, { desc }) => [desc(f.deletedAt)],
      });

      return {
        success: true,
        data: {
          files: trashedFiles,
          folders: trashedFolders,
        },
      };
    },
    {
      detail: { tags: ['Trash'], summary: 'List all items currently in trash' },
    }
  )

  // 2. Restore item from trash
  .post(
    '/restore/:resourceType/:resourceId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { resourceType, resourceId } = params;

      if (resourceType === 'file') {
        const file = await db.query.files.findFirst({ where: eq(files.id, resourceId) });
        if (!file || file.ownerId !== currentUser.id) throw new AppError(403, 'FORBIDDEN', 'Access denied');
        await FileLifecycleService.restoreFile(resourceId);
      } else {
        const folder = await db.query.folders.findFirst({ where: eq(folders.id, resourceId) });
        if (!folder || folder.ownerId !== currentUser.id) throw new AppError(403, 'FORBIDDEN', 'Access denied');
        await FileLifecycleService.restoreFolder(resourceId);
      }

      return {
        success: true,
        message: `${resourceType === 'file' ? 'File' : 'Folder'} restored successfully`,
      };
    },
    {
      params: t.Object({
        resourceType: t.Union([t.Literal('file'), t.Literal('folder')]),
        resourceId: t.String({ format: 'uuid' }),
      }),
      detail: { tags: ['Trash'], summary: 'Restore item from trash' },
    }
  )

  // 3. Permanently delete item
  .delete(
    '/permanent/:resourceType/:resourceId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { resourceType, resourceId } = params;

      if (resourceType === 'file') {
        const file = await db.query.files.findFirst({ where: eq(files.id, resourceId) });
        if (!file || file.ownerId !== currentUser.id) throw new AppError(403, 'FORBIDDEN', 'Access denied');
        const res = await FileLifecycleService.permanentlyDeleteFile(resourceId);
        return {
          success: true,
          message: 'File permanently deleted',
          data: { r2ObjectDeleted: res.r2ObjectDeleted },
        };
      } else {
        const folder = await db.query.folders.findFirst({ where: eq(folders.id, resourceId) });
        if (!folder || folder.ownerId !== currentUser.id) throw new AppError(403, 'FORBIDDEN', 'Access denied');
        await FileLifecycleService.permanentlyDeleteFolder(resourceId);
        return {
          success: true,
          message: 'Folder and contents permanently deleted',
        };
      }
    },
    {
      params: t.Object({
        resourceType: t.Union([t.Literal('file'), t.Literal('folder')]),
        resourceId: t.String({ format: 'uuid' }),
      }),
      detail: { tags: ['Trash'], summary: 'Permanently delete item and purge R2 object' },
    }
  )

  // 4. Empty Trash (Permanently purge all trashed items)
  .delete(
    '/empty',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const trashedFiles = await db.query.files.findMany({
        where: and(eq(files.ownerId, currentUser.id), isNotNull(files.deletedAt)),
      });

      for (const f of trashedFiles) {
        await FileLifecycleService.permanentlyDeleteFile(f.id);
      }

      const trashedFolders = await db.query.folders.findMany({
        where: and(eq(folders.ownerId, currentUser.id), isNotNull(folders.deletedAt)),
      });

      for (const fol of trashedFolders) {
        await FileLifecycleService.permanentlyDeleteFolder(fol.id);
      }

      return {
        success: true,
        message: 'Trash emptied successfully',
      };
    },
    {
      detail: { tags: ['Trash'], summary: 'Empty trash permanently' },
    }
  );

