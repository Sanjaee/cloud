import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { users, sessions, passwordResetTokens, securityEvents } from '../../database/schema';
import { eq, and, gt } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin, hashToken } from '../../middleware/authentication';
import { APP_CONSTANTS } from '../../config/constants';

export const securityRoutes = new Elysia({ prefix: '/security' })
  .use(authPlugin)
  // 1. Change Password
  .post(
    '/change-password',
    async ({ currentUser, body, request }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { currentPassword, newPassword } = body;

      const user = await db.query.users.findFirst({
        where: eq(users.id, currentUser.id),
      });

      if (!user) throw new AppError(404, 'NOT_FOUND', 'User not found');

      if (user.passwordHash) {
        const isMatch = await Bun.password.verify(currentPassword, user.passwordHash);
        if (!isMatch) {
          throw new AppError(400, 'INVALID_PASSWORD', 'Current password is incorrect');
        }
      }

      const newPasswordHash = await Bun.password.hash(newPassword, {
        algorithm: 'argon2id',
        memoryCost: 19456,
        timeCost: 2,
      });

      await db
        .update(users)
        .set({ passwordHash: newPasswordHash, updatedAt: new Date() })
        .where(eq(users.id, currentUser.id));

      // Revoke all sessions
      await db.delete(sessions).where(eq(sessions.userId, currentUser.id));

      // Log security event
      await db.insert(securityEvents).values({
        userId: currentUser.id,
        eventType: 'password_changed',
        ipAddress: request.headers.get('x-forwarded-for') || '127.0.0.1',
        userAgent: request.headers.get('user-agent'),
      });

      return {
        success: true,
        message: 'Password changed successfully. Please log in again.',
      };
    },
    {
      body: t.Object({
        currentPassword: t.String(),
        newPassword: t.String({ minLength: 8 }),
      }),
      detail: { tags: ['Security'], summary: 'Change password with existing password confirmation' },
    }
  )

  // 2. Forgot Password (Anti-enumeration: always returns generic success)
  .post(
    '/forgot-password',
    async ({ body }) => {
      const { email } = body;
      const user = await db.query.users.findFirst({
        where: eq(users.email, email.toLowerCase()),
      });

      if (user) {
        const rawToken = crypto.randomUUID() + crypto.randomUUID();
        const tokenHash = await hashToken(rawToken);
        const expiresAt = new Date(Date.now() + APP_CONSTANTS.PASSWORD_RESET_EXPIRATION_HOURS * 3600 * 1000);

        await db.insert(passwordResetTokens).values({
          userId: user.id,
          tokenHash,
          expiresAt,
        });

        // In production email is dispatched, for testing token is logged or available
        console.log(`[Password Reset] Link for ${email}: /reset-password?token=${rawToken}`);
      }

      return {
        success: true,
        message: 'If an account exists with this email, password reset instructions have been sent.',
      };
    },
    {
      body: t.Object({
        email: t.String({ format: 'email' }),
      }),
      detail: { tags: ['Security'], summary: 'Request password reset email' },
    }
  )

  // 3. Reset Password
  .post(
    '/reset-password',
    async ({ body, request }) => {
      const { token, newPassword } = body;
      const tokenHash = await hashToken(token);

      const resetRecord = await db.query.passwordResetTokens.findFirst({
        where: and(
          eq(passwordResetTokens.tokenHash, tokenHash),
          gt(passwordResetTokens.expiresAt, new Date())
        ),
      });

      if (!resetRecord || resetRecord.usedAt) {
        throw new AppError(400, 'INVALID_TOKEN', 'Reset token is invalid or has expired');
      }

      const newPasswordHash = await Bun.password.hash(newPassword, {
        algorithm: 'argon2id',
        memoryCost: 19456,
        timeCost: 2,
      });

      await db
        .update(users)
        .set({ passwordHash: newPasswordHash, updatedAt: new Date() })
        .where(eq(users.id, resetRecord.userId));

      await db
        .update(passwordResetTokens)
        .set({ usedAt: new Date() })
        .where(eq(passwordResetTokens.id, resetRecord.id));

      // Revoke all existing sessions
      await db.delete(sessions).where(eq(sessions.userId, resetRecord.userId));

      // Log event
      await db.insert(securityEvents).values({
        userId: resetRecord.userId,
        eventType: 'password_reset_completed',
        ipAddress: request.headers.get('x-forwarded-for') || '127.0.0.1',
        userAgent: request.headers.get('user-agent'),
      });

      return {
        success: true,
        message: 'Password has been reset successfully. Please log in with your new password.',
      };
    },
    {
      body: t.Object({
        token: t.String(),
        newPassword: t.String({ minLength: 8 }),
      }),
      detail: { tags: ['Security'], summary: 'Reset password with token' },
    }
  )

  // 4. Security Events / Login History
  .get(
    '/events',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const events = await db.query.securityEvents.findMany({
        where: eq(securityEvents.userId, currentUser.id),
        orderBy: (e, { desc }) => [desc(e.createdAt)],
        limit: 50,
      });

      return {
        success: true,
        data: { events },
      };
    },
    {
      detail: { tags: ['Security'], summary: 'Get recent security events and login history' },
    }
  );

