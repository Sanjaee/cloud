import { pgTable, text, varchar, timestamp, bigint, uuid, boolean, index } from 'drizzle-orm/pg-core';
import { users } from './users';
import { folders } from './folders';
import { storageObjects } from './storage-objects';

export const files = pgTable('files', {
  id: uuid('id').primaryKey().defaultRandom(),
  ownerId: uuid('owner_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  folderId: uuid('folder_id').references(() => folders.id, { onDelete: 'cascade' }),
  storageObjectId: uuid('storage_object_id').notNull().references(() => storageObjects.id, { onDelete: 'restrict' }),
  name: varchar('name', { length: 255 }).notNull(),
  originalName: varchar('original_name', { length: 255 }).notNull(),
  mimeType: varchar('mime_type', { length: 128 }).notNull(),
  size: bigint('size', { mode: 'number' }).notNull(),
  isStarred: boolean('is_starred').notNull().default(false),
  deletedAt: timestamp('deleted_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('files_owner_idx').on(table.ownerId),
  index('files_folder_idx').on(table.folderId),
  index('files_storage_obj_idx').on(table.storageObjectId),
  index('files_deleted_idx').on(table.deletedAt),
]);

