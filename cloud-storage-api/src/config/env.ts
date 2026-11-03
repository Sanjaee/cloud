import { z } from 'zod';

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.string().default('3000'),
  DATABASE_URL: z.string().min(1),
  WEB_ORIGIN: z.string().default('http://localhost:5173,http://localhost:3000'),
  AUTH_SECRET: z.string().default('rea_auth_secret_enterprise_2026_super_secure_random_key_abc123xyz'),
  SESSION_SECRET: z.string().default('rea_session_secret_enterprise_2026_super_secure_random_key_def456uvw'),
  R2_ACCOUNT_ID: z.string().min(1),
  R2_ACCESS_KEY_ID: z.string().min(1),
  R2_SECRET_ACCESS_KEY: z.string().min(1),
  R2_BUCKET_NAME: z.string().default('lostmedia'),
  R2_ENDPOINT: z.string().min(1),
  R2_PUBLIC_DOMAIN: z.string().optional(),
  RATE_LIMIT_ENABLED: z.string().default('true'),
});

const clean = (val?: string) => (val ? val.replace(/^\uFEFF/, '').trim() : undefined);

export const env = envSchema.parse({
  NODE_ENV: clean(process.env.NODE_ENV),
  PORT: clean(process.env.PORT),
  DATABASE_URL: clean(process.env.DATABASE_URL),
  WEB_ORIGIN: clean(process.env.WEB_ORIGIN),
  AUTH_SECRET: clean(process.env.AUTH_SECRET),
  SESSION_SECRET: clean(process.env.SESSION_SECRET),
  R2_ACCOUNT_ID: clean(process.env.R2_ACCOUNT_ID),
  R2_ACCESS_KEY_ID: clean(process.env.R2_ACCESS_KEY_ID),
  R2_SECRET_ACCESS_KEY: clean(process.env.R2_SECRET_ACCESS_KEY),
  R2_BUCKET_NAME: clean(process.env.R2_BUCKET_NAME) || 'lostmedia',
  R2_ENDPOINT: clean(process.env.R2_ENDPOINT),
  R2_PUBLIC_DOMAIN: clean(process.env.R2_PUBLIC_DOMAIN),
  RATE_LIMIT_ENABLED: clean(process.env.RATE_LIMIT_ENABLED),
});

