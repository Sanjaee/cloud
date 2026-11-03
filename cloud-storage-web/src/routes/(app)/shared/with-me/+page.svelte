<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { selectionStore } from '$lib/stores/selection.store';
  import UnifiedResourceGrid from '$lib/components/resource-grid/UnifiedResourceGrid.svelte';
  import { Users } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { FileItem, FolderItem } from '$lib/api/types';

  let sharedFiles = $state<any[]>([]);
  let sharedFolders = $state<any[]>([]);
  let isLoading = $state(true);

  async function loadShared() {
    isLoading = true;
    try {
      const res = await apiRequest('/shares/shared-with-me');
      sharedFiles = res.files || [];
      sharedFolders = res.folders || [];
    } catch (err) {
      console.error('Failed to load shared items:', err);
    } finally {
      isLoading = false;
    }
  }

  function handlePreview(file: any) {
    selectionStore.getState().setPreviewFile(file);
  }

  function goToFolder(folder: FolderItem) {
    window.location.href = `/drive?folder=${folder.id}`;
  }

  async function handleBatchDownload(files: FileItem[]) {
    for (const file of files) {
      const res = await apiRequest(`/downloads/${file.id}`);
      window.open(res.downloadUrl, '_blank');
    }
  }

  onMount(() => {
    loadShared();
  });
</script>

<div class="space-y-6">
  <div>
    <h1 class="text-xl font-bold text-white flex items-center gap-2">
      <Users class="w-5 h-5 text-zinc-400" />
      Shared with Me
    </h1>
    <p class="text-xs text-zinc-400 mt-0.5">
      File dan folder yang dibagikan oleh pengguna lain ke akun Anda.
    </p>
  </div>

  <UnifiedResourceGrid
    files={sharedFiles}
    folders={sharedFolders}
    context="shared-with-me"
    {isLoading}
    emptyTitle="Belum ada item yang dibagikan"
    emptySubtitle="Ketika seseorang membagikan file atau folder kepada Anda, item tersebut akan muncul di sini."
    onFolderDblClick={goToFolder}
    onFileDblClick={handlePreview}
    onBatchDownload={handleBatchDownload}
  />
</div>
