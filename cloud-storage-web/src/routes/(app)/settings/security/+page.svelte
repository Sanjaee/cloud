<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { formatDate, timeAgo } from '$lib/utils/format-date';
  import {
    Shield,
    Smartphone,
    Globe,
    Key,
    Lock,
    QrCode,
    Check,
    AlertCircle,
    Trash2,
    LogOut,
    CheckCircle,
    X,
    Copy,
    RefreshCw,
    Download,
    Eye,
  } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { ActiveSession, SecurityEvent } from '$lib/api/types';

  let sessions = $state<ActiveSession[]>([]);
  let events = $state<SecurityEvent[]>([]);
  let mfaEnabled = $state(false);
  let isLoading = $state(true);

  // Password change state
  let currentPassword = $state('');
  let newPassword = $state('');
  let confirmNewPassword = $state('');
  let pwdMessage = $state('');
  let pwdError = $state('');
  let isUpdatingPwd = $state(false);

  // MFA setup & verification state
  let mfaSetupData = $state<{ secret: string; qrCodeDataUrl: string } | null>(null);
  let totpCode = $state('');
  let backupCodes = $state<string[]>([]);
  let mfaMessage = $state('');
  let mfaError = $state('');
  let isSettingUpMfa = $state(false);
  let isConfirmingMfa = $state(false);
  let showBackupCodesModal = $state(false);
  let codesCopied = $state(false);

  // Disable MFA state
  let showDisableModal = $state(false);
  let disablePassword = $state('');
  let disableTotpCode = $state('');
  let isDisablingMfa = $state(false);
  let disableError = $state('');

  async function loadSecurityData() {
    isLoading = true;
    try {
      const [sessRes, eventsRes, mfaRes] = await Promise.all([
        apiRequest('/sessions'),
        apiRequest('/security/events'),
        apiRequest('/mfa/status'),
      ]);
      sessions = sessRes.sessions || [];
      events = eventsRes.events || [];
      mfaEnabled = mfaRes.isEnabled ?? false;
    } catch (err) {
      console.error('Failed to load security data:', err);
    } finally {
      isLoading = false;
    }
  }

  async function handleRevokeSession(sessionId: string) {
    try {
      await apiRequest(`/sessions/${sessionId}`, { method: 'DELETE' });
      sessions = sessions.filter((s) => s.id !== sessionId);
    } catch {}
  }

  async function handleRevokeOtherSessions() {
    try {
      await apiRequest('/sessions/revoke-others', { method: 'POST' });
      sessions = sessions.filter((s) => s.isCurrent);
    } catch {}
  }

  async function handleChangePassword(e: Event) {
    e.preventDefault();
    if (newPassword.length < 8) {
      pwdError = 'New password must be at least 8 characters long.';
      return;
    }
    if (newPassword !== confirmNewPassword) {
      pwdError = 'New passwords do not match.';
      return;
    }

    isUpdatingPwd = true;
    pwdError = '';
    pwdMessage = '';

    try {
      await apiRequest('/security/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      pwdMessage = 'Password updated successfully. Other sessions revoked.';
      currentPassword = '';
      newPassword = '';
      confirmNewPassword = '';
    } catch (err: any) {
      pwdError = err.message || 'Failed to change password';
    } finally {
      isUpdatingPwd = false;
    }
  }

  async function startMfaSetup(regenerate = false) {
    mfaError = '';
    mfaMessage = '';
    isSettingUpMfa = true;
    try {
      const res = await apiRequest('/mfa/setup', {
        method: 'POST',
        body: JSON.stringify({ regenerate }),
      });
      mfaSetupData = res;
      totpCode = '';
    } catch (err: any) {
      mfaError = err.message || 'Gagal menyiapkan MFA';
    } finally {
      isSettingUpMfa = false;
    }
  }

  async function confirmMfa() {
    const cleanCode = totpCode.replace(/[\s-]+/g, '').trim();
    if (!cleanCode || cleanCode.length !== 6) {
      mfaError = 'Harap masukkan 6 digit kode angka dari aplikasi Authenticator';
      return;
    }

    mfaError = '';
    isConfirmingMfa = true;
    try {
      const res = await apiRequest('/mfa/enable', {
        method: 'POST',
        body: JSON.stringify({ code: cleanCode }),
      });
      backupCodes = res.backupCodes || [];
      mfaEnabled = true;
      mfaSetupData = null;
      totpCode = '';
      mfaMessage = 'Two-Factor Authentication (TOTP) berhasil diaktifkan!';
      showBackupCodesModal = true;
    } catch (err: any) {
      mfaError = err.message || 'Kode verifikasi tidak valid atau kedaluwarsa';
    } finally {
      isConfirmingMfa = false;
    }
  }

  async function handleDisableMfa() {
    isDisablingMfa = true;
    disableError = '';
    try {
      await apiRequest('/mfa/disable', {
        method: 'POST',
        body: JSON.stringify({
          password: disablePassword || undefined,
          code: disableTotpCode || undefined,
        }),
      });
      mfaEnabled = false;
      showDisableModal = false;
      disablePassword = '';
      disableTotpCode = '';
      mfaMessage = 'Two-Factor Authentication berhasil dinonaktifkan';
      backupCodes = [];
    } catch (err: any) {
      disableError = err.message || 'Gagal menonaktifkan MFA';
    } finally {
      isDisablingMfa = false;
    }
  }

  function copyBackupCodes() {
    if (backupCodes.length === 0) return;
    navigator.clipboard.writeText(backupCodes.join('\n'));
    codesCopied = true;
    setTimeout(() => (codesCopied = false), 2500);
  }

  function downloadBackupCodes() {
    if (backupCodes.length === 0) return;
    const content = `REA CLOUD STORAGE - 2FA BACKUP CODES\nGenerated: ${new Date().toISOString()}\n\nEach code can be used once to access your account if you lose your authenticator app:\n\n${backupCodes.join('\n')}\n\nKeep these codes safe and private!`;
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'rea-cloud-backup-codes.txt';
    a.click();
    URL.revokeObjectURL(url);
  }

  onMount(() => {
    loadSecurityData();
  });
</script>

<div class="space-y-8 max-w-4xl pb-12">
  <div>
    <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
      <Shield class="w-7 h-7 text-blue-600" />
      Account Security & Devices
    </h1>
    <p class="text-xs text-slate-500 mt-1">
      Manage active devices, session security, password rotation, and Multi-Factor Authentication (TOTP).
    </p>
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else}
    <!-- Active Sessions Section -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h3 class="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2">
            <Smartphone class="w-5 h-5 text-blue-600" />
            Active Sessions & Devices ({sessions.length})
          </h3>
          <p class="text-xs text-slate-500 mt-0.5">
            Devices that currently have active session cookies or API tokens.
          </p>
        </div>

        {#if sessions.length > 1}
          <button
            onclick={handleRevokeOtherSessions}
            class="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 hover:bg-red-100 dark:bg-red-950/60 text-red-600 rounded-xl text-xs font-semibold transition cursor-pointer"
          >
            <LogOut class="w-3.5 h-3.5" />
            Sign Out Other Devices
          </button>
        {/if}
      </div>

      <div class="divide-y divide-slate-100 dark:divide-slate-800">
        {#each sessions as s}
          <div class="py-3.5 flex items-center justify-between text-xs">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-500">
                <Globe class="w-5 h-5" />
              </div>

              <div>
                <div class="flex items-center gap-2">
                  <span class="font-bold text-slate-900 dark:text-white">{s.ipAddress || '127.0.0.1'}</span>
                  {#if s.isCurrent}
                    <span class="px-2 py-0.5 bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 rounded-md text-[10px] font-bold">
                      Current Device
                    </span>
                  {/if}
                </div>
                <span class="text-[11px] text-slate-400 block mt-0.5 truncate max-w-sm">
                  {s.userAgent || 'Web Browser'} • Active {timeAgo(s.lastUsedAt)}
                </span>
              </div>
            </div>

            {#if !s.isCurrent}
              <button
                onclick={() => handleRevokeSession(s.id)}
                class="p-1.5 hover:bg-red-50 dark:hover:bg-red-950 text-slate-400 hover:text-red-600 rounded-lg transition cursor-pointer"
                title="Revoke session"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            {/if}
          </div>
        {/each}
      </div>
    </div>

    <!-- Password Management Section -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs">
      <h3 class="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2 mb-1">
        <Key class="w-5 h-5 text-blue-600" />
        Change Password
      </h3>
      <p class="text-xs text-slate-500 mb-5">
        Passwords are encrypted with state-of-the-art Argon2id hashing. Changing your password revokes all other sessions.
      </p>

      {#if pwdError}
        <div class="mb-4 p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl text-xs text-red-700 dark:text-red-300 flex items-center gap-2">
          <AlertCircle class="w-4 h-4 shrink-0" />
          <span>{pwdError}</span>
        </div>
      {/if}

      {#if pwdMessage}
        <div class="mb-4 p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-xl text-xs text-emerald-700 dark:text-emerald-300 flex items-center gap-2">
          <CheckCircle class="w-4 h-4 shrink-0" />
          <span>{pwdMessage}</span>
        </div>
      {/if}

      <form onsubmit={handleChangePassword} class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label for="curr-pwd" class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Current Password</label>
          <input
            id="curr-pwd"
            type="password"
            bind:value={currentPassword}
            required
            placeholder="••••••••"
            class="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
          />
        </div>

        <div>
          <label for="new-pwd" class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">New Password (8+ chars)</label>
          <input
            id="new-pwd"
            type="password"
            bind:value={newPassword}
            required
            minlength="8"
            placeholder="••••••••"
            class="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
          />
        </div>

        <div>
          <label for="conf-pwd" class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Confirm New Password</label>
          <input
            id="conf-pwd"
            type="password"
            bind:value={confirmNewPassword}
            required
            minlength="8"
            placeholder="••••••••"
            class="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
          />
        </div>

        <div class="md:col-span-3 flex justify-end">
          <button
            type="submit"
            disabled={isUpdatingPwd || !currentPassword || !newPassword}
            class="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition disabled:opacity-50 cursor-pointer"
          >
            {isUpdatingPwd ? 'Updating...' : 'Update Password'}
          </button>
        </div>
      </form>
    </div>

    <!-- MFA / Two-Factor Authentication Section -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs relative">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h3 class="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2">
            <Lock class="w-5 h-5 text-blue-600" />
            Two-Factor Authentication (TOTP)
          </h3>
          <p class="text-xs text-slate-500 mt-0.5">
            Require a 6-digit TOTP code from Google Authenticator, Authy, or 1Password when signing in.
          </p>
        </div>

        <span class="px-3 py-1 rounded-full text-xs font-bold {mfaEnabled ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300' : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400'}">
          {mfaEnabled ? 'Enabled' : 'Disabled'}
        </span>
      </div>

      <!-- Popup Alert Messages -->
      {#if mfaMessage}
        <div class="mb-4 p-3.5 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-2xl text-xs text-emerald-700 dark:text-emerald-300 flex items-center justify-between gap-2 shadow-xs animate-in fade-in duration-200">
          <div class="flex items-center gap-2.5">
            <CheckCircle class="w-4 h-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
            <span class="font-medium">{mfaMessage}</span>
          </div>
          <button onclick={() => (mfaMessage = '')} class="p-1 hover:bg-emerald-100 dark:hover:bg-emerald-900/40 rounded-lg text-emerald-600 cursor-pointer">
            <X class="w-3.5 h-3.5" />
          </button>
        </div>
      {/if}

      {#if mfaError && !mfaSetupData}
        <div class="mb-4 p-3.5 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-2xl text-xs text-red-700 dark:text-red-300 flex items-center justify-between gap-2 shadow-xs animate-in fade-in duration-200">
          <div class="flex items-center gap-2.5">
            <AlertCircle class="w-4 h-4 shrink-0 text-red-600 dark:text-red-400" />
            <span class="font-medium">{mfaError}</span>
          </div>
          <button onclick={() => (mfaError = '')} class="p-1 hover:bg-red-100 dark:hover:bg-red-900/40 rounded-lg text-red-600 cursor-pointer">
            <X class="w-3.5 h-3.5" />
          </button>
        </div>
      {/if}

      <!-- MFA Enabled view -->
      {#if mfaEnabled}
        <div class="p-5 bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200/60 dark:border-emerald-900/60 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-900/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center shrink-0">
              <Shield class="w-5 h-5" />
            </div>
            <div>
              <h4 class="text-xs font-bold text-slate-900 dark:text-white">Akun Anda Dilindungi 2FA</h4>
              <p class="text-[11px] text-slate-500 mt-0.5">Kode autentikasi dari ponsel Anda aktif untuk setiap sesi login.</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            {#if backupCodes.length > 0}
              <button
                type="button"
                onclick={() => (showBackupCodesModal = true)}
                class="px-3.5 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                Lihat Backup Codes
              </button>
            {/if}
            <button
              type="button"
              onclick={() => {
                showDisableModal = true;
                disableError = '';
                disableTotpCode = '';
                disablePassword = '';
              }}
              class="px-3.5 py-1.5 bg-red-50 dark:bg-red-950/40 hover:bg-red-100 dark:hover:bg-red-900/50 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900 rounded-xl text-xs font-semibold transition cursor-pointer"
            >
              Nonaktifkan 2FA
            </button>
          </div>
        </div>
      {:else if !mfaSetupData}
        <button
          type="button"
          onclick={() => startMfaSetup(false)}
          disabled={isSettingUpMfa}
          class="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer flex items-center gap-2"
        >
          {#if isSettingUpMfa}
            <div class="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            <span>Menyiapkan...</span>
          {:else}
            <QrCode class="w-4 h-4" />
            <span>Setup Two-Factor Authentication</span>
          {/if}
        </button>
      {/if}

      <!-- MFA Setup Wizard -->
      {#if mfaSetupData && !mfaEnabled}
        <div class="p-6 bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
          <div class="flex items-center justify-between">
            <h4 class="font-bold text-xs uppercase tracking-wider text-slate-700 dark:text-slate-200 flex items-center gap-1.5">
              <QrCode class="w-4 h-4 text-blue-600" />
              Scan QR Code Dengan Aplikasi Authenticator
            </h4>
            <div class="flex items-center gap-2">
              <button
                type="button"
                onclick={() => startMfaSetup(true)}
                title="Regenerate QR Code & Secret Key"
                class="flex items-center gap-1 text-[11px] text-blue-600 hover:text-blue-700 dark:text-blue-400 font-semibold cursor-pointer"
              >
                <RefreshCw class="w-3 h-3" />
                Refresh QR
              </button>
              <button
                type="button"
                onclick={() => {
                  mfaSetupData = null;
                  mfaError = '';
                }}
                class="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg cursor-pointer"
              >
                <X class="w-4 h-4" />
              </button>
            </div>
          </div>

          <!-- Error popup banner inside wizard -->
          {#if mfaError}
            <div class="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl text-xs text-red-700 dark:text-red-300 flex items-center justify-between gap-2 shadow-xs animate-in shake duration-200">
              <div class="flex items-center gap-2">
                <AlertCircle class="w-4 h-4 shrink-0 text-red-600" />
                <span>{mfaError}</span>
              </div>
              <button onclick={() => (mfaError = '')} class="p-1 hover:bg-red-100 dark:hover:bg-red-900/40 rounded-lg text-red-500 cursor-pointer">
                <X class="w-3 h-3" />
              </button>
            </div>
          {/if}

          <div class="flex flex-col sm:flex-row items-center gap-6">
            <div class="p-2.5 bg-white rounded-2xl shadow-md border border-slate-200 shrink-0">
              <img
                src={mfaSetupData.qrCodeDataUrl}
                alt="MFA QR Code"
                class="w-40 h-40 object-contain rounded-lg"
              />
            </div>

            <div class="space-y-3 flex-1 text-xs w-full">
              <p class="text-slate-600 dark:text-slate-400">
                Gunakan <strong>Google Authenticator</strong>, <strong>Authy</strong>, atau <strong>1Password</strong> untuk memindai QR code di samping, atau masukkan manual secret key berikut:
              </p>
              <div class="flex items-center gap-2">
                <code class="flex-1 p-2.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl font-mono text-xs text-blue-600 dark:text-blue-400 font-bold select-all tracking-wider">
                  {mfaSetupData.secret}
                </code>
                <button
                  type="button"
                  onclick={() => {
                    navigator.clipboard.writeText(mfaSetupData!.secret);
                    mfaMessage = 'Secret key disalin ke clipboard';
                    setTimeout(() => (mfaMessage = ''), 2000);
                  }}
                  title="Copy secret key"
                  class="p-2.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:border-blue-400 rounded-xl text-slate-500 hover:text-blue-600 transition cursor-pointer"
                >
                  <Copy class="w-4 h-4" />
                </button>
              </div>

              <div>
                <label for="totp-input" class="block text-xs font-semibold mb-1 text-slate-700 dark:text-slate-300">
                  Masukkan 6-digit kode dari HP untuk verifikasi:
                </label>
                <div class="flex items-center gap-2">
                  <input
                    id="totp-input"
                    type="text"
                    inputmode="numeric"
                    autocomplete="one-time-code"
                    maxlength="6"
                    placeholder="123456"
                    bind:value={totpCode}
                    onkeydown={(e) => e.key === 'Enter' && confirmMfa()}
                    oninput={(e: any) => {
                      totpCode = e.target.value.replace(/[^\d]/g, '').slice(0, 6);
                      if (mfaError) mfaError = '';
                    }}
                    class="w-36 text-center font-mono text-base tracking-widest px-3.5 py-2 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30 rounded-xl text-slate-900 dark:text-white"
                  />
                  <button
                    type="button"
                    disabled={isConfirmingMfa || totpCode.length !== 6}
                    onclick={confirmMfa}
                    class="px-5 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold rounded-xl text-xs shadow-md transition cursor-pointer flex items-center gap-1.5"
                  >
                    {#if isConfirmingMfa}
                      <div class="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Memverifikasi...</span>
                    {:else}
                      <Check class="w-4 h-4" />
                      <span>Confirm & Enable</span>
                    {/if}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      {/if}
    </div>

    <!-- Backup Codes Modal Popup -->
    {#if showBackupCodesModal && backupCodes.length > 0}
      <div class="fixed inset-0 bg-slate-950/70 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
        <div class="w-full max-w-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-2xl p-6 overflow-hidden animate-in zoom-in-95 duration-200">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-10 h-10 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 flex items-center justify-center shrink-0">
              <CheckCircle class="w-6 h-6" />
            </div>
            <div>
              <h3 class="font-bold text-base text-slate-900 dark:text-white">
                2FA Berhasil Diaktifkan! 🎉
              </h3>
              <p class="text-xs text-slate-500">
                Simpan 10 Kode Pemulihan Cadangan (Backup Codes) Anda di tempat aman.
              </p>
            </div>
          </div>

          <div class="p-3 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 rounded-xl text-[11px] text-amber-800 dark:text-amber-200 mb-4">
            Jika Anda kehilangan akses ke HP/Google Authenticator, setiap kode di bawah ini dapat digunakan satu kali untuk login.
          </div>

          <div class="grid grid-cols-2 gap-2 font-mono text-xs font-bold text-slate-800 dark:text-slate-200 p-3 bg-slate-50 dark:bg-slate-800/40 rounded-2xl border border-slate-200 dark:border-slate-700 mb-5">
            {#each backupCodes as code}
              <div class="p-2 bg-white dark:bg-slate-900 rounded-lg text-center border border-slate-200 dark:border-slate-700 shadow-2xs select-all">
                {code}
              </div>
            {/each}
          </div>

          <div class="flex flex-col sm:flex-row items-center justify-between gap-3">
            <div class="flex items-center gap-2 w-full sm:w-auto">
              <button
                type="button"
                onclick={copyBackupCodes}
                class="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3.5 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                {#if codesCopied}
                  <Check class="w-4 h-4 text-emerald-500" />
                  <span>Disalin!</span>
                {:else}
                  <Copy class="w-4 h-4" />
                  <span>Salin Semua</span>
                {/if}
              </button>

              <button
                type="button"
                onclick={downloadBackupCodes}
                class="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3.5 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                <Download class="w-4 h-4" />
                <span>Download .TXT</span>
              </button>
            </div>

            <button
              type="button"
              onclick={() => (showBackupCodesModal = false)}
              class="w-full sm:w-auto px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer"
            >
              Saya Sudah Menyimpan Kode
            </button>
          </div>
        </div>
      </div>
    {/if}

    <!-- Disable MFA Modal Popup -->
    {#if showDisableModal}
      <div class="fixed inset-0 bg-slate-950/70 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
        <div class="w-full max-w-md bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-2xl p-6 overflow-hidden animate-in zoom-in-95 duration-200">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2 text-red-600">
              <AlertCircle class="w-5 h-5" />
              Nonaktifkan 2FA
            </h3>
            <button onclick={() => (showDisableModal = false)} class="p-1 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl cursor-pointer">
              <X class="w-5 h-5 text-slate-400" />
            </button>
          </div>

          {#if disableError}
            <div class="mb-4 p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl text-xs text-red-700 dark:text-red-300">
              {disableError}
            </div>
          {/if}

          <p class="text-xs text-slate-600 dark:text-slate-400 mb-4">
            Menonaktifkan Two-Factor Authentication akan menurunkan tingkat keamanan akun Anda. Masukkan kode TOTP atau backup code untuk mengonfirmasi:
          </p>

          <div class="space-y-3 mb-5">
            <div>
              <label for="disable-code" class="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Kode TOTP (6 digit) atau Backup Code:
              </label>
              <input
                id="disable-code"
                type="text"
                bind:value={disableTotpCode}
                placeholder="123456 atau ABCD-1234"
                class="w-full px-3.5 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-900 dark:text-white"
              />
            </div>
          </div>

          <div class="flex items-center justify-end gap-2">
            <button
              type="button"
              onclick={() => (showDisableModal = false)}
              class="px-4 py-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400 rounded-xl text-xs font-semibold cursor-pointer"
            >
              Batal
            </button>
            <button
              type="button"
              disabled={isDisablingMfa}
              onclick={handleDisableMfa}
              class="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer"
            >
              {isDisablingMfa ? 'Menonaktifkan...' : 'Konfirmasi Nonaktifkan'}
            </button>
          </div>
        </div>
      </div>
    {/if}

    <!-- Security Events Log -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs">
      <h3 class="font-bold text-base text-slate-900 dark:text-white mb-1">Recent Security Activity</h3>
      <p class="text-xs text-slate-500 mb-4">Audit trail of logins, password resets, and security modifications.</p>

      <div class="divide-y divide-slate-100 dark:divide-slate-800 max-h-60 overflow-y-auto">
        {#each events as ev}
          <div class="py-2.5 flex items-center justify-between text-xs">
            <div>
              <span class="font-semibold text-slate-800 dark:text-slate-200 capitalize">
                {ev.eventType.replace(/_/g, ' ')}
              </span>
              <span class="text-[11px] text-slate-400 block">IP: {ev.ipAddress || '127.0.0.1'}</span>
            </div>
            <span class="text-slate-400 text-[11px]">{formatDate(ev.createdAt)}</span>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>

