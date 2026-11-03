<script lang="ts">
  import { onMount } from 'svelte';
  import { apiRequest } from '$lib/api/client';
  import { Cloud, Lock, Mail, User, ArrowRight, AlertCircle, CheckCircle, ShieldCheck, Eye, EyeOff } from 'lucide-svelte';

  let email = $state('');
  let username = $state('');
  let password = $state('');
  let showPassword = $state(false);
  let error = $state('');
  let isSuccess = $state(false);
  let isLoading = $state(false);
  let isGoogleLoading = $state(false);

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
      error = 'Harap penuhi semua syarat keamanan password (minimal 8 karakter, huruf besar, huruf kecil, dan angka).';
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
  <title>Create Account — Zacode Cloud Storage</title>
</svelte:head>

<div class="flex min-h-screen items-center justify-center p-4 bg-[#09090b] text-white">
  <div class="w-full max-w-sm sm:max-w-[400px] bg-[#121214] border border-zinc-800 rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6">
    <!-- Header -->
    <div class="text-center space-y-1.5">
      <h1 class="text-2xl font-bold tracking-tight text-white">Create an account</h1>
      <p class="text-xs text-zinc-400">Get 10 GB secure private storage instantly</p>
    </div>

    {#if isSuccess}
      <div class="p-5 bg-zinc-900 border border-zinc-800 rounded-xl text-center space-y-3 animate-in fade-in">
        <CheckCircle class="w-10 h-10 text-emerald-400 mx-auto" />
        <h3 class="font-bold text-white text-base">Registration Successful!</h3>
        <p class="text-xs text-zinc-400">
          Akun Anda telah berhasil dibuat. Silakan login untuk mulai menggunakan Zacode Cloud.
        </p>
        <a
          href="/login"
          class="block w-full py-2.5 bg-white text-zinc-950 hover:bg-zinc-200 font-semibold text-sm rounded-lg shadow-xs transition mt-2"
        >
          Sign In Now
        </a>
      </div>
    {:else}
      <!-- Error Alert -->
      {#if error}
        <div class="p-3 bg-red-950/40 border border-red-900/60 rounded-xl flex items-start gap-2.5 text-xs text-red-300 animate-in fade-in">
          <AlertCircle class="w-4 h-4 shrink-0 text-red-400 mt-0.5" />
          <div class="flex-1">
            <p class="font-medium text-red-200">Registration Error</p>
            <p class="text-[11px] text-red-400 mt-0.5 leading-relaxed">{error}</p>
          </div>
        </div>
      {/if}

      <!-- Google Sign-Up Button -->
      <button
        type="button"
        onclick={handleGoogleSignUp}
        disabled={isGoogleLoading || isLoading}
        class="w-full flex items-center justify-center gap-2.5 py-2.5 px-4 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-white rounded-lg text-sm font-medium transition cursor-pointer active:scale-[0.99] disabled:opacity-50"
      >
        {#if isGoogleLoading}
          <div class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
          <span>Connecting to Google...</span>
        {:else}
          <!-- Official Google "G" Logo SVG -->
          <svg class="w-4 h-4 shrink-0" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>Sign up with Google</span>
        {/if}
      </button>

      <!-- Divider: "Or continue with" -->
      <div class="relative my-4">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-zinc-800"></div>
        </div>
        <div class="relative flex justify-center text-[10px] uppercase">
          <span class="bg-[#121214] px-2 text-zinc-500 font-medium tracking-wider">
            Or continue with email
          </span>
        </div>
      </div>

      <!-- Registration Form -->
      <form onsubmit={handleRegister} class="space-y-3.5">
        <div class="space-y-1">
          <label for="username" class="text-xs font-medium text-zinc-300 block">
            Username
          </label>
          <input
            id="username"
            type="text"
            bind:value={username}
            required
            placeholder="johndoe"
            class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-sm text-white placeholder:text-zinc-600 focus:outline-none focus:border-zinc-600 transition"
          />
        </div>

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
          <label for="password" class="text-xs font-medium text-zinc-300 block">
            Password
          </label>
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
          <div class="grid grid-cols-2 gap-1 pt-1 text-[10px] text-zinc-500">
            <span class={hasLength ? 'text-emerald-400' : ''}>• 8+ Karakter</span>
            <span class={hasUpper ? 'text-emerald-400' : ''}>• Huruf Besar</span>
            <span class={hasLower ? 'text-emerald-400' : ''}>• Huruf Kecil</span>
            <span class={hasNumber ? 'text-emerald-400' : ''}>• Angka</span>
          </div>
        </div>

        <button
          type="submit"
          disabled={isLoading || isGoogleLoading || !isPasswordStrong}
          class="w-full mt-2 py-2.5 bg-white text-zinc-950 hover:bg-zinc-200 disabled:opacity-50 font-semibold text-sm rounded-lg shadow-xs transition cursor-pointer active:scale-[0.99] flex items-center justify-center gap-2"
        >
          {#if isLoading}
            <div class="w-4 h-4 border-2 border-zinc-950 border-t-transparent rounded-full animate-spin"></div>
            <span>Creating account...</span>
          {:else}
            <span>Create Account</span>
          {/if}
        </button>
      </form>
    {/if}

    <!-- Footer Links -->
    <div class="text-center text-xs text-zinc-400 pt-1">
      <span>Already have an account?</span>
      <a href="/login" class="text-white hover:underline font-medium ml-1">
        Sign in
      </a>
    </div>
  </div>
</div>
