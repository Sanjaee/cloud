import { pgTable, text, varchar, timestamp, uuid, boolean, index, uniqueIndex } from 'drizzle-orm/pg-core';

export const shareLinks = pgTable('share_links', {
  id: uuid('id').primaryKey().defaultRandom(),
  resourceType: varchar('resource_type', { length: 16 }).notNull(), // 'file' | 'folder'
  resourceId: uuid('resource_id').notNull(),
  tokenHash: text('token_hash').notNull().unique(),
  permission: varchar('permission', { length: 32 }).notNull().default('viewer'),
  passwordHash: text('password_hash'),
  allowDownload: boolean('allow_download').notNull().default(true),
  expiresAt: timestamp('expires_at', { withTimezone: true }),
  revokedAt: timestamp('revoked_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  uniqueIndex('share_links_token_idx').on(table.tokenHash),
  index('share_links_resource_idx').on(table.resourceType, table.resourceId),
]);

