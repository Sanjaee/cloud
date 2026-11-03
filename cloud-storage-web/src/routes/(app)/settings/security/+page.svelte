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
    pwdError = '';
    pwdMessage = '';

    if (newPassword !== confirmNewPassword) {
      pwdError = 'New passwords do not match';
      return;
    }

    if (newPassword.length < 8) {
      pwdError = 'Password must be at least 8 characters';
      return;
    }

    isUpdatingPwd = true;
    try {
      await apiRequest('/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword }),
      });
      pwdMessage = 'Password updated successfully. Other sessions revoked.';
      currentPassword = '';
      newPassword = '';
      confirmNewPassword = '';
    } catch (err: any) {
      pwdError = err.message || 'Failed to update password';
    } finally {
      isUpdatingPwd = false;
    }
  }

  async function startMfaSetup(forceRegenerate = false) {
    isSettingUpMfa = true;
    mfaError = '';
    mfaMessage = '';
    try {
      const url = forceRegenerate ? '/mfa/setup?force=true' : '/mfa/setup';
      const res = await apiRequest(url, { method: 'POST' });
      mfaSetupData = {
        secret: res.secret,
        qrCodeDataUrl: res.qrCodeDataUrl,
      };
      totpCode = '';
    } catch (err: any) {
      mfaError = err.message || 'Failed to initialize MFA setup';
    } finally {
      isSettingUpMfa = false;
    }
  }

  async function confirmMfa() {
    if (!totpCode || totpCode.length !== 6) {
      mfaError = 'Please enter a valid 6-digit TOTP code';
      return;
    }

    isConfirmingMfa = true;
    mfaError = '';
    mfaMessage = '';

    try {
      const res = await apiRequest('/mfa/enable', {
        method: 'POST',
        body: JSON.stringify({
          totpCode: totpCode.trim(),
        }),
      });

      backupCodes = res.backupCodes || [];
      mfaEnabled = true;
      mfaSetupData = null;
      totpCode = '';
      mfaMessage = 'Two-Factor Authentication (2FA) is now enabled!';
      showBackupCodesModal = true;
      await loadSecurityData();
    } catch (err: any) {
      mfaError = err.message || 'Failed to verify TOTP code. Please make sure the code matches your phone.';
    } finally {
      isConfirmingMfa = false;
    }
  }

  async function handleDisableMfa() {
    disableError = '';
    isDisablingMfa = true;

    try {
      await apiRequest('/mfa/disable', {
        method: 'POST',
        body: JSON.stringify({
          password: disablePassword || undefined,
          totpCode: disableTotpCode ? disableTotpCode.trim() : undefined,
        }),
      });

      mfaEnabled = false;
      showDisableModal = false;
      disablePassword = '';
      disableTotpCode = '';
      mfaMessage = '2FA has been disabled.';
      backupCodes = [];
      await loadSecurityData();
    } catch (err: any) {
      disableError = err.message || 'Failed to disable 2FA';
    } finally {
      isDisablingMfa = false;
    }
  }

  function copyBackupCodes() {
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

<div class="space-y-6 max-w-4xl pb-12">
  <div>
    <h1 class="text-xl font-bold text-white flex items-center gap-2">
      <Shield class="w-5 h-5 text-zinc-400" />
      Account Security & 2FA
    </h1>
    <p class="text-xs text-zinc-400 mt-0.5">
      Manage active devices, session security, password rotation, and Multi-Factor Authentication.
    </p>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Loading security settings...</p>
    </div>
  {:else}
    <!-- Active Sessions Section -->
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-5 shadow-xs">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h3 class="font-semibold text-sm text-white flex items-center gap-2">
            <Smartphone class="w-4 h-4 text-zinc-400" />
            Active Sessions ({sessions.length})
          </h3>
          <p class="text-xs text-zinc-400 mt-0.5">
            Devices that currently have active session cookies or API tokens.
          </p>
        </div>

        {#if sessions.length > 1}
          <button
            onclick={handleRevokeOtherSessions}
            class="flex items-center gap-1.5 px-2.5 py-1 bg-red-950/40 hover:bg-red-900/40 border border-red-900/60 text-red-300 rounded-lg text-xs font-medium transition cursor-pointer"
          >
            <LogOut class="w-3.5 h-3.5" />
            Sign Out Others
          </button>
        {/if}
      </div>

      <div class="divide-y divide-zinc-800/60">
        {#each sessions as s}
          <div class="py-3 flex items-center justify-between text-xs">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-zinc-800 text-zinc-300 flex items-center justify-center shrink-0">
                <Globe class="w-4 h-4" />
              </div>

              <div>
                <div class="flex items-center gap-2">
                  <span class="font-medium text-white">{s.ipAddress || '127.0.0.1'}</span>
                  {#if s.isCurrent}
                    <span class="px-1.5 py-0.5 bg-emerald-950/60 text-emerald-400 border border-emerald-900/60 rounded text-[10px] font-medium">
                      Current
                    </span>
                  {/if}
                </div>
                <span class="text-[10px] text-zinc-500 block mt-0.5 truncate max-w-sm">
                  {s.userAgent || 'Web Browser'} • Active {timeAgo(s.lastUsedAt)}
                </span>
              </div>
            </div>

            {#if !s.isCurrent}
              <button
                onclick={() => handleRevokeSession(s.id)}
                class="p-1 hover:bg-red-950/40 text-zinc-500 hover:text-red-400 rounded transition cursor-pointer"
                title="Revoke session"
              >
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            {/if}
          </div>
        {/each}
      </div>
    </div>

    <!-- Password Management Section -->
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-5 shadow-xs">
      <h3 class="font-semibold text-sm text-white flex items-center gap-2 mb-1">
        <Key class="w-4 h-4 text-zinc-400" />
        Change Password
      </h3>
      <p class="text-xs text-zinc-400 mb-4">
        Passwords are encrypted with state-of-the-art Argon2id hashing. Changing your password revokes other sessions.
      </p>

      {#if pwdError}
        <div class="mb-3 p-2.5 bg-red-950/40 border border-red-900/60 rounded-lg text-xs text-red-300 flex items-center gap-2">
          <AlertCircle class="w-3.5 h-3.5 shrink-0" />
          <span>{pwdError}</span>
        </div>
      {/if}

      {#if pwdMessage}
        <div class="mb-3 p-2.5 bg-emerald-950/40 border border-emerald-900/60 rounded-lg text-xs text-emerald-300 flex items-center gap-2">
          <CheckCircle class="w-3.5 h-3.5 shrink-0" />
          <span>{pwdMessage}</span>
        </div>
      {/if}

      <form onsubmit={handleChangePassword} class="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div>
          <label for="curr-pwd" class="block text-[11px] font-medium text-zinc-400 mb-1">Current Password</label>
          <input
            id="curr-pwd"
            type="password"
            bind:value={currentPassword}
            required
            placeholder="••••••••"
            class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs focus:outline-none focus:border-zinc-700 text-white placeholder:text-zinc-600"
          />
        </div>

        <div>
          <label for="new-pwd" class="block text-[11px] font-medium text-zinc-400 mb-1">New Password (8+ chars)</label>
          <input
            id="new-pwd"
            type="password"
            bind:value={newPassword}
            required
            minlength="8"
            placeholder="••••••••"
            class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs focus:outline-none focus:border-zinc-700 text-white placeholder:text-zinc-600"
          />
        </div>

        <div>
          <label for="conf-pwd" class="block text-[11px] font-medium text-zinc-400 mb-1">Confirm New Password</label>
          <input
            id="conf-pwd"
            type="password"
            bind:value={confirmNewPassword}
            required
            minlength="8"
            placeholder="••••••••"
            class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs focus:outline-none focus:border-zinc-700 text-white placeholder:text-zinc-600"
          />
        </div>

        <div class="md:col-span-3 flex justify-end pt-1">
          <button
            type="submit"
            disabled={isUpdatingPwd || !currentPassword || !newPassword}
            class="px-4 py-2 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition disabled:opacity-50 cursor-pointer active:scale-95"
          >
            {isUpdatingPwd ? 'Updating...' : 'Update Password'}
          </button>
        </div>
      </form>
    </div>

    <!-- MFA / Two-Factor Authentication Section -->
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-5 shadow-xs relative">
      <div class="flex items-center justify-between mb-4">
        <div>
          <h3 class="font-semibold text-sm text-white flex items-center gap-2">
            <Lock class="w-4 h-4 text-zinc-400" />
            Two-Factor Authentication (TOTP)
          </h3>
          <p class="text-xs text-zinc-400 mt-0.5">
            Require a 6-digit TOTP code from Google Authenticator, Authy, or 1Password when signing in.
          </p>
        </div>

        <span class="px-2.5 py-0.5 rounded-md text-xs font-medium {mfaEnabled ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-900/60' : 'bg-zinc-800 text-zinc-400'}">
          {mfaEnabled ? 'Enabled' : 'Disabled'}
        </span>
      </div>

      <!-- Popup Alert Messages -->
      {#if mfaMessage}
        <div class="mb-3 p-3 bg-emerald-950/40 border border-emerald-900/60 rounded-lg text-xs text-emerald-300 flex items-center justify-between gap-2 shadow-xs">
          <div class="flex items-center gap-2">
            <CheckCircle class="w-4 h-4 shrink-0 text-emerald-400" />
            <span class="font-medium">{mfaMessage}</span>
          </div>
          <button onclick={() => (mfaMessage = '')} class="p-1 hover:bg-emerald-900/40 rounded text-emerald-400 cursor-pointer">
            <X class="w-3.5 h-3.5" />
          </button>
        </div>
      {/if}

      {#if mfaError && !mfaSetupData}
        <div class="mb-3 p-3 bg-red-950/40 border border-red-900/60 rounded-lg text-xs text-red-300 flex items-center justify-between gap-2 shadow-xs">
          <div class="flex items-center gap-2">
            <AlertCircle class="w-4 h-4 shrink-0 text-red-400" />
            <span class="font-medium">{mfaError}</span>
          </div>
          <button onclick={() => (mfaError = '')} class="p-1 hover:bg-red-900/40 rounded text-red-400 cursor-pointer">
            <X class="w-3.5 h-3.5" />
          </button>
        </div>
      {/if}

      <!-- MFA Enabled view -->
      {#if mfaEnabled}
        <div class="p-4 bg-emerald-950/20 border border-emerald-900/40 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-lg bg-emerald-900/40 text-emerald-400 flex items-center justify-center shrink-0">
              <Shield class="w-4 h-4" />
            </div>
            <div>
              <h4 class="text-xs font-semibold text-white">Akun Anda Dilindungi 2FA</h4>
              <p class="text-[11px] text-zinc-400 mt-0.5">Kode autentikasi dari ponsel Anda aktif untuk setiap sesi login.</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            {#if backupCodes.length > 0}
              <button
                type="button"
                onclick={() => (showBackupCodesModal = true)}
                class="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-lg text-xs font-medium transition cursor-pointer"
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
              class="px-3 py-1.5 bg-red-950/40 hover:bg-red-900/50 text-red-400 border border-red-900/60 rounded-lg text-xs font-medium transition cursor-pointer"
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
          class="px-4 py-2 bg-white text-zinc-950 hover:bg-zinc-200 disabled:opacity-50 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer flex items-center gap-2 active:scale-95"
        >
          {#if isSettingUpMfa}
            <div class="w-3.5 h-3.5 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin"></div>
            <span>Menyiapkan...</span>
          {:else}
            <QrCode class="w-4 h-4" />
            <span>Setup Two-Factor Authentication</span>
          {/if}
        </button>
      {/if}

      <!-- MFA Setup Wizard -->
      {#if mfaSetupData && !mfaEnabled}
        <div class="p-5 bg-zinc-900/60 border border-zinc-800 rounded-xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
          <div class="flex items-center justify-between">
            <h4 class="font-semibold text-xs text-white flex items-center gap-1.5">
              <QrCode class="w-4 h-4 text-zinc-400" />
              Scan QR Code Dengan Authenticator
            </h4>
            <div class="flex items-center gap-2">
              <button
                type="button"
                onclick={() => startMfaSetup(true)}
                title="Regenerate QR Code & Secret Key"
                class="flex items-center gap-1 text-[11px] text-zinc-400 hover:text-white font-medium cursor-pointer"
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
                class="p-1 text-zinc-400 hover:text-white rounded-md cursor-pointer"
              >
                <X class="w-4 h-4" />
              </button>
            </div>
          </div>

          {#if mfaError}
            <div class="p-2.5 bg-red-950/40 border border-red-900/60 rounded-lg text-xs text-red-300 flex items-center justify-between gap-2 shadow-xs">
              <div class="flex items-center gap-2">
                <AlertCircle class="w-4 h-4 shrink-0 text-red-400" />
                <span>{mfaError}</span>
              </div>
              <button onclick={() => (mfaError = '')} class="p-1 hover:bg-red-900/40 rounded text-red-400 cursor-pointer">
                <X class="w-3 h-3" />
              </button>
            </div>
          {/if}

          <div class="flex flex-col sm:flex-row items-center gap-5">
            <div class="p-2.5 bg-white rounded-xl shadow-md border border-zinc-300 shrink-0">
              <img
                src={mfaSetupData.qrCodeDataUrl}
                alt="MFA QR Code"
                class="w-36 h-36 object-contain rounded"
              />
            </div>

            <div class="space-y-3 flex-1 text-xs w-full">
              <p class="text-zinc-400">
                Pindai QR code dengan Google Authenticator atau Authy, atau salin secret key:
              </p>
              <div class="flex items-center gap-2">
                <code class="flex-1 p-2 bg-zinc-900 border border-zinc-800 rounded-lg font-mono text-xs text-zinc-200 font-bold select-all tracking-wider">
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
                  class="p-2 bg-zinc-900 border border-zinc-800 hover:border-zinc-700 rounded-lg text-zinc-400 hover:text-white transition cursor-pointer"
                >
                  <Copy class="w-3.5 h-3.5" />
                </button>
              </div>

              <div>
                <label for="totp-input" class="block text-[11px] font-medium mb-1 text-zinc-300">
                  Masukkan 6-digit kode verifikasi:
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
                    class="w-32 text-center font-mono text-sm tracking-widest px-3 py-1.5 bg-zinc-900 border border-zinc-800 focus:border-zinc-700 rounded-lg text-white"
                  />
                  <button
                    type="button"
                    disabled={isConfirmingMfa || totpCode.length !== 6}
                    onclick={confirmMfa}
                    class="px-4 py-1.5 bg-white text-zinc-950 hover:bg-zinc-200 disabled:opacity-50 font-semibold rounded-lg text-xs shadow-xs transition cursor-pointer flex items-center gap-1.5 active:scale-95"
                  >
                    {#if isConfirmingMfa}
                      <div class="w-3 h-3 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin"></div>
                      <span>Verifying...</span>
                    {:else}
                      <Check class="w-3.5 h-3.5" />
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
      <div class="fixed inset-0 bg-black/70 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
        <div class="w-full max-w-md bg-[#121214] border border-zinc-800/80 rounded-xl shadow-2xl p-5 overflow-hidden animate-in zoom-in-95 duration-150">
          <div class="flex items-center gap-2.5 mb-3">
            <div class="w-8 h-8 rounded-lg bg-emerald-950/60 text-emerald-400 flex items-center justify-center shrink-0">
              <CheckCircle class="w-4 h-4" />
            </div>
            <div>
              <h3 class="font-semibold text-sm text-white">
                2FA Berhasil Diaktifkan! 🎉
              </h3>
              <p class="text-[11px] text-zinc-400">
                Simpan 10 Backup Codes ini di tempat yang aman.
              </p>
            </div>
          </div>

          <div class="p-2.5 bg-zinc-900/80 border border-zinc-800 rounded-lg text-[11px] text-zinc-300 mb-3">
            Gunakan backup code ini jika Anda kehilangan akses ke aplikasi authenticator di ponsel Anda.
          </div>

          <div class="grid grid-cols-2 gap-1.5 font-mono text-xs font-semibold text-zinc-200 p-2.5 bg-zinc-950 rounded-lg border border-zinc-800/80 mb-4">
            {#each backupCodes as code}
              <div class="p-1.5 bg-[#121214] rounded text-center border border-zinc-800 select-all">
                {code}
              </div>
            {/each}
          </div>

          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-1.5">
              <button
                type="button"
                onclick={copyBackupCodes}
                class="flex items-center gap-1 px-2.5 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-lg text-xs font-medium transition cursor-pointer"
              >
                {#if codesCopied}
                  <Check class="w-3.5 h-3.5 text-emerald-400" />
                  <span>Copied</span>
                {:else}
                  <Copy class="w-3.5 h-3.5" />
                  <span>Copy</span>
                {/if}
              </button>

              <button
                type="button"
                onclick={downloadBackupCodes}
                class="flex items-center gap-1 px-2.5 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-lg text-xs font-medium transition cursor-pointer"
              >
                <Download class="w-3.5 h-3.5" />
                <span>.TXT</span>
              </button>
            </div>

            <button
              type="button"
              onclick={() => (showBackupCodesModal = false)}
              class="px-3.5 py-1.5 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer"
            >
              Done
            </button>
          </div>
        </div>
      </div>
    {/if}

    <!-- Disable MFA Modal Popup -->
    {#if showDisableModal}
      <div class="fixed inset-0 bg-black/70 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
        <div class="w-full max-w-sm bg-[#121214] border border-zinc-800/80 rounded-xl shadow-2xl p-5 overflow-hidden animate-in zoom-in-95 duration-150">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-semibold text-sm text-red-400 flex items-center gap-1.5">
              <AlertCircle class="w-4 h-4" />
              Nonaktifkan 2FA
            </h3>
            <button onclick={() => (showDisableModal = false)} class="p-1 hover:bg-zinc-800 rounded-md cursor-pointer">
              <X class="w-4 h-4 text-zinc-400" />
            </button>
          </div>

          {#if disableError}
            <div class="mb-3 p-2 bg-red-950/40 border border-red-900/60 rounded-lg text-xs text-red-300">
              {disableError}
            </div>
          {/if}

          <p class="text-xs text-zinc-400 mb-3">
            Masukkan kode TOTP dari ponsel atau backup code untuk konfirmasi nonaktifkan:
          </p>

          <div class="space-y-3 mb-4">
            <div>
              <input
                id="disable-code"
                type="text"
                bind:value={disableTotpCode}
                placeholder="123456 atau ABCD-1234"
                class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs text-white placeholder:text-zinc-500 focus:outline-none focus:border-zinc-700"
              />
            </div>
          </div>

          <div class="flex items-center justify-end gap-2">
            <button
              type="button"
              onclick={() => (showDisableModal = false)}
              class="px-3 py-1.5 hover:bg-zinc-800 text-zinc-400 hover:text-white rounded-lg text-xs font-medium cursor-pointer"
            >
              Batal
            </button>
            <button
              type="button"
              disabled={isDisablingMfa}
              onclick={handleDisableMfa}
              class="px-3 py-1.5 bg-red-950/60 hover:bg-red-900/60 border border-red-900/60 text-red-300 hover:text-white rounded-lg text-xs font-medium shadow-xs transition cursor-pointer"
            >
              {isDisablingMfa ? 'Menonaktifkan...' : 'Konfirmasi Nonaktifkan'}
            </button>
          </div>
        </div>
      </div>
    {/if}

    <!-- Security Events Log -->
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-5 shadow-xs">
      <h3 class="font-semibold text-sm text-white mb-0.5">Recent Security Activity</h3>
      <p class="text-xs text-zinc-400 mb-3">Audit trail of logins, password resets, and security modifications.</p>

      <div class="divide-y divide-zinc-800/60 max-h-56 overflow-y-auto">
        {#each events as ev}
          <div class="py-2 flex items-center justify-between text-xs">
            <div>
              <span class="font-medium text-zinc-200 capitalize">
                {ev.eventType.replace(/_/g, ' ')}
              </span>
              <span class="text-[10px] text-zinc-500 block">IP: {ev.ipAddress || '127.0.0.1'}</span>
            </div>
            <span class="text-zinc-500 text-[10px]">{formatDate(ev.createdAt)}</span>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>
