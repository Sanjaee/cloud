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
    CornerUpLeft,
  } from 'lucide-svelte';
  import { onMount, onDestroy } from 'svelte';
  import type { FileItem, FolderItem, BreadcrumbItem } from '$lib/api/types';

  let currentFolderId = $state<string | null>(null);
  let folders = $state<FolderItem[]>([]);
  let files = $state<FileItem[]>([]);
  let breadcrumbs = $state<BreadcrumbItem[]>([]);
  let isLoading = $state(true);
  let isDragOver = $state(false);
  let viewMode = $state('grid');

  // Context Menu State
  let activeMenu = $state<{
    type: 'file' | 'folder';
    item: any;
    x: number;
    y: number;
  } | null>(null);

  selectionStore.subscribe((s) => {
    viewMode = s.viewMode;
  });

  async function loadData(folderId: string | null = null) {
    currentFolderId = folderId;
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

  // Drag and Drop Upload
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
    loadData();
    window.addEventListener('click', onGlobalClick);
    window.addEventListener('folder-created', () => loadData(currentFolderId));
    window.addEventListener('file-uploaded', () => loadData(currentFolderId));
  });

  onDestroy(() => {
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
  class="relative min-h-full flex flex-col {isDragOver ? 'ring-4 ring-blue-500/40 rounded-3xl bg-blue-50/20' : ''}"
>
  <!-- Breadcrumbs Bar -->
  <div class="flex items-center gap-2 mb-6 text-sm">
    {#each breadcrumbs as bc, i}
      <button
        onclick={() => loadData(bc.id)}
        class="flex items-center gap-1.5 font-medium hover:text-blue-600 transition cursor-pointer {i === breadcrumbs.length - 1
          ? 'font-bold text-slate-900 dark:text-white'
          : 'text-slate-500 dark:text-slate-400'}"
      >
        <span>{bc.name}</span>
      </button>
      {#if i < breadcrumbs.length - 1}
        <ChevronRight class="w-4 h-4 text-slate-400" />
      {/if}
    {/each}
  </div>

  {#if isLoading}
    <div class="flex-1 flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else}
    <!-- Folders Section -->
    {#if folders.length > 0}
      <div class="mb-8">
        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-3">
          Folders ({folders.length})
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {#each folders as fol}
            <div
              role="button"
              tabindex="0"
              ondblclick={() => handleFolderDblClick(fol)}
              onkeydown={(e) => e.key === 'Enter' && handleFolderDblClick(fol)}
              oncontextmenu={(e) => openContextMenu(e, 'folder', fol)}
              class="group relative flex items-center gap-3 p-3 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-blue-400 dark:hover:border-blue-600 rounded-2xl shadow-xs hover:shadow-md transition cursor-pointer select-none"
            >
              <div class="w-9 h-9 rounded-xl flex items-center justify-center bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 shrink-0">
                <Folder class="w-5 h-5 fill-current" />
              </div>

              <span class="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate flex-1">
                {fol.name}
              </span>

              {#if fol.isStarred}
                <Star class="w-3.5 h-3.5 text-amber-500 fill-amber-500 shrink-0" />
              {/if}

              <button
                onclick={(e) => openContextMenu(e, 'folder', fol)}
                class="opacity-0 group-hover:opacity-100 p-1 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-400 transition"
              >
                <MoreVertical class="w-3.5 h-3.5" />
              </button>
            </div>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Files Section -->
    <div>
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          Files ({files.length})
        </h3>
      </div>

      {#if files.length === 0 && folders.length === 0}
        <div class="text-center py-20 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-3xl p-8">
          <UploadCloud class="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h4 class="font-bold text-slate-800 dark:text-slate-200 text-sm">Drop files here to upload</h4>
          <p class="text-xs text-slate-500 mt-1 mb-4">
            Upload directly to Cloudflare R2 object storage with fast presigned transfers.
          </p>
        </div>
      {:else if viewMode === 'grid'}
        <!-- Grid View -->
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {#each files as f}
            {@const IconComponent = getIcon(f.mimeType, f.name)}
            <div
              role="button"
              tabindex="0"
              ondblclick={() => handleFileDblClick(f)}
              onkeydown={(e) => e.key === 'Enter' && handleFileDblClick(f)}
              oncontextmenu={(e) => openContextMenu(e, 'file', f)}
              class="group relative flex flex-col bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:border-blue-400 dark:hover:border-blue-600 rounded-2xl shadow-xs hover:shadow-md transition overflow-hidden cursor-pointer select-none"
            >
              <!-- Thumbnail / Icon Area -->
              <div class="h-32 bg-slate-50 dark:bg-slate-800/40 flex items-center justify-center">
                <IconComponent class="w-12 h-12 text-blue-500/80 stroke-1 group-hover:scale-105 transition" />
              </div>

              <!-- File Info -->
              <div class="p-3">
                <div class="flex items-start justify-between gap-1 mb-1">
                  <span class="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate flex-1">
                    {f.name}
                  </span>
                  {#if f.isStarred}
                    <Star class="w-3.5 h-3.5 text-amber-500 fill-amber-500 shrink-0" />
                  {/if}
                </div>

                <div class="flex items-center justify-between text-[11px] text-slate-400">
                  <span>{formatBytes(f.size)}</span>
                  <span>{timeAgo(f.updatedAt)}</span>
                </div>
              </div>
            </div>
          {/each}
        </div>
      {:else}
        <!-- Table View -->
        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-xs">
          <table class="w-full text-left text-xs">
            <thead class="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase font-semibold tracking-wider border-b border-slate-200 dark:border-slate-800">
              <tr>
                <th class="py-3 px-4">Name</th>
                <th class="py-3 px-4">Size</th>
                <th class="py-3 px-4">Last Modified</th>
                <th class="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
              {#each files as f}
                {@const IconComponent = getIcon(f.mimeType, f.name)}
                <tr
                  ondblclick={() => handleFileDblClick(f)}
                  oncontextmenu={(e) => openContextMenu(e, 'file', f)}
                  class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition cursor-pointer"
                >
                  <td class="py-3 px-4 flex items-center gap-3">
                    <IconComponent class="w-4 h-4 text-blue-500 shrink-0" />
                    <span class="font-semibold text-slate-900 dark:text-slate-100 truncate max-w-xs">{f.name}</span>
                    {#if f.isStarred}
                      <Star class="w-3 h-3 text-amber-500 fill-amber-500" />
                    {/if}
                  </td>
                  <td class="py-3 px-4 text-slate-500">{formatBytes(f.size)}</td>
                  <td class="py-3 px-4 text-slate-500">{timeAgo(f.updatedAt)}</td>
                  <td class="py-3 px-4 text-right">
                    <button
                      onclick={(e) => openContextMenu(e, 'file', f)}
                      class="p-1 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-lg text-slate-400 hover:text-slate-700 dark:hover:text-white"
                    >
                      <MoreVertical class="w-4 h-4" />
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
</div>

<!-- Context Menu Popover -->
{#if activeMenu}
  <div
    class="fixed z-50 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl py-1.5 w-48 text-xs font-medium text-slate-700 dark:text-slate-300 animate-in fade-in zoom-in-95 duration-100"
    style="left: {Math.min(window.innerWidth - 200, activeMenu.x)}px; top: {Math.min(window.innerHeight - 250, activeMenu.y)}px"
  >
    {#if activeMenu.type === 'file'}
      <button
        onclick={() => {
          selectionStore.getState().setPreviewFile(activeMenu!.item);
          activeMenu = null;
        }}
        class="w-full flex items-center gap-2.5 px-3.5 py-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-left cursor-pointer"
      >
        <Eye class="w-4 h-4 text-slate-400" />
        Preview
      </button>

      <button
        onclick={() => {
          handleDownload(activeMenu!.item);
          activeMenu = null;
        }}
        class="w-full flex items-center gap-2.5 px-3.5 py-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-left cursor-pointer"
      >
        <Download class="w-4 h-4 text-slate-400" />
        Download
      </button>
    {/if}

    <button
      onclick={() => {
        handleShare(activeMenu!.type, activeMenu!.item);
        activeMenu = null;
      }}
      class="w-full flex items-center gap-2.5 px-3.5 py-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-left cursor-pointer"
    >
      <Share2 class="w-4 h-4 text-slate-400" />
      Share
    </button>

    <button
      onclick={() => {
        handleToggleStar(activeMenu!.type, activeMenu!.item);
        activeMenu = null;
      }}
      class="w-full flex items-center gap-2.5 px-3.5 py-2 hover:bg-slate-100 dark:hover:bg-slate-800 text-left cursor-pointer"
    >
      <Star class="w-4 h-4 text-slate-400" />
      {activeMenu.item.isStarred ? 'Remove from Starred' : 'Add to Starred'}
    </button>

    <div class="my-1 border-t border-slate-100 dark:border-slate-800"></div>

    <button
      onclick={() => {
        handleDelete(activeMenu!.type, activeMenu!.item);
        activeMenu = null;
      }}
      class="w-full flex items-center gap-2.5 px-3.5 py-2 hover:bg-red-50 dark:hover:bg-red-950/40 text-red-600 text-left cursor-pointer"
    >
      <Trash2 class="w-4 h-4" />
      Move to Trash
    </button>
  </div>
{/if}

