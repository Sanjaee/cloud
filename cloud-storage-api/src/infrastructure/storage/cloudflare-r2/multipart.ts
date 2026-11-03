import {
  CreateMultipartUploadCommand,
  UploadPartCommand,
  CompleteMultipartUploadCommand,
  AbortMultipartUploadCommand,
} from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { r2Client, BUCKET_NAME } from './client';
import { APP_CONSTANTS } from '@/config/constants';

export async function initiateMultipartUpload(storageKey: string, mimeType: string) {
  const command = new CreateMultipartUploadCommand({
    Bucket: BUCKET_NAME,
    Key: storageKey,
    ContentType: mimeType,
  });
  const res = await r2Client.send(command);
  return { uploadId: res.UploadId! };
}

export async function getPresignedPartUrl(storageKey: string, uploadId: string, partNumber: number) {
  const command = new UploadPartCommand({
    Bucket: BUCKET_NAME,
    Key: storageKey,
    UploadId: uploadId,
    PartNumber: partNumber,
  });
  const uploadUrl = await getSignedUrl(r2Client, command, {
    expiresIn: APP_CONSTANTS.PRESIGNED_UPLOAD_EXPIRATION_SEC,
  });
  return { uploadUrl, partNumber };
}

export async function completeMultipartUpload(
  storageKey: string,
  uploadId: string,
  parts: { PartNumber: number; ETag: string }[]
) {
  const sortedParts = [...parts].sort((a, b) => a.PartNumber - b.PartNumber);
  const command = new CompleteMultipartUploadCommand({
    Bucket: BUCKET_NAME,
    Key: storageKey,
    UploadId: uploadId,
    MultipartUpload: {
      Parts: sortedParts,
    },
  });
  return await r2Client.send(command);
}

export async function abortMultipartUpload(storageKey: string, uploadId: string) {
  const command = new AbortMultipartUploadCommand({
    Bucket: BUCKET_NAME,
    Key: storageKey,
    UploadId: uploadId,
  });
  return await r2Client.send(command);
}

