import { pgTable, text, varchar, timestamp, uuid, boolean, index, uniqueIndex } from 'drizzle-orm/pg-core';
import { users } from './users';

export const refreshTokens = pgTable('refresh_tokens', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  tokenHash: text('token_hash').notNull().unique(),
  familyId: uuid('family_id').notNull(), // tracks rotating token families to detect reuse
  deviceId: varchar('device_id', { length: 128 }),
  isRevoked: boolean('is_revoked').notNull().default(false),
  expiresAt: timestamp('expires_at', { withTimezone: true }).notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('refresh_tokens_user_idx').on(table.userId),
  uniqueIndex('refresh_tokens_hash_idx').on(table.tokenHash),
  index('refresh_tokens_family_idx').on(table.familyId),
]);

