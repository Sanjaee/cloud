import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { files, storageObjects } from '../../database/schema';
import { eq } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';
import { PermissionEngine } from '../../services/permission-engine';
import { createPresignedDownloadUrl } from '../../infrastructure/storage/cloudflare-r2/download';

export const downloadRoutes = new Elysia({ prefix: '/downloads' })
  .use(authPlugin)
  // 1. Get Authorized Download URL
  .get(
    '/:fileId',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess) {
        throw new AppError(403, 'FORBIDDEN', 'Access denied: You do not have permission to download this file');
      }

      const file = await db.query.files.findFirst({
        where: eq(files.id, params.fileId),
      });

      if (!file) throw new AppError(404, 'NOT_FOUND', 'File not found');

      const storageObj = await db.query.storageObjects.findFirst({
        where: eq(storageObjects.id, file.storageObjectId),
      });

      if (!storageObj) throw new AppError(404, 'NOT_FOUND', 'Storage object not found');

      const { downloadUrl, expiresIn } = await createPresignedDownloadUrl(
        storageObj.storageKey,
        file.name,
        'attachment'
      );

      return {
        success: true,
        data: {
          downloadUrl,
          filename: file.name,
          mimeType: file.mimeType,
          size: file.size,
          expiresIn,
        },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Downloads'], summary: 'Get authorized presigned download URL for file' },
    }
  )

  // 2. Get Authorized Preview URL (Inline Disposition)
  .get(
    '/:fileId/preview',
    async ({ currentUser, params }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess) {
        throw new AppError(403, 'FORBIDDEN', 'Access denied: You do not have permission to preview this file');
      }

      const file = await db.query.files.findFirst({
        where: eq(files.id, params.fileId),
      });

      if (!file) throw new AppError(404, 'NOT_FOUND', 'File not found');

      const storageObj = await db.query.storageObjects.findFirst({
        where: eq(storageObjects.id, file.storageObjectId),
      });

      if (!storageObj) throw new AppError(404, 'NOT_FOUND', 'Storage object not found');

      const { downloadUrl, expiresIn } = await createPresignedDownloadUrl(
        storageObj.storageKey,
        file.name,
        'inline'
      );

      return {
        success: true,
        data: {
          previewUrl: downloadUrl,
          mimeType: file.mimeType,
          name: file.name,
          expiresIn,
        },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Downloads'], summary: 'Get authorized presigned preview URL for file' },
    }
  );

