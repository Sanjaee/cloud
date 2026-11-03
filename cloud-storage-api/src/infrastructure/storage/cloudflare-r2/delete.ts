import { DeleteObjectCommand } from '@aws-sdk/client-s3';
import { r2Client, BUCKET_NAME } from './client';

export async function deleteR2Object(storageKey: string): Promise<boolean> {
  try {
    const command = new DeleteObjectCommand({
      Bucket: BUCKET_NAME,
      Key: storageKey,
    });
    await r2Client.send(command);
    return true;
  } catch (error) {
    console.error(`Failed to delete R2 object ${storageKey}:`, error);
    return false;
  }
}

