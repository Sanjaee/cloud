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

<div class="space-y-8 max-w-7xl mx-auto">
  <!-- Search Header -->
  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
    <div>
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-2xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0">
          <Search class="w-5 h-5" />
        </div>
        <div>
          <h1 class="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
            Hasil Pencarian: "{query}"
          </h1>
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Ditemukan {files.length} file dan {folders.length} folder di drive Anda
          </p>
        </div>
      </div>
    </div>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-24 gap-3">
      <div class="w-9 h-9 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs font-medium text-slate-400">Mencari file dan lokasi folder...</p>
    </div>
  {:else if !query}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Search class="w-12 h-12 text-slate-400 mx-auto mb-3 stroke-1" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">Ketik kata kunci untuk mencari</h3>
      <p class="text-xs text-slate-500 mt-1">Cari berdasarkan nama file atau folder di cloud storage Anda.</p>
    </div>
  {:else if files.length === 0 && folders.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Search class="w-12 h-12 text-slate-400 mx-auto mb-3 stroke-1" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">Tidak ada hasil yang cocok</h3>
      <p class="text-xs text-slate-500 mt-1">
        Tidak ditemukan file atau folder yang mengandung kata kunci "{query}".
      </p>
    </div>
  {:else}
    <!-- Matching Folders Section -->
    {#if folders.length > 0}
      <div>
        <h2 class="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3 flex items-center gap-2">
          <Folder class="w-4 h-4 text-amber-500" />
          <span>Folder ({folders.length})</span>
        </h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
          {#each folders as fol}
            <div
              role="button"
              tabindex="0"
              onclick={() => navigateToFolder(fol.id)}
              onkeydown={(e) => e.key === 'Enter' && navigateToFolder(fol.id)}
              class="group flex flex-col p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-blue-400 dark:hover:border-blue-500 rounded-2xl shadow-xs hover:shadow-md transition cursor-pointer"
            >
              <div class="flex items-center justify-between mb-2">
                <div class="w-10 h-10 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-500 flex items-center justify-center shrink-0">
                  <Folder class="w-5 h-5 fill-current" />
                </div>
                <div class="p-1 rounded-lg text-slate-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 group-hover:bg-blue-50 dark:group-hover:bg-blue-950/60 transition">
                  <ArrowUpRight class="w-4 h-4" />
                </div>
              </div>

              <span class="text-sm font-semibold text-slate-900 dark:text-white truncate">
                {fol.name}
              </span>

              <!-- Folder Path Location -->
              <div class="mt-2 pt-2 border-t border-slate-100 dark:border-slate-800/80 flex items-center gap-1.5 text-[11px] text-slate-500 dark:text-slate-400 truncate">
                <span class="text-slate-400 shrink-0">Lokasi:</span>
                <span class="truncate font-medium text-blue-600 dark:text-blue-400 hover:underline">
                  {fol.parentPath || 'My Drive'}
                </span>
              </div>
            </div>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Matching Files Section -->
    {#if files.length > 0}
      <div>
        <h2 class="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3 flex items-center gap-2">
          <FileText class="w-4 h-4 text-blue-500" />
          <span>Files ({files.length})</span>
        </h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {#each files as f}
            {@const IconComponent = getIcon(f.mimeType, f.name)}
            <div
              class="group relative flex flex-col bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-blue-400 dark:hover:border-blue-500 rounded-2xl shadow-xs hover:shadow-md transition overflow-hidden"
            >
              <!-- Card Header / Preview Area -->
              <div
                role="button"
                tabindex="0"
                onclick={() => selectionStore.getState().setPreviewFile(f)}
                onkeydown={(e) => e.key === 'Enter' && selectionStore.getState().setPreviewFile(f)}
                class="h-28 bg-slate-50 dark:bg-slate-800/40 flex items-center justify-center overflow-hidden relative cursor-pointer group-hover:bg-slate-100/60 transition"
              >
                {#if getFileCategory(f.mimeType, f.name) === 'image'}
                  <img
                    src="/api/v1/downloads/{f.id}/view"
                    alt={f.name}
                    loading="lazy"
                    class="w-full h-full object-cover group-hover:scale-105 transition duration-200"
                  />
                {:else if getFileCategory(f.mimeType, f.name) === 'pdf'}
                  <div class="p-3 bg-rose-50 dark:bg-rose-950/40 rounded-xl text-rose-500 group-hover:scale-110 transition">
                    <FileText class="w-8 h-8" />
                  </div>
                {:else}
                  <IconComponent class="w-10 h-10 text-blue-500 stroke-1 group-hover:scale-105 transition" />
                {/if}

                <!-- Quick Preview Overlay button -->
                <div class="absolute inset-0 bg-slate-950/20 opacity-0 group-hover:opacity-100 flex items-center justify-center transition">
                  <span class="px-2.5 py-1 bg-white/90 dark:bg-slate-900/90 rounded-lg text-xs font-semibold text-slate-800 dark:text-slate-100 shadow-sm flex items-center gap-1.5">
                    <Eye class="w-3.5 h-3.5" />
                    Preview
                  </span>
                </div>
              </div>

              <!-- Card Content -->
              <div class="p-3.5 flex flex-col flex-1 justify-between gap-3">
                <div>
                  <div class="flex items-start justify-between gap-1 mb-1">
                    <button
                      onclick={() => selectionStore.getState().setPreviewFile(f)}
                      class="text-xs font-semibold text-slate-900 dark:text-white truncate flex-1 text-left hover:text-blue-600 transition cursor-pointer"
                      title={f.name}
                    >
                      {f.name}
                    </button>
                    {#if f.isStarred}
                      <Star class="w-3.5 h-3.5 text-amber-500 fill-amber-500 shrink-0" />
                    {/if}
                  </div>

                  <div class="flex items-center justify-between text-[11px] text-slate-400">
                    <span>{formatBytes(f.size)}</span>
                    <span>{timeAgo(f.updatedAt)}</span>
                  </div>
                </div>

                <!-- Folder Location Link -->
                <div class="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between gap-2">
                  <button
                    onclick={() => navigateToFolder(f.folderId)}
                    class="flex items-center gap-1.5 text-[11px] text-slate-500 dark:text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 truncate text-left transition cursor-pointer group/loc"
                    title="Buka folder: {f.folderPath || 'My Drive'}"
                  >
                    <FolderOpen class="w-3.5 h-3.5 text-blue-500 shrink-0 group-hover/loc:scale-110 transition" />
                    <span class="truncate font-medium">{f.folderPath || 'My Drive'}</span>
                  </button>

                  <button
                    onclick={() => navigateToFolder(f.folderId)}
                    class="shrink-0 text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-blue-950/60 hover:text-blue-600 dark:hover:text-blue-400 transition cursor-pointer"
                    title="Buka lokasi folder di My Drive"
                  >
                    Buka
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
