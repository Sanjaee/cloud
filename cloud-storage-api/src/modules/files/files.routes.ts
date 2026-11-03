import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { files, folders, users, storageObjects } from '../../database/schema';
import { eq, and, isNull, sql } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';
import { PermissionEngine } from '../../services/permission-engine';
import { FileLifecycleService } from '../../services/file-lifecycle.service';

export const fileRoutes = new Elysia({ prefix: '/files' })
  .use(authPlugin)
  // 1. List files in folder (or root)
  .get(
    '/',
    async ({ currentUser, query }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const folderId = query.folderId || null;

      let fileList;
      if (folderId) {
        // Check permission on parent folder
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, folderId);
        if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied to this folder');

        fileList = await db
          .select({
            id: files.id,
            ownerId: files.ownerId,
            folderId: files.folderId,
            storageObjectId: files.storageObjectId,
            name: files.name,
            originalName: files.originalName,
            mimeType: files.mimeType,
            size: files.size,
            isStarred: files.isStarred,
            createdAt: files.createdAt,
            updatedAt: files.updatedAt,
          })
          .from(files)
          .innerJoin(storageObjects, eq(files.storageObjectId, storageObjects.id))
          .where(
            and(
              eq(files.folderId, folderId),
              isNull(files.deletedAt),
              eq(storageObjects.status, 'ready')
            )
          )
          .orderBy(files.name);
      } else {
        // My Drive root files (owned by user and ready in storage)
        fileList = await db
          .select({
            id: files.id,
            ownerId: files.ownerId,
            folderId: files.folderId,
            storageObjectId: files.storageObjectId,
            name: files.name,
            originalName: files.originalName,
            mimeType: files.mimeType,
            size: files.size,
            isStarred: files.isStarred,
            createdAt: files.createdAt,
            updatedAt: files.updatedAt,
          })
          .from(files)
          .innerJoin(storageObjects, eq(files.storageObjectId, storageObjects.id))
          .where(
            and(
              eq(files.ownerId, currentUser.id),
              isNull(files.folderId),
              isNull(files.deletedAt),
              eq(storageObjects.status, 'ready')
            )
          )
          .orderBy(files.name);
      }

      return {
        success: true,
        data: {
          files: fileList.map((f) => ({
            id: f.id,
            ownerId: f.ownerId,
            folderId: f.folderId,
            storageObjectId: f.storageObjectId,
            name: f.name,
            originalName: f.originalName,
            mimeType: f.mimeType,
            size: f.size,
            isStarred: f.isStarred,
            isOwner: f.ownerId === currentUser.id,
            createdAt: f.createdAt,
            updatedAt: f.updatedAt,
          })),
        },
      };
    },
    {
      query: t.Object({ folderId: t.Optional(t.String({ format: 'uuid' })) }),
      detail: { tags: ['Files'], summary: 'List verified ready files in a folder or root' },
    }
  )

  // 2. Get file details
  .get(
    '/:fileId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied to this file');

      const file = await db.query.files.findFirst({
        where: and(eq(files.id, params.fileId), isNull(files.deletedAt)),
      });

      if (!file) throw new AppError(404, 'NOT_FOUND', 'File not found');

      const owner = await db.query.users.findFirst({
        where: eq(users.id, file.ownerId),
        columns: { id: true, email: true, username: true },
      });

      return {
        success: true,
        data: {
          file: {
            ...file,
            owner,
            permission: perm.permission,
            isOwner: perm.isOwner,
          },
        },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Files'], summary: 'Get file metadata and permissions' },
    }
  )

  // 3. Rename file
  .patch(
    '/:fileId/rename',
    async ({ currentUser, params, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess || !PermissionEngine.can('editor', perm.permission)) {
        throw new AppError(403, 'FORBIDDEN', 'Editor permission required to rename file');
      }

      const [updated] = await db
        .update(files)
        .set({ name: body.name.trim(), updatedAt: new Date() })
        .where(eq(files.id, params.fileId))
        .returning();

      return {
        success: true,
        message: 'File renamed successfully',
        data: { file: updated },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      body: t.Object({ name: t.String({ minLength: 1, maxLength: 255 }) }),
      detail: { tags: ['Files'], summary: 'Rename file' },
    }
  )

  // 4. Move file
  .patch(
    '/:fileId/move',
    async ({ currentUser, params, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess || !PermissionEngine.can('editor', perm.permission)) {
        throw new AppError(403, 'FORBIDDEN', 'Editor permission required to move file');
      }

      const targetFolderId = body.targetFolderId || null;
      if (targetFolderId) {
        const targetPerm = await PermissionEngine.resolveFolderPermission(currentUser.id, targetFolderId);
        if (!targetPerm.hasAccess || !PermissionEngine.can('editor', targetPerm.permission)) {
          throw new AppError(403, 'FORBIDDEN', 'Editor permission required in target destination folder');
        }
      }

      const [updated] = await db
        .update(files)
        .set({ folderId: targetFolderId, updatedAt: new Date() })
        .where(eq(files.id, params.fileId))
        .returning();

      return {
        success: true,
        message: 'File moved successfully',
        data: { file: updated },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      body: t.Object({ targetFolderId: t.Nullable(t.String({ format: 'uuid' })) }),
      detail: { tags: ['Files'], summary: 'Move file to a different folder' },
    }
  )

  // 5. Star / Unstar file
  .patch(
    '/:fileId/star',
    async ({ currentUser, params, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied');

      const [updated] = await db
        .update(files)
        .set({ isStarred: body.isStarred, updatedAt: new Date() })
        .where(eq(files.id, params.fileId))
        .returning();

      return {
        success: true,
        data: { file: updated },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      body: t.Object({ isStarred: t.Boolean() }),
      detail: { tags: ['Files'], summary: 'Star or unstar file' },
    }
  )

  // 6. Soft delete file (Move to Trash)
  .delete(
    '/:fileId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess || !perm.isOwner) {
        throw new AppError(403, 'FORBIDDEN', 'Only the file owner can delete this file');
      }

      await FileLifecycleService.softDeleteFile(params.fileId);

      return {
        success: true,
        message: 'File moved to trash',
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Files'], summary: 'Move file to trash' },
    }
  )

  // 7. Batch move files and folders to trash
  .post(
    '/batch-delete',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const fileIds: string[] = body.fileIds || [];
      const folderIds: string[] = body.folderIds || [];

      let deletedFilesCount = 0;
      let deletedFoldersCount = 0;

      for (const fileId of fileIds) {
        try {
          const perm = await PermissionEngine.resolveFilePermission(currentUser.id, fileId);
          if (perm.hasAccess && perm.isOwner) {
            await FileLifecycleService.softDeleteFile(fileId);
            deletedFilesCount++;
          }
        } catch {}
      }

      for (const folderId of folderIds) {
        try {
          const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, folderId);
          if (perm.hasAccess && perm.isOwner) {
            await FileLifecycleService.softDeleteFolder(folderId);
            deletedFoldersCount++;
          }
        } catch {}
      }

      return {
        success: true,
        message: `${deletedFilesCount} file(s) and ${deletedFoldersCount} folder(s) moved to trash`,
        data: { deletedFilesCount, deletedFoldersCount },
      };
    },
    {
      body: t.Object({
        fileIds: t.Optional(t.Array(t.String({ format: 'uuid' }))),
        folderIds: t.Optional(t.Array(t.String({ format: 'uuid' }))),
      }),
      detail: { tags: ['Files'], summary: 'Batch move files and folders to trash' },
    }
  );

