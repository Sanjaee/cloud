import { db } from '@/infrastructure/database/client';
import { files, folders, storageObjects, shortcuts, filePermissions, folderPermissions } from '@/database/schema';
import { eq, and, sql, isNull, inArray } from 'drizzle-orm';
import { deleteR2Object } from '@/infrastructure/storage/cloudflare-r2/delete';
import { QuotaEngine } from './quota-engine';

export class FileLifecycleService {
  /**
   * Soft deletes a file (moves to Trash).
   */
  static async softDeleteFile(fileId: string): Promise<void> {
    await db
      .update(files)
      .set({ deletedAt: new Date(), updatedAt: new Date() })
      .where(eq(files.id, fileId));
  }

  /**
   * Restores a soft-deleted file from Trash.
   */
  static async restoreFile(fileId: string): Promise<void> {
    await db
      .update(files)
      .set({ deletedAt: null, updatedAt: new Date() })
      .where(eq(files.id, fileId));
  }

  /**
   * Permanently deletes a file.
   * Safety check: Deletes the physical R2 object ONLY if no other active or deleted file records
   * reference the underlying storageObject.
   */
  static async permanentlyDeleteFile(fileId: string): Promise<{ r2ObjectDeleted: boolean }> {
    const file = await db.query.files.findFirst({
      where: eq(files.id, fileId),
    });

    if (!file) return { r2ObjectDeleted: false };

    const storageObjId = file.storageObjectId;
    const ownerId = file.ownerId;
    const fileSize = file.size;

    // 1. Delete shortcuts pointing to this file
    await db.delete(shortcuts).where(and(eq(shortcuts.targetType, 'file'), eq(shortcuts.targetId, fileId)));

    // 2. Delete file permissions
    await db.delete(filePermissions).where(eq(filePermissions.fileId, fileId));

    // 3. Delete file record
    await db.delete(files).where(eq(files.id, fileId));

    // 4. Check if any other files still reference this storageObject
    const otherReferences = await db
      .select({ count: sql<number>`count(*)::int` })
      .from(files)
      .where(eq(files.storageObjectId, storageObjId));

    const remainingCount = otherReferences[0]?.count || 0;
    let r2ObjectDeleted = false;

    if (remainingCount === 0) {
      // Safe to delete physical storage object from R2!
      const storageObj = await db.query.storageObjects.findFirst({
        where: eq(storageObjects.id, storageObjId),
      });

      if (storageObj) {
        await deleteR2Object(storageObj.storageKey);
        await db.delete(storageObjects).where(eq(storageObjects.id, storageObjId));
        await QuotaEngine.decrementUsage(ownerId, fileSize);
        r2ObjectDeleted = true;
      }
    }

    return { r2ObjectDeleted };
  }

  /**
   * Soft deletes a folder and its contents recursively.
   */
  static async softDeleteFolder(folderId: string): Promise<void> {
    const now = new Date();
    // Soft delete the folder
    await db.update(folders).set({ deletedAt: now, updatedAt: now }).where(eq(folders.id, folderId));

    // Soft delete contained files
    await db.update(files).set({ deletedAt: now, updatedAt: now }).where(eq(files.folderId, folderId));

    // Recursively soft delete child subfolders
    const subfolders = await db.query.folders.findMany({
      where: and(eq(folders.parentId, folderId), isNull(folders.deletedAt)),
    });

    for (const sub of subfolders) {
      await this.softDeleteFolder(sub.id);
    }
  }

  /**
   * Restores a folder and its contents.
   */
  static async restoreFolder(folderId: string): Promise<void> {
    const now = new Date();
    await db.update(folders).set({ deletedAt: null, updatedAt: now }).where(eq(folders.id, folderId));
    await db.update(files).set({ deletedAt: null, updatedAt: now }).where(eq(files.folderId, folderId));

    const subfolders = await db.query.folders.findMany({
      where: eq(folders.parentId, folderId),
    });

    for (const sub of subfolders) {
      await this.restoreFolder(sub.id);
    }
  }

  /**
   * Permanently deletes a folder and all nested files and subfolders.
   */
  static async permanentlyDeleteFolder(folderId: string): Promise<void> {
    // Delete files in this folder permanently
    const containedFiles = await db.query.files.findMany({
      where: eq(files.folderId, folderId),
    });

    for (const f of containedFiles) {
      await this.permanentlyDeleteFile(f.id);
    }

    // Recursively delete subfolders
    const subfolders = await db.query.folders.findMany({
      where: eq(folders.parentId, folderId),
    });

    for (const sub of subfolders) {
      await this.permanentlyDeleteFolder(sub.id);
    }

    // Delete shortcuts and folder permissions
    await db.delete(shortcuts).where(and(eq(shortcuts.targetType, 'folder'), eq(shortcuts.targetId, folderId)));
    await db.delete(folderPermissions).where(eq(folderPermissions.folderId, folderId));

    // Delete the folder record itself
    await db.delete(folders).where(eq(folders.id, folderId));
  }
}

