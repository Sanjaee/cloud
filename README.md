# Zacode Cloud Storage Platform

Complete multi-user cloud storage platform inspired by Google Drive, built with secure authentication, private storage, collaborative file sharing via Cloudflare R2, and independent Vercel deployments.

## Architecture

This repository contains two independent applications:

- **`cloud-storage-api/`**: Independent backend REST API built with **ElysiaJS**, **Bun**, **Drizzle ORM**, **PostgreSQL (Neon)**, and **Cloudflare R2**.
  - Dual auth: Web session cookies (`HttpOnly`, `SameSite=Lax`) & Mobile Bearer JWTs with rotating refresh token families.
  - Argon2id password hashing.
  - RFC 6238 TOTP Multi-Factor Authentication.
  - Native reference-based file & folder sharing (zero duplicate R2 storage).
  - OpenAPI 3.x specification at `/swagger`.
  - Tested with automated E2E test suite (`tests/test-api-e2e.ts`).

- **`cloud-storage-web/`**: Independent web client built with **SvelteKit**, **Svelte 5** (Runes), **TypeScript**, **Tailwind CSS**, **TanStack Query**, **TanStack Table**, and **Zustand**.
  - Google Drive-inspired UI (My Drive, Shared with me, Shared by me, Recent, Starred, Trash, Storage).
  - Browser Web Worker for non-blocking client-side WebP image compression.
  - Concurrency-controlled Upload Manager drawer with direct presigned PUT transfers to R2.
  - Inline file preview modal (Images, PDFs, Videos, Audios, Code).
  - Account security & active session management.

## Deployment on Vercel
Both applications deploy to Vercel independently:
1. **API**: Root directory `cloud-storage-api` -> `api.example.com`
2. **Web**: Root directory `cloud-storage-web` -> `app.example.com`

