<script lang="ts">
  import { uploadStore, type UploadItem } from '$lib/stores/upload.store';
  import { formatBytes } from '$lib/utils/file-size';
  import {
    ChevronUp,
    ChevronDown,
    X,
    CheckCircle,
    AlertCircle,
    RefreshCw,
    UploadCloud,
    FileText,
  } from 'lucide-svelte';

  let items = $state<UploadItem[]>([]);
  let isOpen = $state(false);
  let isMinimized = $state(false);

  uploadStore.subscribe((s) => {
    items = s.items;
    isOpen = s.isOpen;
    isMinimized = s.isMinimized;
  });

  let activeCount = $derived(
    items.filter((i) => i.status === 'uploading' || i.status === 'compressing' || i.status === 'queued').length
  );
  let completedCount = $derived(items.filter((i) => i.status === 'completed').length);
</script>

{#if isOpen && items.length > 0}
  <div class="fixed bottom-5 right-5 w-80 sm:w-96 bg-[#121214] rounded-xl shadow-2xl border border-zinc-800/80 overflow-hidden z-50 animate-in slide-in-from-bottom-5 duration-150">
    <!-- Header -->
    <div class="flex items-center justify-between px-3.5 py-2.5 bg-zinc-900/90 border-b border-zinc-800/80 text-white select-none">
      <div class="flex items-center gap-2 text-xs font-semibold">
        <UploadCloud class="w-4 h-4 text-zinc-300" />
        {#if activeCount > 0}
          <span>Uploading {activeCount} {activeCount === 1 ? 'file' : 'files'}...</span>
        {:else}
          <span>{completedCount} uploads completed</span>
        {/if}
      </div>

      <div class="flex items-center gap-1">
        <button
          onclick={() => uploadStore.getState().toggleMinimized()}
          class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
        >
          {#if isMinimized}
            <ChevronUp class="w-3.5 h-3.5" />
          {:else}
            <ChevronDown class="w-3.5 h-3.5" />
          {/if}
        </button>

        <button
          onclick={() => uploadStore.getState().toggleOpen()}
          class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white transition cursor-pointer"
        >
          <X class="w-3.5 h-3.5" />
        </button>
      </div>
    </div>

    <!-- Content -->
    {#if !isMinimized}
      <div class="max-h-72 overflow-y-auto divide-y divide-zinc-800/60">
        {#each items as item (item.id)}
          <div class="p-3 hover:bg-zinc-850/40 transition">
            <div class="flex items-start justify-between gap-2.5 mb-1.5">
              <div class="flex items-center gap-2 min-w-0">
                <FileText class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                <span class="text-xs font-medium text-zinc-200 truncate">
                  {item.name}
                </span>
              </div>

              <div class="flex items-center gap-1 shrink-0">
                {#if item.status === 'completed'}
                  <CheckCircle class="w-3.5 h-3.5 text-emerald-400" />
                {:else if item.status === 'failed'}
                  <button
                    onclick={() => uploadStore.getState().retryUpload(item.id)}
                    class="p-1 hover:bg-zinc-800 rounded text-zinc-400 hover:text-white cursor-pointer"
                    title="Retry"
                  >
                    <RefreshCw class="w-3 h-3 text-zinc-300" />
                  </button>
                  <AlertCircle class="w-3.5 h-3.5 text-red-400" />
                {:else}
                  <button
                    onclick={() => uploadStore.getState().cancelUpload(item.id)}
                    class="p-1 hover:bg-zinc-800 rounded text-zinc-500 hover:text-red-400 transition cursor-pointer"
                    title="Cancel"
                  >
                    <X class="w-3 h-3" />
                  </button>
                {/if}
              </div>
            </div>

            <!-- Progress bar -->
            <div class="w-full h-1 bg-zinc-800 rounded-full overflow-hidden mb-1.5">
              <div
                class="h-full rounded-full transition-all duration-300 {item.status === 'completed'
                  ? 'bg-emerald-400'
                  : item.status === 'failed'
                    ? 'bg-red-500'
                    : 'bg-white'}"
                style="width: {item.progress}%"
              ></div>
            </div>

            <!-- Stats line -->
            <div class="flex items-center justify-between text-[10px] text-zinc-500 font-mono">
              <span>{formatBytes(item.size)}</span>
              <span>
                {#if item.status === 'completed'}
                  Done
                {:else if item.status === 'verifying'}
                  Verifying...
                {:else if item.status === 'uploading'}
                  {item.progress}% • {formatBytes(item.speedBytesPerSec)}/s
                {:else if item.status === 'failed'}
                  Failed
                {:else}
                  {item.status}
                {/if}
              </span>
            </div>
          </div>
        {/each}
      </div>

      <div class="p-2 bg-zinc-900/60 border-t border-zinc-800/80 flex justify-end">
        <button
          onclick={() => uploadStore.getState().clearCompleted()}
          class="text-[10px] font-medium text-zinc-400 hover:text-white cursor-pointer"
        >
          Clear completed
        </button>
      </div>
    {/if}
  </div>
{/if}
