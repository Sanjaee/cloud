import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { files, storageObjects, uploadSessions, multipartParts } from '../../database/schema';
import { eq, and } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin } from '../../middleware/authentication';
import { QuotaEngine } from '../../services/quota-engine';
import { PermissionEngine } from '../../services/permission-engine';
import { createPresignedUploadUrl } from '../../infrastructure/storage/cloudflare-r2/upload';
import { verifyR2Object } from '../../infrastructure/storage/cloudflare-r2/verify';
import { PutObjectCommand } from '@aws-sdk/client-s3';
import { r2Client, BUCKET_NAME } from '../../infrastructure/storage/cloudflare-r2/client';
import { configureR2Cors } from '../../infrastructure/storage/cloudflare-r2/cors';
import {
  initiateMultipartUpload,
  getPresignedPartUrl,
  completeMultipartUpload,
  abortMultipartUpload,
} from '../../infrastructure/storage/cloudflare-r2/multipart';
import { APP_CONSTANTS } from '../../config/constants';

export const uploadRoutes = new Elysia({ prefix: '/uploads' })
  .use(authPlugin)
  // 1. Initiate upload session
  .post(
    '/initiate',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { filename, mimeType, size, checksum, idempotencyKey } = body;
      let folderId = body.folderId;
      if (!folderId || folderId === 'null' || folderId === 'undefined' || folderId === '') {
        folderId = null;
      }

      // Check max file size
      if (size > APP_CONSTANTS.MAX_UPLOAD_SIZE_BYTES) {
        throw new AppError(400, 'FILE_TOO_LARGE', `Maximum file size is ${APP_CONSTANTS.MAX_UPLOAD_SIZE_BYTES / (1024 * 1024 * 1024)}GB`);
      }

      // Check quota
      const quota = await QuotaEngine.checkQuota(currentUser.id, size);
      if (!quota.allowed) {
        throw new AppError(
          400,
          'QUOTA_EXCEEDED',
          `Insufficient storage quota. Required: ${size} bytes, Available: ${quota.availableBytes} bytes`
        );
      }

      // If uploading into a folder, verify editor permission
      if (folderId) {
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, folderId);
        if (!perm.hasAccess || !PermissionEngine.can('editor', perm.permission)) {
          throw new AppError(403, 'FORBIDDEN', 'Editor permission required in target folder');
        }
      }

      // Check idempotency if key provided
      if (idempotencyKey) {
        const existingSession = await db.query.uploadSessions.findFirst({
          where: eq(uploadSessions.idempotencyKey, idempotencyKey),
        });
        if (existingSession && existingSession.status === 'completed') {
          const file = await db.query.files.findFirst({
            where: eq(files.storageObjectId, existingSession.storageObjectId),
          });
          return { success: true, message: 'Upload already completed', data: { file } };
        }
      }

      // Generate immutable storage key: objects/{uuid}
      const objectUuid = crypto.randomUUID();
      const storageKey = `objects/${objectUuid}`;

      // Insert storageObject (pending status)
      const [storageObj] = await db
        .insert(storageObjects)
        .values({
          ownerId: currentUser.id,
          storageKey,
          mimeType,
          size,
          checksum,
          status: 'pending',
        })
        .returning();

      // Insert file representation
      const [fileRecord] = await db
        .insert(files)
        .values({
          ownerId: currentUser.id,
          folderId: folderId || null,
          storageObjectId: storageObj.id,
          name: filename,
          originalName: filename,
          mimeType,
          size,
        })
        .returning();

      const sessionExpiresAt = new Date(Date.now() + APP_CONSTANTS.PRESIGNED_UPLOAD_EXPIRATION_SEC * 1000);
      const isMultipart = size > APP_CONSTANTS.MULTIPART_THRESHOLD_BYTES;

      if (isMultipart) {
        const { uploadId } = await initiateMultipartUpload(storageKey, mimeType);
        const totalParts = Math.ceil(size / APP_CONSTANTS.MULTIPART_CHUNK_SIZE_BYTES);

        const [session] = await db
          .insert(uploadSessions)
          .values({
            userId: currentUser.id,
            storageObjectId: storageObj.id,
            idempotencyKey,
            status: 'uploading',
            uploadType: 'multipart',
            uploadId,
            totalParts,
            expiresAt: sessionExpiresAt,
          })
          .returning();

        return {
          success: true,
          data: {
            uploadSessionId: session.id,
            fileId: fileRecord.id,
            storageObjectId: storageObj.id,
            uploadType: 'multipart',
            uploadId,
            totalParts,
            chunkSize: APP_CONSTANTS.MULTIPART_CHUNK_SIZE_BYTES,
          },
        };
      } else {
        // Direct single PUT upload URL
        const { uploadUrl, expiresIn } = await createPresignedUploadUrl(
          storageKey,
          mimeType,
          size,
          checksum
        );

        const [session] = await db
          .insert(uploadSessions)
          .values({
            userId: currentUser.id,
            storageObjectId: storageObj.id,
            idempotencyKey,
            status: 'initiated',
            uploadType: 'single',
            totalParts: 1,
            expiresAt: sessionExpiresAt,
          })
          .returning();

        return {
          success: true,
          data: {
            uploadSessionId: session.id,
            fileId: fileRecord.id,
            storageObjectId: storageObj.id,
            uploadType: 'single',
            uploadUrl,
            expiresIn,
          },
        };
      }
    },
    {
      body: t.Object({
        filename: t.String({ minLength: 1, maxLength: 255 }),
        mimeType: t.String(),
        size: t.Number({ minimum: 1 }),
        folderId: t.Optional(t.Nullable(t.String({ format: 'uuid' }))),
        checksum: t.Optional(t.String()),
        idempotencyKey: t.Optional(t.String()),
      }),
      detail: { tags: ['Uploads'], summary: 'Initiate direct R2 upload session' },
    }
  )

  // 2. Get Presigned URL for a multipart chunk
  .post(
    '/multipart/part-url',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { uploadSessionId, partNumber } = body;

      const session = await db.query.uploadSessions.findFirst({
        where: and(eq(uploadSessions.id, uploadSessionId), eq(uploadSessions.userId, currentUser.id)),
      });

      if (!session || !session.uploadId) {
        throw new AppError(404, 'NOT_FOUND', 'Multipart upload session not found');
      }

      const storageObj = await db.query.storageObjects.findFirst({
        where: eq(storageObjects.id, session.storageObjectId),
      });

      if (!storageObj) throw new AppError(404, 'NOT_FOUND', 'Storage object not found');

      const { uploadUrl } = await getPresignedPartUrl(storageObj.storageKey, session.uploadId, partNumber);

      return {
        success: true,
        data: { uploadUrl, partNumber },
      };
    },
    {
      body: t.Object({
        uploadSessionId: t.String({ format: 'uuid' }),
        partNumber: t.Number({ minimum: 1 }),
      }),
      detail: { tags: ['Uploads'], summary: 'Get presigned URL for multipart part' },
    }
  )

  // 3. Complete and finalize upload
  .post(
    '/complete',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { uploadSessionId, parts } = body;

      const session = await db.query.uploadSessions.findFirst({
        where: and(eq(uploadSessions.id, uploadSessionId), eq(uploadSessions.userId, currentUser.id)),
      });

      if (!session) throw new AppError(404, 'NOT_FOUND', 'Upload session not found');

      // Idempotency: already completed
      if (session.status === 'completed') {
        const file = await db.query.files.findFirst({
          where: eq(files.storageObjectId, session.storageObjectId),
        });
        return { success: true, message: 'Upload already completed', data: { file } };
      }

      const storageObj = await db.query.storageObjects.findFirst({
        where: eq(storageObjects.id, session.storageObjectId),
      });

      if (!storageObj) throw new AppError(404, 'NOT_FOUND', 'Storage object not found');

      // If multipart, finalize with S3
      if (session.uploadType === 'multipart' && session.uploadId) {
        if (!parts || !parts.length) {
          throw new AppError(400, 'PARTS_REQUIRED', 'Multipart parts are required to complete upload');
        }
        await completeMultipartUpload(storageObj.storageKey, session.uploadId, parts);
      }

      // Verify physical object in Cloudflare R2
      const verification = await verifyR2Object(storageObj.storageKey);
      if (!verification.exists) {
        throw new AppError(400, 'OBJECT_NOT_FOUND_IN_R2', 'Upload verification failed: file is missing from R2 storage');
      }

      // Mark storageObject ready
      await db
        .update(storageObjects)
        .set({ status: 'ready' })
        .where(eq(storageObjects.id, storageObj.id));

      // Mark upload session completed
      await db
        .update(uploadSessions)
        .set({ status: 'completed', completedAt: new Date() })
        .where(eq(uploadSessions.id, session.id));

      // Atomically increment owner's storageUsed
      await QuotaEngine.incrementUsage(currentUser.id, storageObj.size);

      const file = await db.query.files.findFirst({
        where: eq(files.storageObjectId, storageObj.id),
      });

      return {
        success: true,
        message: 'Upload completed and verified successfully',
        data: { file },
      };
    },
    {
      body: t.Object({
        uploadSessionId: t.String({ format: 'uuid' }),
        parts: t.Optional(
          t.Array(
            t.Object({
              PartNumber: t.Number(),
              ETag: t.String(),
            })
          )
        ),
      }),
      detail: { tags: ['Uploads'], summary: 'Complete and verify uploaded object' },
    }
  )

  // 4. Direct Upload via Server (Ultra-reliable & for files <= 4MB)
  .post(
    '/direct',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const file = body.file as File;
      let folderId = body.folderId;
      if (!folderId || folderId === 'null' || folderId === 'undefined' || folderId === '') {
        folderId = null;
      }

      if (!file) throw new AppError(400, 'FILE_REQUIRED', 'No file was provided in the upload request');

      const size = file.size;
      const mimeType = file.type || 'application/octet-stream';
      const filename = file.name;

      // Check max size
      if (size > APP_CONSTANTS.MAX_UPLOAD_SIZE_BYTES) {
        throw new AppError(400, 'FILE_TOO_LARGE', 'File exceeds maximum upload size');
      }

      // Check quota
      const quota = await QuotaEngine.checkQuota(currentUser.id, size);
      if (!quota.allowed) {
        throw new AppError(
          400,
          'QUOTA_EXCEEDED',
          `Insufficient storage quota. Required: ${size} bytes, Available: ${quota.availableBytes} bytes`
        );
      }

      // Verify folder permission if target folder specified
      if (folderId) {
        const perm = await PermissionEngine.resolveFolderPermission(currentUser.id, folderId);
        if (!perm.hasAccess || !PermissionEngine.can('editor', perm.permission)) {
          throw new AppError(403, 'FORBIDDEN', 'Editor permission required in target folder');
        }
      }

      // Upload directly to Cloudflare R2 from backend server
      const objectUuid = crypto.randomUUID();
      const storageKey = `objects/${objectUuid}`;
      const arrayBuffer = await file.arrayBuffer();
      const buffer = Buffer.from(arrayBuffer);

      await r2Client.send(
        new PutObjectCommand({
          Bucket: BUCKET_NAME,
          Key: storageKey,
          ContentType: mimeType,
          Body: buffer,
        })
      );

      // Create storageObject with ready status
      const [storageObj] = await db
        .insert(storageObjects)
        .values({
          ownerId: currentUser.id,
          storageKey,
          mimeType,
          size,
          status: 'ready',
        })
        .returning();

      // Create file representation
      const [fileRecord] = await db
        .insert(files)
        .values({
          ownerId: currentUser.id,
          folderId: folderId || null,
          storageObjectId: storageObj.id,
          name: filename,
          originalName: filename,
          mimeType,
          size,
        })
        .returning();

      // Increment quota usage atomically
      await QuotaEngine.incrementUsage(currentUser.id, size);

      return {
        success: true,
        message: 'File uploaded and verified successfully',
        data: { file: fileRecord },
      };
    },
    {
      body: t.Object({
        file: t.File(),
        folderId: t.Optional(t.Nullable(t.String())),
      }),
      detail: { tags: ['Uploads'], summary: 'Direct atomic upload via API backend' },
    }
  )

  // 5. Setup Cloudflare R2 CORS rules
  .post(
    '/setup-cors',
    async () => {
      try {
        const rules = await configureR2Cors();
        return {
          success: true,
          message: 'Cloudflare R2 CORS configured successfully',
          data: { rules },
        };
      } catch (err: any) {
        return {
          success: false,
          error: {
            code: 'R2_CORS_ERROR',
            message: err.message || 'Failed to configure Cloudflare R2 CORS',
          },
        };
      }
    },
    {
      detail: { tags: ['Uploads'], summary: 'Configure CORS on Cloudflare R2 bucket' },
    }
  );

