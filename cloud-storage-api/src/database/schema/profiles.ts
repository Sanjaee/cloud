import { pgTable, text, varchar, timestamp, uuid, index } from 'drizzle-orm/pg-core';
import { users } from './users';

export const userProfiles = pgTable('user_profiles', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }).unique(),
  displayName: varchar('display_name', { length: 128 }),
  avatarUrl: text('avatar_url'),
  bio: text('bio'),
  updatedAt: timestamp('updated_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('profiles_user_id_idx').on(table.userId),
]);

