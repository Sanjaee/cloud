import { PutBucketCorsCommand, GetBucketCorsCommand } from '@aws-sdk/client-s3';
import { r2Client, BUCKET_NAME } from './client';

export async function configureR2Cors() {
  const command = new PutBucketCorsCommand({
    Bucket: BUCKET_NAME,
    CORSConfiguration: {
      CORSRules: [
        {
          AllowedHeaders: ['*'],
          AllowedMethods: ['GET', 'PUT', 'POST', 'DELETE', 'HEAD'],
          AllowedOrigins: ['*'],
          ExposeHeaders: ['ETag', 'Content-Length', 'Content-Type'],
          MaxAgeSeconds: 86400,
        },
      ],
    },
  });

  await r2Client.send(command);
  
  const getCmd = new GetBucketCorsCommand({ Bucket: BUCKET_NAME });
  const res = await r2Client.send(getCmd);
  return res.CORSRules;
}

