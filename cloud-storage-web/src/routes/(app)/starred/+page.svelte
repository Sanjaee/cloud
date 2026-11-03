<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo } from '$lib/utils/format-date';
  import { Star, FileText, Folder, Eye, Download } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { FileItem, FolderItem } from '$lib/api/types';

  let starredFiles = $state<FileItem[]>([]);
  let starredFolders = $state<FolderItem[]>([]);
  let isLoading = $state(true);

  async function loadStarred() {
    isLoading = true;
    try {
      const [filesRes, foldersRes] = await Promise.all([
        apiRequest('/files'),
        apiRequest('/folders'),
      ]);
      starredFiles = (filesRes.files || []).filter((f: any) => f.isStarred);
      starredFolders = (foldersRes.folders || []).filter((fol: any) => fol.isStarred);
    } finally {
      isLoading = false;
    }
  }

  function handlePreview(file: FileItem) {
    selectionStore.getState().setPreviewFile(file);
  }

  async function handleDownload(file: FileItem) {
    const res = await apiRequest(`/downloads/${file.id}`);
    window.open(res.downloadUrl, '_blank');
  }

  onMount(() => {
    loadStarred();
  });
</script>

<div class="space-y-6">
  <div>
    <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
      <Star class="w-7 h-7 text-amber-500 fill-amber-500" />
      Starred
    </h1>
    <p class="text-xs text-slate-500 mt-1">Quick access to all your starred files and folders.</p>
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if starredFiles.length === 0 && starredFolders.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Star class="w-12 h-12 text-slate-400 mx-auto mb-3" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">No starred items</h3>
      <p class="text-xs text-slate-500 mt-1">Star any file or folder to find it easily here.</p>
    </div>
  {:else}
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
      {#each starredFiles as f}
        <div
          role="button"
          tabindex="0"
          ondblclick={() => handlePreview(f)}
          onkeydown={(e) => e.key === 'Enter' && handlePreview(f)}
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

