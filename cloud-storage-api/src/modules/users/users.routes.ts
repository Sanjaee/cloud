import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { users, userProfiles, files, folders, shortcuts, notifications } from '../../database/schema';
import { eq, or, ilike, and, ne } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin, requireAuth } from '../../middleware/authentication';
import { QuotaEngine } from '../../services/quota-engine';
import { APP_CONSTANTS } from '../../config/constants';

export const userRoutes = new Elysia({ prefix: '/users' })
  .use(authPlugin)
  // 1. Get current user profile & storage overview
  .get(
    '/me',
    async ({ currentUser }: any) => {
      if (!currentUser) {
        throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');
      }

      const profile = await db.query.userProfiles.findFirst({
        where: eq(userProfiles.userId, currentUser.id),
      });

      const quota = await QuotaEngine.checkQuota(currentUser.id, 0);

      return {
        success: true,
        data: {
          user: {
            id: currentUser.id,
            email: currentUser.email,
            username: currentUser.username,
            status: currentUser.status,
            emailVerifiedAt: currentUser.emailVerifiedAt,
            profile: profile || null,
            storage: {
              quotaBytes: quota.quotaBytes,
              usedBytes: quota.usedBytes,
              availableBytes: quota.availableBytes,
              percentage: quota.percentage,
            },
          },
        },
      };
    },
    {
      detail: { tags: ['Users'], summary: 'Get current user profile and storage quota' },
    }
  )

  // 2. Update profile
  .patch(
    '/profile',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { displayName, bio, avatarUrl } = body;

      const [updated] = await db
        .update(userProfiles)
        .set({
          ...(displayName !== undefined ? { displayName } : {}),
          ...(bio !== undefined ? { bio } : {}),
          ...(avatarUrl !== undefined ? { avatarUrl } : {}),
          updatedAt: new Date(),
        })
        .where(eq(userProfiles.userId, currentUser.id))
        .returning();

      return {
        success: true,
        message: 'Profile updated successfully',
        data: { profile: updated },
      };
    },
    {
      body: t.Object({
        displayName: t.Optional(t.String({ maxLength: 128 })),
        bio: t.Optional(t.String({ maxLength: 500 })),
        avatarUrl: t.Optional(t.String()),
      }),
      detail: { tags: ['Users'], summary: 'Update profile details' },
    }
  )

  // 3. Search users (for native sharing dialog autocomplete)
  .get(
    '/search',
    async ({ currentUser, query }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const q = (query.q || '').trim();
      if (q.length < 2) {
        return { success: true, data: { users: [] } };
      }

      const matches = await db.query.users.findMany({
        where: and(
          ne(users.id, currentUser.id), // do not include self
          or(ilike(users.email, `%${q}%`), ilike(users.username, `%${q}%`))
        ),
        columns: {
          id: true,
          email: true,
          username: true,
        },
        limit: 10,
      });

      return {
        success: true,
        data: { users: matches },
      };
    },
    {
      query: t.Object({
        q: t.String(),
      }),
      detail: { tags: ['Users'], summary: 'Search users by email or username for sharing' },
    }
  )

  // 4. Delete account
  .delete(
    '/me',
    async ({ currentUser, body, cookie }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const user = await db.query.users.findFirst({
        where: eq(users.id, currentUser.id),
      });

      if (!user) throw new AppError(404, 'NOT_FOUND', 'User not found');

      if (user.passwordHash) {
        const isPasswordValid = await Bun.password.verify(body.password, user.passwordHash);
        if (!isPasswordValid) {
          throw new AppError(401, 'INVALID_PASSWORD', 'Password confirmation failed');
        }
      }

      // Delete user cascades to userProfiles, sessions, shortcuts, etc.
      await db.delete(users).where(eq(users.id, currentUser.id));
      if (cookie[APP_CONSTANTS.SESSION_COOKIE_NAME]?.remove) {
        cookie[APP_CONSTANTS.SESSION_COOKIE_NAME].remove();
      }
      if (cookie['rea_session']?.remove) {
        cookie['rea_session'].remove();
      }

      return {
        success: true,
        message: 'Account successfully deleted',
      };
    },
    {
      body: t.Object({
        password: t.String(),
      }),
      detail: { tags: ['Users'], summary: 'Delete user account with password confirmation' },
    }
  );

