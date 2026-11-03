import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { mfaCredentials, users, securityEvents } from '../../database/schema';
import { eq } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin, hashToken } from '../../middleware/authentication';
import { authenticator } from 'otplib';
import QRCode from 'qrcode';

export const mfaRoutes = new Elysia({ prefix: '/mfa' })
  .use(authPlugin)
  // 1. Get MFA Status
  .get(
    '/status',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const mfa = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      return {
        success: true,
        data: {
          isEnabled: mfa?.isEnabled ?? false,
        },
      };
    },
    {
      detail: { tags: ['MFA'], summary: 'Check if MFA is enabled' },
    }
  )

  // 2. Setup MFA: Generate TOTP secret & QR code
  .post(
    '/setup',
    async ({ currentUser }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const secret = authenticator.generateSecret();
      const otpauthUrl = authenticator.keyuri(
        currentUser.email,
        'REA Cloud Storage',
        secret
      );

      const qrCodeDataUrl = await QRCode.toDataURL(otpauthUrl);

      // Save secret (not yet enabled until verified)
      const existing = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      if (existing) {
        await db
          .update(mfaCredentials)
          .set({ secret, isEnabled: false, updatedAt: new Date() })
          .where(eq(mfaCredentials.userId, currentUser.id));
      } else {
        await db.insert(mfaCredentials).values({
          userId: currentUser.id,
          secret,
          isEnabled: false,
        });
      }

      return {
        success: true,
        data: {
          secret,
          qrCodeDataUrl,
          otpauthUrl,
        },
      };
    },
    {
      detail: { tags: ['MFA'], summary: 'Generate TOTP secret and QR code for setup' },
    }
  )

  // 3. Verify & Enable MFA
  .post(
    '/enable',
    async ({ currentUser, body, request }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { code } = body;
      const mfa = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      if (!mfa || !mfa.secret) {
        throw new AppError(400, 'MFA_NOT_INITIALIZED', 'Please run MFA setup first');
      }

      const isValid = authenticator.check(code, mfa.secret);
      if (!isValid) {
        throw new AppError(400, 'INVALID_CODE', 'The 6-digit TOTP code is incorrect');
      }

      // Generate 10 backup codes
      const rawBackupCodes: string[] = [];
      const hashedBackupCodes: string[] = [];

      for (let i = 0; i < 10; i++) {
        const rawCode = crypto.randomUUID().substring(0, 8).toUpperCase();
        rawBackupCodes.push(rawCode);
        hashedBackupCodes.push(await hashToken(rawCode));
      }

      await db
        .update(mfaCredentials)
        .set({
          isEnabled: true,
          backupCodesHash: hashedBackupCodes,
          updatedAt: new Date(),
        })
        .where(eq(mfaCredentials.userId, currentUser.id));

      await db.insert(securityEvents).values({
        userId: currentUser.id,
        eventType: 'mfa_enabled',
        ipAddress: request.headers.get('x-forwarded-for') || '127.0.0.1',
        userAgent: request.headers.get('user-agent'),
      });

      return {
        success: true,
        message: 'MFA successfully enabled',
        data: {
          backupCodes: rawBackupCodes, // Presented once to user
        },
      };
    },
    {
      body: t.Object({
        code: t.String({ minLength: 6, maxLength: 6 }),
      }),
      detail: { tags: ['MFA'], summary: 'Confirm TOTP code and enable MFA' },
    }
  )

  // 4. Disable MFA
  .post(
    '/disable',
    async ({ currentUser, body, request }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { password, code } = body;

      const user = await db.query.users.findFirst({
        where: eq(users.id, currentUser.id),
      });

      if (!user) throw new AppError(404, 'NOT_FOUND', 'User not found');

      if (user.passwordHash) {
        const isPassValid = await Bun.password.verify(password, user.passwordHash);
        if (!isPassValid) {
          throw new AppError(401, 'INVALID_PASSWORD', 'Password is incorrect');
        }
      }

      const mfa = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      if (mfa && mfa.isEnabled) {
        const isValid = authenticator.check(code, mfa.secret);
        if (!isValid) {
          throw new AppError(400, 'INVALID_CODE', 'TOTP code is incorrect');
        }

        await db
          .update(mfaCredentials)
          .set({ isEnabled: false, updatedAt: new Date() })
          .where(eq(mfaCredentials.userId, currentUser.id));

        await db.insert(securityEvents).values({
          userId: currentUser.id,
          eventType: 'mfa_disabled',
          ipAddress: request.headers.get('x-forwarded-for') || '127.0.0.1',
          userAgent: request.headers.get('user-agent'),
        });
      }

      return {
        success: true,
        message: 'MFA has been disabled',
      };
    },
    {
      body: t.Object({
        password: t.String(),
        code: t.String({ minLength: 6, maxLength: 6 }),
      }),
      detail: { tags: ['MFA'], summary: 'Disable MFA with password and TOTP confirmation' },
    }
  );
