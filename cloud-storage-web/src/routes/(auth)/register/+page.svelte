<script lang="ts">
  import { onMount } from 'svelte';
  import { apiRequest } from '$lib/api/client';
  import { Cloud, Lock, Mail, User, ArrowRight, AlertCircle, CheckCircle, Sparkles, ShieldCheck } from 'lucide-svelte';

  let email = $state('');
  let username = $state('');
  let password = $state('');
  let error = $state('');
  let isSuccess = $state(false);
  let isLoading = $state(false);
  let isGoogleLoading = $state(false);
  let showManualForm = $state(false);

  // Password requirements
  let hasLength = $derived(password.length >= 8);
  let hasUpper = $derived(/[A-Z]/.test(password));
  let hasLower = $derived(/[a-z]/.test(password));
  let hasNumber = $derived(/[0-9]/.test(password));
  let isPasswordStrong = $derived(hasLength && hasUpper && hasLower && hasNumber);

  onMount(() => {
    const params = new URLSearchParams(window.location.search);
    const err = params.get('error');
    if (err) {
      error = decodeURIComponent(err);
    }
  });

  async function handleGoogleSignUp() {
    if (isGoogleLoading) return;
    error = '';
    isGoogleLoading = true;

    try {
      const res = await apiRequest('/auth/google/url');
      if (res?.data?.url) {
        window.location.href = res.data.url;
      } else {
        window.location.href = '/api/v1/auth/google';
      }
    } catch {
      window.location.href = '/api/v1/auth/google';
    }
  }

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

<svelte:head>
  <title>Create Account — REA Cloud Storage</title>
</svelte:head>

<div class="flex min-h-screen items-center justify-center p-4 bg-gradient-to-br from-slate-100 via-slate-50 to-blue-50/30 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
  <div class="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200/80 dark:border-slate-800 p-8 sm:p-10 transition-all">
    <div class="flex flex-col items-center mb-8">
      <div class="w-16 h-16 bg-gradient-to-tr from-blue-600 to-indigo-600 rounded-2xl flex items-center justify-center text-white shadow-xl shadow-blue-500/25 mb-4 ring-4 ring-blue-500/10">
        <Cloud class="w-9 h-9" />
      </div>
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Create Account</h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1.5 text-center">
        Get 10 GB of secure multi-user cloud storage instantly
      </p>
    </div>

    {#if isSuccess}
      <div class="p-6 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-2xl text-center animate-in fade-in">
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
        <div class="mb-6 p-4 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900/60 rounded-2xl flex items-start gap-3 text-red-700 dark:text-red-300 text-sm animate-in fade-in">
          <AlertCircle class="w-5 h-5 shrink-0 mt-0.5 text-red-600 dark:text-red-400" />
          <div class="flex-1">
            <p class="font-medium">Registration Error</p>
            <p class="text-xs text-red-600 dark:text-red-400 mt-0.5">{error}</p>
          </div>
        </div>
      {/if}

      <!-- PRIMARY: Google Sign-Up Button -->
      <div class="space-y-4">
        <button
          type="button"
          onclick={handleGoogleSignUp}
          disabled={isGoogleLoading || isLoading}
          class="w-full relative flex items-center justify-center gap-3.5 py-3.5 px-5 bg-white hover:bg-slate-50 dark:bg-slate-800/90 dark:hover:bg-slate-800 text-slate-800 dark:text-slate-100 font-medium text-base rounded-2xl border border-slate-300 dark:border-slate-700 shadow-sm hover:shadow-md hover:border-slate-400 dark:hover:border-slate-600 transition duration-150 disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer focus:outline-none focus:ring-4 focus:ring-blue-500/15"
        >
          {#if isGoogleLoading}
            <div class="w-5 h-5 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
            <span>Connecting to Google...</span>
          {:else}
            <!-- Official Google "G" Logo SVG -->
            <svg class="w-5 h-5 shrink-0" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
              />
            </svg>
            <span class="tracking-tight">Sign up with Google</span>
          {/if}
        </button>

        <div class="flex items-center gap-2 justify-center text-xs text-slate-500 dark:text-slate-400">
          <ShieldCheck class="w-3.5 h-3.5 text-emerald-500" />
          <span>Automatic verified setup • No password needed</span>
        </div>
      </div>

      <!-- Divider -->
      <div class="relative my-7">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-slate-200 dark:border-slate-800"></div>
        </div>
        <div class="relative flex justify-center text-xs">
          <button
            type="button"
            onclick={() => showManualForm = !showManualForm}
            class="bg-white dark:bg-slate-900 px-3 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 font-medium cursor-pointer transition"
          >
            {showManualForm ? 'Hide manual registration' : 'Or register with email'}
          </button>
        </div>
      </div>

      <!-- Manual Form -->
      {#if showManualForm}
        <form onsubmit={handleRegister} class="space-y-4 animate-in fade-in duration-200">
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
                placeholder="you@example.com"
                class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white transition"
              />
            </div>
          </div>

          <div>
            <label for="username" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Username
            </label>
            <div class="relative">
              <User class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
              <input
                id="username"
                type="text"
                bind:value={username}
                required
                minlength="3"
                placeholder="username"
                class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white transition"
              />
            </div>
          </div>

          <div>
            <label for="password" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Password (Argon2id Encrypted)
            </label>
            <div class="relative">
              <Lock class="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
              <input
                id="password"
                type="password"
                bind:value={password}
                required
                placeholder="••••••••••••"
                class="w-full pl-11 pr-4 py-2.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white transition"
              />
            </div>
          </div>

          <!-- Password Checklist -->
          <div class="p-3 bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-700/60 rounded-xl text-xs space-y-1 text-slate-500 dark:text-slate-400">
            <div class="flex items-center gap-1.5 {hasLength ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}">
              <span>{hasLength ? '✓' : '○'}</span> At least 8 characters
            </div>
            <div class="flex items-center gap-1.5 {hasUpper && hasLower ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}">
              <span>{hasUpper && hasLower ? '✓' : '○'}</span> Uppercase and lowercase letters
            </div>
            <div class="flex items-center gap-1.5 {hasNumber ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}">
              <span>{hasNumber ? '✓' : '○'}</span> At least one number
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading || isGoogleLoading || !isPasswordStrong}
            class="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 bg-slate-800 hover:bg-slate-900 dark:bg-slate-700 dark:hover:bg-slate-600 text-white font-medium text-sm rounded-xl shadow-md disabled:opacity-50 transition cursor-pointer"
          >
            {#if isLoading}
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Creating Account...</span>
            {:else}
              <span>Create Account</span>
              <ArrowRight class="w-4 h-4" />
            {/if}
          </button>
        </form>
      {/if}
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
