import { pgTable, text, varchar, timestamp, bigint, uuid, index } from 'drizzle-orm/pg-core';

export const users = pgTable('users', {
  id: uuid('id').primaryKey().defaultRandom(),
  email: varchar('email', { length: 255 }).notNull().unique(),
  username: varchar('username', { length: 64 }).notNull().unique(),
  googleId: varchar('google_id', { length: 255 }).unique(),
  authProvider: varchar('auth_provider', { length: 32 }).notNull().default('local'), // 'local' | 'google'
  passwordHash: text('password_hash'),
  emailVerifiedAt: timestamp('email_verified_at', { withTimezone: true }),
  status: varchar('status', { length: 32 }).notNull().default('active'), // 'active' | 'suspended' | 'pending_verification'
  storageQuota: bigint('storage_quota', { mode: 'number' }).notNull().default(10 * 1024 * 1024 * 1024), // 10 GB
  storageUsed: bigint('storage_used', { mode: 'number' }).notNull().default(0),
  lastLoginAt: timestamp('last_login_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('users_email_idx').on(table.email),
  index('users_username_idx').on(table.username),
  index('users_google_id_idx').on(table.googleId),
]);
