import { Elysia, t } from 'elysia';
import { db } from '../../infrastructure/database/client';
import { users, userProfiles, sessions, emailVerificationTokens, refreshTokens, mfaCredentials, securityEvents } from '../../database/schema';
import { eq, and, gt } from 'drizzle-orm';
import { AppError } from '../../middleware/error-handler';
import { hashToken, authPlugin } from '../../middleware/authentication';
import { APP_CONSTANTS } from '../../config/constants';
import { authenticator } from 'otplib';
import { GoogleOAuthService } from './google-oauth.service';
import { env } from '../../config/env';

export const authRoutes = new Elysia({ prefix: '/auth' })
  .use(authPlugin)

  // 0a. Initiate Google OAuth Redirect
  .get(
    '/google',
    async ({ cookie, redirect, query }) => {
      const state = crypto.randomUUID();

      cookie['oauth_state'].set({
        value: state,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        maxAge: 600, // 10 minutes
      });

      const callbackUrl = query.redirect_uri || env.GOOGLE_CALLBACK_URL;
      const authUrl = GoogleOAuthService.getAuthorizationUrl(callbackUrl, state);
      return redirect(authUrl);
    },
    {
      query: t.Object({
        redirect_uri: t.Optional(t.String()),
      }),
      detail: { tags: ['Google OAuth'], summary: 'Redirect to Google OAuth 2.0 authorization page' },
    }
  )

  // 0b. Get Google OAuth URL for frontend SPA
  .get(
    '/google/url',
    async ({ cookie, query }) => {
      const state = crypto.randomUUID();

      cookie['oauth_state'].set({
        value: state,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        maxAge: 600,
      });

      const callbackUrl = query.redirect_uri || env.GOOGLE_CALLBACK_URL;
      const authUrl = GoogleOAuthService.getAuthorizationUrl(callbackUrl, state);

      return {
        success: true,
        data: {
          url: authUrl,
          state,
        },
      };
    },
    {
      query: t.Object({
        redirect_uri: t.Optional(t.String()),
      }),
      detail: { tags: ['Google OAuth'], summary: 'Get Google OAuth 2.0 authorization URL' },
    }
  )

  // 0c. Google OAuth 2.0 Callback
  .get(
    '/google/callback',
    async ({ query, cookie, redirect, request }) => {
      // Resolve absolute frontend URL for OAuth redirection
      const validOrigins = (env.WEB_ORIGIN || '')
        .split(',')
        .map((s) => s.trim())
        .filter((s) => s.startsWith('http://') || s.startsWith('https://'));
      const frontendUrl =
        env.FRONTEND_URL && env.FRONTEND_URL.startsWith('http')
          ? env.FRONTEND_URL
          : validOrigins[0] || 'https://cloud-storage-web-seven.vercel.app';

      // Check if user cancelled or Google returned error
      if (query.error) {
        const errorDesc = query.error_description || query.error;
        return redirect(`${frontendUrl}/login?error=${encodeURIComponent(errorDesc)}`);
      }

      const { code, state } = query;
      if (!code || !state) {
        return redirect(`${frontendUrl}/login?error=${encodeURIComponent('Invalid OAuth callback response from Google')}`);
      }

      // Validate CSRF state
      const savedState = cookie['oauth_state']?.value;
      if (savedState && savedState !== state) {
        return redirect(`${frontendUrl}/login?error=${encodeURIComponent('Security verification failed: OAuth state mismatch')}`);
      }
      cookie['oauth_state']?.remove();

      try {
        const callbackUrl = env.GOOGLE_CALLBACK_URL;
        const tokens = await GoogleOAuthService.exchangeCodeForTokens(code, callbackUrl);
        const googleUser = await GoogleOAuthService.fetchGoogleUserInfo(tokens.accessToken);
        const user = await GoogleOAuthService.findOrCreateGoogleUser(googleUser);

        const { ticket } = await GoogleOAuthService.createSessionAndTicket(user.id, request, cookie);

        // Redirect to dashboard with ticket for automatic session handover
        return redirect(`${frontendUrl}/drive?ticket=${ticket}`);
      } catch (err: any) {
        console.error('Google OAuth callback error:', err);
        return redirect(`${frontendUrl}/login?error=${encodeURIComponent(err.message || 'Google authentication failed')}`);
      }
    },
    {
      query: t.Object({
        code: t.Optional(t.String()),
        state: t.Optional(t.String()),
        error: t.Optional(t.String()),
        error_description: t.Optional(t.String()),
      }),
      detail: { tags: ['Google OAuth'], summary: 'Google OAuth 2.0 callback endpoint' },
    }
  )

  // 0d. Exchange Ticket for Session Cookie (SPA & Cross-Domain Handover)
  .post(
    '/google/exchange-ticket',
    async ({ body, cookie }) => {
      const { ticket } = body;
      const result = await GoogleOAuthService.exchangeTicketForSession(ticket, cookie);
      return {
        success: true,
        message: 'Session established successfully',
        data: result,
      };
    },
    {
      body: t.Object({
        ticket: t.String({ minLength: 10 }),
      }),
      detail: { tags: ['Google OAuth'], summary: 'Exchange single-use ticket for session cookie' },
    }
  )

  // 0e. Mobile Direct Token Sign-In
  .post(
    '/google/token',
    async ({ body, jwt, request }) => {
      const { idToken, code, redirectUri, deviceId } = body;

      let googleUser: any;
      if (idToken) {
        googleUser = await GoogleOAuthService.verifyGoogleIdToken(idToken);
      } else if (code) {
        const callbackUrl = redirectUri || env.GOOGLE_CALLBACK_URL;
        const tokens = await GoogleOAuthService.exchangeCodeForTokens(code, callbackUrl);
        googleUser = await GoogleOAuthService.fetchGoogleUserInfo(tokens.accessToken);
      } else {
        throw new AppError(400, 'INVALID_REQUEST', 'Either idToken or code must be provided');
      }

      const user = await GoogleOAuthService.findOrCreateGoogleUser(googleUser);

      const accessToken = await jwt.sign({
        sub: user.id,
        email: user.email,
        username: user.username,
        exp: Math.floor(Date.now() / 1000) + APP_CONSTANTS.ACCESS_TOKEN_EXPIRATION_SEC,
      });

      const rawRefreshToken = crypto.randomUUID() + crypto.randomUUID();
      const refreshTokenHash = await hashToken(rawRefreshToken);
      const refreshExpiresAt = new Date(
        Date.now() + APP_CONSTANTS.REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 3600 * 1000
      );

      await db.insert(refreshTokens).values({
        userId: user.id,
        tokenHash: refreshTokenHash,
        familyId: crypto.randomUUID(),
        deviceId: deviceId || 'mobile-device',
        expiresAt: refreshExpiresAt,
      });

      return {
        success: true,
        message: 'Google authentication successful',
        data: {
          user: {
            id: user.id,
            email: user.email,
            username: user.username,
            status: user.status,
            storageQuota: user.storageQuota,
            storageUsed: user.storageUsed,
          },
          tokens: {
            accessToken,
            refreshToken: rawRefreshToken,
            expiresIn: APP_CONSTANTS.ACCESS_TOKEN_EXPIRATION_SEC,
          },
        },
      };
    },
    {
      body: t.Object({
        idToken: t.Optional(t.String()),
        code: t.Optional(t.String()),
        redirectUri: t.Optional(t.String()),
        deviceId: t.Optional(t.String()),
      }),
      detail: { tags: ['Google OAuth'], summary: 'Mobile direct token/code authentication' },
    }
  )

  // 0f. Get current authenticated user profile
  .get(
    '/me',
    async ({ currentUser }: any) => {
      if (!currentUser) {
        throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');
      }
      const profile = await db.query.userProfiles.findFirst({
        where: eq(userProfiles.userId, currentUser.id),
      });

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
            storageQuota: currentUser.storageQuota,
            storageUsed: currentUser.storageUsed,
          },
        },
      };
    },
    {
      detail: { tags: ['Authentication'], summary: 'Get current authenticated user profile' },
    }
  )
  // 1. Register
  .post(
    '/register',
    async ({ body, set, request }) => {
      const { email, username, password } = body;

      // Check existing email or username
      const existing = await db.query.users.findFirst({
        where: (u, { or, eq }) => or(eq(u.email, email.toLowerCase()), eq(u.username, username)),
      });

      if (existing) {
        throw new AppError(409, 'USER_EXISTS', 'A user with this email or username already exists');
      }

      // Hash password with Argon2id
      const passwordHash = await Bun.password.hash(password, {
        algorithm: 'argon2id',
        memoryCost: 19456,
        timeCost: 2,
      });

      // Insert User
      const [newUser] = await db
        .insert(users)
        .values({
          email: email.toLowerCase(),
          username,
          passwordHash,
          status: 'active',
          emailVerifiedAt: new Date(), // verified for seamless access, verification token also issued
        })
        .returning();

      // Create Profile
      await db.insert(userProfiles).values({
        userId: newUser.id,
        displayName: username,
      });

      // Generate Verification Token
      const rawVerifToken = crypto.randomUUID() + crypto.randomUUID();
      const tokenHash = await hashToken(rawVerifToken);
      const expiresAt = new Date(Date.now() + APP_CONSTANTS.EMAIL_VERIFICATION_EXPIRATION_HOURS * 3600 * 1000);

      await db.insert(emailVerificationTokens).values({
        userId: newUser.id,
        tokenHash,
        expiresAt,
      });

      // Log Security Event
      await db.insert(securityEvents).values({
        userId: newUser.id,
        eventType: 'user_registered',
        ipAddress: request.headers.get('x-forwarded-for') || '127.0.0.1',
        userAgent: request.headers.get('user-agent'),
      });

      set.status = 201;
      return {
        success: true,
        message: 'Registration successful',
        data: {
          user: {
            id: newUser.id,
            email: newUser.email,
            username: newUser.username,
          },
          verificationToken: rawVerifToken, // in prod sent via email
        },
      };
    },
    {
      body: t.Object({
        email: t.String({ format: 'email' }),
        username: t.String({ minLength: 3, maxLength: 64 }),
        password: t.String({ minLength: 8 }),
      }),
      detail: { tags: ['Authentication'], summary: 'Register a new user account' },
    }
  )

  // 2. Login (Supports both Web Cookie and Mobile Bearer Tokens)
  .post(
    '/login',
    async ({ body, cookie, jwt, request }) => {
      const { email, password, mfaCode, deviceId, deviceName } = body;

      const user = await db.query.users.findFirst({
        where: eq(users.email, email.toLowerCase()),
      });

      if (!user) {
        throw new AppError(401, 'INVALID_CREDENTIALS', 'Invalid email or password');
      }

      if (!user.passwordHash) {
        throw new AppError(401, 'OAUTH_ACCOUNT', 'This account uses Google Sign-In. Please sign in with Google.');
      }

      // Verify Argon2id password
      const passwordMatch = await Bun.password.verify(password, user.passwordHash);
      if (!passwordMatch) {
        // Record failed attempt
        await db.insert(securityEvents).values({
          userId: user.id,
          eventType: 'login_failed',
          ipAddress: request.headers.get('x-forwarded-for') || '127.0.0.1',
          userAgent: request.headers.get('user-agent'),
        });
        throw new AppError(401, 'INVALID_CREDENTIALS', 'Invalid email or password');
      }

      // Check MFA if enabled
      const mfa = await db.query.mfaCredentials.findFirst({
        where: and(eq(mfaCredentials.userId, user.id), eq(mfaCredentials.isEnabled, true)),
      });

      if (mfa) {
        if (!mfaCode) {
          return {
            success: true,
            mfaRequired: true,
            message: 'MFA code is required to complete authentication',
          };
        }

        const isValidTotp = authenticator.check(mfaCode, mfa.secret);
        if (!isValidTotp) {
          throw new AppError(401, 'INVALID_MFA_CODE', 'Invalid or expired TOTP code');
        }
      }

      // 1. Create Server Session for Web
      const rawSessionToken = crypto.randomUUID() + crypto.randomUUID();
      const sessionTokenHash = await hashToken(rawSessionToken);
      const sessionExpiresAt = new Date(
        Date.now() + APP_CONSTANTS.SESSION_DURATION_DAYS * 24 * 3600 * 1000
      );

      const ipAddress = request.headers.get('x-forwarded-for') || '127.0.0.1';
      const userAgent = request.headers.get('user-agent') || 'Unknown';

      const [newSession] = await db
        .insert(sessions)
        .values({
          userId: user.id,
          tokenHash: sessionTokenHash,
          ipAddress,
          userAgent,
          expiresAt: sessionExpiresAt,
        })
        .returning();

      // Set HttpOnly cookie for Web
      cookie[APP_CONSTANTS.SESSION_COOKIE_NAME].set({
        value: rawSessionToken,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        expires: sessionExpiresAt,
      });

      // 2. Generate Access JWT & Refresh Token for Mobile / API clients
      const accessToken = await jwt.sign({
        sub: user.id,
        email: user.email,
        username: user.username,
        exp: Math.floor(Date.now() / 1000) + APP_CONSTANTS.ACCESS_TOKEN_EXPIRATION_SEC,
      });

      const rawRefreshToken = crypto.randomUUID() + crypto.randomUUID();
      const refreshTokenHash = await hashToken(rawRefreshToken);
      const refreshExpiresAt = new Date(
        Date.now() + APP_CONSTANTS.REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 3600 * 1000
      );

      await db.insert(refreshTokens).values({
        userId: user.id,
        tokenHash: refreshTokenHash,
        familyId: crypto.randomUUID(),
        deviceId: deviceId || 'web-browser',
        expiresAt: refreshExpiresAt,
      });

      // Log login success
      await db.insert(securityEvents).values({
        userId: user.id,
        eventType: 'login_success',
        ipAddress,
        userAgent,
        metadata: { sessionId: newSession.id, deviceId, deviceName },
      });

      return {
        success: true,
        message: 'Login successful',
        data: {
          user: {
            id: user.id,
            email: user.email,
            username: user.username,
            status: user.status,
            storageQuota: user.storageQuota,
            storageUsed: user.storageUsed,
          },
          tokens: {
            accessToken,
            refreshToken: rawRefreshToken,
            expiresIn: APP_CONSTANTS.ACCESS_TOKEN_EXPIRATION_SEC,
          },
        },
      };
    },
    {
      body: t.Object({
        email: t.String({ format: 'email' }),
        password: t.String(),
        mfaCode: t.Optional(t.String()),
        deviceId: t.Optional(t.String()),
        deviceName: t.Optional(t.String()),
      }),
      detail: { tags: ['Authentication'], summary: 'Log in with credentials' },
    }
  )

  // 3. Mobile Refresh Token Rotation
  .post(
    '/refresh',
    async ({ body, jwt }) => {
      const { refreshToken } = body;
      const hashed = await hashToken(refreshToken);

      const tokenRecord = await db.query.refreshTokens.findFirst({
        where: eq(refreshTokens.tokenHash, hashed),
      });

      if (!tokenRecord) {
        throw new AppError(401, 'INVALID_REFRESH_TOKEN', 'Refresh token is invalid');
      }

      // Reuse detection: if token is already revoked, revoke the entire family!
      if (tokenRecord.isRevoked || tokenRecord.expiresAt < new Date()) {
        await db
          .update(refreshTokens)
          .set({ isRevoked: true })
          .where(eq(refreshTokens.familyId, tokenRecord.familyId));
        throw new AppError(401, 'TOKEN_REUSE_DETECTED', 'Security alert: Refresh token reuse detected');
      }

      // Revoke current token in rotation
      await db
        .update(refreshTokens)
        .set({ isRevoked: true })
        .where(eq(refreshTokens.id, tokenRecord.id));

      const user = await db.query.users.findFirst({
        where: eq(users.id, tokenRecord.userId),
      });

      if (!user || user.status === 'suspended') {
        throw new AppError(401, 'USER_INACTIVE', 'User account is inactive');
      }

      // Issue new access JWT and new rotating refresh token
      const newAccessToken = await jwt.sign({
        sub: user.id,
        email: user.email,
        username: user.username,
        exp: Math.floor(Date.now() / 1000) + APP_CONSTANTS.ACCESS_TOKEN_EXPIRATION_SEC,
      });

      const newRawRefreshToken = crypto.randomUUID() + crypto.randomUUID();
      const newRefreshTokenHash = await hashToken(newRawRefreshToken);
      const refreshExpiresAt = new Date(
        Date.now() + APP_CONSTANTS.REFRESH_TOKEN_EXPIRATION_DAYS * 24 * 3600 * 1000
      );

      await db.insert(refreshTokens).values({
        userId: user.id,
        tokenHash: newRefreshTokenHash,
        familyId: tokenRecord.familyId,
        deviceId: tokenRecord.deviceId,
        expiresAt: refreshExpiresAt,
      });

      return {
        success: true,
        data: {
          accessToken: newAccessToken,
          refreshToken: newRawRefreshToken,
          expiresIn: APP_CONSTANTS.ACCESS_TOKEN_EXPIRATION_SEC,
        },
      };
    },
    {
      body: t.Object({
        refreshToken: t.String(),
      }),
      detail: { tags: ['Authentication'], summary: 'Rotate refresh token (Mobile)' },
    }
  )

  // 4. Logout (Current Device)
  .post(
    '/logout',
    async ({ cookie, currentSessionId, currentUser }) => {
      if (currentSessionId) {
        await db.delete(sessions).where(eq(sessions.id, currentSessionId));
      }
      cookie[APP_CONSTANTS.SESSION_COOKIE_NAME].remove();

      return {
        success: true,
        message: 'Successfully logged out',
      };
    },
    {
      detail: { tags: ['Authentication'], summary: 'Log out current session' },
    }
  )

  // 5. Logout All Devices
  .post(
    '/logout-all',
    async ({ currentUser, cookie }) => {
      if (!currentUser) {
        throw new AppError(401, 'UNAUTHORIZED', 'Authentication required');
      }

      // Revoke all sessions and all refresh tokens for this user
      await db.delete(sessions).where(eq(sessions.userId, currentUser.id));
      await db
        .update(refreshTokens)
        .set({ isRevoked: true })
        .where(eq(refreshTokens.userId, currentUser.id));

      cookie[APP_CONSTANTS.SESSION_COOKIE_NAME].remove();

      return {
        success: true,
        message: 'Logged out from all devices',
      };
    },
    {
      detail: { tags: ['Authentication'], summary: 'Log out all devices and revoke tokens' },
    }
  )

  // 6. Verify Email
  .post(
    '/verify-email',
    async ({ body }) => {
      const { token } = body;
      const hashed = await hashToken(token);

      const verif = await db.query.emailVerificationTokens.findFirst({
        where: and(
          eq(emailVerificationTokens.tokenHash, hashed),
          gt(emailVerificationTokens.expiresAt, new Date())
        ),
      });

      if (!verif || verif.usedAt) {
        throw new AppError(400, 'INVALID_TOKEN', 'Verification token is invalid or has expired');
      }

      await db
        .update(users)
        .set({ emailVerifiedAt: new Date(), updatedAt: new Date() })
        .where(eq(users.id, verif.userId));

      await db
        .update(emailVerificationTokens)
        .set({ usedAt: new Date() })
        .where(eq(emailVerificationTokens.id, verif.id));

      return {
        success: true,
        message: 'Email successfully verified',
      };
    },
    {
      body: t.Object({
        token: t.String(),
      }),
      detail: { tags: ['Authentication'], summary: 'Verify email with single-use token' },
    }
  );
