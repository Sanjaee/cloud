<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo, formatDate } from '$lib/utils/format-date';
  import {
    Users,
    FileText,
    Folder,
    Download,
    Eye,
    BookmarkPlus,
    Check,
  } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { FileItem, FolderItem } from '$lib/api/types';

  let sharedFiles = $state<any[]>([]);
  let sharedFolders = $state<any[]>([]);
  let isLoading = $state(true);
  let addedShortcutId = $state<string | null>(null);

  async function loadShared() {
    isLoading = true;
    try {
      const res = await apiRequest('/shares/shared-with-me');
      sharedFiles = res.files || [];
      sharedFolders = res.folders || [];
    } catch (err) {
      console.error('Failed to load shared items:', err);
    } finally {
      isLoading = false;
    }
  }

  async function handleDownload(file: any) {
    const res = await apiRequest(`/downloads/${file.id}`);
    window.open(res.downloadUrl, '_blank');
  }

  function handlePreview(file: any) {
    selectionStore.getState().setPreviewFile(file);
  }

  async function handleAddShortcut(type: 'file' | 'folder', id: string) {
    try {
      await apiRequest('/shortcuts', {
        method: 'POST',
        body: JSON.stringify({ targetType: type, targetId: id }),
      });
      addedShortcutId = id;
      setTimeout(() => (addedShortcutId = null), 2000);
    } catch (err) {
      console.error('Failed to add shortcut:', err);
    }
  }

  onMount(() => {
    loadShared();
  });
</script>

<div class="space-y-6">
  <div>
    <h1 class="text-xl font-bold text-white flex items-center gap-2">
      <Users class="w-5 h-5 text-zinc-400" />
      Shared with Me
    </h1>
    <p class="text-xs text-zinc-400 mt-0.5">
      Files and folders other users have shared with your account.
    </p>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Loading shared items...</p>
    </div>
  {:else if sharedFiles.length === 0 && sharedFolders.length === 0}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Users class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
      <h3 class="font-semibold text-white text-sm">No files shared with you yet</h3>
      <p class="text-xs text-zinc-400 mt-1">When someone shares a file or folder with you, it will show up here.</p>
    </div>
  {:else}
    <!-- Shared Files Table -->
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl overflow-hidden shadow-xs">
      <table class="w-full text-left text-xs">
        <thead class="bg-zinc-900/80 text-zinc-400 uppercase font-semibold text-[11px] tracking-wider border-b border-zinc-800/80">
          <tr>
            <th class="py-2.5 px-3">Name</th>
            <th class="py-2.5 px-3">Shared By</th>
            <th class="py-2.5 px-3">Access</th>
            <th class="py-2.5 px-3">Size</th>
            <th class="py-2.5 px-3">Date</th>
            <th class="py-2.5 px-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-zinc-800/60">
          {#each sharedFiles as f}
            <tr class="hover:bg-zinc-850/60 transition">
              <td class="py-2.5 px-3 font-medium text-zinc-200 flex items-center gap-2">
                <FileText class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                <span class="truncate max-w-xs">{f.name}</span>
              </td>
              <td class="py-2.5 px-3 text-zinc-400">
                <span class="font-medium text-zinc-200">{f.ownerUsername}</span>
                <span class="block text-[10px] text-zinc-500">{f.ownerEmail}</span>
              </td>
              <td class="py-2.5 px-3">
                <span class="px-2 py-0.5 bg-zinc-800 text-zinc-300 rounded text-[10px] font-medium capitalize">
                  {f.permission}
                </span>
              </td>
              <td class="py-2.5 px-3 text-zinc-400 font-mono text-[11px]">{formatBytes(f.size)}</td>
              <td class="py-2.5 px-3 text-zinc-500 text-[11px]">{formatDate(f.sharedAt)}</td>
              <td class="py-2.5 px-3 text-right space-x-1">
                <button
                  onclick={() => handlePreview(f)}
                  class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                  title="Preview"
                >
                  <Eye class="w-3.5 h-3.5" />
                </button>

                <button
                  onclick={() => handleDownload(f)}
                  class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                  title="Download"
                >
                  <Download class="w-3.5 h-3.5" />
                </button>

                <button
                  onclick={() => handleAddShortcut('file', f.id)}
                  class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                  title="Add shortcut to My Drive"
                >
                  {#if addedShortcutId === f.id}
                    <Check class="w-3.5 h-3.5 text-emerald-400" />
                  {:else}
                    <BookmarkPlus class="w-3.5 h-3.5" />
                  {/if}
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>
