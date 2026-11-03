import { db } from '../infrastructure/database/client';
import { files, folders, filePermissions, folderPermissions, users } from '../database/schema';
import { eq, and, isNull, or, gt } from 'drizzle-orm';

export type PermissionLevel = 'viewer' | 'commenter' | 'editor' | 'owner';

export interface PermissionCheckResult {
  hasAccess: boolean;
  permission: PermissionLevel | null;
  isOwner: boolean;
  source: 'owner' | 'direct' | 'inherited' | null;
  inheritedFromFolderId?: string;
}

const PERMISSION_WEIGHT: Record<PermissionLevel, number> = {
  viewer: 1,
  commenter: 2,
  editor: 3,
  owner: 4,
};

export class PermissionEngine {
  /**
   * Resolves a user's effective permission on a file.
   * Priority:
   * 1. Resource Owner -> 'owner'
   * 2. Direct File Permission (if not expired) -> 'editor' | 'commenter' | 'viewer'
   * 3. Inherited Folder Permission -> climbs folder ancestry up to root
   */
  static async resolveFilePermission(
    userId: string,
    fileId: string
  ): Promise<PermissionCheckResult> {
    const file = await db.query.files.findFirst({
      where: and(eq(files.id, fileId), isNull(files.deletedAt)),
    });

    if (!file) {
      return { hasAccess: false, permission: null, isOwner: false, source: null };
    }

    // 1. Owner check
    if (file.ownerId === userId) {
      return { hasAccess: true, permission: 'owner', isOwner: true, source: 'owner' };
    }

    const now = new Date();

    // 2. Direct file permission check
    const directPerm = await db.query.filePermissions.findFirst({
      where: and(
        eq(filePermissions.fileId, fileId),
        eq(filePermissions.grantedTo, userId),
        or(isNull(filePermissions.expiresAt), gt(filePermissions.expiresAt, now))
      ),
    });

    if (directPerm) {
      return {
        hasAccess: true,
        permission: directPerm.permission as PermissionLevel,
        isOwner: false,
        source: 'direct',
      };
    }

    // 3. Inherited folder permission check
    if (file.folderId) {
      const folderPerm = await this.resolveFolderPermission(userId, file.folderId);
      if (folderPerm.hasAccess && folderPerm.permission) {
        return {
          hasAccess: true,
          permission: folderPerm.permission,
          isOwner: false,
          source: 'inherited',
          inheritedFromFolderId: folderPerm.inheritedFromFolderId || file.folderId,
        };
      }
    }

    return { hasAccess: false, permission: null, isOwner: false, source: null };
  }

  /**
   * Resolves a user's effective permission on a folder.
   * Recursively traverses parent folders if no direct grant exists.
   */
  static async resolveFolderPermission(
    userId: string,
    folderId: string
  ): Promise<PermissionCheckResult> {
    let currentFolderId: string | null = folderId;
    const now = new Date();
    const visited = new Set<string>();

    while (currentFolderId) {
      if (visited.has(currentFolderId)) break; // cycle protection
      visited.add(currentFolderId);

      const folder: any = await db.query.folders.findFirst({
        where: and(eq(folders.id, currentFolderId), isNull(folders.deletedAt)),
      });

      if (!folder) {
        return { hasAccess: false, permission: null, isOwner: false, source: null };
      }

      // Owner of this folder
      if (folder.ownerId === userId) {
        return {
          hasAccess: true,
          permission: 'owner',
          isOwner: true,
          source: currentFolderId === folderId ? 'owner' : 'inherited',
          inheritedFromFolderId: currentFolderId !== folderId ? currentFolderId : undefined,
        };
      }

      // Direct permission on this folder
      const perm = await db.query.folderPermissions.findFirst({
        where: and(
          eq(folderPermissions.folderId, currentFolderId),
          eq(folderPermissions.grantedTo, userId),
          or(isNull(folderPermissions.expiresAt), gt(folderPermissions.expiresAt, now))
        ),
      });

      if (perm) {
        return {
          hasAccess: true,
          permission: perm.permission as PermissionLevel,
          isOwner: false,
          source: currentFolderId === folderId ? 'direct' : 'inherited',
          inheritedFromFolderId: currentFolderId !== folderId ? currentFolderId : undefined,
        };
      }

      currentFolderId = folder.parentId;
    }

    return { hasAccess: false, permission: null, isOwner: false, source: null };
  }

  static can(requiredLevel: PermissionLevel, userLevel: PermissionLevel | null): boolean {
    if (!userLevel) return false;
    return PERMISSION_WEIGHT[userLevel] >= PERMISSION_WEIGHT[requiredLevel];
  }
}

