<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { confirmDialog } from '$lib/stores/confirm.store';
  import UnifiedResourceGrid from '$lib/components/resource-grid/UnifiedResourceGrid.svelte';
  import { Trash2 } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { FileItem, FolderItem } from '$lib/api/types';

  let trashedFiles = $state<FileItem[]>([]);
  let trashedFolders = $state<FolderItem[]>([]);
  let isLoading = $state(true);

  async function loadTrash() {
    isLoading = true;
    try {
      const res = await apiRequest('/trash');
      trashedFiles = res.files || [];
      trashedFolders = res.folders || [];
    } catch (err) {
      console.error('Failed to load trash:', err);
    } finally {
      isLoading = false;
    }
  }

  async function handleSingleRestore(type: 'file' | 'folder', item: any) {
    try {
      await apiRequest(`/trash/restore/${type}/${item.id}`, { method: 'POST' });
      await loadTrash();
    } catch (err) {
      console.error('Failed to restore item:', err);
    }
  }

  async function handleSinglePermanentDelete(type: 'file' | 'folder', item: any) {
    if (!await confirmDialog({
      title: 'Hapus Permanen',
      description: `Hapus permanen "${item.name}"? File akan dihapus selamanya dari Cloudflare R2 dan kuota penyimpanan Anda akan dikembalikan. Tindakan ini tidak dapat dibatalkan.`,
      confirmText: 'Hapus Selamanya',
      variant: 'destructive',
    })) return;
    try {
      await apiRequest(`/trash/permanent/${type}/${item.id}`, { method: 'DELETE' });
      await loadTrash();
    } catch (err) {
      console.error('Failed to permanently delete item:', err);
    }
  }

  async function handleBatchRestore(fileIds: string[], folderIds: string[]) {
    try {
      await apiRequest('/trash/restore/batch', {
        method: 'POST',
        body: JSON.stringify({ fileIds, folderIds }),
      });
      await loadTrash();
    } catch (err) {
      console.error('Batch restore error:', err);
      for (const id of fileIds) await apiRequest(`/trash/restore/file/${id}`, { method: 'POST' }).catch(() => {});
      for (const id of folderIds) await apiRequest(`/trash/restore/folder/${id}`, { method: 'POST' }).catch(() => {});
      await loadTrash();
    }
  }

  async function handleBatchPermanentDelete(fileIds: string[], folderIds: string[]) {
    try {
      await apiRequest('/trash/permanent/batch', {
        method: 'POST',
        body: JSON.stringify({ fileIds, folderIds }),
      });
      await loadTrash();
    } catch (err) {
      console.error('Batch delete error:', err);
      for (const id of fileIds) await apiRequest(`/trash/permanent/file/${id}`, { method: 'DELETE' }).catch(() => {});
      for (const id of folderIds) await apiRequest(`/trash/permanent/folder/${id}`, { method: 'DELETE' }).catch(() => {});
      await loadTrash();
    }
  }

  async function handleEmptyTrash() {
    if (!await confirmDialog({
      title: 'Kosongkan Tempat Sampah',
      description: 'Apakah Anda yakin ingin menghapus permanen SEMUA item di sampah? Semua data akan dihapus selamanya dari Cloudflare R2 dan tindakan ini tidak dapat dibatalkan.',
      confirmText: 'Kosongkan Semua',
      variant: 'destructive',
    })) return;
    try {
      await apiRequest('/trash/empty', { method: 'DELETE' });
      await loadTrash();
    } catch (err) {
      console.error('Failed to empty trash:', err);
    }
  }

  function handleFileDblClick(f: FileItem) {
    selectionStore.getState().setPreviewFile(f);
  }

  onMount(() => {
    loadTrash();
  });
</script>

<div class="space-y-6">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-xl font-bold text-white flex items-center gap-2">
        <Trash2 class="w-5 h-5 text-zinc-400" />
        Trash
      </h1>
      <p class="text-xs text-zinc-400 mt-0.5">
        Item di sampah dapat dipulihkan atau dihapus secara permanen dari Cloudflare R2 storage.
      </p>
    </div>

    {#if trashedFiles.length > 0 || trashedFolders.length > 0}
      <button
        onclick={handleEmptyTrash}
        class="flex items-center gap-1.5 px-3 py-1.5 bg-red-950/60 hover:bg-red-900/60 border border-red-800/60 text-red-300 hover:text-white rounded-lg text-xs font-medium transition cursor-pointer"
      >
        <Trash2 class="w-3.5 h-3.5" />
        Empty Trash
      </button>
    {/if}
  </div>

  <UnifiedResourceGrid
    files={trashedFiles}
    folders={trashedFolders}
    context="trash"
    {isLoading}
    emptyTitle="Sampah kosong"
    emptySubtitle="Item yang dihapus akan muncul di sini sampai Anda memulihkan atau menghapusnya secara permanen."
    onFileDblClick={handleFileDblClick}
    onSingleRestore={handleSingleRestore}
    onSinglePermanentDelete={handleSinglePermanentDelete}
    onBatchRestore={handleBatchRestore}
    onBatchPermanentDelete={handleBatchPermanentDelete}
  />
</div>
