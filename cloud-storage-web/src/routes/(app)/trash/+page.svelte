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
      <h1 class="text-xl font-bold text-white flex items-center gap-2">
        <Trash2 class="w-5 h-5 text-zinc-400" />
        Trash
      </h1>
      <p class="text-xs text-zinc-400 mt-0.5">
        Items in trash can be restored or permanently purged from Cloudflare R2 storage.
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

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Loading trash...</p>
    </div>
  {:else if trashedFiles.length === 0 && trashedFolders.length === 0}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Trash2 class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
      <h3 class="font-semibold text-white text-sm">Trash is empty</h3>
      <p class="text-xs text-zinc-400 mt-1">Deleted items will stay here until you restore or purge them.</p>
    </div>
  {:else}
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl overflow-hidden shadow-xs">
      <table class="w-full text-left text-xs">
        <thead class="bg-zinc-900/80 text-zinc-400 uppercase font-semibold text-[11px] tracking-wider border-b border-zinc-800/80">
          <tr>
            <th class="py-2.5 px-3">Name</th>
            <th class="py-2.5 px-3">Size</th>
            <th class="py-2.5 px-3">Date Deleted</th>
            <th class="py-2.5 px-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-zinc-800/60">
          {#each trashedFolders as fol}
            <tr class="hover:bg-zinc-850/60 transition">
              <td class="py-2.5 px-3 font-medium text-zinc-200 flex items-center gap-2">
                <Folder class="w-3.5 h-3.5 text-amber-400 fill-amber-400/20 shrink-0" />
                <span class="truncate">{fol.name}</span>
              </td>
              <td class="py-2.5 px-3 text-zinc-500">—</td>
              <td class="py-2.5 px-3 text-zinc-500 text-[11px]">{formatDate(fol.updatedAt)}</td>
              <td class="py-2.5 px-3 text-right space-x-1">
                <button
                  onclick={() => handleRestore('folder', fol.id)}
                  class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                  title="Restore"
                >
                  <RotateCcw class="w-3.5 h-3.5" />
                </button>
                <button
                  onclick={() => handlePermanentDelete('folder', fol.id)}
                  class="p-1 hover:bg-red-950/60 text-zinc-400 hover:text-red-400 rounded transition cursor-pointer"
                  title="Permanently Delete"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </td>
            </tr>
          {/each}

          {#each trashedFiles as f}
            <tr class="hover:bg-zinc-850/60 transition">
              <td class="py-2.5 px-3 font-medium text-zinc-200 flex items-center gap-2">
                <FileText class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                <span class="truncate max-w-xs">{f.name}</span>
              </td>
              <td class="py-2.5 px-3 text-zinc-400 font-mono text-[11px]">{formatBytes(f.size)}</td>
              <td class="py-2.5 px-3 text-zinc-500 text-[11px]">{formatDate(f.updatedAt)}</td>
              <td class="py-2.5 px-3 text-right space-x-1">
                <button
                  onclick={() => handleRestore('file', f.id)}
                  class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                  title="Restore"
                >
                  <RotateCcw class="w-3.5 h-3.5" />
                </button>
                <button
                  onclick={() => handlePermanentDelete('file', f.id)}
                  class="p-1 hover:bg-red-950/60 text-zinc-400 hover:text-red-400 rounded transition cursor-pointer"
                  title="Permanently Delete"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>
