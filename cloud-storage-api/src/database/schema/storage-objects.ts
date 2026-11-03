import { pgTable, text, varchar, timestamp, bigint, uuid, index, uniqueIndex } from 'drizzle-orm/pg-core';
import { users } from './users';

export const storageObjects = pgTable('storage_objects', {
  id: uuid('id').primaryKey().defaultRandom(),
  ownerId: uuid('owner_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  storageKey: text('storage_key').notNull().unique(), // Immutable R2 key: "objects/{uuid}"
  mimeType: varchar('mime_type', { length: 128 }).notNull(),
  size: bigint('size', { mode: 'number' }).notNull(),
  checksum: varchar('checksum', { length: 128 }),
  status: varchar('status', { length: 32 }).notNull().default('pending'), // 'pending' | 'ready' | 'deleted'
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('storage_objects_owner_idx').on(table.ownerId),
  uniqueIndex('storage_objects_key_idx').on(table.storageKey),
  index('storage_objects_status_idx').on(table.status),
]);

