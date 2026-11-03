<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { formatDate } from '$lib/utils/format-date';
  import { Share2, FileText, Trash2, Users } from 'lucide-svelte';
  import { onMount } from 'svelte';

  let sharedFiles = $state<any[]>([]);
  let sharedFolders = $state<any[]>([]);
  let isLoading = $state(true);

  async function loadSharedByMe() {
    isLoading = true;
    try {
      const res = await apiRequest('/shares/shared-by-me');
      sharedFiles = res.sharedFiles || [];
      sharedFolders = res.sharedFolders || [];
    } catch (err) {
      console.error('Failed to load items shared by me:', err);
    } finally {
      isLoading = false;
    }
  }

  async function handleRevoke(type: 'file' | 'folder', resourceId: string, recipientEmail: string) {
    if (!confirm(`Revoke sharing access for ${recipientEmail}?`)) return;
    try {
      const collabsRes = await apiRequest(`/shares/${type}/${resourceId}/collaborators`);
      const targetCollab = collabsRes.collaborators?.find((c: any) => c.email === recipientEmail);
      if (targetCollab) {
        await apiRequest(`/shares/${type}/${resourceId}/collaborators/${targetCollab.userId}`, {
          method: 'DELETE',
        });
        await loadSharedByMe();
      }
    } catch (err) {
      console.error('Failed to revoke access:', err);
    }
  }

  onMount(() => {
    loadSharedByMe();
  });
</script>

<div class="space-y-6">
  <div>
    <h1 class="text-xl font-bold text-white flex items-center gap-2">
      <Share2 class="w-5 h-5 text-zinc-400" />
      Shared by Me
    </h1>
    <p class="text-xs text-zinc-400 mt-0.5">
      All files and folders you have shared with other collaborators.
    </p>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Loading items shared by you...</p>
    </div>
  {:else if sharedFiles.length === 0 && sharedFolders.length === 0}
    <div class="text-center py-16 bg-[#121214] border border-zinc-800/80 rounded-2xl p-8 max-w-lg mx-auto">
      <Share2 class="w-10 h-10 text-zinc-500 mx-auto mb-3" />
      <h3 class="font-semibold text-white text-sm">You haven't shared any items yet</h3>
      <p class="text-xs text-zinc-400 mt-1">Right-click on any file in My Drive and choose "Share" to collaborate.</p>
    </div>
  {:else}
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl overflow-hidden shadow-xs">
      <table class="w-full text-left text-xs">
        <thead class="bg-zinc-900/80 text-zinc-400 uppercase font-semibold text-[11px] tracking-wider border-b border-zinc-800/80">
          <tr>
            <th class="py-2.5 px-3">Name</th>
            <th class="py-2.5 px-3">Recipient</th>
            <th class="py-2.5 px-3">Role</th>
            <th class="py-2.5 px-3">Date Shared</th>
            <th class="py-2.5 px-3 text-right">Revoke</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-zinc-800/60">
          {#each sharedFiles as f}
            <tr class="hover:bg-zinc-850/60 transition">
              <td class="py-2.5 px-3 font-medium text-zinc-200 flex items-center gap-2">
                <FileText class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                <span class="truncate max-w-xs">{f.fileName}</span>
              </td>
              <td class="py-2.5 px-3 text-zinc-400">
                <span class="font-medium text-zinc-200">{f.recipientUsername}</span>
                <span class="block text-[10px] text-zinc-500">{f.recipientEmail}</span>
              </td>
              <td class="py-2.5 px-3">
                <span class="px-2 py-0.5 bg-zinc-800 text-zinc-300 rounded text-[10px] font-medium capitalize">
                  {f.permission}
                </span>
              </td>
              <td class="py-2.5 px-3 text-zinc-500 text-[11px]">{formatDate(f.createdAt)}</td>
              <td class="py-2.5 px-3 text-right">
                <button
                  onclick={() => handleRevoke('file', f.fileId, f.recipientEmail)}
                  class="p-1 hover:bg-red-950/60 text-zinc-400 hover:text-red-400 rounded transition cursor-pointer"
                  title="Revoke access"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>
