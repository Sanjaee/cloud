<script lang="ts">
  import { onMount } from 'svelte';
  import { apiRequest } from '$lib/api/client';
  import { authStore } from '$lib/stores/auth.store';
  import { ShieldCheck, AlertCircle, Eye, EyeOff } from 'lucide-svelte';

  let email = $state('');
  let password = $state('');
  let showPassword = $state(false);
  let mfaCode = $state('');
  let mfaRequired = $state(false);
  let googleMfaToken = $state('');
  let mfaEmail = $state('');
  let isMfaVerifying = $state(false);
  let error = $state('');
  let isLoading = $state(false);
  let isGoogleLoading = $state(false);
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
      const saved = localStorage.getItem('zacode_last_google_user') || localStorage.getItem('rea_last_google_user');
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
      const res = await apiRequest(`/auth/google/url${queryString}`);
      if (res?.data?.url) {
        window.location.href = res.data.url;
      } else {
        window.location.href = `/api/v1/auth/google${queryString}`;
      }
    } catch {
      const queryParams = new URLSearchParams();
      if (options.loginHint) queryParams.set('login_hint', options.loginHint);
      if (options.prompt) queryParams.set('prompt', options.prompt);
      const queryString = queryParams.toString() ? `?${queryParams.toString()}` : '';
      window.location.href = `/api/v1/auth/google${queryString}`;
    }
  }

  function handleForgetAccount() {
    try {
      localStorage.removeItem('zacode_last_google_user');
      localStorage.removeItem('rea_last_google_user');
    } catch {}
    lastGoogleUser = null;
  }

  function handleMfaPaste(e: ClipboardEvent) {
    const pastedText = e.clipboardData?.getData('text') || '';
    // If user pasted multiple backup codes or list, automatically take the first valid code
    const matches = pastedText.match(/[A-Za-z0-9]{4}-[A-Za-z0-9]{4}|[A-Za-z0-9]{8}/g);
    if (matches && matches.length > 0) {
      e.preventDefault();
      mfaCode = matches[0].toUpperCase();
    }
  }

  async function handleGoogleMfaVerify(e?: Event) {
    if (e) e.preventDefault();
    const cleanCode = mfaCode.trim();
    if (!cleanCode) {
      error = 'Harap masukkan 6-digit kode TOTP atau salah satu Kode Cadangan';
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
        await apiRequest('/auth/google/exchange-ticket', {
          method: 'POST',
          body: JSON.stringify({ ticket: res.data.ticket }),
        });
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
          mfaCode: mfaCode.trim() || undefined,
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
  <title>Sign In — Zacode Cloud Storage</title>
</svelte:head>

<div class="flex min-h-screen items-center justify-center p-4 bg-[#09090b] text-white">
  <div class="w-full max-w-sm sm:max-w-[400px] bg-[#121214] border border-zinc-800 rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6">
    <!-- Header -->
    <div class="text-center space-y-1.5">
      <h1 class="text-2xl font-bold tracking-tight text-white">Welcome back</h1>
      <p class="text-xs text-zinc-400">
        {#if mfaRequired}
          Verifikasi keamanan akun Anda
        {:else}
          Login with your Google account
        {/if}
      </p>
    </div>

    <!-- Error Alert -->
    {#if error}
      <div class="p-3 bg-red-950/40 border border-red-900/60 rounded-xl flex items-start gap-2.5 text-xs text-red-300 animate-in fade-in">
        <AlertCircle class="w-4 h-4 shrink-0 text-red-400 mt-0.5" />
        <div class="flex-1">
          <p class="font-medium text-red-200">Authentication Error</p>
          <p class="text-[11px] text-red-400 mt-0.5 leading-relaxed">{error}</p>
        </div>
      </div>
    {/if}

    {#if mfaRequired}
      <!-- 2FA CHALLENGE VIEW (Shadcn style) -->
      <div class="space-y-4 animate-in fade-in duration-200">
        <div class="p-3.5 bg-zinc-900/80 border border-zinc-800 rounded-xl space-y-2">
          <div class="flex items-center gap-2">
            <ShieldCheck class="w-4 h-4 text-emerald-400 shrink-0" />
            <span class="text-xs font-semibold text-white">Verifikasi Dua Langkah</span>
          </div>
          <p class="text-[11px] text-zinc-400 leading-relaxed">
            Masukkan 6-digit kode dari <strong>Google Authenticator</strong> atau gunakan salah satu dari 10 <strong>Kode Cadangan</strong> Anda (contoh: <code class="text-emerald-400 font-mono font-semibold">58B5-A215</code>).
          </p>
        </div>

        <form onsubmit={googleMfaToken ? handleGoogleMfaVerify : handleSubmit} class="space-y-4">
          <div class="space-y-1.5">
            <label for="mfa-input" class="text-xs font-medium text-zinc-300 block">
              Kode TOTP atau Backup Code
            </label>
            <input
              id="mfa-input"
              type="text"
              bind:value={mfaCode}
              onpaste={handleMfaPaste}
              placeholder="123456 atau 58B5-A215"
              class="w-full text-center text-base font-mono tracking-wider px-3 py-2.5 bg-zinc-900 border border-zinc-800 rounded-lg text-white placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600 uppercase"
            />
            <p class="text-[10px] text-zinc-500 text-center">
              *Jika menempelkan (paste) daftar kode, kode pertama akan otomatis diambil.
            </p>
          </div>

          <button
            type="submit"
            disabled={isMfaVerifying || isLoading || !mfaCode.trim()}
            class="w-full py-2.5 bg-white text-zinc-950 hover:bg-zinc-200 disabled:opacity-50 font-semibold text-sm rounded-lg shadow-xs transition cursor-pointer active:scale-[0.99] flex items-center justify-center gap-2"
          >
            {#if isMfaVerifying || isLoading}
              <div class="w-4 h-4 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin"></div>
              <span>Memverifikasi...</span>
            {:else}
              <ShieldCheck class="w-4 h-4" />
              <span>Verifikasi & Masuk</span>
            {/if}
          </button>

          <div class="text-center pt-1">
            <button
              type="button"
              onclick={() => {
                mfaRequired = false;
                googleMfaToken = '';
                mfaCode = '';
                error = '';
                window.history.replaceState({}, document.title, window.location.pathname);
              }}
              class="text-xs text-zinc-500 hover:text-zinc-300 transition cursor-pointer"
            >
              Batalkan dan kembali ke menu login
            </button>
          </div>
        </form>
      </div>
    {:else}
      <!-- PRIMARY: GOOGLE SIGN-IN BUTTONS -->
      <div class="space-y-3">
        {#if lastGoogleUser}
          <!-- 1-Click Fast Re-login for Last Account -->
          <button
            type="button"
            onclick={() => handleGoogleSignIn({ loginHint: lastGoogleUser!.email })}
            disabled={isGoogleLoading || isLoading}
            class="w-full flex items-center justify-center gap-2.5 py-2.5 px-4 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-white rounded-lg text-sm font-medium transition cursor-pointer active:scale-[0.99] disabled:opacity-50"
          >
            {#if isGoogleLoading}
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Menghubungkan...</span>
            {:else}
              {#if lastGoogleUser.avatarUrl}
                <img
                  src={lastGoogleUser.avatarUrl}
                  alt={lastGoogleUser.name}
                  class="w-4 h-4 rounded-full object-cover shrink-0"
                />
              {:else}
                <!-- Official Google "G" Logo SVG -->
                <svg class="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                </svg>
              {/if}
              <span class="truncate">Continue as {lastGoogleUser.name || lastGoogleUser.email}</span>
            {/if}
          </button>

          <div class="flex items-center justify-between text-[11px] text-zinc-500 px-1">
            <button
              type="button"
              onclick={() => handleGoogleSignIn({ prompt: 'select_account' })}
              class="hover:text-zinc-300 transition cursor-pointer hover:underline"
            >
              Gunakan akun Google lain
            </button>
            <button
              type="button"
              onclick={handleForgetAccount}
              class="hover:text-zinc-300 transition cursor-pointer hover:underline"
            >
              Lupakan
            </button>
          </div>
        {:else}
          <!-- Standard Google Sign-In button -->
          <button
            type="button"
            onclick={() => handleGoogleSignIn()}
            disabled={isGoogleLoading || isLoading}
            class="w-full flex items-center justify-center gap-2.5 py-2.5 px-4 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-white rounded-lg text-sm font-medium transition cursor-pointer active:scale-[0.99] disabled:opacity-50"
          >
            {#if isGoogleLoading}
              <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              <span>Connecting to Google...</span>
            {:else}
              <!-- Official Colorful Google "G" Logo SVG -->
              <svg class="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
              </svg>
              <span>Login with Google</span>
            {/if}
          </button>
        {/if}
      </div>

      <!-- Divider: "Or continue with" -->
      <div class="relative my-4">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-zinc-800"></div>
        </div>
        <div class="relative flex justify-center text-[10px] uppercase">
          <span class="bg-[#121214] px-2 text-zinc-500 font-medium tracking-wider">
            Or continue with
          </span>
        </div>
      </div>

      <!-- Email & Password Form -->
      <form onsubmit={handleSubmit} class="space-y-3.5">
        <div class="space-y-1">
          <label for="email" class="text-xs font-medium text-zinc-300 block">
            Email
          </label>
          <input
            id="email"
            type="email"
            bind:value={email}
            required
            placeholder="m@example.com"
            class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-sm text-white placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600 transition"
          />
        </div>

        <div class="space-y-1">
          <div class="flex items-center justify-between">
            <label for="password" class="text-xs font-medium text-zinc-300">
              Password
            </label>
            <a href="/forgot-password" class="text-xs text-zinc-400 hover:text-white transition">
              Forgot your password?
            </a>
          </div>
          <div class="relative">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              bind:value={password}
              required
              placeholder="••••••••"
              class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-sm text-white placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600 transition pr-9"
            />
            <button
              type="button"
              onclick={() => (showPassword = !showPassword)}
              class="absolute right-2.5 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 cursor-pointer"
            >
              {#if showPassword}
                <EyeOff class="w-4 h-4" />
              {:else}
                <Eye class="w-4 h-4" />
              {/if}
            </button>
          </div>
        </div>

        <button
          type="submit"
          disabled={isLoading || isGoogleLoading}
          class="w-full mt-2 py-2.5 bg-white text-zinc-950 hover:bg-zinc-200 disabled:opacity-50 font-semibold text-sm rounded-lg shadow-xs transition cursor-pointer active:scale-[0.99] flex items-center justify-center gap-2"
        >
          {#if isLoading}
            <div class="w-4 h-4 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin"></div>
            <span>Logging in...</span>
          {:else}
            <span>Login</span>
          {/if}
        </button>
      </form>
    {/if}

    <!-- Footer Links -->
    <div class="text-center text-xs text-zinc-400 pt-1">
      <span>Don't have an account?</span>
      <a href="/register" class="text-white hover:underline font-medium ml-1">
        Sign up
      </a>
    </div>
  </div>
</div>
