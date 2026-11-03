import { describe, it, expect } from 'bun:test';
import { GoogleOAuthService } from '../src/modules/auth/google-oauth.service';
import { db } from '../src/infrastructure/database/client';
import { users } from '../src/database/schema';
import { eq } from 'drizzle-orm';

describe('Google OAuth Service', () => {
  it('generates a valid Google OAuth URL', () => {
    const url = GoogleOAuthService.getAuthorizationUrl(
      'https://cloud-storage-api-tau.vercel.app/api/v1/auth/google/callback',
      'test-state-123'
    );
    expect(url).toContain('https://accounts.google.com/o/oauth2/v2/auth');
    expect(url).toContain('client_id=');
    expect(url).toContain('response_type=code');
    expect(url).toContain('scope=openid+email+profile');
    expect(url).toContain('state=test-state-123');
  });

  it('verifies account linking logic for existing users', async () => {
    const testGoogleUser = {
      sub: 'google_sub_unit_test_' + Date.now(),
      email: 'unit_test_oauth_' + Date.now() + '@example.com',
      email_verified: true,
      name: 'OAuth Unit Tester',
      picture: 'https://lh3.googleusercontent.com/avatar_test.jpg',
    };

    // First creation
    const created = await GoogleOAuthService.findOrCreateGoogleUser(testGoogleUser);
    expect(created.email).toBe(testGoogleUser.email);
    expect(created.googleId).toBe(testGoogleUser.sub);
    expect(created.authProvider).toBe('google');

    // Second call with same sub -> must not duplicate, returns same user
    const existing = await GoogleOAuthService.findOrCreateGoogleUser(testGoogleUser);
    expect(existing.id).toBe(created.id);
  });

  it('rejects Google users whose email is not verified', async () => {
    const unverifiedUser = {
      sub: 'unverified_sub_' + Date.now(),
      email: 'unverified_' + Date.now() + '@example.com',
      email_verified: false,
      name: 'Unverified Tester',
    };

    let threw = false;
    try {
      await GoogleOAuthService.findOrCreateGoogleUser(unverifiedUser);
    } catch {
      threw = true;
    }
    expect(threw).toBe(true);
  });

  it('links an existing email-registered account with Google when email matches', async () => {
    const email = 'link_test_' + Date.now() + '@example.com';
    const username = 'linkuser_' + Math.floor(Math.random() * 100000);

    // Seed existing local account
    const [localUser] = await db.insert(users).values({
      email,
      username,
      status: 'active',
    }).returning();

    expect(localUser.googleId).toBeNull();

    // Google user logs in with matching verified email
    const googleProfile = {
      sub: 'google_id_linked_' + Date.now(),
      email,
      email_verified: true,
      name: 'Linked User',
      picture: 'https://lh3.googleusercontent.com/avatar_linked.jpg',
    };

    const linked = await GoogleOAuthService.findOrCreateGoogleUser(googleProfile);
    expect(linked.id).toBe(localUser.id);
    expect(linked.googleId).toBe(googleProfile.sub);
  });

  it('creates and exchanges single-use OAuth handover ticket', async () => {
    const email = 'ticket_user_' + Date.now() + '@example.com';
    const username = 'ticket_user_' + Math.floor(Math.random() * 100000);

    const [user] = await db.insert(users).values({
      email,
      username,
      status: 'active',
    }).returning();

    const mockCookie: any = {
      rea_session: {
        set: (opts: any) => { mockCookie.rea_session.opts = opts; },
        value: undefined,
      },
    };

    const mockReq = new Request('http://localhost:3000/api/v1/auth/google/callback', {
      headers: { 'user-agent': 'Test-Agent' },
    });

    const { ticket } = await GoogleOAuthService.createSessionAndTicket(user.id, mockReq, mockCookie);
    expect(ticket).toBeDefined();
    expect(ticket.length).toBeGreaterThan(20);

    // Exchange ticket
    const exchanged = await GoogleOAuthService.exchangeTicketForSession(ticket, mockCookie);
    expect(exchanged.user.id).toBe(user.id);
    expect(mockCookie.rea_session.opts.value).toBeDefined();

    // Reusing ticket must fail
    let reuseFailed = false;
    try {
      await GoogleOAuthService.exchangeTicketForSession(ticket, mockCookie);
    } catch {
      reuseFailed = true;
    }
    expect(reuseFailed).toBe(true);
  });
});
