import { db } from '../../infrastructure/database/client';
import { users, userProfiles, sessions, oauthExchangeTickets, securityEvents, folders } from '../../database/schema';
import { eq, or, and, gt } from 'drizzle-orm';
import { env } from '../../config/env';
import { APP_CONSTANTS } from '../../config/constants';
import { AppError } from '../../middleware/error-handler';
import { hashToken } from '../../middleware/authentication';

export interface GoogleUserInfo {
  sub: string;
  email: string;
  email_verified: boolean;
  name: string;
  picture?: string;
  given_name?: string;
  family_name?: string;
}

export class GoogleOAuthService {
  /**
   * Builds the official Google OAuth 2.0 Authorization URL
   */
  static getAuthorizationUrl(
    redirectUri?: string,
    state?: string,
    loginHint?: string,
    prompt?: string
  ): string {
    const callback = redirectUri || env.GOOGLE_CALLBACK_URL;
    const params = new URLSearchParams({
      client_id: env.GOOGLE_CLIENT_ID,
      redirect_uri: callback,
      response_type: 'code',
      scope: 'openid email profile',
      access_type: 'offline',
      state: state || crypto.randomUUID(),
    });

    if (loginHint) {
      params.set('login_hint', loginHint);
    }

    // If explicit prompt is provided, use it (e.g. 'select_account' when switching account)
    // If loginHint is provided and no explicit prompt, omit prompt so Google auto-signs-in without showing account chooser
    if (prompt) {
      params.set('prompt', prompt);
    } else if (!loginHint) {
      params.set('prompt', 'select_account');
    }

    return `https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`;
  }

  /**
   * Exchanges authorization code for Google access token and ID token
   */
  static async exchangeCodeForTokens(code: string, redirectUri: string) {
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        code,
        client_id: env.GOOGLE_CLIENT_ID,
        client_secret: env.GOOGLE_CLIENT_SECRET,
        redirect_uri: redirectUri,
        grant_type: 'authorization_code',
      }),
    });

    if (!response.ok) {
      const errBody = await response.text();
      console.error('Google token exchange error:', errBody);
      throw new AppError(400, 'OAUTH_TOKEN_EXCHANGE_FAILED', 'Failed to exchange authorization code with Google');
    }

    const data = await response.json();
    return {
      accessToken: data.access_token as string,
      idToken: data.id_token as string,
      expiresIn: data.expires_in as number,
    };
  }

  /**
   * Fetches user profile from Google OpenID userinfo endpoint
   */
  static async fetchGoogleUserInfo(accessToken: string): Promise<GoogleUserInfo> {
    const response = await fetch('https://openidconnect.googleapis.com/v1/userinfo', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!response.ok) {
      throw new AppError(400, 'OAUTH_USERINFO_FAILED', 'Failed to fetch user information from Google');
    }

    const data = (await response.json()) as any;
    return {
      sub: data.sub,
      email: data.email,
      email_verified: Boolean(data.email_verified),
      name: data.name || data.email.split('@')[0],
      picture: data.picture,
      given_name: data.given_name,
      family_name: data.family_name,
    };
  }

  /**
   * Verifies Google ID token (for Mobile clients sending direct ID tokens)
   */
  static async verifyGoogleIdToken(idToken: string): Promise<GoogleUserInfo> {
    const response = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`);
    if (!response.ok) {
      throw new AppError(401, 'INVALID_GOOGLE_TOKEN', 'Google ID token verification failed');
    }

    const data = (await response.json()) as any;
    if (data.aud !== env.GOOGLE_CLIENT_ID) {
      throw new AppError(401, 'INVALID_AUDIENCE', 'Google ID token audience mismatch');
    }

    return {
      sub: data.sub,
      email: data.email,
      email_verified: data.email_verified === 'true' || Boolean(data.email_verified),
      name: data.name || data.email.split('@')[0],
      picture: data.picture,
      given_name: data.given_name,
      family_name: data.family_name,
    };
  }

  /**
   * Securely finds existing user or creates new user from verified Google profile
   */
  static async findOrCreateGoogleUser(googleUser: GoogleUserInfo) {
    if (!googleUser.email_verified) {
      throw new AppError(403, 'EMAIL_NOT_VERIFIED', 'Google account email is not verified by Google');
    }

    const email = googleUser.email.toLowerCase().trim();

    // 1. Search by Google Provider ID (Already linked)
    const existingByGoogleId = await db.query.users.findFirst({
      where: eq(users.googleId, googleUser.sub),
    });

    if (existingByGoogleId) {
      // Update last login
      await db
        .update(users)
        .set({
          lastLoginAt: new Date(),
          updatedAt: new Date(),
        })
        .where(eq(users.id, existingByGoogleId.id));

      // Update avatar if not present
      if (googleUser.picture) {
        const profile = await db.query.userProfiles.findFirst({
          where: eq(userProfiles.userId, existingByGoogleId.id),
        });
        if (profile && !profile.avatarUrl) {
          await db
            .update(userProfiles)
            .set({ avatarUrl: googleUser.picture, updatedAt: new Date() })
            .where(eq(userProfiles.id, profile.id));
        }
      }

      return existingByGoogleId;
    }

    // 2. Search by verified email (Account Linking)
    const existingByEmail = await db.query.users.findFirst({
      where: eq(users.email, email),
    });

    if (existingByEmail) {
      // Securely link Google account
      const [linkedUser] = await db
        .update(users)
        .set({
          googleId: googleUser.sub,
          authProvider: 'google',
          emailVerifiedAt: existingByEmail.emailVerifiedAt || new Date(),
          lastLoginAt: new Date(),
          updatedAt: new Date(),
        })
        .where(eq(users.id, existingByEmail.id))
        .returning();

      // Update profile avatar/display name if empty
      const profile = await db.query.userProfiles.findFirst({
        where: eq(userProfiles.userId, existingByEmail.id),
      });

      if (profile) {
        await db
          .update(userProfiles)
          .set({
            displayName: profile.displayName || googleUser.name,
            avatarUrl: profile.avatarUrl || googleUser.picture || null,
            updatedAt: new Date(),
          })
          .where(eq(userProfiles.id, profile.id));
      } else {
        await db.insert(userProfiles).values({
          userId: existingByEmail.id,
          displayName: googleUser.name,
          avatarUrl: googleUser.picture || null,
        });
      }

      return linkedUser;
    }

    // 3. New User Registration
    let baseUsername = (googleUser.name || email.split('@')[0])
      .toLowerCase()
      .replace(/[^a-z0-9_-]/g, '_')
      .replace(/_+/g, '_')
      .replace(/^_+|_+$/g, '')
      .slice(0, 24);

    if (baseUsername.length < 3) {
      baseUsername = `user_${crypto.randomUUID().slice(0, 8)}`;
    }

    // Check if username is already taken
    let finalUsername = baseUsername;
    const usernameTaken = await db.query.users.findFirst({
      where: eq(users.username, finalUsername),
    });

    if (usernameTaken) {
      finalUsername = `${baseUsername}_${Math.floor(1000 + Math.random() * 9000)}`;
    }

    // Insert new user
    const [newUser] = await db
      .insert(users)
      .values({
        email,
        username: finalUsername,
        googleId: googleUser.sub,
        authProvider: 'google',
        passwordHash: null,
        status: 'active',
        emailVerifiedAt: new Date(),
        storageQuota: 10 * 1024 * 1024 * 1024, // 10 GB
        storageUsed: 0,
        lastLoginAt: new Date(),
      })
      .returning();

    // Create user profile
    await db.insert(userProfiles).values({
      userId: newUser.id,
      displayName: googleUser.name || finalUsername,
      avatarUrl: googleUser.picture || null,
    });

    // Create initial root folders for user workspace
    try {
      await db.insert(folders).values([
        { ownerId: newUser.id, name: 'Documents', color: '#3B82F6' },
        { ownerId: newUser.id, name: 'Photos', color: '#10B981' },
        { ownerId: newUser.id, name: 'Projects', color: '#8B5CF6' },
      ]);
    } catch (e) {
      console.warn('Initial folder creation skipped:', e);
    }

    return newUser;
  }

  /**
   * Creates server session and cross-domain exchange ticket
   */
  static async createSessionAndTicket(
    userId: string,
    request: Request,
    cookie: any
  ) {
    const rawSessionToken = crypto.randomUUID() + crypto.randomUUID();
    const sessionTokenHash = await hashToken(rawSessionToken);
    const sessionExpiresAt = new Date(
      Date.now() + APP_CONSTANTS.SESSION_DURATION_DAYS * 24 * 3600 * 1000
    );

    const ipAddress = request.headers.get('x-forwarded-for') || '127.0.0.1';
    const userAgent = request.headers.get('user-agent') || 'Google-OAuth';

    const [newSession] = await db
      .insert(sessions)
      .values({
        userId,
        tokenHash: sessionTokenHash,
        ipAddress,
        userAgent,
        expiresAt: sessionExpiresAt,
      })
      .returning();

    // Set HttpOnly cookie on current response
    if (cookie && cookie[APP_CONSTANTS.SESSION_COOKIE_NAME]?.set) {
      cookie[APP_CONSTANTS.SESSION_COOKIE_NAME].set({
        value: rawSessionToken,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        expires: sessionExpiresAt,
      });
    }

    // Create single-use 2-minute ticket for cross-origin or proxy handover
    const rawTicket = `${crypto.randomUUID().replace(/-/g, '')}${crypto.randomUUID().replace(/-/g, '')}`;
    const ticketHash = await hashToken(rawTicket);
    const ticketExpiresAt = new Date(Date.now() + 120 * 1000); // 120 seconds

    await db.insert(oauthExchangeTickets).values({
      userId,
      ticketHash,
      sessionToken: rawSessionToken,
      expiresAt: ticketExpiresAt,
    });

    // Log security event
    await db.insert(securityEvents).values({
      userId,
      eventType: 'google_oauth_login_success',
      ipAddress,
      userAgent,
      metadata: { sessionId: newSession.id },
    });

    return {
      session: newSession,
      rawSessionToken,
      sessionExpiresAt,
      ticket: rawTicket,
    };
  }

  /**
   * Exchanges a one-time ticket for a session cookie
   */
  static async exchangeTicketForSession(ticket: string, cookie: any) {
    const hashed = await hashToken(ticket);

    const ticketRecord = await db.query.oauthExchangeTickets.findFirst({
      where: and(
        eq(oauthExchangeTickets.ticketHash, hashed),
        gt(oauthExchangeTickets.expiresAt, new Date())
      ),
    });

    if (!ticketRecord || ticketRecord.usedAt) {
      throw new AppError(400, 'INVALID_TICKET', 'Authentication ticket is invalid or has expired');
    }

    // Mark ticket as used immediately (single-use)
    await db
      .update(oauthExchangeTickets)
      .set({ usedAt: new Date() })
      .where(eq(oauthExchangeTickets.id, ticketRecord.id));

    // Look up user
    const user = await db.query.users.findFirst({
      where: eq(users.id, ticketRecord.userId),
    });

    if (!user || user.status === 'suspended') {
      throw new AppError(401, 'USER_INACTIVE', 'User account is inactive');
    }

    const sessionExpiresAt = new Date(
      Date.now() + APP_CONSTANTS.SESSION_DURATION_DAYS * 24 * 3600 * 1000
    );

    // Set HttpOnly cookie
    if (cookie && cookie[APP_CONSTANTS.SESSION_COOKIE_NAME]?.set) {
      cookie[APP_CONSTANTS.SESSION_COOKIE_NAME].set({
        value: ticketRecord.sessionToken,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        expires: sessionExpiresAt,
      });
    }

    return {
      user: {
        id: user.id,
        email: user.email,
        username: user.username,
        status: user.status,
        storageQuota: user.storageQuota,
        storageUsed: user.storageUsed,
      },
    };
  }
}
