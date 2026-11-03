<script lang="ts">
  import { page } from '$app/state';
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo } from '$lib/utils/format-date';
  import { getFileCategory } from '$lib/utils/file-type';
  import {
    Search,
    FileText,
    Folder,
    FolderOpen,
    Eye,
    ChevronRight,
    Star,
    ArrowUpRight,
    Download,
    Image,
    Film,
    Music,
    FileSpreadsheet,
    FileArchive,
  } from 'lucide-svelte';
  import type { FileItem, FolderItem } from '$lib/api/types';

  let query = $derived(page.url.searchParams.get('q') || '');
  let files = $state<FileItem[]>([]);
  let folders = $state<FolderItem[]>([]);
  let isLoading = $state(false);

  async function performSearch(q: string) {
    if (!q || !q.trim()) {
      files = [];
      folders = [];
      return;
    }
    isLoading = true;
    try {
      const res = await apiRequest(`/files/search?q=${encodeURIComponent(q.trim())}`);
      files = res.files || [];
      folders = res.folders || [];
    } catch (err) {
      console.error('Failed to search files:', err);
      files = [];
      folders = [];
    } finally {
      isLoading = false;
    }
  }

  $effect(() => {
    performSearch(query);
  });

  function getIcon(mimeType: string, filename: string) {
    const cat = getFileCategory(mimeType, filename);
    if (cat === 'image') return Image;
    if (cat === 'video') return Film;
    if (cat === 'audio') return Music;
    if (cat === 'spreadsheet') return FileSpreadsheet;
    if (cat === 'archive') return FileArchive;
    return FileText;
  }

  function navigateToFolder(folderId: string | null) {
    if (folderId) {
      window.location.href = `/drive?folder=${folderId}`;
    } else {
      window.location.href = '/drive';
    }
  }
</script>

<div class="space-y-6">
  <!-- Search Header -->
  <div class="flex items-center justify-between pb-4 border-b border-zinc-800/80">
    <div class="flex items-center gap-3">
      <div class="w-8 h-8 rounded-lg bg-zinc-850 text-zinc-300 flex items-center justify-center shrink-0">
        <Search class="w-4 h-4" />
      </div>
      <div>
        <h1 class="text-xl font-bold text-white tracking-tight">
          Search results: "{query}"
        </h1>
        <p class="text-xs text-zinc-400 mt-0.5">
          Found {files.length} files and {folders.length} folders in your drive
        </p>
      </div>
    </div>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Searching files and folders...</p>
    </div>
  {:else if !query}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Search class="w-10 h-10 text-zinc-500 mx-auto mb-3 stroke-1" />
      <h3 class="font-semibold text-white text-sm">Enter a keyword to search</h3>
      <p class="text-xs text-zinc-400 mt-1">Search through your files, folders, and categories.</p>
    </div>
  {:else if files.length === 0 && folders.length === 0}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Search class="w-10 h-10 text-zinc-500 mx-auto mb-3 stroke-1" />
      <h3 class="font-semibold text-white text-sm">No matching items found</h3>
      <p class="text-xs text-zinc-400 mt-1">
        No files or folders matching "{query}".
      </p>
    </div>
  {:else}
    <!-- Matching Folders Section -->
    {#if folders.length > 0}
      <div>
        <h2 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-2.5 flex items-center gap-2">
          <Folder class="w-3.5 h-3.5 text-amber-400" />
          <span>Folders ({folders.length})</span>
        </h2>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {#each folders as fol}
            <div
              role="button"
              tabindex="0"
              onclick={() => navigateToFolder(fol.id)}
              onkeydown={(e) => e.key === 'Enter' && navigateToFolder(fol.id)}
              class="group flex flex-col p-3 bg-[#121214] hover:bg-zinc-900 border border-zinc-800/80 hover:border-zinc-700 rounded-xl shadow-xs transition cursor-pointer"
            >
              <div class="flex items-center justify-between mb-2">
                <div class="w-7 h-7 rounded-lg bg-zinc-800/80 text-amber-400 flex items-center justify-center shrink-0">
                  <Folder class="w-4 h-4 fill-amber-400/20" />
                </div>
                <ArrowUpRight class="w-3.5 h-3.5 text-zinc-500 group-hover:text-white transition" />
              </div>

              <span class="text-xs font-medium text-zinc-200 truncate group-hover:text-white transition">
                {fol.name}
              </span>

              <div class="mt-2 pt-1.5 border-t border-zinc-800/60 text-[10px] text-zinc-500 truncate">
                <span>📁 {fol.parentPath || 'My Drive'}</span>
              </div>
            </div>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Matching Files Section -->
    {#if files.length > 0}
      <div>
        <h2 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-2.5 flex items-center gap-2">
          <FileText class="w-3.5 h-3.5 text-zinc-400" />
          <span>Files ({files.length})</span>
        </h2>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
          {#each files as f}
            {@const IconComponent = getIcon(f.mimeType, f.name)}
            {@const cat = getFileCategory(f.mimeType, f.name)}
            <div class="group relative flex flex-col bg-[#121214] hover:bg-zinc-900/90 border border-zinc-800/80 hover:border-zinc-700 rounded-xl shadow-xs transition overflow-hidden">
              <!-- Thumbnail -->
              <div
                role="button"
                tabindex="0"
                onclick={() => selectionStore.getState().setPreviewFile(f)}
                onkeydown={(e) => e.key === 'Enter' && selectionStore.getState().setPreviewFile(f)}
                class="h-28 bg-zinc-950/70 flex items-center justify-center overflow-hidden relative cursor-pointer"
              >
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
                {:else if cat === 'pdf'}
                  <div class="p-2.5 bg-rose-950/30 border border-rose-900/40 rounded-lg text-rose-400">
                    <FileText class="w-6 h-6" />
                  </div>
                {:else}
                  <div class="p-2.5 bg-zinc-900 border border-zinc-800 rounded-lg text-zinc-400">
                    <IconComponent class="w-6 h-6 stroke-1" />
                  </div>
                {/if}
              </div>

              <!-- Content -->
              <div class="p-2.5 bg-[#121214] border-t border-zinc-800/80 flex flex-col justify-between gap-2">
                <div>
                  <div class="flex items-center justify-between gap-1 mb-1">
                    <button
                      onclick={() => selectionStore.getState().setPreviewFile(f)}
                      class="text-xs font-medium text-zinc-200 truncate flex-1 text-left hover:text-white transition cursor-pointer"
                    >
                      {f.name}
                    </button>
                    {#if f.isStarred}
                      <Star class="w-3 h-3 text-amber-400 fill-amber-400 shrink-0" />
                    {/if}
                  </div>
                  <div class="flex items-center justify-between text-[10px] text-zinc-500">
                    <span>{formatBytes(f.size)}</span>
                    <span>{timeAgo(f.updatedAt)}</span>
                  </div>
                </div>

                <!-- Folder Location Link -->
                <div class="pt-1.5 border-t border-zinc-800/60 flex items-center justify-between gap-1">
                  <button
                    onclick={() => navigateToFolder(f.folderId)}
                    class="flex items-center gap-1 text-[10px] text-zinc-400 hover:text-white truncate text-left transition cursor-pointer"
                  >
                    <FolderOpen class="w-3 h-3 text-amber-400 shrink-0" />
                    <span class="truncate">{f.folderPath || 'My Drive'}</span>
                  </button>

                  <button
                    onclick={() => navigateToFolder(f.folderId)}
                    class="shrink-0 text-[10px] px-1.5 py-0.5 rounded bg-zinc-800 hover:bg-zinc-700 text-zinc-300 hover:text-white transition cursor-pointer"
                  >
                    Open
                  </button>
                </div>
              </div>
            </div>
          {/each}
        </div>
      </div>
    {/if}
  {/if}
</div>
