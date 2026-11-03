<script lang="ts">
  import { page } from '$app/state';
  import { apiRequest } from '$lib/api/client';
  import { Cloud, Lock, ArrowRight, AlertCircle, CheckCircle } from 'lucide-svelte';

  let token = $derived(page.url.searchParams.get('token') || '');
  let newPassword = $state('');
  let confirmPassword = $state('');
  let error = $state('');
  let isSuccess = $state(false);
  let isLoading = $state(false);

  async function handleReset(e: Event) {
    e.preventDefault();
    if (newPassword.length < 8) {
      error = 'Password must be at least 8 characters long.';
      return;
    }
    if (newPassword !== confirmPassword) {
      error = 'Passwords do not match.';
      return;
    }

    error = '';
    isLoading = true;

    try {
      await apiRequest('/security/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword }),
      });
      isSuccess = true;
    } catch (err: any) {
      error = err.message || 'Password reset failed. The link may have expired.';
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="flex min-h-screen items-center justify-center p-4 bg-gradient-to-br from-slate-100 via-slate-50 to-blue-50/30 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
  <div class="w-full max-w-md bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200/80 dark:border-slate-800 p-8">
    <div class="flex flex-col items-center mb-6">
      <div class="w-14 h-14 bg-blue-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-blue-500/30 mb-4">
        <Cloud class="w-8 h-8" />
      </div>
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Create New Password</h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Please enter your new strong password</p>
    </div>

    {#if isSuccess}
      <div class="p-6 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-xl text-center">
        <CheckCircle class="w-12 h-12 text-emerald-600 dark:text-emerald-400 mx-auto mb-3" />
        <h3 class="font-bold text-emerald-900 dark:text-emerald-200 text-lg">Password Changed!</h3>
        <p class="text-xs text-emerald-700 dark:text-emerald-300 mt-1 mb-5">
          Your password has been securely updated. You can now log in with your new credentials.
        </p>
        <a
          href="/login"
          class="inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-semibold shadow-md transition"
        >
          Proceed to Sign In
        </a>
      </div>
    {:else}
      {#if error}
        <div class="mb-5 p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-xl flex items-center gap-3 text-red-700 dark:text-red-300 text-sm">
          <AlertCircle class="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      {/if}

      <form onsubmit={handleReset} class="space-y-4">
        <div>
          <label for="new-pass" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            New Password
          </label>
          <div class="relative">
            <Lock class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              id="new-pass"
              type="password"
              bind:value={newPassword}
              required
              minlength="8"
              placeholder="••••••••••••"
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
            />
          </div>
        </div>

        <div>
          <label for="conf-pass" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Confirm Password
          </label>
          <div class="relative">
            <Lock class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              id="conf-pass"
              type="password"
              bind:value={confirmPassword}
              required
              minlength="8"
              placeholder="••••••••••••"
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={isLoading}
          class="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm rounded-xl shadow-lg shadow-blue-500/20 disabled:opacity-50 transition cursor-pointer"
        >
          {#if isLoading}
            <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            Updating Password...
          {:else}
            Update Password
            <ArrowRight class="w-4 h-4" />
          {/if}
        </button>
      </form>
    {/if}
  </div>
</div>

