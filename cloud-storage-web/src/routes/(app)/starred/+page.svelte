<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo } from '$lib/utils/format-date';
  import { Star, FileText, Folder, Image, Film, Music, FileSpreadsheet } from 'lucide-svelte';
  import { getFileCategory } from '$lib/utils/file-type';
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

  function goToFolder(folderId: string) {
    window.location.href = `/drive?folder=${folderId}`;
  }

  onMount(() => {
    loadStarred();
  });
</script>

<div class="space-y-6">
  <div>
    <h1 class="text-xl font-bold text-white flex items-center gap-2">
      <Star class="w-5 h-5 text-amber-400 fill-amber-400" />
      Starred Items
    </h1>
    <p class="text-xs text-zinc-400 mt-0.5">Quick access to important files and folders you have starred.</p>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Loading starred items...</p>
    </div>
  {:else if starredFiles.length === 0 && starredFolders.length === 0}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Star class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
      <h3 class="font-semibold text-white text-sm">No starred items yet</h3>
      <p class="text-xs text-zinc-400 mt-1">Right-click any file or folder to add it to your starred list.</p>
    </div>
  {:else}
    <!-- Starred Folders -->
    {#if starredFolders.length > 0}
      <div>
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-2.5">
          Folders ({starredFolders.length})
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {#each starredFolders as fol}
            <button
              onclick={() => goToFolder(fol.id)}
              class="group flex items-center gap-2.5 p-2.5 bg-[#121214] hover:bg-zinc-900 border border-zinc-800/80 hover:border-zinc-700 rounded-xl shadow-xs transition cursor-pointer select-none text-left"
            >
              <div class="w-7 h-7 rounded-lg flex items-center justify-center bg-zinc-800/80 text-amber-400 shrink-0">
                <Folder class="w-4 h-4 fill-amber-400/20" />
              </div>
              <span class="text-xs font-medium text-zinc-200 truncate flex-1 group-hover:text-white transition">{fol.name}</span>
              <Star class="w-3 h-3 text-amber-400 fill-amber-400 shrink-0" />
            </button>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Starred Files -->
    {#if starredFiles.length > 0}
      <div>
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-2.5">
          Files ({starredFiles.length})
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
          {#each starredFiles as f}
            {@const cat = getFileCategory(f.mimeType, f.name)}
            <div
              role="button"
              tabindex="0"
              ondblclick={() => handlePreview(f)}
              onkeydown={(e) => e.key === 'Enter' && handlePreview(f)}
              class="group relative flex flex-col bg-[#121214] hover:bg-zinc-900/90 border border-zinc-800/80 hover:border-zinc-700 rounded-xl shadow-xs transition overflow-hidden cursor-pointer select-none"
            >
              <div class="h-28 bg-zinc-950/70 flex items-center justify-center overflow-hidden relative">
                {#if cat === 'image'}
                  <img
                    src="/api/v1/downloads/{f.id}/view"
                    alt={f.name}
                    loading="lazy"
                    class="w-full h-full object-cover group-hover:scale-105 transition"
                    onerror={(e) => {
                      const img = e.currentTarget as HTMLImageElement;
                      img.style.display = 'none';
                      const fb = img.nextElementSibling as HTMLElement;
                      if (fb) fb.classList.remove('hidden');
                    }}
                  />
                  <div class="hidden flex-col items-center justify-center text-zinc-500">
                    <Image class="w-8 h-8 text-zinc-500 stroke-1" />
                  </div>
                {:else}
                  <div class="p-2.5 bg-zinc-900 border border-zinc-800 rounded-lg text-zinc-400">
                    <FileText class="w-6 h-6 stroke-1" />
                  </div>
                {/if}
              </div>

              <div class="p-2.5 bg-[#121214] border-t border-zinc-800/80">
                <div class="flex items-center justify-between gap-1 mb-1">
                  <span class="text-xs font-medium text-zinc-200 truncate flex-1 group-hover:text-white transition">{f.name}</span>
                  <Star class="w-3 h-3 text-amber-400 fill-amber-400 shrink-0" />
                </div>
                <div class="flex items-center justify-between text-[10px] text-zinc-500">
                  <span>{formatBytes(f.size)}</span>
                  <span>{timeAgo(f.updatedAt)}</span>
                </div>
              </div>
            </div>
          {/each}
        </div>
      </div>
    {/if}
  {/if}
</div>
