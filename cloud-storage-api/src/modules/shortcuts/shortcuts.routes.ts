import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { shortcuts, files, folders, users } from '../../database/schema';
import { eq, and, isNull } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';
import { PermissionEngine } from '../../services/permission-engine';

export const shortcutRoutes = new Elysia({ prefix: '/shortcuts' })
  .use(authPlugin)
  // 1. Add Shortcut to My Drive
  .post(
    '/',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { targetType, targetId, parentFolderId } = body;

      // Verify user has access to target resource
      if (targetType === 'file') {
        const perm = await PermissionEngine.resolveFilePermission(currentUser.id, targetId);
        if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied to target file');
      } else {
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, targetId);
        if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied to target folder');
      }

      // Check if shortcut already exists in same location
      const existing = await db.query.shortcuts.findFirst({
        where: and(
          eq(shortcuts.userId, currentUser.id),
          eq(shortcuts.targetType, targetType),
          eq(shortcuts.targetId, targetId),
          parentFolderId ? eq(shortcuts.parentFolderId, parentFolderId) : isNull(shortcuts.parentFolderId)
        ),
      });

      if (existing) {
        return {
          success: true,
          message: 'Shortcut already exists in this folder',
          data: { shortcut: existing },
        };
      }

      const [newShortcut] = await db
        .insert(shortcuts)
        .values({
          userId: currentUser.id,
          targetType,
          targetId,
          parentFolderId: parentFolderId || null,
        })
        .returning();

      return {
        success: true,
        message: 'Shortcut added to My Drive successfully',
        data: { shortcut: newShortcut },
      };
    },
    {
      body: t.Object({
        targetType: t.Union([t.Literal('file'), t.Literal('folder')]),
        targetId: t.String({ format: 'uuid' }),
        parentFolderId: t.Optional(t.Nullable(t.String({ format: 'uuid' }))),
      }),
      detail: { tags: ['Shortcuts'], summary: 'Add a shortcut to My Drive for shared file or folder' },
    }
  )

  // 2. List Shortcuts
  .get(
    '/',
    async ({ currentUser, query }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const parentFolderId = query.parentFolderId || null;

      const userShortcuts = await db.query.shortcuts.findMany({
        where: and(
          eq(shortcuts.userId, currentUser.id),
          parentFolderId ? eq(shortcuts.parentFolderId, parentFolderId) : isNull(shortcuts.parentFolderId)
        ),
      });

      // Hydrate targets
      const hydrated = await Promise.all(
        userShortcuts.map(async s => {
          if (s.targetType === 'file') {
            const f = await db.query.files.findFirst({
              where: eq(files.id, s.targetId),
            });
            return { ...s, target: f };
          } else {
            const fol = await db.query.folders.findFirst({
              where: eq(folders.id, s.targetId),
            });
            return { ...s, target: fol };
          }
        })
      );

      return {
        success: true,
        data: { shortcuts: hydrated },
      };
    },
    {
      query: t.Object({
        parentFolderId: t.Optional(t.String({ format: 'uuid' })),
      }),
      detail: { tags: ['Shortcuts'], summary: 'List shortcuts in folder' },
    }
  )

  // 3. Remove Shortcut
  .delete(
    '/:shortcutId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const deleted = await db
        .delete(shortcuts)
        .where(and(eq(shortcuts.id, params.shortcutId), eq(shortcuts.userId, currentUser.id)))
        .returning();

      if (!deleted.length) {
        throw new AppError(404, 'NOT_FOUND', 'Shortcut not found');
      }

      return {
        success: true,
        message: 'Shortcut removed successfully (original resource is preserved)',
      };
    },
    {
      params: t.Object({ shortcutId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Shortcuts'], summary: 'Delete a shortcut' },
    }
  );

