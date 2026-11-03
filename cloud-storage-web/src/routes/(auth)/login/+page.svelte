<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { authStore } from '$lib/stores/auth.store';
  import { Cloud, Lock, Mail, ShieldCheck, ArrowRight, AlertCircle } from 'lucide-svelte';

  let email = $state('');
  let password = $state('');
  let mfaCode = $state('');
  let mfaRequired = $state(false);
  let error = $state('');
  let isLoading = $state(false);

  async function handleSubmit(e: Event) {
    e.preventDefault();
    error = '';
    isLoading = true;

    try {
      const res = await apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          email,
          password,
          mfaCode: mfaCode || undefined,
        }),
      });

      if (res?.mfaRequired) {
        mfaRequired = true;
        isLoading = false;
        return;
      }

      await authStore.getState().fetchUser();
      window.location.href = '/drive';
    } catch (err: any) {
      error = err.message || 'Login failed. Please check your credentials.';
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="flex min-h-screen items-center justify-center p-4 bg-gradient-to-br from-slate-100 via-slate-50 to-blue-50/30 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
  <div class="w-full max-w-md bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-200/80 dark:border-slate-800 p-8">
    <div class="flex flex-col items-center mb-8">
      <div class="w-14 h-14 bg-blue-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-blue-500/30 mb-4">
        <Cloud class="w-8 h-8" />
      </div>
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">REA Cloud Storage</h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Sign in to access your cloud workspace</p>
    </div>

    {#if error}
      <div class="mb-6 p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-xl flex items-center gap-3 text-red-700 dark:text-red-300 text-sm">
        <AlertCircle class="w-5 h-5 shrink-0" />
        <span>{error}</span>
      </div>
    {/if}

    <form onsubmit={handleSubmit} class="space-y-4">
      <div>
        <label for="email" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
          Email Address
        </label>
        <div class="relative">
          <Mail class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            id="email"
            type="email"
            bind:value={email}
            required
            placeholder="you@company.com"
            class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-slate-900 dark:text-white transition"
          />
        </div>
      </div>

      <div>
        <div class="flex justify-between items-center mb-1.5">
          <label for="password" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
            Password
          </label>
          <a href="/forgot-password" class="text-xs text-blue-600 dark:text-blue-400 hover:underline">
            Forgot password?
          </a>
        </div>
        <div class="relative">
          <Lock class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
          <input
            id="password"
            type="password"
            bind:value={password}
            required
            placeholder="••••••••••••"
            class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-slate-900 dark:text-white transition"
          />
        </div>
      </div>

      {#if mfaRequired}
        <div class="p-4 bg-blue-50/70 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 rounded-xl animate-in fade-in">
          <div class="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-blue-700 dark:text-blue-300 mb-2">
            <ShieldCheck class="w-4 h-4" />
            Two-Factor Authentication Code
          </div>
          <input
            type="text"
            bind:value={mfaCode}
            placeholder="6-digit code"
            maxlength="6"
            class="w-full text-center text-lg font-mono tracking-widest py-2 bg-white dark:bg-slate-800 border border-blue-300 dark:border-blue-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      {/if}

      <button
        type="submit"
        disabled={isLoading}
        class="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm rounded-xl shadow-lg shadow-blue-500/20 disabled:opacity-50 transition cursor-pointer"
      >
        {#if isLoading}
          <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
          Signing In...
        {:else}
          Sign In
          <ArrowRight class="w-4 h-4" />
        {/if}
      </button>
    </form>

    <div class="mt-8 pt-6 border-t border-slate-200 dark:border-slate-800 text-center">
      <p class="text-sm text-slate-600 dark:text-slate-400">
        Don't have an account yet?
        <a href="/register" class="font-semibold text-blue-600 dark:text-blue-400 hover:underline ml-1">
          Create Account
        </a>
      </p>
    </div>
  </div>
</div>

