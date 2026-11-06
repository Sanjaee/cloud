# Zacode Cloud Storage — Android Native App

A modern, production-grade native Android client for the **Zacode Cloud Storage** platform, engineered with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Clean Architecture**.

---

## 🏛 Architecture & Tech Stack

- **UI & Design System**: Jetpack Compose, Material 3, Dark Shadcn Design System (`#09090B`, `#121214`, `#18181B`).
- **Authentication**: Android Credential Manager & Google Identity Services (**Strictly Google Sign-In Only** — zero password forms).
- **Two-Factor Authentication (2FA)**: 6-digit TOTP verification & 10 recovery backup code matching.
- **Networking**: Retrofit 2, OkHttp 4, Kotlinx Serialization.
- **Security**: Android Keystore-backed `EncryptedSharedPreferences` for Access and Rotating Refresh Tokens.
- **Background Tasks**: WorkManager for direct Cloudflare R2 presigned chunked/multipart uploads and background file downloads.
- **Offline Caching**: Room Database (`files`, `folders`) with reactive Kotlin Flows.
- **Image Loading**: Coil 2 with Bearer token authentication and crossfade transitions.
- **Dependency Injection**: Hilt.

---

## 📱 Feature Overview

1. **Google Sign-In Only**:
   - Zero credentials / email-password screens.
   - Handled via `CredentialManager` with Google ID Token exchange against `/api/v1/auth/google/token`.
   - Automatic 401 token refresh via rotating refresh token family (`TokenAuthenticator`).

2. **Authenticator / MFA**:
   - Google Authenticator / TOTP 6-digit codes.
   - Backup recovery code support (individual tokens or pasted list auto-extraction).

3. **My Drive**:
   - Dynamic folder navigation with breadcrumb trail.
   - 2-column unified resource grid (`FileCard`, `FolderCard`) with Coil image thumbnails.
   - Floating Action Button to create folders or pick files for upload.
   - Multi-selection mode for batch operations (delete, move, download).

4. **Trash**:
   - Matches the `/drive` unified resource card grid with thumbnails.
   - Individual & batch restore (`POST /api/v1/trash/restore/batch`).
   - Permanent purge & empty trash dialogs with `ShadcnAlertDialog`.

5. **Shared**:
   - Tabs for *Shared with me* and *Shared by me*.
   - Collaborator role badges (`viewer`, `commenter`, `editor`).
   - Collaborator revocation.

6. **Recent & Starred**:
   - Quick-access feeds of recently modified items and favorited files/folders.

7. **Storage Quota**:
   - Real-time progress bar of used vs. available storage on Cloudflare R2.
   - File count and percentage usage breakdown.

8. **Security & Active Sessions**:
   - View active device sessions with IP addresses and user agents.
   - Revoke other sessions or logout securely.

---

## 🛠 Build & Run

### Prerequisites
- JDK 17 (Eclipse Adoptium Temurin 17.0.17 recommended)
- Android SDK Platform 34 / 35
- Android Build Tools 35.0.0

### Run Tests
```powershell
cd cloud-storage-android
.\gradlew.bat testDebugUnitTest
```

### Build APK
```powershell
cd cloud-storage-android
.\gradlew.bat assembleDebug
```
The debug APK will be generated at:
`cloud-storage-android/app/build/outputs/apk/debug/app-debug.apk`

