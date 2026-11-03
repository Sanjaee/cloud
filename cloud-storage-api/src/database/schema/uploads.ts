import { pgTable, text, varchar, timestamp, bigint, integer, uuid, index } from 'drizzle-orm/pg-core';
import { users } from './users';
import { storageObjects } from './storage-objects';

export const uploadSessions = pgTable('upload_sessions', {
  id: uuid('id').primaryKey().defaultRandom(),
  userId: uuid('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  storageObjectId: uuid('storage_object_id').notNull().references(() => storageObjects.id, { onDelete: 'cascade' }),
  idempotencyKey: varchar('idempotency_key', { length: 128 }).unique(),
  status: varchar('status', { length: 32 }).notNull().default('initiated'), // 'initiated' | 'uploading' | 'completed' | 'aborted'
  uploadType: varchar('upload_type', { length: 32 }).notNull().default('single'), // 'single' | 'multipart'
  uploadId: text('upload_id'), // S3 Multipart UploadId if multipart
  totalParts: integer('total_parts').default(1),
  completedParts: integer('completed_parts').default(0),
  expiresAt: timestamp('expires_at', { withTimezone: true }).notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
  completedAt: timestamp('completed_at', { withTimezone: true }),
}, (table) => [
  index('upload_sessions_user_idx').on(table.userId),
  index('upload_sessions_status_idx').on(table.status),
]);

export const multipartParts = pgTable('multipart_parts', {
  id: uuid('id').primaryKey().defaultRandom(),
  uploadSessionId: uuid('upload_session_id').notNull().references(() => uploadSessions.id, { onDelete: 'cascade' }),
  partNumber: integer('part_number').notNull(),
  etag: text('etag').notNull(),
  size: bigint('size', { mode: 'number' }).notNull(),
  createdAt: timestamp('created_at', { withTimezone: true }).notNull().defaultNow(),
}, (table) => [
  index('multipart_parts_session_idx').on(table.uploadSessionId),
]);

