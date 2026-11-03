# REA Cloud Storage Web

Independent production-grade Web frontend for Multi-User Cloud Storage SaaS platform.
Built with **SvelteKit**, **Svelte 5**, **TypeScript**, **Tailwind CSS**, **TanStack Query**, **TanStack Table**, **Zustand**, and **Lucide**.

## Features
- **Svelte 5 Runes**: Modern reactive architecture (`$state`, `$derived`, `$props`).
- **Complete Authentication Flow**:
  - Email & password registration with real-time password strength validation.
  - Login with Argon2id verification and optional Two-Factor Authentication (TOTP).
  - Forgot password & reset password workflows.
- **Google Drive-Inspired File Manager**:
  - My Drive, Shared with me, Shared by me, Recent, Starred, Trash, and Storage views.
  - Breadcrumbs navigation.
  - Grid view & TanStack Table list view with column sorting.
  - Multi-select and custom right-click context menu.
- **Native Shared References**:
  - Share files and folders without duplicating Cloudflare R2 objects.
  - Granular permissions (`viewer`, `commenter`, `editor`).
  - Add shortcuts to My Drive without consuming storage quota.
- **Direct Cloudflare R2 Upload Manager**:
  - Browser uploads directly to R2 using short-lived presigned URLs.
  - Non-blocking browser Web Worker for WebP image compression.
  - Concurrency management, pause, retry, upload speed, and progress tracking.
- **Inline File Preview Modal**:
  - Preview images, PDFs, videos, audios, and text/code files securely.
- **Account Security & Device Dashboard**:
  - Active session inspection, remote session revocation, password update, and TOTP MFA setup.

## Setup & Running Locally
```bash
# Install dependencies
bun install

# Start development server (proxies /api to localhost:3000)
bun run dev

# Type check
bun run check

# Build for Vercel
bun run build
```

## Environment Variables
- `PUBLIC_API_URL`: Points to your `cloud-storage-api` deployment (e.g. `https://api.example.com`).

