import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { shareLinks, files, folders, storageObjects } from '../../database/schema';
import { eq, and, isNull, gt } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin, hashToken } from '../../middleware/authentication';
import { PermissionEngine } from '../../services/permission-engine';
import { createPresignedDownloadUrl } from '../../infrastructure/storage/cloudflare-r2/download';

export const publicLinkRoutes = new Elysia({ prefix: '/public-links' })
  .use(authPlugin)
  // 1. Create Public Share Link
  .post(
    '/',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { resourceType, resourceId, permission, password, allowDownload, expiresAt } = body;

      // Verify owner
      if (resourceType === 'file') {
        const perm = await PermissionEngine.resolveFilePermission(currentUser.id, resourceId);
        if (!perm.hasAccess || !perm.isOwner) {
          throw new AppError(403, 'FORBIDDEN', 'Only the owner can create public links');
        }
      } else {
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, resourceId);
        if (!perm.hasAccess || !perm.isOwner) {
          throw new AppError(403, 'FORBIDDEN', 'Only the owner can create public links');
        }
      }

      const rawToken = crypto.randomUUID().replace(/-/g, '') + crypto.randomUUID().replace(/-/g, '');
      const tokenHash = await hashToken(rawToken);

      let passwordHash: string | null = null;
      if (password) {
        passwordHash = await Bun.password.hash(password, { algorithm: 'argon2id', memoryCost: 19456, timeCost: 2 });
      }

      const [link] = await db
        .insert(shareLinks)
        .values({
          resourceType,
          resourceId,
          tokenHash,
          permission: permission || 'viewer',
          passwordHash,
          allowDownload: allowDownload ?? true,
          expiresAt: expiresAt ? new Date(expiresAt) : null,
        })
        .returning();

      return {
        success: true,
        data: {
          id: link.id,
          token: rawToken,
          permission: link.permission,
          hasPassword: Boolean(password),
          allowDownload: link.allowDownload,
          expiresAt: link.expiresAt,
          createdAt: link.createdAt,
        },
      };
    },
    {
      body: t.Object({
        resourceType: t.Union([t.Literal('file'), t.Literal('folder')]),
        resourceId: t.String({ format: 'uuid' }),
        permission: t.Optional(t.Union([t.Literal('viewer'), t.Literal('commenter')])),
        password: t.Optional(t.String()),
        allowDownload: t.Optional(t.Boolean()),
        expiresAt: t.Optional(t.Nullable(t.String())),
      }),
      detail: { tags: ['Public Links'], summary: 'Create a secure public sharing link' },
    }
  )

  // 2. Access Public Link (Public endpoint, no auth required)
  .post(
    '/access/:token',
    async ({ params, body }) => {
      const { token } = params;
      const { password } = body || {};
      const tokenHash = await hashToken(token);

      const link = await db.query.shareLinks.findFirst({
        where: and(
          eq(shareLinks.tokenHash, tokenHash),
          isNull(shareLinks.revokedAt)
        ),
      });

      if (!link) {
        throw new AppError(404, 'LINK_NOT_FOUND', 'This share link does not exist or has been revoked');
      }

      if (link.expiresAt && link.expiresAt < new Date()) {
        throw new AppError(410, 'LINK_EXPIRED', 'This share link has expired');
      }

      // If password protected, verify password
      if (link.passwordHash) {
        if (!password) {
          return {
            success: true,
            passwordRequired: true,
            message: 'Password required to access this resource',
          };
        }

        const isMatch = await Bun.password.verify(password, link.passwordHash);
        if (!isMatch) {
          throw new AppError(401, 'INVALID_PASSWORD', 'Incorrect password for this share link');
        }
      }

      // Fetch resource
      if (link.resourceType === 'file') {
        const file = await db.query.files.findFirst({
          where: and(eq(files.id, link.resourceId), isNull(files.deletedAt)),
        });

        if (!file) throw new AppError(404, 'NOT_FOUND', 'The shared file is no longer available');

        let downloadUrl: string | null = null;
        if (link.allowDownload) {
          const storageObj = await db.query.storageObjects.findFirst({
            where: eq(storageObjects.id, file.storageObjectId),
          });
          if (storageObj) {
            const presigned = await createPresignedDownloadUrl(storageObj.storageKey, file.name, 'attachment');
            downloadUrl = presigned.downloadUrl;
          }
        }

        return {
          success: true,
          data: {
            resourceType: 'file',
            file: {
              id: file.id,
              name: file.name,
              mimeType: file.mimeType,
              size: file.size,
              createdAt: file.createdAt,
            },
            permission: link.permission,
            allowDownload: link.allowDownload,
            downloadUrl,
          },
        };
      } else {
        const folder = await db.query.folders.findFirst({
          where: and(eq(folders.id, link.resourceId), isNull(folders.deletedAt)),
        });

        if (!folder) throw new AppError(404, 'NOT_FOUND', 'The shared folder is no longer available');

        const folderFiles = await db.query.files.findMany({
          where: and(eq(files.folderId, folder.id), isNull(files.deletedAt)),
        });

        return {
          success: true,
          data: {
            resourceType: 'folder',
            folder: {
              id: folder.id,
              name: folder.name,
              color: folder.color,
            },
            files: folderFiles,
            permission: link.permission,
            allowDownload: link.allowDownload,
          },
        };
      }
    },
    {
      params: t.Object({ token: t.String() }),
      body: t.Optional(t.Object({ password: t.Optional(t.String()) })),
      detail: { tags: ['Public Links'], summary: 'Access shared resource via public token' },
    }
  )

  // 3. Revoke Public Link
  .delete(
    '/:linkId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const link = await db.query.shareLinks.findFirst({
        where: eq(shareLinks.id, params.linkId),
      });

      if (!link) throw new AppError(404, 'NOT_FOUND', 'Share link not found');

      // Verify owner
      if (link.resourceType === 'file') {
        const file = await db.query.files.findFirst({ where: eq(files.id, link.resourceId) });
        if (!file || file.ownerId !== currentUser.id) throw new AppError(403, 'FORBIDDEN', 'Access denied');
      } else {
        const folder = await db.query.folders.findFirst({ where: eq(folders.id, link.resourceId) });
        if (!folder || folder.ownerId !== currentUser.id) throw new AppError(403, 'FORBIDDEN', 'Access denied');
      }

      await db
        .update(shareLinks)
        .set({ revokedAt: new Date() })
        .where(eq(shareLinks.id, params.linkId));

      return {
        success: true,
        message: 'Share link revoked successfully',
      };
    },
    {
      params: t.Object({ linkId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Public Links'], summary: 'Revoke public share link' },
    }
  );

