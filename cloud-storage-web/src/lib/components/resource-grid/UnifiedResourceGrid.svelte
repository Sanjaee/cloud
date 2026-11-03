<script lang="ts">
  import { selectionStore, type BatchResourceItem } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo, formatDate } from '$lib/utils/format-date';
  import { getFileCategory } from '$lib/utils/file-type';
  import type { FileItem, FolderItem } from '$lib/api/types';
  import {
    Folder,
    FileText,
    Image,
    Film,
    Music,
    FileSpreadsheet,
    FileArchive,
    Star,
    MoreVertical,
    CheckSquare,
    Square,
    Check,
    X,
    Share2,
    Trash2,
    RotateCcw,
    Download,
    BookmarkPlus,
    LayoutGrid,
    List,
    UploadCloud,
    Plus,
    AlertCircle,
  } from 'lucide-svelte';

  interface Props {
    files?: FileItem[];
    folders?: FolderItem[];
    context?: 'drive' | 'trash' | 'starred' | 'recent' | 'shared-with-me' | 'shared-by-me';
    isLoading?: boolean;
    emptyTitle?: string;
    emptySubtitle?: string;
    showCategories?: boolean;
    onFolderDblClick?: (folder: FolderItem) => void;
    onFileDblClick?: (file: FileItem) => void;
    onContextMenu?: (e: MouseEvent, type: 'file' | 'folder', item: any) => void;
    onToggleStar?: (type: 'file' | 'folder', item: any) => Promise<void>;
    onBatchShare?: (items: BatchResourceItem[]) => void;
    onBatchDelete?: (fileIds: string[], folderIds: string[]) => Promise<void>;
    onBatchRestore?: (fileIds: string[], folderIds: string[]) => Promise<void>;
    onBatchPermanentDelete?: (fileIds: string[], folderIds: string[]) => Promise<void>;
    onBatchDownload?: (files: FileItem[]) => Promise<void>;
    onSingleRestore?: (type: 'file' | 'folder', item: any) => Promise<void>;
    onSinglePermanentDelete?: (type: 'file' | 'folder', item: any) => Promise<void>;
  }

  let {
    files = [],
    folders = [],
    context = 'drive',
    isLoading = false,
    emptyTitle = 'No items found',
    emptySubtitle = 'There are no files or folders to display in this view.',
    showCategories = true,
    onFolderDblClick,
    onFileDblClick,
    onContextMenu,
    onToggleStar,
    onBatchShare,
    onBatchDelete,
    onBatchRestore,
    onBatchPermanentDelete,
    onBatchDownload,
    onSingleRestore,
    onSinglePermanentDelete,
  }: Props = $props();

  let categoryFilter = $state('all');
  let viewMode = $state<'grid' | 'table'>('grid');
  let selectedFileIds = $state<Set<string>>(new Set());
  let selectedFolderIds = $state<Set<string>>(new Set());
  let isActionBusy = $state(false);

  // Sync with global store selection
  selectionStore.subscribe((s) => {
    selectedFileIds = s.selectedFileIds;
    selectedFolderIds = s.selectedFolderIds;
    viewMode = s.viewMode;
  });

  let filteredFiles = $derived.by(() => {
    if (categoryFilter === 'all') return files;
    return files.filter((f) => {
      const cat = getFileCategory(f.mimeType, f.name);
      if (categoryFilter === 'documents') return cat === 'document' || cat === 'pdf' || cat === 'spreadsheet';
      if (categoryFilter === 'images') return cat === 'image';
      if (categoryFilter === 'media') return cat === 'video' || cat === 'audio';
      if (categoryFilter === 'archives') return cat === 'archive';
      return false;
    });
  });

  let totalSelectedCount = $derived(selectedFileIds.size + selectedFolderIds.size);

  function toggleFile(id: string, e: MouseEvent) {
    e.stopPropagation();
    selectionStore.getState().toggleFileSelection(id, true);
  }

  function toggleFolder(id: string, e: MouseEvent) {
    e.stopPropagation();
    selectionStore.getState().toggleFolderSelection(id, true);
  }

  function handleSelectAll() {
    const isAllSelected =
      selectedFileIds.size === filteredFiles.length &&
      selectedFolderIds.size === folders.length &&
      filteredFiles.length + folders.length > 0;

    if (isAllSelected) {
      selectionStore.getState().clearSelection();
    } else {
      selectionStore.getState().selectAll(
        filteredFiles.map((f) => f.id),
        folders.map((fol) => fol.id)
      );
    }
  }

  function getIcon(mimeType: string, filename: string) {
    const cat = getFileCategory(mimeType, filename);
    if (cat === 'image') return Image;
    if (cat === 'video') return Film;
    if (cat === 'audio') return Music;
    if (cat === 'spreadsheet') return FileSpreadsheet;
    if (cat === 'archive') return FileArchive;
    return FileText;
  }

  function triggerBatchShare() {
    if (totalSelectedCount === 0) return;
    const items: BatchResourceItem[] = [];
    for (const id of selectedFileIds) {
      const f = files.find((file) => file.id === id);
      if (f) items.push({ type: 'file', item: f });
    }
    for (const id of selectedFolderIds) {
      const fol = folders.find((folder) => folder.id === id);
      if (fol) items.push({ type: 'folder', item: fol });
    }

    if (onBatchShare) {
      onBatchShare(items);
    } else {
      if (items.length === 1) {
        selectionStore.getState().setSharingResource(items[0]);
      } else {
        selectionStore.getState().setBatchSharingResources(items);
      }
    }
  }

  async function triggerBatchDelete() {
    if (totalSelectedCount === 0 || !onBatchDelete) return;
    isActionBusy = true;
    try {
      await onBatchDelete(Array.from(selectedFileIds), Array.from(selectedFolderIds));
      selectionStore.getState().clearSelection();
    } finally {
      isActionBusy = false;
    }
  }

  async function triggerBatchRestore() {
    if (totalSelectedCount === 0 || !onBatchRestore) return;
    isActionBusy = true;
    try {
      await onBatchRestore(Array.from(selectedFileIds), Array.from(selectedFolderIds));
      selectionStore.getState().clearSelection();
    } finally {
      isActionBusy = false;
    }
  }

  async function triggerBatchPermanentDelete() {
    if (totalSelectedCount === 0 || !onBatchPermanentDelete) return;
    if (!confirm(`Hapus permanen ${totalSelectedCount} item yang dipilih dari Cloudflare R2? Tindakan ini tidak dapat dibatalkan.`)) return;
    isActionBusy = true;
    try {
      await onBatchPermanentDelete(Array.from(selectedFileIds), Array.from(selectedFolderIds));
      selectionStore.getState().clearSelection();
    } finally {
      isActionBusy = false;
    }
  }

  async function triggerBatchDownload() {
    if (selectedFileIds.size === 0 || !onBatchDownload) return;
    const selectedFiles = files.filter((f) => selectedFileIds.has(f.id));
    await onBatchDownload(selectedFiles);
  }
</script>

<div class="space-y-4">
  <!-- Filter Pills & Toolbar Controls Row -->
  <div class="flex flex-wrap items-center justify-between gap-3">
    <!-- Category Tabs -->
    {#if showCategories}
      <div class="inline-flex items-center p-1 bg-[#121214] border border-zinc-800/80 rounded-lg text-xs overflow-x-auto max-w-full">
        {#each [
          { id: 'all', label: 'All Items' },
          { id: 'documents', label: 'Documents' },
          { id: 'images', label: 'Images' },
          { id: 'media', label: 'Media' },
          { id: 'archives', label: 'Archives' },
        ] as tab}
          <button
            onclick={() => (categoryFilter = tab.id)}
            class="px-3 py-1 rounded-md font-medium transition cursor-pointer whitespace-nowrap {categoryFilter === tab.id
              ? 'bg-zinc-800 text-white font-semibold shadow-xs'
              : 'text-zinc-400 hover:text-zinc-200'}"
          >
            {tab.label}
          </button>
        {/each}
      </div>
    {:else}
      <div></div>
    {/if}

    <!-- Selection & View Mode Controls -->
    <div class="flex items-center gap-2">
      <!-- Select All Button -->
      <button
        onclick={handleSelectAll}
        class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-[#121214] hover:bg-zinc-850 border border-zinc-800/80 hover:border-zinc-700 rounded-lg text-xs font-medium text-zinc-300 transition cursor-pointer select-none"
      >
        {#if selectedFileIds.size === filteredFiles.length && selectedFolderIds.size === folders.length && filteredFiles.length + folders.length > 0}
          <CheckSquare class="w-3.5 h-3.5 text-white" />
          <span>Deselect All</span>
        {:else}
          <Square class="w-3.5 h-3.5 text-zinc-500" />
          <span>Select All</span>
        {/if}
      </button>

      {#if totalSelectedCount > 0}
        <span class="text-xs font-medium text-zinc-300 px-2.5 py-1 rounded-md bg-zinc-800/90 border border-zinc-700/60 shadow-xs">
          {totalSelectedCount} selected
        </span>
      {/if}

      <!-- Grid / Table View Toggle -->
      <div class="hidden sm:inline-flex items-center p-0.5 bg-[#121214] border border-zinc-800/80 rounded-lg text-xs">
        <button
          onclick={() => selectionStore.getState().setViewMode('grid')}
          class="p-1.5 rounded-md transition cursor-pointer {viewMode === 'grid'
            ? 'bg-zinc-800 text-white shadow-xs'
            : 'text-zinc-400 hover:text-zinc-200'}"
          title="Grid view"
        >
          <LayoutGrid class="w-3.5 h-3.5" />
        </button>
        <button
          onclick={() => selectionStore.getState().setViewMode('table')}
          class="p-1.5 rounded-md transition cursor-pointer {viewMode === 'table'
            ? 'bg-zinc-800 text-white shadow-xs'
            : 'text-zinc-400 hover:text-zinc-200'}"
          title="List / Table view"
        >
          <List class="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  </div>

  <!-- Loading State -->
  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-24 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Memuat item...</p>
    </div>
  {:else if folders.length === 0 && filteredFiles.length === 0}
    <!-- Empty State -->
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-md mx-auto">
      <UploadCloud class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
      <h3 class="font-semibold text-white text-sm">{emptyTitle}</h3>
      <p class="text-xs text-zinc-400 mt-1">{emptySubtitle}</p>
    </div>
  {:else}
    <!-- FOLDERS SECTION -->
    {#if folders.length > 0 && categoryFilter === 'all'}
      <div>
        <div class="flex items-center justify-between mb-2.5">
          <h3 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400">
            Folders ({folders.length})
          </h3>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {#each folders as fol}
            {@const isSelected = selectedFolderIds.has(fol.id)}
            <div
              role="button"
              tabindex="0"
              ondblclick={() => onFolderDblClick?.(fol)}
              onkeydown={(e) => e.key === 'Enter' && onFolderDblClick?.(fol)}
              oncontextmenu={(e) => onContextMenu?.(e, 'folder', fol)}
              class="group relative flex items-center gap-2.5 p-2.5 bg-[#121214] hover:bg-zinc-900 border {isSelected
                ? 'border-white ring-1 ring-white/40 bg-zinc-900'
                : 'border-zinc-800/80 hover:border-zinc-700'} rounded-xl shadow-xs transition cursor-pointer select-none"
            >
              <!-- Checkbox Toggle -->
              <button
                type="button"
                onclick={(e) => toggleFolder(fol.id, e)}
                class="shrink-0 p-0.5 rounded text-zinc-500 hover:text-white transition {isSelected
                  ? 'opacity-100 text-white'
                  : 'opacity-0 group-hover:opacity-100'}"
                title="Select folder"
              >
                {#if isSelected}
                  <CheckSquare class="w-3.5 h-3.5 text-white" />
                {:else}
                  <Square class="w-3.5 h-3.5 text-zinc-500 hover:text-white" />
                {/if}
              </button>

              <div class="w-7 h-7 rounded-lg flex items-center justify-center bg-zinc-800/80 text-amber-400 shrink-0">
                <Folder class="w-4 h-4 fill-amber-400/20" />
              </div>

              <span class="text-xs font-medium text-zinc-200 truncate flex-1 group-hover:text-white transition">
                {fol.name}
              </span>

              {#if fol.isStarred}
                <Star class="w-3 h-3 text-amber-400 fill-amber-400 shrink-0" />
              {/if}

              <!-- Trash Quick Actions -->
              {#if context === 'trash'}
                <div class="opacity-0 group-hover:opacity-100 flex items-center gap-0.5 transition">
                  {#if onSingleRestore}
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        onSingleRestore('folder', fol);
                      }}
                      class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white"
                      title="Restore"
                    >
                      <RotateCcw class="w-3 h-3" />
                    </button>
                  {/if}
                  {#if onSinglePermanentDelete}
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        onSinglePermanentDelete('folder', fol);
                      }}
                      class="p-1 hover:bg-red-950/60 rounded text-zinc-400 hover:text-red-400"
                      title="Delete Permanently"
                    >
                      <Trash2 class="w-3 h-3" />
                    </button>
                  {/if}
                </div>
              {:else if onContextMenu}
                <button
                  onclick={(e) => onContextMenu(e, 'folder', fol)}
                  class="opacity-0 group-hover:opacity-100 p-1 hover:bg-zinc-800 rounded-md text-zinc-400 hover:text-white transition cursor-pointer"
                >
                  <MoreVertical class="w-3 h-3" />
                </button>
              {/if}
            </div>
          {/each}
        </div>
      </div>
    {/if}

    <!-- FILES SECTION -->
    <div class="space-y-3">
      {#if files.length > 0}
        <div class="flex items-center justify-between">
          <h3 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400">
            Files ({filteredFiles.length})
          </h3>
        </div>
      {/if}

      {#if viewMode === 'grid'}
        <!-- GRID VIEW (Sejajar with Responsive Cards & Image Thumbnails) -->
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
          {#each filteredFiles as f}
            {@const IconComponent = getIcon(f.mimeType, f.name)}
            {@const isSelected = selectedFileIds.has(f.id)}
            {@const category = getFileCategory(f.mimeType, f.name)}
            <div
              role="button"
              tabindex="0"
              ondblclick={() => onFileDblClick?.(f)}
              onkeydown={(e) => e.key === 'Enter' && onFileDblClick?.(f)}
              oncontextmenu={(e) => onContextMenu?.(e, 'file', f)}
              class="group relative flex flex-col bg-[#121214] hover:bg-zinc-900/90 border {isSelected
                ? 'border-white ring-1 ring-white/50 bg-zinc-900'
                : 'border-zinc-800/80 hover:border-zinc-700'} rounded-xl shadow-xs transition overflow-hidden cursor-pointer select-none"
            >
              <!-- Checkbox Overlay (Top Left) -->
              <button
                type="button"
                onclick={(e) => toggleFile(f.id, e)}
                class="absolute top-2 left-2 z-10 p-1 rounded-md transition {isSelected
                  ? 'opacity-100 bg-white text-zinc-950 shadow-xs'
                  : 'opacity-0 group-hover:opacity-100 bg-black/60 backdrop-blur-xs text-zinc-300 hover:text-white'}"
                title="Select file"
              >
                {#if isSelected}
                  <Check class="w-3 h-3 stroke-[3]" />
                {:else}
                  <Square class="w-3 h-3 text-zinc-400 hover:text-white" />
                {/if}
              </button>

              <!-- Star Indicator (Top Right) -->
              {#if f.isStarred}
                <div class="absolute top-2 right-2 z-10 p-1 rounded-md bg-black/50 backdrop-blur-xs">
                  <Star class="w-3 h-3 text-amber-400 fill-amber-400" />
                </div>
              {/if}

              <!-- Trash Quick Action Pills (Top Right on Hover) -->
              {#if context === 'trash'}
                <div class="absolute top-2 right-2 z-10 opacity-0 group-hover:opacity-100 flex items-center gap-1 bg-black/70 backdrop-blur-xs p-0.5 rounded-lg transition">
                  {#if onSingleRestore}
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        onSingleRestore('file', f);
                      }}
                      class="p-1 hover:bg-zinc-800 rounded text-zinc-300 hover:text-white"
                      title="Restore"
                    >
                      <RotateCcw class="w-3 h-3" />
                    </button>
                  {/if}
                  {#if onSinglePermanentDelete}
                    <button
                      onclick={(e) => {
                        e.stopPropagation();
                        onSinglePermanentDelete('file', f);
                      }}
                      class="p-1 hover:bg-red-950/80 rounded text-zinc-300 hover:text-red-400"
                      title="Permanently Delete"
                    >
                      <Trash2 class="w-3 h-3" />
                    </button>
                  {/if}
                </div>
              {/if}

              <!-- Thumbnail / Category Preview Area -->
              <div class="h-28 sm:h-32 bg-zinc-950/70 flex items-center justify-center overflow-hidden relative">
                {#if category === 'image'}
                  <img
                    src="/api/v1/downloads/{f.id}/view"
                    alt={f.name}
                    loading="lazy"
                    class="w-full h-full object-cover group-hover:scale-105 transition duration-200"
                    onerror={(e) => {
                      const img = e.currentTarget as HTMLImageElement;
                      img.style.display = 'none';
                      const fallback = img.nextElementSibling as HTMLElement;
                      if (fallback) fallback.classList.remove('hidden');
                    }}
                  />
                  <!-- Fallback Icon if Image 404s/fails -->
                  <div class="hidden flex-col items-center justify-center gap-1 text-zinc-500">
                    <Image class="w-8 h-8 text-zinc-500 stroke-1" />
                    <span class="text-[10px] text-zinc-500">Image</span>
                  </div>
                {:else if category === 'pdf'}
                  <div class="flex flex-col items-center justify-center gap-1.5 text-rose-400">
                    <div class="p-2.5 bg-rose-950/30 border border-rose-900/40 rounded-lg group-hover:scale-105 transition">
                      <FileText class="w-6 h-6 text-rose-400" />
                    </div>
                    <span class="text-[9px] font-bold tracking-wider uppercase text-rose-400/90">PDF</span>
                  </div>
                {:else if category === 'spreadsheet'}
                  <div class="flex flex-col items-center justify-center gap-1.5 text-emerald-400">
                    <div class="p-2.5 bg-emerald-950/30 border border-emerald-900/40 rounded-lg group-hover:scale-105 transition">
                      <FileSpreadsheet class="w-6 h-6 text-emerald-400" />
                    </div>
                    <span class="text-[9px] font-bold tracking-wider uppercase text-emerald-400/90">XLSX</span>
                  </div>
                {:else if category === 'archive'}
                  <div class="flex flex-col items-center justify-center gap-1.5 text-amber-400">
                    <div class="p-2.5 bg-amber-950/30 border border-amber-900/40 rounded-lg group-hover:scale-105 transition">
                      <FileArchive class="w-6 h-6 text-amber-400" />
                    </div>
                    <span class="text-[9px] font-bold tracking-wider uppercase text-amber-400/90">Archive</span>
                  </div>
                {:else if category === 'video'}
                  <div class="flex flex-col items-center justify-center gap-1.5 text-violet-400">
                    <div class="p-2.5 bg-violet-950/30 border border-violet-900/40 rounded-lg group-hover:scale-105 transition">
                      <Film class="w-6 h-6 text-violet-400" />
                    </div>
                    <span class="text-[9px] font-bold tracking-wider uppercase text-violet-400/90">Video</span>
                  </div>
                {:else if category === 'audio'}
                  <div class="flex flex-col items-center justify-center gap-1.5 text-cyan-400">
                    <div class="p-2.5 bg-cyan-950/30 border border-cyan-900/40 rounded-lg group-hover:scale-105 transition">
                      <Music class="w-6 h-6 text-cyan-400" />
                    </div>
                    <span class="text-[9px] font-bold tracking-wider uppercase text-cyan-400/90">Audio</span>
                  </div>
                {:else}
                  <div class="p-2.5 bg-zinc-900 border border-zinc-800 rounded-lg group-hover:scale-105 transition">
                    <IconComponent class="w-6 h-6 text-zinc-400 stroke-1" />
                  </div>
                {/if}
              </div>

              <!-- File Info Footer -->
              <div class="p-2.5 bg-[#121214] border-t border-zinc-800/80">
                <div class="flex items-center justify-between gap-1 mb-1">
                  <span class="text-xs font-medium text-zinc-200 truncate flex-1 group-hover:text-white transition">
                    {f.name}
                  </span>
                </div>

                <div class="flex items-center justify-between text-[10px] text-zinc-500">
                  <span>{formatBytes(f.size)}</span>
                  <span>{timeAgo(f.updatedAt || (f as any).deletedAt)}</span>
                </div>
              </div>
            </div>
          {/each}
        </div>
      {:else}
        <!-- TABLE VIEW -->
        <div class="bg-[#121214] border border-zinc-800/80 rounded-xl overflow-hidden shadow-xs">
          <table class="w-full text-left text-xs">
            <thead class="bg-zinc-900/80 text-zinc-400 uppercase font-semibold text-[11px] tracking-wider border-b border-zinc-800/80">
              <tr>
                <th class="py-2.5 px-3 w-8">
                  <button onclick={handleSelectAll} class="text-zinc-500 hover:text-white cursor-pointer">
                    {#if selectedFileIds.size === filteredFiles.length && filteredFiles.length > 0}
                      <CheckSquare class="w-3.5 h-3.5 text-white" />
                    {:else}
                      <Square class="w-3.5 h-3.5" />
                    {/if}
                  </button>
                </th>
                <th class="py-2.5 px-3">Name</th>
                <th class="py-2.5 px-3">Size</th>
                <th class="py-2.5 px-3">Last Modified</th>
                <th class="py-2.5 px-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-zinc-800/60">
              {#each filteredFiles as f}
                {@const isSelected = selectedFileIds.has(f.id)}
                {@const IconComponent = getIcon(f.mimeType, f.name)}
                <tr
                  role="button"
                  tabindex="0"
                  ondblclick={() => onFileDblClick?.(f)}
                  oncontextmenu={(e) => onContextMenu?.(e, 'file', f)}
                  class="hover:bg-zinc-850/60 transition cursor-pointer {isSelected ? 'bg-zinc-850/80' : ''}"
                >
                  <td class="py-2.5 px-3" onclick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      onclick={(e) => toggleFile(f.id, e)}
                      class="text-zinc-500 hover:text-white cursor-pointer"
                    >
                      {#if isSelected}
                        <CheckSquare class="w-3.5 h-3.5 text-white" />
                      {:else}
                        <Square class="w-3.5 h-3.5" />
                      {/if}
                    </button>
                  </td>
                  <td class="py-2.5 px-3 font-medium text-zinc-200 flex items-center gap-2">
                    <IconComponent class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                    <span class="truncate max-w-xs">{f.name}</span>
                  </td>
                  <td class="py-2.5 px-3 text-zinc-400 font-mono text-[11px]">{formatBytes(f.size)}</td>
                  <td class="py-2.5 px-3 text-zinc-500 text-[11px]">{timeAgo(f.updatedAt || (f as any).deletedAt)}</td>
                  <td class="py-2.5 px-3 text-right space-x-1">
                    {#if context === 'trash'}
                      {#if onSingleRestore}
                        <button
                          onclick={(e) => {
                            e.stopPropagation();
                            onSingleRestore('file', f);
                          }}
                          class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                          title="Restore"
                        >
                          <RotateCcw class="w-3.5 h-3.5" />
                        </button>
                      {/if}
                      {#if onSinglePermanentDelete}
                        <button
                          onclick={(e) => {
                            e.stopPropagation();
                            onSinglePermanentDelete('file', f);
                          }}
                          class="p-1 hover:bg-red-950/60 text-zinc-400 hover:text-red-400 rounded transition cursor-pointer"
                          title="Permanently Delete"
                        >
                          <Trash2 class="w-3.5 h-3.5" />
                        </button>
                      {/if}
                    {:else if onContextMenu}
                      <button
                        onclick={(e) => onContextMenu(e, 'file', f)}
                        class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                      >
                        <MoreVertical class="w-3.5 h-3.5" />
                      </button>
                    {/if}
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    </div>
  {/if}

  <!-- FLOATING BATCH ACTION TOOLBAR (When Items are Selected) -->
  {#if totalSelectedCount > 0}
    <div class="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-[#18181b]/95 border border-zinc-700/80 text-white rounded-xl px-4 py-2 shadow-2xl backdrop-blur-md flex items-center gap-3 animate-in fade-in slide-in-from-bottom-4 duration-150">
      <div class="flex items-center gap-2 border-r border-zinc-700/80 pr-3">
        <span class="w-5 h-5 rounded-full bg-white text-zinc-950 font-bold text-[10px] flex items-center justify-center">
          {totalSelectedCount}
        </span>
        <span class="text-xs font-medium text-zinc-200">Selected</span>
      </div>

      <!-- TRASH CONTEXT ACTIONS -->
      {#if context === 'trash'}
        {#if onBatchRestore}
          <button
            onclick={triggerBatchRestore}
            disabled={isActionBusy}
            class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-zinc-800 rounded-lg text-xs font-medium text-emerald-300 hover:text-emerald-200 transition cursor-pointer"
            title="Restore selected items"
          >
            <RotateCcw class="w-3.5 h-3.5" />
            <span>{isActionBusy ? 'Memulihkan...' : 'Restore'}</span>
          </button>
        {/if}

        {#if onBatchPermanentDelete}
          <button
            onclick={triggerBatchPermanentDelete}
            disabled={isActionBusy}
            class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-red-950/60 text-red-400 hover:text-red-300 rounded-lg text-xs font-medium transition cursor-pointer"
            title="Delete permanently"
          >
            <Trash2 class="w-3.5 h-3.5" />
            <span>{isActionBusy ? 'Menghapus...' : 'Delete Permanently'}</span>
          </button>
        {/if}
      {:else}
        <!-- REGULAR DRIVE / SHARED / STARRED / RECENT ACTIONS -->
        <button
          onclick={triggerBatchShare}
          class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-zinc-800 rounded-lg text-xs font-medium text-zinc-200 hover:text-white transition cursor-pointer"
          title="Share selected items"
        >
          <Share2 class="w-3.5 h-3.5" />
          <span>Share</span>
        </button>

        {#if onBatchDownload && selectedFileIds.size > 0}
          <button
            onclick={triggerBatchDownload}
            class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-zinc-800 rounded-lg text-xs font-medium text-zinc-200 hover:text-white transition cursor-pointer"
            title="Download selected items"
          >
            <Download class="w-3.5 h-3.5" />
            <span>Download</span>
          </button>
        {/if}

        {#if onBatchDelete}
          <button
            onclick={triggerBatchDelete}
            disabled={isActionBusy}
            class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-red-950/60 text-red-400 hover:text-red-300 rounded-lg text-xs font-medium transition cursor-pointer"
            title="Move to trash"
          >
            <Trash2 class="w-3.5 h-3.5" />
            <span>{isActionBusy ? 'Menghapus...' : 'Delete'}</span>
          </button>
        {/if}
      {/if}

      <!-- Clear Selection Button -->
      <button
        onclick={() => selectionStore.getState().clearSelection()}
        class="p-1 hover:bg-zinc-800 rounded-md text-zinc-400 hover:text-white transition cursor-pointer ml-1"
        title="Clear selection"
      >
        <X class="w-3.5 h-3.5" />
      </button>
    </div>
  {/if}
</div>

