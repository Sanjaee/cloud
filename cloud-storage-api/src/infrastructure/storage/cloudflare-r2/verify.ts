import { HeadObjectCommand } from '@aws-sdk/client-s3';
import { r2Client, BUCKET_NAME } from './client';

export async function verifyR2Object(storageKey: string): Promise<{
  exists: boolean;
  contentLength?: number;
  contentType?: string;
  eTag?: string;
}> {
  try {
    const command = new HeadObjectCommand({
      Bucket: BUCKET_NAME,
      Key: storageKey,
    });
    const res = await r2Client.send(command);
    return {
      exists: true,
      contentLength: res.ContentLength,
      contentType: res.ContentType,
      eTag: res.ETag,
    };
  } catch (error: any) {
    if (error.name === 'NotFound' || error.$metadata?.httpStatusCode === 404) {
      return { exists: false };
    }
    throw error;
  }
}

