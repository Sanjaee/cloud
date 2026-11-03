import { db } from '@/infrastructure/database/client';
import { users, storageObjects, files } from '@/database/schema';
import { eq, and, sql } from 'drizzle-orm';

export interface QuotaCheckResult {
  allowed: boolean;
  usedBytes: number;
  quotaBytes: number;
  availableBytes: number;
  percentage: number;
}

export class QuotaEngine {
  /**
   * Calculates actual physical storage used by user.
   * Only storageObjects physically owned by user with status 'ready' are counted.
   */
  static async calculatePhysicalUsage(userId: string): Promise<number> {
    const result = await db
      .select({
        total: sql<number>`COALESCE(SUM(${storageObjects.size}), 0)::bigint`,
      })
      .from(storageObjects)
      .where(and(eq(storageObjects.ownerId, userId), eq(storageObjects.status, 'ready')));

    return Number(result[0]?.total || 0);
  }

  /**
   * Verifies if adding `incomingSizeBytes` violates the user's storage quota.
   */
  static async checkQuota(userId: string, incomingSizeBytes: number): Promise<QuotaCheckResult> {
    const user = await db.query.users.findFirst({
      where: eq(users.id, userId),
      columns: {
        storageQuota: true,
        storageUsed: true,
      },
    });

    if (!user) {
      throw new Error('User not found');
    }

    const quotaBytes = user.storageQuota;
    const usedBytes = user.storageUsed;
    const availableBytes = Math.max(0, quotaBytes - usedBytes);
    const allowed = usedBytes + incomingSizeBytes <= quotaBytes;
    const percentage = quotaBytes > 0 ? Math.min(100, Math.round((usedBytes / quotaBytes) * 100)) : 0;

    return {
      allowed,
      usedBytes,
      quotaBytes,
      availableBytes,
      percentage,
    };
  }

  /**
   * Atomically adds size to user's storageUsed.
   */
  static async incrementUsage(userId: string, addedBytes: number): Promise<void> {
    await db
      .update(users)
      .set({
        storageUsed: sql`${users.storageUsed} + ${addedBytes}`,
        updatedAt: new Date(),
      })
      .where(eq(users.id, userId));
  }

  /**
   * Atomically decrements size from user's storageUsed.
   */
  static async decrementUsage(userId: string, freedBytes: number): Promise<void> {
    await db
      .update(users)
      .set({
        storageUsed: sql`GREATEST(0, ${users.storageUsed} - ${freedBytes})`,
        updatedAt: new Date(),
      })
      .where(eq(users.id, userId));
  }
}

