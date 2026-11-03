import { Elysia } from 'elysia';
import { AppError } from './error-handler';
import { env } from '@/config/env';

interface RateLimitRecord {
  count: number;
  resetAt: number;
}

const rateLimitStore = new Map<string, RateLimitRecord>();

// Cleanup stale records periodically
setInterval(() => {
  const now = Date.now();
  for (const [key, value] of rateLimitStore.entries()) {
    if (value.resetAt < now) {
      rateLimitStore.delete(key);
    }
  }
}, 60000).unref();

export function createRateLimiter(limit: number = 60, windowMs: number = 60000) {
  return (app: Elysia) =>
    app.onRequest(({ request }) => {
      if (env.RATE_LIMIT_ENABLED === 'false') return;

      const ip =
        request.headers.get('x-forwarded-for')?.split(',')[0].trim() ||
        request.headers.get('x-real-ip') ||
        '127.0.0.1';

      const key = `${ip}:${new URL(request.url).pathname}`;
      const now = Date.now();
      const record = rateLimitStore.get(key);

      if (!record || record.resetAt < now) {
        rateLimitStore.set(key, { count: 1, resetAt: now + windowMs });
        return;
      }

      if (record.count >= limit) {
        throw new AppError(
          429,
          'RATE_LIMIT_EXCEEDED',
          'Too many requests, please try again later.'
        );
      }

      record.count++;
    });
}
