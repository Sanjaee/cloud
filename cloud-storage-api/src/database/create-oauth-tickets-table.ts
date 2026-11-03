import postgres from 'postgres';

const url = 'postgresql://neondb_owner:npg_fj3hUnJqR0oM@ep-frosty-credit-b378nim8-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require';
const sql = postgres(url);

async function main() {
  console.log('Creating oauth_exchange_tickets table if not exists...');
  await sql`
    CREATE TABLE IF NOT EXISTS oauth_exchange_tickets (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      ticket_hash TEXT NOT NULL UNIQUE,
      session_token TEXT NOT NULL,
      expires_at TIMESTAMPTZ NOT NULL,
      used_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `;
  await sql`
    CREATE INDEX IF NOT EXISTS oauth_tickets_user_idx ON oauth_exchange_tickets(user_id);
  `;
  await sql`
    CREATE UNIQUE INDEX IF NOT EXISTS oauth_tickets_hash_idx ON oauth_exchange_tickets(ticket_hash);
  `;
  console.log('✓ oauth_exchange_tickets created successfully!');
  await sql.end();
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
