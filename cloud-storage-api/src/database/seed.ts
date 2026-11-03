import { db } from '../infrastructure/database/client';
import { users, userProfiles, folders } from '../database/schema';
import { eq } from 'drizzle-orm';

async function seed() {
  console.log('Seeding REA Cloud Storage database...');

  const passwordHash = await Bun.password.hash('Password123!', {
    algorithm: 'argon2id',
    memoryCost: 19456,
    timeCost: 2,
  });

  const seedUsers = [
    {
      email: 'afrizaahmad18@gmail.com',
      username: 'test',
      displayName: 'Afriza Ahmad',
    },
    {
      email: 'demo@rea.cloud',
      username: 'demo',
      displayName: 'Demo User',
    },
    {
      email: 'admin@rea.cloud',
      username: 'admin',
      displayName: 'System Admin',
    },
  ];

  for (const item of seedUsers) {
    const existing = await db.query.users.findFirst({
      where: eq(users.email, item.email),
    });

    let userId: string;

    if (!existing) {
      const [newUser] = await db
        .insert(users)
        .values({
          email: item.email,
          username: item.username,
          passwordHash,
          status: 'active',
          emailVerifiedAt: new Date(),
          storageQuota: 10 * 1024 * 1024 * 1024,
          storageUsed: 0,
        })
        .returning();

      userId = newUser.id;
      console.log(`✓ Created user: ${item.email} (${item.username})`);

      await db.insert(userProfiles).values({
        userId: newUser.id,
        displayName: item.displayName,
      });

      // Default folders
      await db.insert(folders).values([
        { ownerId: newUser.id, name: 'Documents', color: '#3B82F6' },
        { ownerId: newUser.id, name: 'Photos', color: '#10B981' },
        { ownerId: newUser.id, name: 'Projects', color: '#8B5CF6' },
      ]);
    } else {
      userId = existing.id;
      console.log(`- User ${item.email} already exists`);
    }
  }

  console.log('\nSeed completed successfully!');
  process.exit(0);
}

seed().catch((err) => {
  console.error('Seed error:', err);
  process.exit(1);
});
