<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.store';
  import { apiRequest } from '$lib/api/client';
  import Sidebar from '$lib/components/sidebar/Sidebar.svelte';
  import Header from '$lib/components/header/Header.svelte';
  import UploadManager from '$lib/components/upload-manager/UploadManager.svelte';
  import FilePreviewModal from '$lib/components/file-preview/FilePreviewModal.svelte';
  import ShareDialog from '$lib/components/sharing/ShareDialog.svelte';
  import NewFolderDialog from '$lib/components/dialogs/NewFolderDialog.svelte';
  import type { QuotaInfo } from '$lib/api/types';

  let { children } = $props();
  let isLoading = $state(true);
  let isNewFolderOpen = $state(false);
  let quota = $state<QuotaInfo | null>(null);

  async function loadQuota() {
    try {
      const q = await apiRequest('/quotas');
      quota = q;
    } catch {}
  }

  onMount(async () => {
    await authStore.getState().fetchUser();
    if (!authStore.getState().isAuthenticated) {
      window.location.href = '/login';
      return;
    }
    await loadQuota();
    isLoading = false;
  });
</script>

{#if isLoading}
  <div class="flex h-screen items-center justify-center bg-slate-50 dark:bg-slate-950">
    <div class="flex flex-col items-center gap-3">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
      <p class="text-sm font-medium text-slate-500">Loading your workspace...</p>
    </div>
  </div>
{:else}
  <div class="flex flex-col h-screen overflow-hidden bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-white">
    <Header />

    <div class="flex flex-1 overflow-hidden">
      <Sidebar
        {quota}
        onNewFolder={() => (isNewFolderOpen = true)}
      />

      <main class="flex-1 overflow-y-auto p-6">
        {@render children()}
      </main>
    </div>

    <!-- Modals & Drawers -->
    <UploadManager />
    <FilePreviewModal />
    <ShareDialog />
    <NewFolderDialog
      isOpen={isNewFolderOpen}
      onClose={() => (isNewFolderOpen = false)}
      onCreated={() => {
        window.dispatchEvent(new CustomEvent('folder-created'));
      }}
    />
  </div>
{/if}

