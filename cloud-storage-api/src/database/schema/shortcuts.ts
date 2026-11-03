import { pgTable, varchar, timestamp, uuid, index } from 'drizzle-orm/pg-core';
import { users } from './users';
import { folders } from './folders';

export const shortcuts = pgTable('shortcuts', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  targetType: varchar('target_type', { length: 16 }).notNull(), // 'file' | 'folder'
  targetId: uuid('target_id').notNull(),
  parentFolderId: uuid('parent_folder_id').references(() => folders.id, { onDelete: 'cascade' }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('shortcuts_user_idx').on(table.userId),
  index('shortcuts_parent_idx').on(table.parentFolderId),
]);

