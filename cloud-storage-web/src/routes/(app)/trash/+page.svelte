<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { formatDate } from '$lib/utils/format-date';
  import { Trash2, RotateCcw, AlertTriangle, FileText, Folder } from 'lucide-svelte';
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

  async function handleRestore(type: 'file' | 'folder', id: string) {
    try {
      await apiRequest(`/trash/restore/${type}/${id}`, { method: 'POST' });
      await loadTrash();
    } catch (err) {
      console.error('Failed to restore item:', err);
    }
  }

  async function handlePermanentDelete(type: 'file' | 'folder', id: string) {
    if (!confirm('Permanently delete this item? This action cannot be undone and will purge the object from Cloudflare R2.')) return;
    try {
      await apiRequest(`/trash/permanent/${type}/${id}`, { method: 'DELETE' });
      await loadTrash();
    } catch (err) {
      console.error('Failed to permanently delete item:', err);
    }
  }

  async function handleEmptyTrash() {
    if (!confirm('Are you sure you want to permanently delete all items in trash?')) return;
    try {
      await apiRequest('/trash/empty', { method: 'DELETE' });
      await loadTrash();
    } catch (err) {
      console.error('Failed to empty trash:', err);
    }
  }

  onMount(() => {
    loadTrash();
  });
</script>

<div class="space-y-6">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
        <Trash2 class="w-7 h-7 text-blue-600" />
        Trash
      </h1>
      <p class="text-xs text-slate-500 mt-1">
        Items in trash can be recovered or permanently deleted from Cloudflare R2.
      </p>
    </div>

    {#if trashedFiles.length > 0 || trashedFolders.length > 0}
      <button
        onclick={handleEmptyTrash}
        class="flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer"
      >
        <Trash2 class="w-4 h-4" />
        Empty Trash
      </button>
    {/if}
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if trashedFiles.length === 0 && trashedFolders.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Trash2 class="w-12 h-12 text-slate-400 mx-auto mb-3" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">Trash is empty</h3>
      <p class="text-xs text-slate-500 mt-1">Deleted items will stay here until permanently deleted.</p>
    </div>
  {:else}
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl overflow-hidden shadow-xs">
      <table class="w-full text-left text-xs">
        <thead class="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase font-semibold tracking-wider border-b border-slate-200 dark:border-slate-800">
          <tr>
            <th class="py-3 px-4">Name</th>
            <th class="py-3 px-4">Size</th>
            <th class="py-3 px-4">Date Deleted</th>
            <th class="py-3 px-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          <!-- Trashed Folders -->
          {#each trashedFolders as fol}
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
              <td class="py-3 px-4 font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2.5">
                <Folder class="w-4 h-4 text-amber-500 fill-amber-500 shrink-0" />
                <span>{fol.name}</span>
              </td>
              <td class="py-3 px-4 text-slate-400">—</td>
              <td class="py-3 px-4 text-slate-500">{formatDate(fol.updatedAt)}</td>
              <td class="py-3 px-4 text-right space-x-1">
                <button
                  onclick={() => handleRestore('folder', fol.id)}
                  class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 hover:text-blue-600 transition cursor-pointer"
                  title="Restore"
                >
                  <RotateCcw class="w-4 h-4" />
                </button>
                <button
                  onclick={() => handlePermanentDelete('folder', fol.id)}
                  class="p-1.5 hover:bg-red-50 dark:hover:bg-red-950 text-slate-400 hover:text-red-600 rounded-lg transition cursor-pointer"
                  title="Permanently Delete"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
          {/each}

          <!-- Trashed Files -->
          {#each trashedFiles as f}
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
              <td class="py-3 px-4 font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2.5">
                <FileText class="w-4 h-4 text-blue-500 shrink-0" />
                <span class="truncate max-w-xs">{f.name}</span>
              </td>
              <td class="py-3 px-4 text-slate-500">{formatBytes(f.size)}</td>
              <td class="py-3 px-4 text-slate-500">{formatDate(f.updatedAt)}</td>
              <td class="py-3 px-4 text-right space-x-1">
                <button
                  onclick={() => handleRestore('file', f.id)}
                  class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 hover:text-blue-600 transition cursor-pointer"
                  title="Restore"
                >
                  <RotateCcw class="w-4 h-4" />
                </button>
                <button
                  onclick={() => handlePermanentDelete('file', f.id)}
                  class="p-1.5 hover:bg-red-50 dark:hover:bg-red-950 text-slate-400 hover:text-red-600 rounded-lg transition cursor-pointer"
                  title="Permanently Delete"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

