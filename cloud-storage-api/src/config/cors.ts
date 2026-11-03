import { env } from './env';

export const corsOrigins = env.WEB_ORIGIN.split(',').map(s => s.trim());

export const corsConfig = {
  origin: (request: Request): boolean => {
    const origin = request.headers.get('origin');
    if (!origin) return true; // allow mobile app / curl / same-origin
    return corsOrigins.includes(origin) || corsOrigins.includes('*');
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'HEAD'] as const,
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With', 'X-Device-Id', 'X-Device-Name', 'X-Idempotency-Key'],
  exposeHeaders: ['Content-Length', 'X-Total-Count', 'X-Request-Id'],
  maxAge: 86400,
};

