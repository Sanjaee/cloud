import { pgTable, text, varchar, timestamp, uuid, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const notifications = pgTable('notifications', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  actorId: uuid('actor_id').references(() => users.id, { onDelete: 'set null' }),
  type: varchar('type', { length: 64 }).notNull(), // 'share_received', 'permission_revoked', 'comment_added', 'security_alert'
  resourceType: varchar('resource_type', { length: 16 }),
  resourceId: uuid('resource_id'),
  message: text('message').notNull(),
  readAt: timestamp('read_at', { withTimezone: true }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('notifications_user_idx').on(table.userId),
]);

