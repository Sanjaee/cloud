import { pgTable, text, varchar, timestamp, uuid, boolean, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const devices = pgTable('devices', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  deviceIdentifier: varchar('device_identifier', { length: 255 }).notNull(),
  deviceName: varchar('device_name', { length: 128 }),
  deviceType: varchar('device_type', { length: 64 }), // 'browser' | 'ios' | 'android' | 'desktop'
  ipAddress: varchar('ip_address', { length: 64 }),
  lastSeenAt: timestamp('last_seen_at', { withTimezone: true }).notNull().defaultNow(),
  isTrusted: boolean('is_trusted').notNull().default(false),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('devices_user_idx').on(table.userId),
]);

