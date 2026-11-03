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
  <div class="fixed inset-0 bg-black/70 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
    <div class="w-full max-w-sm bg-[#121214] rounded-xl shadow-2xl border border-zinc-800/80 p-5">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <div class="w-7 h-7 bg-zinc-800 text-zinc-300 rounded-lg flex items-center justify-center">
            <FolderPlus class="w-4 h-4" />
          </div>
          <h3 class="font-semibold text-sm text-white">New Folder</h3>
        </div>

        <button onclick={onClose} class="p-1 text-zinc-400 hover:text-white rounded-md cursor-pointer">
          <X class="w-4 h-4" />
        </button>
      </div>

      {#if error}
        <p class="text-xs text-red-400 mb-3">{error}</p>
      {/if}

      <form onsubmit={handleSubmit} class="space-y-4">
        <input
          type="text"
          bind:value={folderName}
          placeholder="Folder title"
          required
          class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs focus:outline-none focus:border-zinc-700 text-white placeholder:text-zinc-500"
        />

        <div class="flex items-center justify-end gap-2 pt-1">
          <button
            type="button"
            onclick={onClose}
            class="px-3 py-1.5 text-xs font-medium text-zinc-400 hover:text-white rounded-lg hover:bg-zinc-800 transition cursor-pointer"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting || !folderName.trim()}
            class="px-3.5 py-1.5 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition disabled:opacity-50 cursor-pointer active:scale-95"
          >
            {isSubmitting ? 'Creating...' : 'Create Folder'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}
