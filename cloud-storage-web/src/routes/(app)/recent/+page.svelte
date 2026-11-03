<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import UnifiedResourceGrid from '$lib/components/resource-grid/UnifiedResourceGrid.svelte';
  import { Clock } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { FileItem } from '$lib/api/types';

  let recentFiles = $state<FileItem[]>([]);
  let isLoading = $state(true);

  async function loadRecent() {
    isLoading = true;
    try {
      const res = await apiRequest('/files');
      recentFiles = res.files || [];
    } finally {
      isLoading = false;
    }
  }

  function handleFileDblClick(file: FileItem) {
    selectionStore.getState().setPreviewFile(file);
  }

  async function handleBatchDelete(fileIds: string[]) {
    for (const id of fileIds) {
      await apiRequest(`/files/${id}`, { method: 'DELETE' }).catch(() => {});
    }
    await loadRecent();
  }

  async function handleBatchDownload(files: FileItem[]) {
    for (const file of files) {
      const res = await apiRequest(`/downloads/${file.id}`);
      window.open(res.downloadUrl, '_blank');
    }
  }

  onMount(() => {
    loadRecent();
  });
</script>

<div class="space-y-6">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-xl font-bold text-white flex items-center gap-2">
        <Clock class="w-5 h-5 text-zinc-400" />
        Recent Files
      </h1>
      <p class="text-xs text-zinc-400 mt-0.5">File yang baru saja diunggah atau diakses di ruang kerja Anda.</p>
    </div>
  </div>

  <UnifiedResourceGrid
    files={recentFiles}
    context="recent"
    {isLoading}
    emptyTitle="Belum ada aktivitas terbaru"
    emptySubtitle="File yang diunggah atau dimodifikasi akan otomatis muncul di sini."
    onFileDblClick={handleFileDblClick}
    onBatchDelete={handleBatchDelete}
    onBatchDownload={handleBatchDownload}
  />
</div>
