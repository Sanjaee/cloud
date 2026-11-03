import { Elysia } from 'elysia';
import { db, sql } from '../../infrastructure/database/client';
import { r2Client, BUCKET_NAME } from '../../infrastructure/storage/cloudflare-r2/client';
import { HeadBucketCommand } from '@aws-sdk/client-s3';

export const healthRoutes = new Elysia({ prefix: '/health' })
  // 1. Liveness check
  .get('/', () => ({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  }))

  // 2. Readiness check (Database & R2 Storage)
  .get('/ready', async ({ set }) => {
    let dbStatus = 'ok';
    let r2Status = 'ok';

    try {
      await sql`SELECT 1`;
    } catch (e) {
      dbStatus = 'error';
    }

    try {
      await r2Client.send(new HeadBucketCommand({ Bucket: BUCKET_NAME }));
    } catch (e) {
      r2Status = 'error';
    }

    const isHealthy = dbStatus === 'ok' && r2Status === 'ok';
    if (!isHealthy) {
      set.status = 503;
    }

    return {
      status: isHealthy ? 'ready' : 'degraded',
      services: {
        database: dbStatus,
        storageR2: r2Status,
      },
      timestamp: new Date().toISOString(),
    };
  });

