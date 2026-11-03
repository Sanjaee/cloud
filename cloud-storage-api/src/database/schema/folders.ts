import { pgTable, varchar, timestamp, uuid, boolean, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const folders = pgTable('folders', {
  id: uuid('id').primaryKey().defaultRandom(),
  ownerId: uuid('owner_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  parentId: uuid('parent_id'),
  name: varchar('name', { length: 255 }).notNull(),
  color: varchar('color', { length: 16 }),
  isStarred: boolean('is_starred').notNull().default(false),
  deletedAt: timestamp('deleted_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('folders_owner_idx').on(table.ownerId),
  index('folders_parent_idx').on(table.parentId),
  index('folders_deleted_idx').on(table.deletedAt),
]);

