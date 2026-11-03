import { Elysia, t } from 'elysia';
import { db } from '@/infrastructure/database/client';
import { folders, files } from '@/database/schema';
import { eq, and, isNull, sql } from 'drizzle-orm';
import { AppError } from '@/middleware/error-handler';
import { authPlugin } from '@/middleware/authentication';
import { PermissionEngine } from '@/services/permission-engine';
import { FileLifecycleService } from '@/services/file-lifecycle.service';

export const folderRoutes = new Elysia({ prefix: '/folders' })
  .use(authPlugin)
  // 1. Create folder
  .post(
    '/',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { name, parentId, color } = body;

      if (parentId) {
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, parentId);
        if (!perm.hasAccess || !PermissionEngine.can('editor', perm.permission)) {
          throw new AppError(403, 'FORBIDDEN', 'Editor permission required in parent folder to create subfolders');
        }
      }

      const [newFolder] = await db
        .insert(folders)
        .values({
          ownerId: currentUser.id,
          name: name.trim(),
          parentId: parentId || null,
          color: color || '#3b82f6',
        })
        .returning();

      return {
        success: true,
        message: 'Folder created successfully',
        data: { folder: newFolder },
      };
    },
    {
      body: t.Object({
        name: t.String({ minLength: 1, maxLength: 255 }),
        parentId: t.Optional(t.Nullable(t.String({ format: 'uuid' }))),
        color: t.Optional(t.String()),
      }),
      detail: { tags: ['Folders'], summary: 'Create a new folder' },
    }
  )

  // 2. List folders in parent (or root if parentId null)
  .get(
    '/',
    async ({ currentUser, query }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const parentId = query.parentId || null;

      let folderList;
      if (parentId) {
        // Check permission on parent folder
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, parentId);
        if (!perm.hasAccess) {
          throw new AppError(403, 'FORBIDDEN', 'Access denied to this folder');
        }

        folderList = await db.query.folders.findMany({
          where: and(eq(folders.parentId, parentId), isNull(folders.deletedAt)),
          orderBy: (f, { asc }) => [asc(f.name)],
        });
      } else {
        // My Drive root folders (owned by user and no parent)
        folderList = await db.query.folders.findMany({
          where: and(
            eq(folders.ownerId, currentUser.id),
            isNull(folders.parentId),
            isNull(folders.deletedAt)
          ),
          orderBy: (f, { asc }) => [asc(f.name)],
        });
      }

      return {
        success: true,
        data: { folders: folderList },
      };
    },
    {
      query: t.Object({
        parentId: t.Optional(t.String({ format: 'uuid' })),
      }),
      detail: { tags: ['Folders'], summary: 'List folders in a directory' },
    }
  )

  // 3. Get folder breadcrumbs
  .get(
    '/:folderId/breadcrumbs',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, params.folderId);
      if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied');

      const breadcrumbs: { id: string | null; name: string }[] = [];
      let currentId: string | null = params.folderId;
      const visited = new Set<string>();

      while (currentId) {
        if (visited.has(currentId)) break;
        visited.add(currentId);

        const current = await db.query.folders.findFirst({
          where: eq(folders.id, currentId),
        });

        if (!current) break;
        breadcrumbs.unshift({ id: current.id, name: current.name });
        currentId = current.parentId;
      }

      breadcrumbs.unshift({ id: null, name: 'My Drive' });

      return {
        success: true,
        data: { breadcrumbs },
      };
    },
    {
      params: t.Object({ folderId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Folders'], summary: 'Get folder breadcrumb path' },
    }
  )

  // 4. Rename folder
  .patch(
    '/:folderId/rename',
    async ({ currentUser, params, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, params.folderId);
      if (!perm.hasAccess || !PermissionEngine.can('editor', perm.permission)) {
        throw new AppError(403, 'FORBIDDEN', 'Editor permission required to rename folder');
      }

      const [updated] = await db
        .update(folders)
        .set({ name: body.name.trim(), updatedAt: new Date() })
        .where(eq(folders.id, params.folderId))
        .returning();

      return {
        success: true,
        message: 'Folder renamed successfully',
        data: { folder: updated },
      };
    },
    {
      params: t.Object({ folderId: t.String({ format: 'uuid' }) }),
      body: t.Object({ name: t.String({ minLength: 1, maxLength: 255 }) }),
      detail: { tags: ['Folders'], summary: 'Rename folder' },
    }
  )

  // 5. Move folder (Cycle protected)
  .patch(
    '/:folderId/move',
    async ({ currentUser, params, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const folderId = params.folderId;
      const targetParentId = body.targetParentId || null;

      if (folderId === targetParentId) {
        throw new AppError(400, 'INVALID_MOVE', 'Cannot move a folder into itself');
      }

      // Check editor permission on moved folder
      const sourcePerm = await PermissionEngine.resolveFolderPermission(currentUser.id, folderId);
      if (!sourcePerm.hasAccess || !PermissionEngine.can('editor', sourcePerm.permission)) {
        throw new AppError(403, 'FORBIDDEN', 'Editor permission required to move folder');
      }

      // Check editor permission on target parent
      if (targetParentId) {
        const targetPerm = await PermissionEngine.resolveFolderPermission(currentUser.id, targetParentId);
        if (!targetPerm.hasAccess || !PermissionEngine.can('editor', targetPerm.permission)) {
          throw new AppError(403, 'FORBIDDEN', 'Editor permission required in target destination folder');
        }

        // Cycle check: verify targetParentId is not a descendant of folderId
        let checkId: string | null = targetParentId;
        const visited = new Set<string>();
        while (checkId) {
          if (checkId === folderId) {
            throw new AppError(400, 'CYCLE_DETECTED', 'Cannot move a folder into one of its subfolders');
          }
          if (visited.has(checkId)) break;
          visited.add(checkId);
          const p = await db.query.folders.findFirst({ where: eq(folders.id, checkId) });
          checkId = p ? p.parentId : null;
        }
      }

      const [updated] = await db
        .update(folders)
        .set({ parentId: targetParentId, updatedAt: new Date() })
        .where(eq(folders.id, folderId))
        .returning();

      return {
        success: true,
        message: 'Folder moved successfully',
        data: { folder: updated },
      };
    },
    {
      params: t.Object({ folderId: t.String({ format: 'uuid' }) }),
      body: t.Object({ targetParentId: t.Nullable(t.String({ format: 'uuid' })) }),
      detail: { tags: ['Folders'], summary: 'Move folder to new parent' },
    }
  )

  // 6. Star / Unstar folder
  .patch(
    '/:folderId/star',
    async ({ currentUser, params, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, params.folderId);
      if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied');

      const [updated] = await db
        .update(folders)
        .set({ isStarred: body.isStarred, updatedAt: new Date() })
        .where(eq(folders.id, params.folderId))
        .returning();

      return {
        success: true,
        data: { folder: updated },
      };
    },
    {
      params: t.Object({ folderId: t.String({ format: 'uuid' }) }),
      body: t.Object({ isStarred: t.Boolean() }),
      detail: { tags: ['Folders'], summary: 'Star or unstar folder' },
    }
  )

  // 7. Soft delete folder
  .delete(
    '/:folderId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, params.folderId);
      if (!perm.hasAccess || !perm.isOwner) {
        throw new AppError(403, 'FORBIDDEN', 'Only the folder owner can delete this folder');
      }

      await FileLifecycleService.softDeleteFolder(params.folderId);

      return {
        success: true,
        message: 'Folder moved to trash',
      };
    },
    {
      params: t.Object({ folderId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Folders'], summary: 'Move folder to trash' },
    }
  );

