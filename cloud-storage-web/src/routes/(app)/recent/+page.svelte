<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo } from '$lib/utils/format-date';
  import { Clock, FileText, Image, Film, Music, FileSpreadsheet, FileArchive } from 'lucide-svelte';
  import { getFileCategory } from '$lib/utils/file-type';
  import { onMount } from 'svelte';
  import type { FileItem } from '$lib/api/types';

  let recentFiles = $state<FileItem[]>([]);
  let isLoading = $state(true);

  async function loadRecent() {
    isLoading = true;
    try {
      const res = await apiRequest('/files');
      recentFiles = res.files || [];
    } finally {
      isLoading = false;
    }
  }

  function handlePreview(file: FileItem) {
    selectionStore.getState().setPreviewFile(file);
  }

  onMount(() => {
    loadRecent();
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
</script>

<div class="space-y-6">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-xl font-bold text-white flex items-center gap-2">
        <Clock class="w-5 h-5 text-zinc-400" />
        Recent Files
      </h1>
      <p class="text-xs text-zinc-400 mt-0.5">Files uploaded or accessed recently in your workspace.</p>
    </div>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Loading recent files...</p>
    </div>
  {:else if recentFiles.length === 0}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Clock class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
      <h3 class="font-semibold text-white text-sm">No recent activity</h3>
      <p class="text-xs text-zinc-400 mt-1">Uploaded or modified files will appear here automatically.</p>
    </div>
  {:else}
    <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
      {#each recentFiles as f}
        {@const IconComponent = getIcon(f.mimeType, f.name)}
        {@const category = getFileCategory(f.mimeType, f.name)}
        <div
          role="button"
          tabindex="0"
          ondblclick={() => handlePreview(f)}
          onkeydown={(e) => e.key === 'Enter' && handlePreview(f)}
          class="group relative flex flex-col bg-[#121214] hover:bg-zinc-900/90 border border-zinc-800/80 hover:border-zinc-700 rounded-xl shadow-xs transition overflow-hidden cursor-pointer select-none"
        >
          <!-- Thumbnail Area -->
          <div class="h-28 bg-zinc-950/70 flex items-center justify-center overflow-hidden relative">
            {#if category === 'image'}
              <img
                src="/api/v1/downloads/{f.id}/view"
                alt={f.name}
                loading="lazy"
                class="w-full h-full object-cover group-hover:scale-105 transition duration-200"
                onerror={(e) => {
                  const img = e.currentTarget as HTMLImageElement;
                  img.style.display = 'none';
                  const fb = img.nextElementSibling as HTMLElement;
                  if (fb) fb.classList.remove('hidden');
                }}
              />
              <div class="hidden flex-col items-center justify-center gap-1 text-zinc-500">
                <Image class="w-8 h-8 text-zinc-500 stroke-1" />
              </div>
            {:else if category === 'pdf'}
              <div class="p-2.5 bg-rose-950/30 border border-rose-900/40 rounded-lg text-rose-400">
                <FileText class="w-6 h-6" />
              </div>
            {:else}
              <div class="p-2.5 bg-zinc-900 border border-zinc-800 rounded-lg text-zinc-400">
                <IconComponent class="w-6 h-6 stroke-1" />
              </div>
            {/if}
          </div>

          <div class="p-2.5 bg-[#121214] border-t border-zinc-800/80">
            <span class="text-xs font-medium text-zinc-200 truncate block group-hover:text-white transition">{f.name}</span>
            <div class="flex items-center justify-between text-[10px] text-zinc-500 mt-1">
              <span>{formatBytes(f.size)}</span>
              <span>{timeAgo(f.updatedAt)}</span>
            </div>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>
