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

  // MFA setup state
  let mfaSetupData = $state<{ secret: string; qrCodeDataUrl: string } | null>(null);
  let totpCode = $state('');
  let backupCodes = $state<string[]>([]);
  let mfaMessage = $state('');
  let mfaError = $state('');

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

  async function startMfaSetup() {
    mfaError = '';
    mfaMessage = '';
    try {
      const res = await apiRequest('/mfa/setup', { method: 'POST' });
      mfaSetupData = res;
    } catch (err: any) {
      mfaError = err.message;
    }
  }

  async function confirmMfa() {
    if (!totpCode || totpCode.length !== 6) {
      mfaError = 'Please enter a valid 6-digit TOTP code';
      return;
    }

    mfaError = '';
    try {
      const res = await apiRequest('/mfa/enable', {
        method: 'POST',
        body: JSON.stringify({ code: totpCode }),
      });
      backupCodes = res.backupCodes || [];
      mfaEnabled = true;
      mfaSetupData = null;
      mfaMessage = 'MFA successfully enabled!';
    } catch (err: any) {
      mfaError = err.message || 'Invalid verification code';
    }
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
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs">
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

      {#if mfaMessage}
        <div class="mb-4 p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-xl text-xs text-emerald-700 dark:text-emerald-300 flex items-center gap-2">
          <CheckCircle class="w-4 h-4 shrink-0" />
          <span>{mfaMessage}</span>
        </div>
      {/if}

      {#if !mfaEnabled && !mfaSetupData}
        <button
          onclick={startMfaSetup}
          class="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer"
        >
          Setup Two-Factor Authentication
        </button>
      {/if}

      <!-- MFA Setup Wizard -->
      {#if mfaSetupData}
        <div class="p-6 bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-2xl space-y-4">
          <h4 class="font-bold text-xs uppercase tracking-wider text-slate-600 dark:text-slate-300">
            Scan QR Code with your Authenticator App
          </h4>

          <div class="flex flex-col sm:flex-row items-center gap-6">
            <img
              src={mfaSetupData.qrCodeDataUrl}
              alt="MFA QR Code"
              class="w-40 h-40 bg-white p-2 rounded-xl shadow-md border border-slate-200"
            />

            <div class="space-y-3 flex-1 text-xs">
              <p class="text-slate-600 dark:text-slate-400">
                Or manually enter this secret key into your authenticator app:
              </p>
              <code class="block p-2.5 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono text-blue-600 dark:text-blue-400 select-all font-bold">
                {mfaSetupData.secret}
              </code>

              <div>
                <label for="totp-input" class="block text-xs font-semibold mb-1">Enter 6-digit code to verify:</label>
                <div class="flex gap-2">
                  <input
                    id="totp-input"
                    type="text"
                    bind:value={totpCode}
                    maxlength="6"
                    placeholder="123456"
                    class="w-32 text-center font-mono text-base tracking-widest px-3 py-1.5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-xl"
                  />
                  <button
                    onclick={confirmMfa}
                    class="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl text-xs shadow-md cursor-pointer"
                  >
                    Confirm & Enable
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      {/if}

      <!-- Backup codes display -->
      {#if backupCodes.length > 0}
        <div class="mt-4 p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 rounded-2xl">
          <h4 class="font-bold text-xs text-amber-900 dark:text-amber-200 mb-1">
            Save Your One-Time Backup Recovery Codes
          </h4>
          <p class="text-[11px] text-amber-700 dark:text-amber-300 mb-3">
            If you lose access to your authenticator device, each code can be used once to access your account.
          </p>
          <div class="grid grid-cols-2 sm:grid-cols-5 gap-2 font-mono text-xs font-bold text-slate-800 dark:text-slate-200">
            {#each backupCodes as code}
              <span class="p-1.5 bg-white dark:bg-slate-900 rounded-lg text-center border border-amber-200 dark:border-amber-800">
                {code}
              </span>
            {/each}
          </div>
        </div>
      {/if}
    </div>

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

