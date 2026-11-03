import { Elysia } from 'elysia';
import { cors } from '@elysiajs/cors';
import { swagger } from '@elysiajs/swagger';
import { corsConfig } from './config/cors';
import { requestId } from './middleware/request-id';
import { errorHandler } from './middleware/error-handler';
import { createRateLimiter } from './middleware/rate-limit';
import { authPlugin } from './middleware/authentication';

// Modules
import { authRoutes } from './modules/auth/auth.routes';
import { userRoutes } from './modules/users/users.routes';
import { sessionRoutes } from './modules/sessions/sessions.routes';
import { securityRoutes } from './modules/security/security.routes';
import { mfaRoutes } from './modules/mfa/mfa.routes';
import { fileRoutes } from './modules/files/files.routes';
import { folderRoutes } from './modules/folders/folders.routes';
import { uploadRoutes } from './modules/uploads/uploads.routes';
import { downloadRoutes } from './modules/downloads/downloads.routes';
import { sharingRoutes } from './modules/sharing/sharing.routes';
import { shortcutRoutes } from './modules/shortcuts/shortcuts.routes';
import { publicLinkRoutes } from './modules/public-links/public-links.routes';
import { trashRoutes } from './modules/trash/trash.routes';
import { quotaRoutes } from './modules/quotas/quotas.routes';
import { notificationRoutes } from './modules/notifications/notifications.routes';
import { auditRoutes } from './modules/audit/audit.routes';
import { healthRoutes } from './modules/health/health.routes';

export const app = new Elysia()
  // Global Middlewares
  .use(cors(corsConfig as any))
  .use(requestId)
  .use(errorHandler)
  .use(createRateLimiter(120, 60000))
  // OpenAPI Documentation
  .use(
    swagger({
      path: '/swagger',
      documentation: {
        info: {
          title: 'REA Cloud Storage API',
          version: '1.0.0',
          description:
            'Production-grade REST API for Multi-User Cloud Storage SaaS platform. Supports Web and Mobile clients with Argon2id, Cloudflare R2 direct uploads, and native reference-based sharing.',
        },
        tags: [
          { name: 'Authentication', description: 'Authentication and session management' },
          { name: 'Users', description: 'User profile and quota information' },
          { name: 'Files', description: 'File CRUD, metadata, and lifecycle' },
          { name: 'Folders', description: 'Hierarchical folder management' },
          { name: 'Uploads', description: 'Direct Cloudflare R2 presigned upload sessions' },
          { name: 'Downloads', description: 'Authorized presigned download and preview URLs' },
          { name: 'Sharing', description: 'Native reference-based file & folder sharing' },
          { name: 'Shortcuts', description: 'Google Drive-style reference shortcuts' },
          { name: 'Public Links', description: 'Password and expiring public sharing links' },
          { name: 'Trash', description: 'Soft-deletion, restoration, and permanent R2 deletion' },
          { name: 'Quotas', description: 'Physical object storage quotas and statistics' },
          { name: 'Security', description: 'Password reset, security events, and audit logs' },
          { name: 'MFA', description: 'Multi-Factor Authentication (TOTP)' },
        ],
      },
    })
  )
  // Health checks
  .use(healthRoutes)
  // Versioned REST API v1
  .group('/api/v1', (v1) =>
    v1
      .use(authPlugin)
      .use(authRoutes)
      .use(userRoutes)
      .use(sessionRoutes)
      .use(securityRoutes)
      .use(mfaRoutes)
      .use(fileRoutes)
      .use(folderRoutes)
      .use(uploadRoutes)
      .use(downloadRoutes)
      .use(sharingRoutes)
      .use(shortcutRoutes)
      .use(publicLinkRoutes)
      .use(trashRoutes)
      .use(quotaRoutes)
      .use(notificationRoutes)
      .use(auditRoutes)
  );

export type App = typeof app;
export default app;
