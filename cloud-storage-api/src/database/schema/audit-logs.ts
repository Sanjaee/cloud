import { pgTable, text, varchar, timestamp, uuid, jsonb, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const auditLogs = pgTable('audit_logs', {
  id: uuid('id').primaryKey().defaultRandom(),
  actorId: uuid('actor_id').references(() => users.id, { onDelete: 'set null' }),
  action: varchar('action', { length: 64 }).notNull(),
  resourceType: varchar('resource_type', { length: 64 }).notNull(),
  resourceId: uuid('resource_id'),
  metadata: jsonb('metadata'),
  ipAddress: varchar('ip_address', { length: 64 }),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('audit_actor_idx').on(table.actorId),
  index('audit_action_idx').on(table.action),
  index('audit_created_idx').on(table.createdAt),
]);

