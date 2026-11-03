export type PermissionLevel = 'viewer' | 'commenter' | 'editor' | 'owner';

export interface User {
  id: string;
  email: string;
  username: string;
  status: string;
  emailVerifiedAt: string | null;
  storageQuota: number;
  storageUsed: number;
  profile?: {
    displayName?: string;
    avatarUrl?: string;
    bio?: string;
  } | null;
}

export interface FileItem {
  id: string;
  ownerId: string;
  folderId: string | null;
  storageObjectId: string;
  name: string;
  originalName: string;
  mimeType: string;
  size: number;
  isStarred: boolean;
  isOwner?: boolean;
  permission?: PermissionLevel;
  sharedAt?: string;
  createdAt: string;
  updatedAt: string;
  owner?: {
    id: string;
    email: string;
    username: string;
  };
  folderName?: string;
  folderPath?: string;
  breadcrumbs?: BreadcrumbItem[];
}

export interface FolderItem {
  id: string;
  ownerId: string;
  parentId: string | null;
  name: string;
  color?: string;
  isStarred?: boolean;
  isOwner?: boolean;
  permission?: PermissionLevel;
  sharedAt?: string;
  createdAt: string;
  updatedAt: string;
  parentPath?: string;
  folderPath?: string;
  breadcrumbs?: BreadcrumbItem[];
}

export interface BreadcrumbItem {
  id: string | null;
  name: string;
}

export interface CollaboratorInfo {
  permissionId: string;
  userId: string;
  email: string;
  username: string;
  permission: PermissionLevel;
  expiresAt: string | null;
  createdAt: string;
}

export interface QuotaInfo {
  quotaBytes: number;
  usedBytes: number;
  availableBytes: number;
  percentage: number;
  fileCount: number;
  physicalObjectCount: number;
  sharedFileCount: number;
  formattedUsed: string;
  formattedQuota: string;
  formattedAvailable: string;
}

export interface ActiveSession {
  id: string;
  ipAddress: string | null;
  userAgent: string | null;
  lastUsedAt: string;
  createdAt: string;
  isCurrent: boolean;
}

export interface SecurityEvent {
  id: string;
  eventType: string;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
}

export interface NotificationItem {
  id: string;
  userId: string;
  actorId: string | null;
  type: string;
  resourceType: string | null;
  resourceId: string | null;
  message: string;
  readAt: string | null;
  createdAt: string;
}

