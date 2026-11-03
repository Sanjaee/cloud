import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { mfaCredentials, users, securityEvents } from '../../database/schema';
import { eq } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin, hashToken } from '../../middleware/authentication';
import { authenticator } from 'otplib';
import QRCode from 'qrcode';

// Configure TOTP tolerance: window [2, 2] allows +/- 2 time steps (60s drift tolerance)
authenticator.options = { window: [2, 2] };

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

  // 2. Setup MFA: Generate or retrieve pending TOTP secret & QR code
  .post(
    '/setup',
    async ({ currentUser, body }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const existing = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      let secret: string;

      // If user already has an unverified pending secret and didn't explicitly ask to regenerate,
      // reuse it so they don't get mismatched tokens if they reload or re-open the wizard
      if (existing && !existing.isEnabled && existing.secret && !body?.regenerate) {
        secret = existing.secret;
      } else {
        secret = authenticator.generateSecret();
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
      }

      const otpauthUrl = authenticator.keyuri(
        currentUser.email,
        'REA Cloud Storage',
        secret
      );

      const qrCodeDataUrl = await QRCode.toDataURL(otpauthUrl);

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
      body: t.Optional(
        t.Object({
          regenerate: t.Optional(t.Boolean()),
        })
      ),
      detail: { tags: ['MFA'], summary: 'Generate TOTP secret and QR code for setup' },
    }
  )

  // 3. Verify & Enable MFA
  .post(
    '/enable',
    async ({ currentUser, body, request }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const rawCode = body?.code || '';
      const cleanCode = String(rawCode).replace(/[\s-]+/g, '').trim();

      if (!/^\d{6}$/.test(cleanCode)) {
        throw new AppError(400, 'INVALID_CODE', 'Kode verifikasi TOTP harus terdiri dari 6 digit angka');
      }

      const mfa = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      if (!mfa || !mfa.secret) {
        throw new AppError(400, 'MFA_NOT_INITIALIZED', 'Silakan klik Setup Two-Factor Authentication terlebih dahulu');
      }

      authenticator.options = { window: [2, 2] };
      const cleanSecret = mfa.secret.replace(/\s+/g, '');
      const isValid = authenticator.check(cleanCode, cleanSecret);

      if (!isValid) {
        throw new AppError(
          400,
          'INVALID_CODE',
          'Kode TOTP salah atau sudah kedaluwarsa. Pastikan jam/waktu pada ponsel Anda sinkron otomatis.'
        );
      }

      // Generate 10 backup recovery codes (formatted e.g. ABC1-XYZ2)
      const rawBackupCodes: string[] = [];
      const hashedBackupCodes: string[] = [];

      for (let i = 0; i < 10; i++) {
        const rawCode = `${crypto.randomUUID().substring(0, 4)}-${crypto.randomUUID().substring(4, 8)}`.toUpperCase();
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
        message: 'Two-Factor Authentication (TOTP) berhasil diaktifkan!',
        data: {
          backupCodes: rawBackupCodes,
        },
      };
    },
    {
      body: t.Object({
        code: t.String(),
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
      const cleanCode = code ? String(code).replace(/[\s-]+/g, '').trim() : '';

      const user = await db.query.users.findFirst({
        where: eq(users.id, currentUser.id),
      });

      if (!user) throw new AppError(404, 'NOT_FOUND', 'User not found');

      // If user has local password, verify password
      if (user.passwordHash) {
        if (!password) {
          throw new AppError(400, 'PASSWORD_REQUIRED', 'Password diperlukan untuk menonaktifkan MFA');
        }
        const isPassValid = await Bun.password.verify(password, user.passwordHash);
        if (!isPassValid) {
          throw new AppError(401, 'INVALID_PASSWORD', 'Password tidak sesuai');
        }
      }

      const mfa = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      if (mfa && mfa.isEnabled) {
        if (cleanCode) {
          authenticator.options = { window: [2, 2] };
          const isValid = authenticator.check(cleanCode, mfa.secret.replace(/\s+/g, ''));
          if (!isValid) {
            // Also check if it matches any hashed backup code
            const hashedCode = await hashToken(cleanCode.toUpperCase());
            const backupIndex = (mfa.backupCodesHash || []).indexOf(hashedCode);
            if (backupIndex === -1) {
              throw new AppError(400, 'INVALID_CODE', 'Kode TOTP atau Backup Code salah');
            }
          }
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
        message: 'Two-Factor Authentication berhasil dinonaktifkan',
      };
    },
    {
      body: t.Object({
        password: t.Optional(t.String()),
        code: t.Optional(t.String()),
      }),
      detail: { tags: ['MFA'], summary: 'Disable MFA with password and TOTP confirmation' },
    }
  );
