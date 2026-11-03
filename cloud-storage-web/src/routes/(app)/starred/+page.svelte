<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import UnifiedResourceGrid from '$lib/components/resource-grid/UnifiedResourceGrid.svelte';
  import { Star } from 'lucide-svelte';
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

  function goToFolder(folder: FolderItem) {
    window.location.href = `/drive?folder=${folder.id}`;
  }

  async function handleBatchDelete(fileIds: string[], folderIds: string[]) {
    for (const id of fileIds) await apiRequest(`/files/${id}`, { method: 'DELETE' }).catch(() => {});
    for (const id of folderIds) await apiRequest(`/folders/${id}`, { method: 'DELETE' }).catch(() => {});
    await loadStarred();
  }

  async function handleBatchDownload(files: FileItem[]) {
    for (const file of files) {
      const res = await apiRequest(`/downloads/${file.id}`);
      window.open(res.downloadUrl, '_blank');
    }
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
    <p class="text-xs text-zinc-400 mt-0.5">Akses cepat ke file dan folder penting yang telah Anda beri tanda bintang.</p>
  </div>

  <UnifiedResourceGrid
    files={starredFiles}
    folders={starredFolders}
    context="starred"
    {isLoading}
    emptyTitle="Belum ada item berbintang"
    emptySubtitle="Beri tanda bintang pada file atau folder penting untuk menemukannya dengan cepat di sini."
    onFolderDblClick={goToFolder}
    onFileDblClick={handlePreview}
    onBatchDelete={handleBatchDelete}
    onBatchDownload={handleBatchDownload}
  />
</div>
