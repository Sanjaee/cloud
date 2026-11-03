import { pgTable, text, varchar, timestamp, bigint, integer, uuid, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const storageUsageSnapshots = pgTable('storage_usage_snapshots', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  totalBytes: bigint('total_bytes', { mode: 'number' }).notNull(),
  physicalObjectCount: integer('physical_object_count').notNull(),
  fileCount: integer('file_count').notNull(),
  sharedFileCount: integer('shared_file_count').notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('storage_usage_user_idx').on(table.userId),
]);

