import postgres from 'postgres';

const url = 'postgresql://neondb_owner:npg_fj3hUnJqR0oM@ep-frosty-credit-b378nim8-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require';
const sql = postgres(url);

async function migrate() {
  console.log('Starting Google OAuth database migration...');
  
  await sql`
    -- 1. Add google_id column if not exists
    ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE;
  `;
  console.log('✓ Added google_id column');

  await sql`
    -- 2. Add auth_provider column if not exists
    ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(32) NOT NULL DEFAULT 'local';
  `;
  console.log('✓ Added auth_provider column');

  await sql`
    -- 3. Add last_login_at column if not exists
    ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;
  `;
  console.log('✓ Added last_login_at column');

  await sql`
    -- 4. Allow password_hash to be NULL for OAuth-only users
    ALTER TABLE users 
    ALTER COLUMN password_hash DROP NOT NULL;
  `;
  console.log('✓ Altered password_hash to be nullable');

  await sql`
    -- 5. Add index on google_id if not exists
    CREATE INDEX IF NOT EXISTS users_google_id_idx ON users(google_id);
  `;
  console.log('✓ Created index on google_id');

  console.log('Migration completed successfully!');
  await sql.end();
}

migrate().catch(err => {
  console.error('Migration failed:', err);
  process.exit(1);
});

