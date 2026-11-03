<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { FolderPlus, X } from 'lucide-svelte';

  interface Props {
    isOpen: boolean;
    currentFolderId?: string | null;
    onClose: () => void;
    onCreated: () => void;
  }

  let { isOpen, currentFolderId, onClose, onCreated }: Props = $props();
  let folderName = $state('');
  let isSubmitting = $state(false);
  let error = $state('');

  async function handleSubmit(e: Event) {
    e.preventDefault();
    if (!folderName.trim()) return;
    isSubmitting = true;
    error = '';

    try {
      await apiRequest('/folders', {
        method: 'POST',
        body: JSON.stringify({
          name: folderName.trim(),
          parentId: currentFolderId || undefined,
        }),
      });
      folderName = '';
      onCreated();
      onClose();
    } catch (err: any) {
      error = err.message || 'Failed to create folder';
    } finally {
      isSubmitting = false;
    }
  }
</script>

{#if isOpen}
  <div class="fixed inset-0 bg-slate-950/60 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
    <div class="w-full max-w-sm bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 p-6">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 bg-blue-100 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-xl flex items-center justify-center">
            <FolderPlus class="w-5 h-5" />
          </div>
          <h3 class="font-bold text-base text-slate-900 dark:text-white">New Folder</h3>
        </div>

        <button onclick={onClose} class="p-1 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg cursor-pointer">
          <X class="w-4 h-4" />
        </button>
      </div>

      {#if error}
        <p class="text-xs text-red-600 mb-3">{error}</p>
      {/if}

      <form onsubmit={handleSubmit} class="space-y-4">
        <input
          type="text"
          bind:value={folderName}
          placeholder="Folder title"
          required
          class="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
        />

        <div class="flex items-center justify-end gap-2 pt-2">
          <button
            type="button"
            onclick={onClose}
            class="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
          >
            Cancel
          </button>

          <button
            type="submit"
            disabled={isSubmitting || !folderName.trim()}
            class="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl shadow-md transition disabled:opacity-50 cursor-pointer"
          >
            {isSubmitting ? 'Creating...' : 'Create'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

