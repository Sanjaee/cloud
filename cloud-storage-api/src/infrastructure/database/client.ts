import { drizzle } from 'drizzle-orm/postgres-js';
import postgres from 'postgres';
import * as schema from '@/database/schema';

const connectionString = process.env.DATABASE_URL || 'postgresql://neondb_owner:npg_fj3hUnJqR0oM@ep-frosty-credit-b378nim8-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require';

export const sql = postgres(connectionString, {
  max: 10,
  idle_timeout: 20,
  connect_timeout: 10,
  ssl: { rejectUnauthorized: false }
});

export const db = drizzle(sql, { schema });
export type Database = typeof db;

