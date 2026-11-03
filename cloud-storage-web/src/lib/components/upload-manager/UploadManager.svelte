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
  <div class="fixed bottom-6 right-6 w-96 bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden z-50 animate-in slide-in-from-bottom-5">
    <!-- Drawer Header -->
    <div class="flex items-center justify-between px-4 py-3 bg-slate-900 text-white select-none">
      <div class="flex items-center gap-2 text-sm font-semibold">
        <UploadCloud class="w-4 h-4 text-blue-400" />
        {#if activeCount > 0}
          <span>Uploading {activeCount} {activeCount === 1 ? 'item' : 'items'}...</span>
        {:else}
          <span>{completedCount} uploads completed</span>
        {/if}
      </div>

      <div class="flex items-center gap-1">
        <button
          onclick={() => uploadStore.getState().toggleMinimized()}
          class="p-1 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white transition cursor-pointer"
        >
          {#if isMinimized}
            <ChevronUp class="w-4 h-4" />
          {:else}
            <ChevronDown class="w-4 h-4" />
          {/if}
        </button>

        <button
          onclick={() => uploadStore.getState().toggleOpen()}
          class="p-1 hover:bg-slate-800 rounded-lg text-slate-400 hover:text-white transition cursor-pointer"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Drawer Content (List of uploads) -->
    {#if !isMinimized}
      <div class="max-h-80 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800">
        {#each items as item (item.id)}
          <div class="p-3.5 hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
            <div class="flex items-start justify-between gap-3 mb-1.5">
              <div class="flex items-center gap-2 min-w-0">
                <FileText class="w-4 h-4 text-blue-500 shrink-0" />
                <span class="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">
                  {item.name}
                </span>
              </div>

              <!-- Action buttons -->
              <div class="flex items-center gap-1 shrink-0">
                {#if item.status === 'completed'}
                  <CheckCircle class="w-4 h-4 text-emerald-500" />
                {:else if item.status === 'failed'}
                  <button
                    onclick={() => uploadStore.getState().retryUpload(item.id)}
                    class="p-1 hover:bg-slate-200 dark:hover:bg-slate-700 rounded text-slate-500 cursor-pointer"
                    title="Retry"
                  >
                    <RefreshCw class="w-3.5 h-3.5 text-blue-600" />
                  </button>
                  <AlertCircle class="w-4 h-4 text-red-500" />
                {:else}
                  <button
                    onclick={() => uploadStore.getState().cancelUpload(item.id)}
                    class="p-1 hover:bg-slate-200 dark:hover:bg-slate-700 rounded text-slate-400 hover:text-red-500 transition cursor-pointer"
                    title="Cancel"
                  >
                    <X class="w-3.5 h-3.5" />
                  </button>
                {/if}
              </div>
            </div>

            <!-- Progress bar -->
            <div class="w-full h-1.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden mb-1.5">
              <div
                class="h-full rounded-full transition-all duration-300 {item.status === 'completed'
                  ? 'bg-emerald-500'
                  : item.status === 'failed'
                    ? 'bg-red-500'
                    : 'bg-blue-600'}"
                style="width: {item.progress}%"
              ></div>
            </div>

            <!-- Stats line -->
            <div class="flex items-center justify-between text-[11px] text-slate-400">
              <span>{formatBytes(item.size)}</span>
              <span>
                {#if item.status === 'completed'}
                  Done
                {:else if item.status === 'verifying'}
                  Verifying storage...
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

      <div class="p-2.5 bg-slate-50 dark:bg-slate-900 border-t border-slate-100 dark:border-slate-800 flex justify-end">
        <button
          onclick={() => uploadStore.getState().clearCompleted()}
          class="text-[11px] font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 cursor-pointer"
        >
          Clear completed
        </button>
      </div>
    {/if}
  </div>
{/if}

