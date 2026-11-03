<script lang="ts">
  import { onMount } from 'svelte';
  import { apiRequest } from '$lib/api/client';
  import { authStore } from '$lib/stores/auth.store';
  import { Cloud, Lock, Mail, ShieldCheck, ArrowRight, AlertCircle, Sparkles, User, RefreshCw, X } from 'lucide-svelte';

  let email = $state('');
  let password = $state('');
  let mfaCode = $state('');
  let mfaRequired = $state(false);
  let googleMfaToken = $state('');
  let mfaEmail = $state('');
  let isMfaVerifying = $state(false);
  let error = $state('');
  let isLoading = $state(false);
  let isGoogleLoading = $state(false);
  let showPasswordForm = $state(false);
  let lastGoogleUser = $state<{ email: string; name: string; avatarUrl?: string } | null>(null);

  onMount(() => {
    const params = new URLSearchParams(window.location.search);
    const err = params.get('error');
    if (err) {
      error = decodeURIComponent(err);
    }

    if (params.get('mfa_required') === 'true' && params.get('mfa_token')) {
      mfaRequired = true;
      googleMfaToken = params.get('mfa_token') || '';
      mfaEmail = params.get('email') || '';
    }

    try {
      const saved = localStorage.getItem('rea_last_google_user');
      if (saved) {
        lastGoogleUser = JSON.parse(saved);
      }
    } catch {}
  });

  async function handleGoogleSignIn(options: { loginHint?: string; prompt?: string } = {}) {
    if (isGoogleLoading) return;
    error = '';
    isGoogleLoading = true;

    try {
      const queryParams = new URLSearchParams();
      if (options.loginHint) {
        queryParams.set('login_hint', options.loginHint);
      }
      if (options.prompt) {
        queryParams.set('prompt', options.prompt);
      }

      const queryString = queryParams.toString() ? `?${queryParams.toString()}` : '';
      // Get the verified Google OAuth URL from backend
      const res = await apiRequest(`/auth/google/url${queryString}`);
      if (res?.data?.url) {
        window.location.href = res.data.url;
      } else {
        window.location.href = `/api/v1/auth/google${queryString}`;
      }
    } catch {
      // Fallback direct backend route
      const queryParams = new URLSearchParams();
      if (options.loginHint) queryParams.set('login_hint', options.loginHint);
      if (options.prompt) queryParams.set('prompt', options.prompt);
      const queryString = queryParams.toString() ? `?${queryParams.toString()}` : '';
      window.location.href = `/api/v1/auth/google${queryString}`;
    }
  }

  function handleForgetAccount() {
    try {
      localStorage.removeItem('rea_last_google_user');
    } catch {}
    lastGoogleUser = null;
  }

  async function handleGoogleMfaVerify(e?: Event) {
    if (e) e.preventDefault();
    const cleanCode = mfaCode.trim();
    if (!cleanCode) {
      error = 'Harap masukkan 6-digit kode TOTP atau Kode Cadangan (Backup Code)';
      return;
    }

    error = '';
    isMfaVerifying = true;

    try {
      const res = await apiRequest('/auth/google/mfa-verify', {
        method: 'POST',
        body: JSON.stringify({
          mfaToken: googleMfaToken,
          code: cleanCode,
        }),
      });

      if (res?.data?.ticket) {
        // Exchange ticket for session cookie
        await apiRequest('/auth/google/exchange-ticket', {
          method: 'POST',
          body: JSON.stringify({ ticket: res.data.ticket }),
        });

        // Save last user
        await authStore.getState().fetchUser();
        window.location.href = '/drive';
      } else {
        window.location.href = '/drive';
      }
    } catch (err: any) {
      error = err.message || 'Kode verifikasi salah atau kedaluwarsa';
    } finally {
      isMfaVerifying = false;
    }
  }

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

<svelte:head>
  <title>Sign In — REA Cloud Storage</title>
</svelte:head>

<div class="flex min-h-screen items-center justify-center p-4 bg-gradient-to-br from-slate-100 via-slate-50 to-blue-50/30 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950">
  <div class="w-full max-w-md bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200/80 dark:border-slate-800 p-8 sm:p-10 transition-all">
    <!-- Header -->
    <div class="flex flex-col items-center mb-8">
      <div class="w-16 h-16 bg-gradient-to-tr from-blue-600 to-indigo-600 rounded-2xl flex items-center justify-center text-white shadow-xl shadow-blue-500/25 mb-4 ring-4 ring-blue-500/10">
        <Cloud class="w-9 h-9" />
      </div>
      <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Welcome to REA Cloud</h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1.5 text-center">
        Secure multi-user cloud workspace & collaborative storage
      </p>
    </div>

    <!-- Error Banner -->
    {#if error}
      <div class="mb-6 p-4 bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900/60 rounded-2xl flex items-start gap-3 text-red-700 dark:text-red-300 text-sm animate-in fade-in duration-200">
        <AlertCircle class="w-5 h-5 shrink-0 mt-0.5 text-red-600 dark:text-red-400" />
        <div class="flex-1">
          <p class="font-medium">Authentication Error</p>
          <p class="text-xs text-red-600 dark:text-red-400 mt-0.5">{error}</p>
        </div>
      </div>
    {/if}

    {#if mfaRequired}
      <!-- 2FA Challenge View for Google Sign-In or Email Sign-In -->
      <div class="space-y-5 animate-in fade-in zoom-in-95 duration-200">
        <div class="p-4 bg-blue-50/80 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 rounded-2xl">
          <div class="flex items-center gap-3 mb-2">
            <div class="w-10 h-10 rounded-xl bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-md">
              <ShieldCheck class="w-6 h-6" />
            </div>
            <div>
              <h3 class="font-bold text-sm text-slate-900 dark:text-white">
                Verifikasi Dua Langkah (2FA)
              </h3>
              <p class="text-xs text-slate-500 dark:text-slate-400">
                {mfaEmail ? `Akun: ${mfaEmail}` : 'Konfirmasi keamanan login'}
              </p>
            </div>
          </div>
          <p class="text-xs text-slate-600 dark:text-slate-300 mt-2">
            Akun Anda dilindungi 2FA. Masukkan 6-digit kode dari <strong>Google Authenticator / Authy</strong> atau gunakan salah satu dari 10 <strong>Kode Cadangan (Backup Code)</strong> Anda (contoh: <code class="font-mono font-bold text-blue-600 dark:text-blue-400">62FA-EA8D</code>).
          </p>
        </div>

        <form onsubmit={googleMfaToken ? handleGoogleMfaVerify : handleSubmit} class="space-y-4">
          <div>
            <label for="mfa-input" class="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400 mb-1.5">
              Kode TOTP atau Backup Code
            </label>
            <input
              id="mfa-input"
              type="text"
              bind:value={mfaCode}
              placeholder="123456 atau 62FA-EA8D"
              class="w-full text-center text-lg font-mono tracking-widest py-3 px-4 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 uppercase text-slate-900 dark:text-white"
            />
          </div>

          <button
            type="submit"
            disabled={isMfaVerifying || isLoading || !mfaCode.trim()}
            class="w-full py-3.5 px-4 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold text-sm rounded-xl shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
          >
            {#if isMfaVerifying || isLoading}
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Memverifikasi...</span>
            {:else}
              <ShieldCheck class="w-4 h-4" />
              <span>Verifikasi & Masuk</span>
            {/if}
          </button>

          <div class="text-center">
            <button
              type="button"
              onclick={() => {
                mfaRequired = false;
                googleMfaToken = '';
                mfaCode = '';
                error = '';
                window.history.replaceState({}, document.title, window.location.pathname);
              }}
              class="text-xs text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 transition cursor-pointer"
            >
              Batalkan dan kembali ke menu login
            </button>
          </div>
        </form>
      </div>
    {:else}
      <!-- PRIMARY: 1-Click Last Account Sign-In or Standard Google OAuth -->
      {#if lastGoogleUser}
        <div class="mb-5 p-4 bg-blue-50/70 dark:bg-slate-800/80 border border-blue-200/80 dark:border-slate-700/80 rounded-2xl animate-in fade-in duration-200 shadow-xs">
          <div class="flex items-center justify-between mb-3">
            <span class="text-[11px] font-bold uppercase tracking-wider text-blue-700 dark:text-blue-400 flex items-center gap-1.5">
              <Sparkles class="w-3.5 h-3.5" />
              Akun Terakhir Digunakan
            </span>
            <button
              type="button"
              onclick={handleForgetAccount}
              title="Lupakan akun ini"
              class="text-[11px] text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 cursor-pointer"
            >
              Lupakan
            </button>
          </div>

          <!-- User profile preview -->
          <div class="flex items-center gap-3.5 mb-3.5 p-2.5 bg-white/90 dark:bg-slate-900/90 rounded-xl border border-slate-200/70 dark:border-slate-700/60">
            {#if lastGoogleUser.avatarUrl}
              <img
                src={lastGoogleUser.avatarUrl}
                alt={lastGoogleUser.name}
                class="w-10 h-10 rounded-full object-cover border border-slate-200 dark:border-slate-700 shrink-0"
              />
            {:else}
              <div class="w-10 h-10 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center font-bold text-sm shrink-0 shadow-xs">
                {lastGoogleUser.name ? lastGoogleUser.name.charAt(0).toUpperCase() : 'G'}
              </div>
            {/if}

            <div class="truncate flex-1">
              <p class="font-semibold text-xs text-slate-900 dark:text-white truncate">
                {lastGoogleUser.name}
              </p>
              <p class="text-[11px] text-slate-500 dark:text-slate-400 truncate">
                {lastGoogleUser.email}
              </p>
            </div>
          </div>

          <!-- 1-Click Continue as [User] Button (no account picker) -->
          <button
            type="button"
            onclick={() => handleGoogleSignIn({ loginHint: lastGoogleUser!.email })}
            disabled={isGoogleLoading || isLoading}
            class="w-full relative flex items-center justify-center gap-2.5 py-3.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm rounded-xl shadow-md hover:shadow-lg transition duration-150 disabled:opacity-60 disabled:cursor-not-allowed cursor-pointer"
          >
            {#if isGoogleLoading}
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Menghubungkan ke Google...</span>
            {:else}
              <!-- White/Colored Google G icon -->
              <svg class="w-4 h-4 shrink-0 bg-white rounded-full p-0.5" viewBox="0 0 24 24">
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
              <span class="truncate">Lanjutkan sebagai {lastGoogleUser.name}</span>
              <ArrowRight class="w-4 h-4 ml-1" />
            {/if}
          </button>

          <!-- Use another Google account option -->
          <div class="mt-2.5 text-center">
            <button
              type="button"
              onclick={() => handleGoogleSignIn({ prompt: 'select_account' })}
              disabled={isGoogleLoading || isLoading}
              class="text-xs text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 font-medium cursor-pointer transition hover:underline"
            >
              Pilih atau gunakan akun Google lain
            </button>
          </div>
        </div>
      {:else}
        <!-- Standard Google Sign-In button if no last account saved -->
        <div class="space-y-4">
          <button
            type="button"
            onclick={() => handleGoogleSignIn()}
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
              <span class="tracking-tight">Continue with Google</span>
            {/if}
          </button>

          <div class="flex items-center gap-2 justify-center text-xs text-slate-500 dark:text-slate-400">
            <Sparkles class="w-3.5 h-3.5 text-blue-500" />
            <span>One-click sign-in • 10 GB free private storage</span>
          </div>
        </div>
      {/if}

      <!-- Divider for Optional Password Login -->
      <div class="relative my-7">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-slate-200 dark:border-slate-800"></div>
        </div>
        <div class="relative flex justify-center text-xs">
          <button
            type="button"
            onclick={() => showPasswordForm = !showPasswordForm}
            class="bg-white dark:bg-slate-900 px-3 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 font-medium cursor-pointer transition"
          >
            {showPasswordForm ? 'Hide email sign-in' : 'Or sign in with email'}
          </button>
        </div>
      </div>

      <!-- Optional Email/Password Form -->
      {#if showPasswordForm}
        <form onsubmit={handleSubmit} class="space-y-4 animate-in fade-in duration-200">
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

          <button
            type="submit"
            disabled={isLoading || isGoogleLoading}
            class="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 bg-slate-800 hover:bg-slate-900 dark:bg-slate-700 dark:hover:bg-slate-600 text-white font-medium text-sm rounded-xl shadow-md disabled:opacity-50 transition cursor-pointer"
          >
            {#if isLoading}
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Signing In...</span>
            {:else}
              <span>Sign In with Email</span>
              <ArrowRight class="w-4 h-4" />
            {/if}
          </button>
        </form>
      {/if}
    {/if}

    <!-- Footer Links -->
    <div class="mt-8 pt-6 border-t border-slate-200 dark:border-slate-800 text-center">
      <p class="text-sm text-slate-600 dark:text-slate-400">
        New to REA Cloud?
        <a href="/register" class="font-semibold text-blue-600 dark:text-blue-400 hover:underline ml-1">
          Create an account
        </a>
      </p>
    </div>
  </div>
</div>
