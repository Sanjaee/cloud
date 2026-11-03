import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { mfaCredentials, users, securityEvents } from '../../database/schema';
import { eq } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { authPlugin, hashToken, matchBackupCode } from '../../middleware/authentication';
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
        'Zacode Cloud Storage',
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

      const rawCode = body?.code || body?.totpCode || '';
      const cleanCode = String(rawCode).replace(/[\s-]+/g, '').trim();

      if (!cleanCode) {
        throw new AppError(400, 'INVALID_CODE', 'Kode verifikasi TOTP harus diisi');
      }

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

      // Generate 10 backup recovery codes (formatted e.g. 62FA-EA8D)
      const rawBackupCodes: string[] = [];
      const hashedBackupCodes: string[] = [];

      for (let i = 0; i < 10; i++) {
        const p1 = crypto.randomUUID().substring(0, 4).toUpperCase();
        const p2 = crypto.randomUUID().substring(4, 8).toUpperCase();
        const formattedCode = `${p1}-${p2}`;
        rawBackupCodes.push(formattedCode);
        hashedBackupCodes.push(await hashToken(formattedCode));
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
        code: t.Optional(t.String()),
        totpCode: t.Optional(t.String()),
      }),
      detail: { tags: ['MFA'], summary: 'Confirm TOTP code and enable MFA' },
    }
  )

  // 4. Disable MFA
  .post(
    '/disable',
    async ({ currentUser, body, request }: any) => {
      if (!currentUser) throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');

      const { password } = body;
      const rawCode = body?.code || body?.totpCode || '';
      const cleanCode = rawCode ? String(rawCode).replace(/[\s-]+/g, '').trim() : '';

      const user = await db.query.users.findFirst({
        where: eq(users.id, currentUser.id),
      });

      if (!user) throw new AppError(404, 'NOT_FOUND', 'User not found');

      const mfa = await db.query.mfaCredentials.findFirst({
        where: eq(mfaCredentials.userId, currentUser.id),
      });

      if (!mfa || !mfa.isEnabled) {
        return {
          success: true,
          message: 'Two-Factor Authentication sudah tidak aktif',
        };
      }

      let isVerified = false;

      // 1. Verify TOTP or Backup Code if provided
      if (rawCode) {
        authenticator.options = { window: [2, 2] };
        if (/^\d{6}$/.test(cleanCode) && authenticator.check(cleanCode, mfa.secret.replace(/\s+/g, ''))) {
          isVerified = true;
        } else {
          // Check backup code
          const backupIndex = await matchBackupCode(rawCode, mfa.backupCodesHash);
          if (backupIndex !== -1) {
            isVerified = true;
          }
        }
      }

      // 2. Verify password if provided and user has local password
      if (!isVerified && password && user.passwordHash) {
        const isPassValid = await Bun.password.verify(password, user.passwordHash);
        if (isPassValid) {
          isVerified = true;
        }
      }

      if (!isVerified) {
        if (rawCode) {
          throw new AppError(400, 'INVALID_CODE', 'Kode TOTP atau Backup Code salah atau sudah kedaluwarsa');
        } else if (password) {
          throw new AppError(401, 'INVALID_PASSWORD', 'Password yang dimasukkan tidak sesuai');
        } else {
          throw new AppError(400, 'AUTH_REQUIRED', 'Masukkan kode TOTP dari ponsel, Backup Code, atau password akun Anda');
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

      return {
        success: true,
        message: 'Two-Factor Authentication berhasil dinonaktifkan',
      };
    },
    {
      body: t.Object({
        password: t.Optional(t.String()),
        code: t.Optional(t.String()),
        totpCode: t.Optional(t.String()),
      }),
      detail: { tags: ['MFA'], summary: 'Disable MFA with password, TOTP, or backup code confirmation' },
    }
  );
