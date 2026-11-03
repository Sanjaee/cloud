import { pgTable, text, varchar, timestamp, uuid, jsonb, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const securityEvents = pgTable('security_events', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  eventType: varchar('event_type', { length: 64 }).notNull(), // 'login_success', 'login_failed', 'password_changed', 'mfa_enabled', 'session_revoked'
  ipAddress: varchar('ip_address', { length: 64 }),
  userAgent: text('user_agent'),
  metadata: jsonb('metadata'),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('security_events_user_idx').on(table.userId),
  index('security_events_type_idx').on(table.eventType),
  index('security_events_created_idx').on(table.createdAt),
]);

