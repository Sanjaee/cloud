<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { Cloud, Lock, Mail, User, ArrowRight, AlertCircle, CheckCircle } from 'lucide-svelte';

  let email = $state('');
  let username = $state('');
  let password = $state('');
  let error = $state('');
  let isSuccess = $state(false);
  let isLoading = $state(false);

  // Password requirements
  let hasLength = $derived(password.length >= 8);
  let hasUpper = $derived(/[A-Z]/.test(password));
  let hasLower = $derived(/[a-z]/.test(password));
  let hasNumber = $derived(/[0-9]/.test(password));
  let isPasswordStrong = $derived(hasLength && hasUpper && hasLower && hasNumber);

  async function handleRegister(e: Event) {
    e.preventDefault();
    if (!isPasswordStrong) {
      error = 'Please satisfy all password security requirements.';
      return;
    }

    error = '';
    isLoading = true;

    try {
      await apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, username, password }),
      });
      isSuccess = true;
    } catch (err: any) {
      error = err.message || 'Registration failed. Please try again.';
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
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Create Account</h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Get 10 GB of secure multi-user cloud storage</p>
    </div>

    {#if isSuccess}
      <div class="p-6 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-xl text-center">
        <CheckCircle class="w-12 h-12 text-emerald-600 dark:text-emerald-400 mx-auto mb-3" />
        <h3 class="font-bold text-emerald-900 dark:text-emerald-200 text-lg">Registration Successful!</h3>
        <p class="text-xs text-emerald-700 dark:text-emerald-300 mt-1 mb-5">
          Your account has been created. You can now sign in immediately.
        </p>
        <a
          href="/login"
          class="inline-flex items-center justify-center gap-2 px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-semibold shadow-md transition"
        >
          Sign In Now
        </a>
      </div>
    {:else}
      {#if error}
        <div class="mb-5 p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900/60 rounded-xl flex items-center gap-3 text-red-700 dark:text-red-300 text-sm">
          <AlertCircle class="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      {/if}

      <form onsubmit={handleRegister} class="space-y-4">
        <div>
          <label for="reg-email" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Email Address
          </label>
          <div class="relative">
            <Mail class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              id="reg-email"
              type="email"
              bind:value={email}
              required
              placeholder="you@company.com"
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
            />
          </div>
        </div>

        <div>
          <label for="reg-username" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Username
          </label>
          <div class="relative">
            <User class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              id="reg-username"
              type="text"
              bind:value={username}
              required
              minlength="3"
              placeholder="alex_smith"
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
            />
          </div>
        </div>

        <div>
          <label for="reg-pass" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
            Password (Argon2id Encrypted)
          </label>
          <div class="relative">
            <Lock class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
            <input
              id="reg-pass"
              type="password"
              bind:value={password}
              required
              placeholder="••••••••••••"
              class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
            />
          </div>

          <!-- Password Strength Checklist -->
          <div class="mt-2.5 grid grid-cols-2 gap-1.5 text-xs text-slate-500 dark:text-slate-400">
            <div class="flex items-center gap-1.5 {hasLength ? 'text-emerald-600 font-medium' : ''}">
              <div class="w-1.5 h-1.5 rounded-full {hasLength ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-700'}"></div>
              8+ Characters
            </div>
            <div class="flex items-center gap-1.5 {hasUpper ? 'text-emerald-600 font-medium' : ''}">
              <div class="w-1.5 h-1.5 rounded-full {hasUpper ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-700'}"></div>
              1 Uppercase Letter
            </div>
            <div class="flex items-center gap-1.5 {hasLower ? 'text-emerald-600 font-medium' : ''}">
              <div class="w-1.5 h-1.5 rounded-full {hasLower ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-700'}"></div>
              1 Lowercase Letter
            </div>
            <div class="flex items-center gap-1.5 {hasNumber ? 'text-emerald-600 font-medium' : ''}">
              <div class="w-1.5 h-1.5 rounded-full {hasNumber ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-700'}"></div>
              1 Number
            </div>
          </div>
        </div>

        <button
          type="submit"
          disabled={isLoading || !isPasswordStrong}
          class="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm rounded-xl shadow-lg shadow-blue-500/20 disabled:opacity-50 transition cursor-pointer"
        >
          {#if isLoading}
            <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            Creating Account...
          {:else}
            Create Free Account
            <ArrowRight class="w-4 h-4" />
          {/if}
        </button>
      </form>
    {/if}

    <div class="mt-8 pt-6 border-t border-slate-200 dark:border-slate-800 text-center">
      <p class="text-sm text-slate-600 dark:text-slate-400">
        Already have an account?
        <a href="/login" class="font-semibold text-blue-600 dark:text-blue-400 hover:underline ml-1">
          Sign In
        </a>
      </p>
    </div>
  </div>
</div>

