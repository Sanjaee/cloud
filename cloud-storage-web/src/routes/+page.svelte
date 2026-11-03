<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.store';

  onMount(async () => {
    await authStore.getState().fetchUser();
    const isAuth = authStore.getState().isAuthenticated;
    if (isAuth) {
      window.location.href = '/drive';
    } else {
      window.location.href = '/login';
    }
  });
</script>

<div class="flex h-screen items-center justify-center bg-slate-50 dark:bg-slate-950">
  <div class="flex flex-col items-center gap-3">
    <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    <p class="text-sm font-medium text-slate-500">Loading REA Cloud Storage...</p>
  </div>
</div>

