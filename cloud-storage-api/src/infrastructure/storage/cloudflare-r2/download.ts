import { GetObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { r2Client, BUCKET_NAME } from './client';
import { APP_CONSTANTS } from '@/config/constants';

export async function createPresignedDownloadUrl(
  storageKey: string,
  filename: string,
  disposition: 'attachment' | 'inline' = 'attachment'
): Promise<{ downloadUrl: string; expiresIn: number }> {
  // RFC 5987 / 6266 encoding for safe content-disposition filenames
  const encodedFilename = encodeURIComponent(filename);
  const command = new GetObjectCommand({
    Bucket: BUCKET_NAME,
    Key: storageKey,
    ResponseContentDisposition: `${disposition}; filename="${filename.replace(/"/g, '')}"; filename*=UTF-8''${encodedFilename}`,
  });

  const expiresIn = APP_CONSTANTS.PRESIGNED_DOWNLOAD_EXPIRATION_SEC;
  const downloadUrl = await getSignedUrl(r2Client, command, { expiresIn });

  return { downloadUrl, expiresIn };
}

