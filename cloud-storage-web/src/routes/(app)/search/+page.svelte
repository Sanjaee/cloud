<script lang="ts">
  import { page } from '$app/state';
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo } from '$lib/utils/format-date';
  import { Search, FileText, Folder } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { FileItem, FolderItem } from '$lib/api/types';

  let query = $derived(page.url.searchParams.get('q') || '');
  let files = $state<FileItem[]>([]);
  let folders = $state<FolderItem[]>([]);
  let isLoading = $state(false);

  async function performSearch(q: string) {
    if (!q.trim()) return;
    isLoading = true;
    try {
      const [filesRes, foldersRes] = await Promise.all([
        apiRequest('/files'),
        apiRequest('/folders'),
      ]);
      const lower = q.toLowerCase();
      files = (filesRes.files || []).filter((f: any) => f.name.toLowerCase().includes(lower));
      folders = (foldersRes.folders || []).filter((fol: any) => fol.name.toLowerCase().includes(lower));
    } finally {
      isLoading = false;
    }
  }

  $effect(() => {
    performSearch(query);
  });
</script>

<div class="space-y-6">
  <div>
    <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
      <Search class="w-7 h-7 text-blue-600" />
      Search Results for "{query}"
    </h1>
    <p class="text-xs text-slate-500 mt-1">Found {files.length} file(s) and {folders.length} folder(s).</p>
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if files.length === 0 && folders.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Search class="w-12 h-12 text-slate-400 mx-auto mb-3" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">No results found</h3>
      <p class="text-xs text-slate-500 mt-1">Try searching for a different keyword.</p>
    </div>
  {:else}
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
      {#each files as f}
        <div
          role="button"
          tabindex="0"
          ondblclick={() => selectionStore.getState().setPreviewFile(f)}
          onkeydown={(e) => e.key === 'Enter' && selectionStore.getState().setPreviewFile(f)}
          class="flex flex-col p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-xs hover:shadow-md transition cursor-pointer select-none"
        >
          <div class="h-20 flex items-center justify-center text-blue-500 mb-2">
            <FileText class="w-10 h-10" />
          </div>
          <span class="text-xs font-semibold text-slate-900 dark:text-white truncate">{f.name}</span>
          <span class="text-[11px] text-slate-400 mt-1">{formatBytes(f.size)}</span>
        </div>
      {/each}
    </div>
  {/if}
</div>

