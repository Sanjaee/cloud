<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { Cloud, Mail, ArrowLeft, ArrowRight, CheckCircle } from 'lucide-svelte';

  let email = $state('');
  let submitted = $state(false);
  let isLoading = $state(false);

  async function handleForgot(e: Event) {
    e.preventDefault();
    isLoading = true;
    try {
      await apiRequest('/security/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      });
      submitted = true;
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
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Reset Password</h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1 text-center">
        Enter your registered email address and we'll send you recovery instructions.
      </p>
    </div>

    {#if submitted}
      <div class="p-6 bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 rounded-xl text-center">
        <CheckCircle class="w-12 h-12 text-blue-600 dark:text-blue-400 mx-auto mb-3" />
        <h3 class="font-bold text-slate-900 dark:text-white text-base">Check Your Inbox</h3>
        <p class="text-xs text-slate-600 dark:text-slate-400 mt-2 mb-4 leading-relaxed">
          If an account exists for <span class="font-medium text-slate-900 dark:text-slate-200">{email}</span>, you will receive password reset instructions.
        </p>
        <a href="/login" class="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-600 hover:underline">
          <ArrowLeft class="w-4 h-4" /> Back to Sign In
        </a>
      </div>
    {:else}
      <form onsubmit={handleForgot} class="space-y-4">
        <div>
          <label for="forgot-email" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Registered Email Address
          </label>
          <div class="relative">
            <Mail class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              id="forgot-email"
              type="email"
              bind:value={email}
              required
              placeholder="you@company.com"
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
            Sending Instructions...
          {:else}
            Send Reset Link
            <ArrowRight class="w-4 h-4" />
          {/if}
        </button>
      </form>
    {/if}

    <div class="mt-8 pt-6 border-t border-slate-200 dark:border-slate-800 text-center">
      <a href="/login" class="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white">
        <ArrowLeft class="w-4 h-4" />
        Back to Sign In
      </a>
    </div>
  </div>
</div>

