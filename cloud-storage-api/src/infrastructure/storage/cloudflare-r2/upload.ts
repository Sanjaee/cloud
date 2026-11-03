import { PutObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { r2Client, BUCKET_NAME } from './client';
import { APP_CONSTANTS } from '../../../config/constants';

export async function createPresignedUploadUrl(
  storageKey: string,
  mimeType: string,
  contentLength?: number,
  checksum?: string
): Promise<{ uploadUrl: string; expiresIn: number }> {
  // Do NOT pass ContentLength into PutObjectCommand when generating presigned URL.
  // Passing ContentLength forces AWS SDK to sign X-Amz-SignedHeaders=content-length;host,
  // which causes browser XMLHttpRequest/fetch to fail signature verification.
  const command = new PutObjectCommand({
    Bucket: BUCKET_NAME,
    Key: storageKey,
    ContentType: mimeType,
    ...(checksum ? { ChecksumSHA256: checksum } : {}),
  });

  const expiresIn = APP_CONSTANTS.PRESIGNED_UPLOAD_EXPIRATION_SEC;
  const uploadUrl = await getSignedUrl(r2Client, command, {
    expiresIn,
    signableHeaders: new Set(['content-type']),
  });

  return { uploadUrl, expiresIn };
}

