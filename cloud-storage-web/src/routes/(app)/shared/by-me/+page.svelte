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
      // Look up user id from search or collaborators and revoke
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
    <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
      <Share2 class="w-7 h-7 text-blue-600" />
      Shared by me
    </h1>
    <p class="text-xs text-slate-500 mt-1">
      All files and folders you have granted collaborators access to. You can revoke access at any time.
    </p>
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if sharedFiles.length === 0 && sharedFolders.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Share2 class="w-12 h-12 text-slate-400 mx-auto mb-3" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">You haven't shared any resources yet</h3>
      <p class="text-xs text-slate-500 mt-1">Right-click on any file in My Drive and select "Share" to collaborate.</p>
    </div>
  {:else}
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl overflow-hidden shadow-xs">
      <table class="w-full text-left text-xs">
        <thead class="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase font-semibold tracking-wider border-b border-slate-200 dark:border-slate-800">
          <tr>
            <th class="py-3 px-4">Resource</th>
            <th class="py-3 px-4">Shared With</th>
            <th class="py-3 px-4">Permission</th>
            <th class="py-3 px-4">Size</th>
            <th class="py-3 px-4">Shared On</th>
            <th class="py-3 px-4 text-right">Revoke</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
          {#each sharedFiles as f}
            <tr class="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
              <td class="py-3 px-4 font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2.5">
                <FileText class="w-4 h-4 text-blue-500 shrink-0" />
                <span class="truncate max-w-xs">{f.fileName}</span>
              </td>
              <td class="py-3 px-4 text-slate-600 dark:text-slate-400">
                <span class="font-medium text-slate-900 dark:text-slate-200">{f.grantedToUsername}</span>
                <span class="block text-[11px] text-slate-400">{f.grantedToEmail}</span>
              </td>
              <td class="py-3 px-4">
                <span class="px-2.5 py-1 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-lg text-[11px] font-semibold capitalize">
                  {f.permission}
                </span>
              </td>
              <td class="py-3 px-4 text-slate-500">{formatBytes(f.size)}</td>
              <td class="py-3 px-4 text-slate-500">{formatDate(f.grantedAt)}</td>
              <td class="py-3 px-4 text-right">
                <button
                  onclick={() => handleRevoke('file', f.fileId, f.grantedToEmail)}
                  class="p-1.5 hover:bg-red-50 dark:hover:bg-red-950 text-slate-400 hover:text-red-600 rounded-lg transition cursor-pointer"
                  title="Revoke access"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

