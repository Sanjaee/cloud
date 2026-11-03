# Zacode — Enterprise Multi-User Cloud Storage Platform
## Complete Independent Architecture Report

### Repositories Overview

```
c:\Users\afriz\OneDrive\Desktop\cloud\
├── cloud-storage-api/    (ElysiaJS + Bun + Drizzle ORM + Neon DB + Cloudflare R2 + OpenAPI)
└── cloud-storage-web/    (SvelteKit + Svelte 5 + Tailwind CSS + TanStack Query + Zustand)
```

---

### 1. `cloud-storage-api` (Backend)
- **Domain Target**: `api.example.com` (Vercel Project B)
- **Runtime**: Bun (`v1.3.13`) + ElysiaJS (`v1.2.x`)
- **Database**: PostgreSQL (Neon Serverless Pooler) via Drizzle ORM (21 normalized tables, indexed, with foreign keys and cascade rules).
- **Object Storage**: Private Cloudflare R2 bucket (`lostmedia`) via AWS S3 SDK.
- **Authentication**:
  - Web: HttpOnly, Secure, SameSite=Lax session cookies with SHA-256 token hashing and rotation.
  - Mobile: `Authorization: Bearer <jwt>` with rotating refresh token family tracking and reuse detection.
  - Password Hashing: Argon2id.
  - Multi-Factor Authentication: RFC 6238 TOTP with QR code generation and 10 hashed one-time backup codes.
- **Native Sharing Engine**: Reference-based sharing (Viewer, Commenter, Editor) with zero duplication of Cloudflare R2 objects.
- **Folder Inheritance**: Recursive parent folder permission resolution.
- **Shortcuts**: Reference-based shortcuts in My Drive with zero physical quota impact.
- **OpenAPI 3.x**: 57 endpoints documented at `/swagger`, `/swagger/json`, and exported to `docs/openapi.json`.
- **Automated E2E Test Suite**: Passed all 14 end-to-end integration tests (`tests/test-api-e2e.ts`).

---

### 2. `cloud-storage-web` (Frontend)
- **Domain Target**: `app.example.com` (Vercel Project A)
- **Framework**: SvelteKit 2 + Svelte 5 (Runes) + TypeScript strict mode
- **Styling & UI**: Tailwind CSS v4 + Lucide icons
- **State Management**:
  - Server State: `@tanstack/svelte-query`
  - Client State: Zustand vanilla store (`createStore`)
- **Image Compression**: Browser-side Web Worker (`image-compression.worker.ts`) for non-blocking WebP / PNG optimization.
- **Upload Manager**: Multi-file queue with concurrency control, progress per file, upload speed, time remaining, and direct presigned PUT transfers to Cloudflare R2.
- **File Preview**: Inline modal supporting images, PDFs, videos, audios, and text/code files.
- **Security & Devices Dashboard**: Active session management, device tracking, remote session revocation, password update, and TOTP MFA setup.
- **Vercel Build**: Production build verified with code 0 (`bun run build`).

