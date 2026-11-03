import { Elysia, t } from 'elysia';
import { db } from '@/infrastructure/database/client';
import {
  files,
  folders,
  users,
  filePermissions,
  folderPermissions,
  notifications,
} from '@/database/schema';
import { eq, and, or, isNull, inArray, sql } from 'drizzle-orm';
import { AppError } from '@/middleware/error-handler';
import { authPlugin } from '@/middleware/authentication';
import { PermissionEngine } from '@/services/permission-engine';

export const sharingRoutes = new Elysia({ prefix: '/shares' })
  .use(authPlugin)
  // 1. Share resource (file or folder) with another user
  .post(
    '/',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { resourceType, resourceId, recipientEmailOrUsername, permission, expiresAt, message } = body;

      // 1. Verify owner or editor permission on resource
      if (resourceType === 'file') {
        const perm = await PermissionEngine.resolveFilePermission(currentUser.id, resourceId);
        if (!perm.hasAccess || !perm.isOwner) {
          throw new AppError(403, 'FORBIDDEN', 'Only the resource owner can manage sharing permissions');
        }
      } else {
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, resourceId);
        if (!perm.hasAccess || !perm.isOwner) {
          throw new AppError(403, 'FORBIDDEN', 'Only the resource owner can manage sharing permissions');
        }
      }

      // 2. Find recipient user
      const recipient = await db.query.users.findFirst({
        where: or(
          eq(users.email, recipientEmailOrUsername.toLowerCase().trim()),
          eq(users.username, recipientEmailOrUsername.trim())
        ),
      });

      if (!recipient) {
        throw new AppError(404, 'RECIPIENT_NOT_FOUND', 'Target user could not be found');
      }

      if (recipient.id === currentUser.id) {
        throw new AppError(400, 'CANNOT_SHARE_WITH_SELF', 'You already own this resource');
      }

      const expirationDate = expiresAt ? new Date(expiresAt) : null;

      // 3. Upsert permission record
      if (resourceType === 'file') {
        const existing = await db.query.filePermissions.findFirst({
          where: and(eq(filePermissions.fileId, resourceId), eq(filePermissions.grantedTo, recipient.id)),
        });

        if (existing) {
          await db
            .update(filePermissions)
            .set({ permission, expiresAt: expirationDate })
            .where(eq(filePermissions.id, existing.id));
        } else {
          await db.insert(filePermissions).values({
            fileId: resourceId,
            grantedBy: currentUser.id,
            grantedTo: recipient.id,
            permission,
            expiresAt: expirationDate,
          });
        }
      } else {
        const existing = await db.query.folderPermissions.findFirst({
          where: and(eq(folderPermissions.folderId, resourceId), eq(folderPermissions.grantedTo, recipient.id)),
        });

        if (existing) {
          await db
            .update(folderPermissions)
            .set({ permission, expiresAt: expirationDate })
            .where(eq(folderPermissions.id, existing.id));
        } else {
          await db.insert(folderPermissions).values({
            folderId: resourceId,
            grantedBy: currentUser.id,
            grantedTo: recipient.id,
            permission,
            expiresAt: expirationDate,
          });
        }
      }

      // 4. Notify recipient
      await db.insert(notifications).values({
        userId: recipient.id,
        actorId: currentUser.id,
        type: 'share_received',
        resourceType,
        resourceId,
        message: `${currentUser.username} shared a ${resourceType} with you (${permission})${message ? `: "${message}"` : ''}`,
      });

      return {
        success: true,
        message: `Resource shared with ${recipient.username} as ${permission}`,
        data: {
          recipient: {
            id: recipient.id,
            email: recipient.email,
            username: recipient.username,
            permission,
          },
        },
      };
    },
    {
      body: t.Object({
        resourceType: t.Union([t.Literal('file'), t.Literal('folder')]),
        resourceId: t.String({ format: 'uuid' }),
        recipientEmailOrUsername: t.String({ minLength: 1 }),
        permission: t.Union([t.Literal('viewer'), t.Literal('commenter'), t.Literal('editor')]),
        expiresAt: t.Optional(t.Nullable(t.String())),
        message: t.Optional(t.String({ maxLength: 500 })),
      }),
      detail: { tags: ['Sharing'], summary: 'Share an existing file or folder with another user' },
    }
  )

  // 2. List Collaborators for a resource
  .get(
    '/:resourceType/:resourceId/collaborators',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { resourceType, resourceId } = params;

      if (resourceType === 'file') {
        const perms = await db
          .select({
            permissionId: filePermissions.id,
            permission: filePermissions.permission,
            expiresAt: filePermissions.expiresAt,
            createdAt: filePermissions.createdAt,
            userId: users.id,
            username: users.username,
            email: users.email,
          })
          .from(filePermissions)
          .innerJoin(users, eq(filePermissions.grantedTo, users.id))
          .where(eq(filePermissions.fileId, resourceId));

        return { success: true, data: { collaborators: perms } };
      } else {
        const perms = await db
          .select({
            permissionId: folderPermissions.id,
            permission: folderPermissions.permission,
            expiresAt: folderPermissions.expiresAt,
            createdAt: folderPermissions.createdAt,
            userId: users.id,
            username: users.username,
            email: users.email,
          })
          .from(folderPermissions)
          .innerJoin(users, eq(folderPermissions.grantedTo, users.id))
          .where(eq(folderPermissions.folderId, resourceId));

        return { success: true, data: { collaborators: perms } };
      }
    },
    {
      params: t.Object({
        resourceType: t.Union([t.Literal('file'), t.Literal('folder')]),
        resourceId: t.String({ format: 'uuid' }),
      }),
      detail: { tags: ['Sharing'], summary: 'List collaborators with permissions on resource' },
    }
  )

  // 3. Revoke Sharing Access
  .delete(
    '/:resourceType/:resourceId/collaborators/:userId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { resourceType, resourceId, userId } = params;

      if (resourceType === 'file') {
        const file = await db.query.files.findFirst({ where: eq(files.id, resourceId) });
        if (!file || file.ownerId !== currentUser.id) {
          throw new AppError(403, 'FORBIDDEN', 'Only the resource owner can revoke access');
        }

        await db
          .delete(filePermissions)
          .where(and(eq(filePermissions.fileId, resourceId), eq(filePermissions.grantedTo, userId)));
      } else {
        const folder = await db.query.folders.findFirst({ where: eq(folders.id, resourceId) });
        if (!folder || folder.ownerId !== currentUser.id) {
          throw new AppError(403, 'FORBIDDEN', 'Only the resource owner can revoke access');
        }

        await db
          .delete(folderPermissions)
          .where(and(eq(folderPermissions.folderId, resourceId), eq(folderPermissions.grantedTo, userId)));
      }

      return {
        success: true,
        message: 'Access revoked successfully',
      };
    },
    {
      params: t.Object({
        resourceType: t.Union([t.Literal('file'), t.Literal('folder')]),
        resourceId: t.String({ format: 'uuid' }),
        userId: t.String({ format: 'uuid' }),
      }),
      detail: { tags: ['Sharing'], summary: 'Revoke collaborator access' },
    }
  )

  // 4. "Shared With Me" (Files and Folders shared with current user)
  .get(
    '/shared-with-me',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      // Shared files
      const sharedFiles = await db
        .select({
          id: files.id,
          name: files.name,
          originalName: files.originalName,
          mimeType: files.mimeType,
          size: files.size,
          storageObjectId: files.storageObjectId,
          permission: filePermissions.permission,
          sharedAt: filePermissions.createdAt,
          ownerId: users.id,
          ownerUsername: users.username,
          ownerEmail: users.email,
          createdAt: files.createdAt,
          updatedAt: files.updatedAt,
        })
        .from(filePermissions)
        .innerJoin(files, eq(filePermissions.fileId, files.id))
        .innerJoin(users, eq(files.ownerId, users.id))
        .where(
          and(
            eq(filePermissions.grantedTo, currentUser.id),
            isNull(files.deletedAt)
          )
        );

      // Shared folders
      const sharedFolders = await db
        .select({
          id: folders.id,
          name: folders.name,
          color: folders.color,
          permission: folderPermissions.permission,
          sharedAt: folderPermissions.createdAt,
          ownerId: users.id,
          ownerUsername: users.username,
          ownerEmail: users.email,
          createdAt: folders.createdAt,
          updatedAt: folders.updatedAt,
        })
        .from(folderPermissions)
        .innerJoin(folders, eq(folderPermissions.folderId, folders.id))
        .innerJoin(users, eq(folders.ownerId, users.id))
        .where(
          and(
            eq(folderPermissions.grantedTo, currentUser.id),
            isNull(folders.deletedAt)
          )
        );

      return {
        success: true,
        data: {
          files: sharedFiles,
          folders: sharedFolders,
        },
      };
    },
    {
      detail: { tags: ['Sharing'], summary: 'Get all files and folders shared with me' },
    }
  )

  // 5. "Shared By Me" (Files and Folders current user has shared)
  .get(
    '/shared-by-me',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      // Files current user has shared
      const sharedFiles = await db
        .select({
          fileId: files.id,
          fileName: files.name,
          size: files.size,
          mimeType: files.mimeType,
          permission: filePermissions.permission,
          grantedToUsername: users.username,
          grantedToEmail: users.email,
          grantedAt: filePermissions.createdAt,
        })
        .from(filePermissions)
        .innerJoin(files, eq(filePermissions.fileId, files.id))
        .innerJoin(users, eq(filePermissions.grantedTo, users.id))
        .where(eq(filePermissions.grantedBy, currentUser.id));

      // Folders current user has shared
      const sharedFolders = await db
        .select({
          folderId: folders.id,
          folderName: folders.name,
          permission: folderPermissions.permission,
          grantedToUsername: users.username,
          grantedToEmail: users.email,
          grantedAt: folderPermissions.createdAt,
        })
        .from(folderPermissions)
        .innerJoin(folders, eq(folderPermissions.folderId, folders.id))
        .innerJoin(users, eq(folderPermissions.grantedTo, users.id))
        .where(eq(folderPermissions.grantedBy, currentUser.id));

      return {
        success: true,
        data: {
          sharedFiles,
          sharedFolders,
        },
      };
    },
    {
      detail: { tags: ['Sharing'], summary: 'Get all resources current user has shared with others' },
    }
  );

