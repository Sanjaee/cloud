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
    <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
      <Users class="w-7 h-7 text-blue-600" />
      Shared with me
    </h1>
    <p class="text-xs text-slate-500 mt-1">
      Files and folders shared with you by other users. No physical copies are duplicated in Cloudflare R2.
    </p>
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if sharedFiles.length === 0 && sharedFolders.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Users class="w-12 h-12 text-slate-400 mx-auto mb-3" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">No files shared with you yet</h3>
      <p class="text-xs text-slate-500 mt-1">When someone shares a file or folder with you, it will appear here.</p>
    </div>
  {:else}
    <!-- Shared Files Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl overflow-hidden shadow-xs">
      <table class="w-full text-left text-xs">
        <thead class="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase font-semibold tracking-wider border-b border-slate-200 dark:border-slate-800">
          <tr>
            <th class="py-3 px-4">Name</th>
            <th class="py-3 px-4">Shared By</th>
            <th class="py-3 px-4">Permission</th>
            <th class="py-3 px-4">Size</th>
            <th class="py-3 px-4">Date Shared</th>
            <th class="py-3 px-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          {#each sharedFiles as f}
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
              <td class="py-3 px-4 font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2.5">
                <FileText class="w-4 h-4 text-blue-500 shrink-0" />
                <span class="truncate max-w-xs">{f.name}</span>
              </td>
              <td class="py-3 px-4 text-slate-600 dark:text-slate-400">
                <span class="font-medium text-slate-900 dark:text-slate-200">{f.ownerUsername}</span>
                <span class="block text-[11px] text-slate-400">{f.ownerEmail}</span>
              </td>
              <td class="py-3 px-4">
                <span class="px-2.5 py-1 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-lg text-[11px] font-semibold capitalize">
                  {f.permission}
                </span>
              </td>
              <td class="py-3 px-4 text-slate-500">{formatBytes(f.size)}</td>
              <td class="py-3 px-4 text-slate-500">{formatDate(f.sharedAt)}</td>
              <td class="py-3 px-4 text-right space-x-1">
                <button
                  onclick={() => handlePreview(f)}
                  class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 hover:text-blue-600 transition cursor-pointer"
                  title="Preview"
                >
                  <Eye class="w-4 h-4" />
                </button>

                <button
                  onclick={() => handleDownload(f)}
                  class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 hover:text-blue-600 transition cursor-pointer"
                  title="Download"
                >
                  <Download class="w-4 h-4" />
                </button>

                <button
                  onclick={() => handleAddShortcut('file', f.id)}
                  class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-500 hover:text-blue-600 transition cursor-pointer"
                  title="Add shortcut to My Drive"
                >
                  {#if addedShortcutId === f.id}
                    <Check class="w-4 h-4 text-emerald-500" />
                  {:else}
                    <BookmarkPlus class="w-4 h-4" />
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

