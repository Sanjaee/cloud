import { relations } from 'drizzle-orm';
import { users } from './users';
import { userProfiles } from './profiles';
import { sessions } from './sessions';
import { refreshTokens } from './refresh-tokens';
import { devices } from './devices';
import { mfaCredentials } from './mfa';
import { storageObjects } from './storage-objects';
import { folders } from './folders';
import { files } from './files';
import { folderPermissions, filePermissions } from './permissions';
import { shareLinks } from './shares';
import { shortcuts } from './shortcuts';
import { uploadSessions, multipartParts } from './uploads';
import { notifications } from './notifications';
import { storageUsageSnapshots } from './quotas';
import { auditLogs } from './audit-logs';

export * from './users';
export * from './profiles';
export * from './sessions';
export * from './refresh-tokens';
export * from './devices';
export * from './mfa';
export * from './security-events';
export * from './storage-objects';
export * from './folders';
export * from './files';
export * from './permissions';
export * from './shares';
export * from './shortcuts';
export * from './uploads';
export * from './notifications';
export * from './quotas';
export * from './audit-logs';

export const usersRelations = relations(users, ({ one, many }) => ({
  profile: one(userProfiles, {
    fields: [users.id],
    references: [userProfiles.userId],
  }),
  mfa: one(mfaCredentials, {
    fields: [users.id],
    references: [mfaCredentials.userId],
  }),
  sessions: many(sessions),
  refreshTokens: many(refreshTokens),
  devices: many(devices),
  folders: many(folders),
  files: many(files),
  storageObjects: many(storageObjects),
  shortcuts: many(shortcuts),
  notifications: many(notifications),
}));

export const foldersRelations = relations(folders, ({ one, many }) => ({
  owner: one(users, {
    fields: [folders.ownerId],
    references: [users.id],
  }),
  parent: one(folders, {
    fields: [folders.parentId],
    references: [folders.id],
    relationName: 'subfolders',
  }),
  subfolders: many(folders, {
    relationName: 'subfolders',
  }),
  files: many(files),
  permissions: many(folderPermissions),
}));

export const filesRelations = relations(files, ({ one, many }) => ({
  owner: one(users, {
    fields: [files.ownerId],
    references: [users.id],
  }),
  folder: one(folders, {
    fields: [files.folderId],
    references: [folders.id],
  }),
  storageObject: one(storageObjects, {
    fields: [files.storageObjectId],
    references: [storageObjects.id],
  }),
  permissions: many(filePermissions),
}));

export const storageObjectsRelations = relations(storageObjects, ({ one, many }) => ({
  owner: one(users, {
    fields: [storageObjects.ownerId],
    references: [users.id],
  }),
  files: many(files),
}));

