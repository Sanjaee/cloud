import { pgTable, text, timestamp, uuid, boolean, jsonb, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const mfaCredentials = pgTable('mfa_credentials', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }).unique(),
  secret: text('secret').notNull(),
  backupCodesHash: jsonb('backup_codes_hash').$type<string[]>(),
  isEnabled: boolean('is_enabled').notNull().default(false),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('mfa_user_id_idx').on(table.userId),
]);

