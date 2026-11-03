<script lang="ts">
  import { page } from '$app/state';
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import { uploadStore } from '$lib/stores/upload.store';
  import { formatBytes } from '$lib/utils/file-size';
  import { timeAgo } from '$lib/utils/format-date';
  import { getFileCategory } from '$lib/utils/file-type';
  import {
    Folder,
    FileText,
    Image,
    Film,
    Music,
    MoreVertical,
    Star,
    Share2,
    Download,
    Trash2,
    Eye,
    ChevronRight,
    UploadCloud,
    FolderPlus,
    Check,
    CheckSquare,
    Square,
    Layers,
    X,
    Plus,
    HardDrive,
    Database,
    ShieldCheck,
    Sparkles,
    Filter,
    FileSpreadsheet,
    FileArchive,
  } from 'lucide-svelte';
  import { onMount, onDestroy } from 'svelte';
  import type { FileItem, FolderItem, BreadcrumbItem } from '$lib/api/types';

  let innerFolderFileInput: HTMLInputElement;
  let currentFolderId = $state<string | null>(null);
  let folders = $state<FolderItem[]>([]);
  let files = $state<FileItem[]>([]);
  let breadcrumbs = $state<BreadcrumbItem[]>([]);
  let isLoading = $state(true);
  let isDragOver = $state(false);
  let viewMode = $state('grid');
  let selectedFileIds = $state<Set<string>>(new Set());
  let selectedFolderIds = $state<Set<string>>(new Set());
  let isBatchDeleting = $state(false);
  let categoryFilter = $state<string>('all');

  // Context Menu State
  let activeMenu = $state<{
    type: 'file' | 'folder';
    item: any;
    x: number;
    y: number;
  } | null>(null);

  selectionStore.subscribe((s) => {
    viewMode = s.viewMode;
    selectedFileIds = s.selectedFileIds;
    selectedFolderIds = s.selectedFolderIds;
  });

  const totalSelectedCount = $derived(selectedFileIds.size + selectedFolderIds.size);

  // Derived filtered files
  const filteredFiles = $derived.by(() => {
    if (categoryFilter === 'all') return files;
    return files.filter((f) => {
      const cat = getFileCategory(f.mimeType, f.name);
      if (categoryFilter === 'documents') return cat === 'document' || cat === 'pdf' || cat === 'text' || cat === 'spreadsheet';
      if (categoryFilter === 'images') return cat === 'image';
      if (categoryFilter === 'media') return cat === 'video' || cat === 'audio';
      if (categoryFilter === 'archives') return cat === 'archive';
      return true;
    });
  });

  // Calculate stats
  const totalSizeBytes = $derived(files.reduce((acc, f) => acc + (f.size || 0), 0));
  const starredCount = $derived(files.filter(f => f.isStarred).length + folders.filter(f => f.isStarred).length);

  function toggleFile(id: string, e?: MouseEvent) {
    if (e) e.stopPropagation();
    selectionStore.getState().toggleFileSelection(id, true);
  }

  function toggleFolder(id: string, e?: MouseEvent) {
    if (e) e.stopPropagation();
    selectionStore.getState().toggleFolderSelection(id, true);
  }

  function handleSelectAll() {
    const allFileIds = filteredFiles.map((f) => f.id);
    const allFolderIds = folders.map((fol) => fol.id);
    if (
      selectedFileIds.size === allFileIds.length &&
      selectedFolderIds.size === allFolderIds.length &&
      allFileIds.length + allFolderIds.length > 0
    ) {
      selectionStore.getState().clearSelection();
    } else {
      selectionStore.getState().selectAll(allFileIds, allFolderIds);
    }
  }

  async function handleBatchDelete() {
    if (totalSelectedCount === 0) return;
    if (!confirm(`Are you sure you want to move ${totalSelectedCount} item(s) to trash?`)) return;

    isBatchDeleting = true;
    try {
      await apiRequest('/files/batch-delete', {
        method: 'POST',
        body: JSON.stringify({
          fileIds: Array.from(selectedFileIds),
          folderIds: Array.from(selectedFolderIds),
        }),
      });
      selectionStore.getState().clearSelection();
      await loadData(currentFolderId);
    } catch (err: any) {
      alert(err.message || 'Failed to delete selected items');
    } finally {
      isBatchDeleting = false;
    }
  }

  function handleBatchShare() {
    if (totalSelectedCount === 0) return;

    const items: Array<{ type: 'file' | 'folder'; item: any }> = [];
    for (const id of selectedFileIds) {
      const f = files.find((file) => file.id === id);
      if (f) items.push({ type: 'file', item: f });
    }
    for (const id of selectedFolderIds) {
      const fol = folders.find((folder) => folder.id === id);
      if (fol) items.push({ type: 'folder', item: fol });
    }

    if (items.length === 1) {
      selectionStore.getState().setSharingResource(items[0]);
    } else {
      selectionStore.getState().setBatchSharingResources(items);
    }
  }

  async function loadData(folderId: string | null = null) {
    currentFolderId = folderId;
    selectionStore.getState().setActiveFolderId(folderId);

    if (typeof window !== 'undefined') {
      const url = new URL(window.location.href);
      if (folderId) {
        url.searchParams.set('folder', folderId);
      } else {
        url.searchParams.delete('folder');
      }
      window.history.replaceState({}, '', url.toString());
    }

    isLoading = true;
    try {
      const folderParam = folderId ? `?parentId=${folderId}` : '';
      const [foldersRes, filesRes] = await Promise.all([
        apiRequest(`/folders${folderParam}`),
        apiRequest(`/files${folderId ? `?folderId=${folderId}` : ''}`),
      ]);

      folders = foldersRes.folders || [];
      files = filesRes.files || [];

      if (folderId) {
        const bcRes = await apiRequest(`/folders/${folderId}/breadcrumbs`);
        breadcrumbs = bcRes.breadcrumbs || [];
      } else {
        breadcrumbs = [{ id: null, name: 'My Drive' }];
      }
    } catch (err) {
      console.error('Failed to load drive items:', err);
    } finally {
      isLoading = false;
    }
  }

  function handleFolderDblClick(f: FolderItem) {
    selectionStore.getState().clearSelection();
    loadData(f.id);
  }

  function handleFileDblClick(f: FileItem) {
    selectionStore.getState().setPreviewFile(f);
  }

  function openContextMenu(e: MouseEvent, type: 'file' | 'folder', item: any) {
    e.preventDefault();
    e.stopPropagation();
    activeMenu = {
      type,
      item,
      x: e.clientX,
      y: e.clientY,
    };
  }

  async function handleDownload(file: FileItem) {
    const res = await apiRequest(`/downloads/${file.id}`);
    window.open(res.downloadUrl, '_blank');
  }

  async function handleToggleStar(type: 'file' | 'folder', item: any) {
    const endpoint = type === 'file' ? `/files/${item.id}/star` : `/folders/${item.id}/star`;
    await apiRequest(endpoint, {
      method: 'PATCH',
      body: JSON.stringify({ isStarred: !item.isStarred }),
    });
    item.isStarred = !item.isStarred;
  }

  async function handleDelete(type: 'file' | 'folder', item: any) {
    const endpoint = type === 'file' ? `/files/${item.id}` : `/folders/${item.id}`;
    await apiRequest(endpoint, { method: 'DELETE' });
    if (type === 'file') {
      files = files.filter((f) => f.id !== item.id);
    } else {
      folders = folders.filter((fol) => fol.id !== item.id);
    }
  }

  function handleShare(type: 'file' | 'folder', item: any) {
    selectionStore.getState().setSharingResource({ type, item });
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    isDragOver = false;
    if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
      uploadStore.getState().addFiles(e.dataTransfer.files, currentFolderId);
    }
  }

  function onGlobalClick() {
    activeMenu = null;
  }

  onMount(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const initialFolder = urlParams.get('folder');
    loadData(initialFolder || null);

    window.addEventListener('click', onGlobalClick);
    window.addEventListener('folder-created', () => loadData(currentFolderId));
    window.addEventListener('file-uploaded', () => loadData(currentFolderId));
  });

  onDestroy(() => {
    selectionStore.getState().setActiveFolderId(null);
    if (typeof window !== 'undefined') {
      window.removeEventListener('click', onGlobalClick);
      window.removeEventListener('folder-created', () => loadData(currentFolderId));
      window.removeEventListener('file-uploaded', () => loadData(currentFolderId));
    }
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

<div
  role="region"
  aria-label="File drop area"
  ondragover={(e) => {
    e.preventDefault();
    isDragOver = true;
  }}
  ondragleave={() => (isDragOver = false)}
  ondrop={handleDrop}
  class="relative min-h-full flex flex-col space-y-6 {isDragOver ? 'ring-2 ring-white/50 rounded-2xl bg-zinc-900/30' : ''}"
>
  <!-- Hidden File Input for Folder View Uploads -->
  <input
    bind:this={innerFolderFileInput}
    type="file"
    multiple
    class="hidden"
    onchange={(e) => {
      const input = e.target as HTMLInputElement;
      if (input.files && input.files.length > 0) {
        uploadStore.getState().addFiles(input.files, currentFolderId);
        input.value = '';
      }
    }}
  />

  <!-- Top Breadcrumb & Action Row -->
  <div class="flex flex-wrap items-center justify-between gap-3 text-xs">
    <!-- Breadcrumbs -->
    <div class="flex items-center gap-1.5 flex-wrap">
      {#each breadcrumbs as bc, i}
        <button
          onclick={() => loadData(bc.id)}
          class="flex items-center gap-1.5 font-medium transition cursor-pointer {i === breadcrumbs.length - 1
            ? 'font-semibold text-white'
            : 'text-zinc-400 hover:text-zinc-200'}"
        >
          <span>{bc.name}</span>
        </button>
        {#if i < breadcrumbs.length - 1}
          <ChevronRight class="w-3.5 h-3.5 text-zinc-600" />
        {/if}
      {/each}
    </div>

    <!-- Quick Actions -->
    <div class="flex items-center gap-2">
      <button
        type="button"
        onclick={() => innerFolderFileInput.click()}
        class="flex items-center gap-1.5 px-3 py-1.5 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer active:scale-95"
      >
        <Plus class="w-3.5 h-3.5 stroke-[2.5]" />
        <span>Upload Here</span>
      </button>
    </div>
  </div>

  <!-- Shadcn Dashboard Metric Cards Row -->
  <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
    <!-- Card 1: Total Files -->
    <div class="p-4 bg-[#121214] border border-zinc-800/80 hover:border-zinc-700/80 rounded-xl transition shadow-xs">
      <div class="flex items-center justify-between text-xs mb-1">
        <span class="text-zinc-400 font-medium">Total Files</span>
        <span class="text-[10px] font-semibold text-zinc-300 bg-zinc-800/80 px-2 py-0.5 rounded-md flex items-center gap-1">
          <HardDrive class="w-3 h-3 text-zinc-400" />
          Active
        </span>
      </div>
      <div class="text-2xl font-bold tracking-tight text-white">{files.length}</div>
      <div class="text-[11px] text-zinc-500 mt-1 flex items-center justify-between">
        <span>Files in current scope</span>
        <span>{formatBytes(totalSizeBytes)}</span>
      </div>
    </div>

    <!-- Card 2: Folders -->
    <div class="p-4 bg-[#121214] border border-zinc-800/80 hover:border-zinc-700/80 rounded-xl transition shadow-xs">
      <div class="flex items-center justify-between text-xs mb-1">
        <span class="text-zinc-400 font-medium">Folders</span>
        <span class="text-[10px] font-semibold text-zinc-300 bg-zinc-800/80 px-2 py-0.5 rounded-md flex items-center gap-1">
          <Folder class="w-3 h-3 text-amber-400" />
          Tree
        </span>
      </div>
      <div class="text-2xl font-bold tracking-tight text-white">{folders.length}</div>
      <div class="text-[11px] text-zinc-500 mt-1 flex items-center justify-between">
        <span>Subfolders available</span>
        <span>Organized</span>
      </div>
    </div>

    <!-- Card 3: Storage Used -->
    <div class="p-4 bg-[#121214] border border-zinc-800/80 hover:border-zinc-700/80 rounded-xl transition shadow-xs">
      <div class="flex items-center justify-between text-xs mb-1">
        <span class="text-zinc-400 font-medium">Storage Consumed</span>
        <span class="text-[10px] font-semibold text-zinc-300 bg-zinc-800/80 px-2 py-0.5 rounded-md flex items-center gap-1">
          <Database class="w-3 h-3 text-zinc-400" />
          10 GB Cap
        </span>
      </div>
      <div class="text-2xl font-bold tracking-tight text-white">{formatBytes(totalSizeBytes)}</div>
      <div class="text-[11px] text-zinc-500 mt-1 flex items-center justify-between">
        <span>Zero-egress transfers</span>
        <span>Free Tier</span>
      </div>
    </div>

    <!-- Card 4: Region & Cloud -->
    <div class="p-4 bg-[#121214] border border-zinc-800/80 hover:border-zinc-700/80 rounded-xl transition shadow-xs">
      <div class="flex items-center justify-between text-xs mb-1">
        <span class="text-zinc-400 font-medium">Cloud Engine</span>
        <span class="text-[10px] font-semibold text-emerald-400 bg-emerald-950/40 border border-emerald-800/50 px-2 py-0.5 rounded-md flex items-center gap-1">
          <ShieldCheck class="w-3 h-3" />
          Online
        </span>
      </div>
      <div class="text-2xl font-bold tracking-tight text-white">Singapore</div>
      <div class="text-[11px] text-zinc-500 mt-1 flex items-center justify-between">
        <span>Cloudflare R2 + sin1</span>
        <span>Low Latency</span>
      </div>
    </div>
  </div>

  <!-- Shadcn Filter Pill Bar & Selection Controls -->
  <div class="flex flex-wrap items-center justify-between gap-3 pt-2">
    <!-- Category Tabs (shadcn segmented control style) -->
    <div class="inline-flex items-center p-1 bg-[#121214] border border-zinc-800/80 rounded-lg text-xs">
      {#each [
        { id: 'all', label: 'All Items' },
        { id: 'documents', label: 'Documents' },
        { id: 'images', label: 'Images' },
        { id: 'media', label: 'Media' },
        { id: 'archives', label: 'Archives' },
      ] as tab}
        <button
          onclick={() => (categoryFilter = tab.id)}
          class="px-3 py-1 rounded-md font-medium transition cursor-pointer {categoryFilter === tab.id
            ? 'bg-zinc-800 text-white font-semibold shadow-xs'
            : 'text-zinc-400 hover:text-zinc-200'}"
        >
          {tab.label}
        </button>
      {/each}
    </div>

    <!-- Select All & Summary Trigger -->
    <div class="flex items-center gap-2">
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
        <span class="text-xs font-medium text-zinc-400 px-2 py-1 rounded bg-zinc-800/80">
          {totalSelectedCount} selected
        </span>
      {/if}
    </div>
  </div>

  {#if isLoading}
    <div class="flex-1 flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Fetching directory items...</p>
    </div>
  {:else}
    <!-- Folders Section -->
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
              ondblclick={() => handleFolderDblClick(fol)}
              onkeydown={(e) => e.key === 'Enter' && handleFolderDblClick(fol)}
              oncontextmenu={(e) => openContextMenu(e, 'folder', fol)}
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

              <button
                onclick={(e) => openContextMenu(e, 'folder', fol)}
                class="opacity-0 group-hover:opacity-100 p-1 hover:bg-zinc-800 rounded-md text-zinc-400 hover:text-white transition cursor-pointer"
              >
                <MoreVertical class="w-3 h-3" />
              </button>
            </div>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Files Section -->
    <div class="space-y-3">
      <div class="flex items-center justify-between">
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-zinc-400">
          Files ({filteredFiles.length})
        </h3>
      </div>

      {#if filteredFiles.length === 0 && (folders.length === 0 || categoryFilter !== 'all')}
        <div class="text-center py-16 border border-dashed border-zinc-800/80 rounded-2xl p-8 flex flex-col items-center justify-center bg-zinc-900/20">
          <UploadCloud class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
          <h4 class="font-semibold text-white text-sm">
            {currentFolderId ? 'Folder ini masih kosong' : 'Tidak ada file di direktori ini'}
          </h4>
          <p class="text-xs text-zinc-400 mt-1 mb-4 max-w-sm">
            {currentFolderId
              ? 'Tarik file ke sini atau klik tombol di bawah untuk upload langsung ke dalam folder ini.'
              : 'Unggah file dengan menyeret ke layar ini atau klik tombol upload.'}
          </p>
          <button
            type="button"
            onclick={() => innerFolderFileInput.click()}
            class="flex items-center gap-2 px-3.5 py-2 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer active:scale-95"
          >
            <Plus class="w-3.5 h-3.5 stroke-[2.5]" />
            <span>Upload ke folder ini</span>
          </button>
        </div>
      {:else if viewMode === 'grid'}
        <!-- Grid View (Shadcn compact cards) -->
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
          {#each filteredFiles as f}
            {@const IconComponent = getIcon(f.mimeType, f.name)}
            {@const isSelected = selectedFileIds.has(f.id)}
            {@const category = getFileCategory(f.mimeType, f.name)}
            <div
              role="button"
              tabindex="0"
              ondblclick={() => handleFileDblClick(f)}
              onkeydown={(e) => e.key === 'Enter' && handleFileDblClick(f)}
              oncontextmenu={(e) => openContextMenu(e, 'file', f)}
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
                  <span>{timeAgo(f.updatedAt)}</span>
                </div>
              </div>
            </div>
          {/each}
        </div>
      {:else}
        <!-- Table View (Shadcn compact table) -->
        <div class="bg-[#121214] border border-zinc-800/80 rounded-xl overflow-hidden shadow-xs">
          <table class="w-full text-left text-xs">
            <thead class="bg-zinc-900/80 text-zinc-400 text-[11px] uppercase tracking-wider font-semibold border-b border-zinc-800/80">
              <tr>
                <th class="py-2.5 px-3 w-8">
                  <span class="sr-only">Select</span>
                </th>
                <th class="py-2.5 px-3">Name</th>
                <th class="py-2.5 px-3">Size</th>
                <th class="py-2.5 px-3">Modified</th>
                <th class="py-2.5 px-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-zinc-800/60">
              {#each filteredFiles as f}
                {@const IconComponent = getIcon(f.mimeType, f.name)}
                {@const isSelected = selectedFileIds.has(f.id)}
                <tr
                  ondblclick={() => handleFileDblClick(f)}
                  oncontextmenu={(e) => openContextMenu(e, 'file', f)}
                  class="transition cursor-pointer {isSelected
                    ? 'bg-zinc-850 text-white'
                    : 'hover:bg-zinc-850/60 text-zinc-300'}"
                >
                  <td class="py-2.5 px-3">
                    <button
                      type="button"
                      onclick={(e) => toggleFile(f.id, e)}
                      class="text-zinc-500 hover:text-white transition cursor-pointer"
                    >
                      {#if isSelected}
                        <CheckSquare class="w-3.5 h-3.5 text-white" />
                      {:else}
                        <Square class="w-3.5 h-3.5 text-zinc-500" />
                      {/if}
                    </button>
                  </td>
                  <td class="py-2.5 px-3 flex items-center gap-2.5 min-w-0">
                    <div class="w-5 h-5 rounded flex items-center justify-center bg-zinc-800 shrink-0">
                      <IconComponent class="w-3.5 h-3.5 text-zinc-400" />
                    </div>
                    <span class="font-medium text-zinc-200 truncate max-w-sm group-hover:text-white">{f.name}</span>
                    {#if f.isStarred}
                      <Star class="w-3 h-3 text-amber-400 fill-amber-400 shrink-0" />
                    {/if}
                  </td>
                  <td class="py-2.5 px-3 text-zinc-400 font-mono text-[11px]">{formatBytes(f.size)}</td>
                  <td class="py-2.5 px-3 text-zinc-500 text-[11px]">{timeAgo(f.updatedAt)}</td>
                  <td class="py-2.5 px-3 text-right">
                    <button
                      onclick={(e) => openContextMenu(e, 'file', f)}
                      class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
                    >
                      <MoreVertical class="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    </div>
  {/if}

  <!-- Floating Batch Action Toolbar (When items are selected) -->
  {#if totalSelectedCount > 0}
    <div class="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-[#18181b]/95 border border-zinc-700/80 text-white rounded-xl px-4 py-2 shadow-2xl backdrop-blur-md flex items-center gap-3 animate-in fade-in slide-in-from-bottom-4 duration-150">
      <div class="flex items-center gap-2 border-r border-zinc-700/80 pr-3">
        <span class="w-5 h-5 rounded-full bg-white text-zinc-950 font-bold text-[10px] flex items-center justify-center">
          {totalSelectedCount}
        </span>
        <span class="text-xs font-medium text-zinc-200">Selected</span>
      </div>

      <button
        onclick={handleBatchShare}
        class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-zinc-800 rounded-lg text-xs font-medium text-zinc-200 hover:text-white transition cursor-pointer"
        title="Share selected items"
      >
        <Share2 class="w-3.5 h-3.5" />
        <span>Share</span>
      </button>

      <button
        onclick={handleBatchDelete}
        disabled={isBatchDeleting}
        class="flex items-center gap-1.5 px-2.5 py-1 hover:bg-red-950/60 text-red-400 hover:text-red-300 rounded-lg text-xs font-medium transition cursor-pointer"
        title="Delete selected items"
      >
        <Trash2 class="w-3.5 h-3.5" />
        <span>{isBatchDeleting ? 'Deleting...' : 'Delete'}</span>
      </button>

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

<!-- Context Menu Popover (Shadcn compact style) -->
{#if activeMenu}
  <div
    class="fixed z-50 bg-[#09090b] border border-zinc-800 rounded-xl shadow-2xl py-1 w-48 text-xs font-medium text-zinc-300 animate-in fade-in zoom-in-95 duration-100 select-none divide-y divide-zinc-800/80"
    style="left: {Math.min(window.innerWidth - 200, activeMenu.x)}px; top: {Math.min(window.innerHeight - 250, activeMenu.y)}px"
  >
    <div class="py-0.5">
      {#if activeMenu.type === 'file'}
        <button
          onclick={() => {
            selectionStore.getState().setPreviewFile(activeMenu!.item);
            activeMenu = null;
          }}
          class="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-zinc-800/80 text-left text-zinc-200 hover:text-white cursor-pointer"
        >
          <Eye class="w-3.5 h-3.5 text-zinc-400" />
          Preview
        </button>

        <button
          onclick={() => {
            handleDownload(activeMenu!.item);
            activeMenu = null;
          }}
          class="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-zinc-800/80 text-left text-zinc-200 hover:text-white cursor-pointer"
        >
          <Download class="w-3.5 h-3.5 text-zinc-400" />
          Download
        </button>
      {/if}

      <button
        onclick={() => {
          handleShare(activeMenu!.type, activeMenu!.item);
          activeMenu = null;
        }}
        class="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-zinc-800/80 text-left text-zinc-200 hover:text-white cursor-pointer"
      >
        <Share2 class="w-3.5 h-3.5 text-zinc-400" />
        Share
      </button>

      <button
        onclick={() => {
          handleToggleStar(activeMenu!.type, activeMenu!.item);
          activeMenu = null;
        }}
        class="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-zinc-800/80 text-left text-zinc-200 hover:text-white cursor-pointer"
      >
        <Star class="w-3.5 h-3.5 text-zinc-400" />
        {activeMenu.item.isStarred ? 'Remove Star' : 'Add Star'}
      </button>
    </div>

    <div class="py-0.5">
      <button
        onclick={() => {
          handleDelete(activeMenu!.type, activeMenu!.item);
          activeMenu = null;
        }}
        class="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-red-950/40 text-red-400 hover:text-red-300 text-left cursor-pointer"
      >
        <Trash2 class="w-3.5 h-3.5" />
        Move to Trash
      </button>
    </div>
  </div>
{/if}
