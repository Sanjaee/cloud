import { pgTable, varchar, timestamp, uuid, index, uniqueIndex } from 'drizzle-orm/pg-core';
import { users } from './users';
import { folders } from './folders';
import { files } from './files';

// Inherited folder permissions
export const folderPermissions = pgTable('folder_permissions', {
  id: uuid('id').primaryKey().defaultRandom(),
  folderId: uuid('folder_id').notNull().references(() => folders.id, { onDelete: 'cascade' }),
  grantedBy: uuid('granted_by').notNull().references(() => users.id, { onDelete: 'cascade' }),
  grantedTo: uuid('granted_to').notNull().references(() => users.id, { onDelete: 'cascade' }),
  permission: varchar('permission', { length: 32 }).notNull(), // 'viewer' | 'commenter' | 'editor'
  expiresAt: timestamp('expires_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('folder_perm_folder_idx').on(table.folderId),
  index('folder_perm_user_idx').on(table.grantedTo),
  uniqueIndex('folder_perm_unique_idx').on(table.folderId, table.grantedTo),
]);

// Direct file permissions
export const filePermissions = pgTable('file_permissions', {
  id: uuid('id').primaryKey().defaultRandom(),
  fileId: uuid('file_id').notNull().references(() => files.id, { onDelete: 'cascade' }),
  grantedBy: uuid('granted_by').notNull().references(() => users.id, { onDelete: 'cascade' }),
  grantedTo: uuid('granted_to').notNull().references(() => users.id, { onDelete: 'cascade' }),
  permission: varchar('permission', { length: 32 }).notNull(), // 'viewer' | 'commenter' | 'editor'
  expiresAt: timestamp('expires_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('file_perm_file_idx').on(table.fileId),
  index('file_perm_user_idx').on(table.grantedTo),
  uniqueIndex('file_perm_unique_idx').on(table.fileId, table.grantedTo),
]);

