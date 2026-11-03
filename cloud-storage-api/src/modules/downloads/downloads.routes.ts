import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { files, storageObjects } from '../../database/schema';
import { eq } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';
import { PermissionEngine } from '../../services/permission-engine';
import { createPresignedDownloadUrl } from '../../infrastructure/storage/cloudflare-r2/download';
import { verifyR2Object } from '../../infrastructure/storage/cloudflare-r2/verify';

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

      if (!storageObj || storageObj.status !== 'ready') {
        throw new AppError(404, 'NOT_FOUND', 'File data is not ready or upload was not completed');
      }

      // Verify physical object exists in R2
      const verification = await verifyR2Object(storageObj.storageKey);
      if (!verification.exists) {
        throw new AppError(404, 'NOT_FOUND', 'File object does not exist in R2 storage');
      }

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

      if (!storageObj || storageObj.status !== 'ready') {
        throw new AppError(404, 'NOT_FOUND', 'File data is not ready or upload was not completed');
      }

      // Verify physical object exists in R2
      const verification = await verifyR2Object(storageObj.storageKey);
      if (!verification.exists) {
        throw new AppError(404, 'NOT_FOUND', 'File object does not exist in R2 storage');
      }

      let previewMime = file.mimeType;
      if (file.name.toLowerCase().endsWith('.pdf')) {
        previewMime = 'application/pdf';
      }

      const { downloadUrl, expiresIn } = await createPresignedDownloadUrl(
        storageObj.storageKey,
        file.name,
        'inline',
        previewMime
      );

      return {
        success: true,
        data: {
          previewUrl: downloadUrl,
          mimeType: previewMime,
          name: file.name,
          expiresIn,
        },
      };
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Downloads'], summary: 'Get authorized presigned preview URL for file' },
    }
  )

  // 3. Direct Streaming/View redirect for thumbnail <img> and inline previews
  .get(
    '/:fileId/view',
    async ({ currentUser, params, redirect, set }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const perm = await PermissionEngine.resolveFilePermission(currentUser.id, params.fileId);
      if (!perm.hasAccess) throw new AppError(403, 'FORBIDDEN', 'Access denied to this file');

      const file = await db.query.files.findFirst({
        where: eq(files.id, params.fileId),
      });

      if (!file) throw new AppError(404, 'NOT_FOUND', 'File not found');

      const storageObj = await db.query.storageObjects.findFirst({
        where: eq(storageObjects.id, file.storageObjectId),
      });

      if (!storageObj || storageObj.status !== 'ready') {
        throw new AppError(404, 'NOT_FOUND', 'File data is not ready');
      }

      // Verify physical object exists in R2
      const verification = await verifyR2Object(storageObj.storageKey);
      if (!verification.exists) {
        throw new AppError(404, 'NOT_FOUND', 'File object does not exist in R2 storage');
      }

      let mimeType = file.mimeType;
      if (file.name.toLowerCase().endsWith('.pdf')) {
        mimeType = 'application/pdf';
      }

      const { downloadUrl } = await createPresignedDownloadUrl(
        storageObj.storageKey,
        file.name,
        'inline',
        mimeType
      );

      set.headers['Cache-Control'] = 'private, max-age=3600';
      return redirect(downloadUrl);
    },
    {
      params: t.Object({ fileId: t.String({ format: 'uuid' }) }),
      detail: { tags: ['Downloads'], summary: 'View/stream file directly via 302 redirect' },
    }
  );
