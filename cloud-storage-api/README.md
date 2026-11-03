# REA Cloud Storage API

Production-grade, mobile-ready backend API for Multi-User Cloud Storage SaaS platform.
Built with **ElysiaJS**, **Bun**, **Drizzle ORM**, **PostgreSQL (Neon)**, and **Cloudflare R2**.

## Architectural Highlights
- **Dual Authentication**:
  - Web: Server-side HttpOnly, Secure, SameSite=Lax session cookies with SHA-256 token hashing and rotation.
  - Mobile: `Authorization: Bearer <jwt>` with rotating refresh token families & token reuse detection.
- **Argon2id** password hashing.
- **MFA (TOTP)**: RFC 6238 TOTP with hashed one-time backup recovery codes.
- **Native File Sharing**: Reference-based sharing (Viewer, Commenter, Editor). Zero object duplication in Cloudflare R2.
- **Hierarchical Inherited Folder Permissions**: Recursive parent permission resolution.
- **Shortcuts**: Google Drive-style shortcuts pointing to existing resources without physical quota consumption.
- **Direct Cloudflare R2 Uploads**: Short-lived presigned PUT URLs for single uploads and S3 multipart chunks for files > 100MB. Direct browser-to-R2 transfers.
- **Safe Object Lifecycle**: Reference counting prevents physical R2 object deletion while any active or shared references remain.
- **OpenAPI 3.x Specification**: Live documentation available at `/swagger` and `/swagger/json`.
- **Deployable to Vercel**: Stateless architecture with serverless entrypoint `api/index.ts`.

## Setup & Running Locally
```bash
# Install dependencies
bun install

# Push database schema
bun run db:push

# Start development server
bun run dev

# Run end-to-end automated test suite
bun run tests/test-api-e2e.ts
```

## API Documentation
Once the server is running, visit:
- Swagger UI: `http://localhost:3000/swagger`
- OpenAPI JSON: `http://localhost:3000/swagger/json`

