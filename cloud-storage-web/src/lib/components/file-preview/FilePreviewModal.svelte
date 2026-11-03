<script lang="ts">
  import { selectionStore } from '$lib/stores/selection.store';
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { getFileCategory } from '$lib/utils/file-type';
  import { X, Download, FileText, Share2, Info, AlertCircle } from 'lucide-svelte';
  import type { FileItem } from '$lib/api/types';

  let file = $state<FileItem | null>(null);
  let previewUrl = $state<string | null>(null);
  let textContent = $state<string | null>(null);
  let isLoading = $state(true);
  let errorMsg = $state<string | null>(null);

  selectionStore.subscribe(async (s) => {
    file = s.previewFile;
    if (file) {
      isLoading = true;
      previewUrl = null;
      textContent = null;
      errorMsg = null;
      try {
        const res = await apiRequest(`/downloads/${file.id}/preview`);
        previewUrl = res.previewUrl;

        // If code or text, fetch text
        const cat = getFileCategory(file.mimeType, file.name);
        if (cat === 'code' || cat === 'document') {
          try {
            const raw = await fetch(res.previewUrl);
            textContent = await raw.text();
          } catch {}
        }
      } catch (err: any) {
        console.error('Failed to load preview:', err);
        errorMsg = err.message || 'File preview is unavailable';
      } finally {
        isLoading = false;
      }
    }
  });

  async function handleDownload() {
    if (!file) return;
    const res = await apiRequest(`/downloads/${file.id}`);
    window.open(res.downloadUrl, '_blank');
  }

  function close() {
    selectionStore.getState().setPreviewFile(null);
  }
</script>

{#if file}
  <div class="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex flex-col justify-between animate-in fade-in duration-200">
    <!-- Top toolbar -->
    <div class="h-16 px-6 bg-slate-900/90 border-b border-slate-800 flex items-center justify-between text-white shrink-0">
      <div class="flex items-center gap-3 min-w-0">
        <FileText class="w-5 h-5 text-blue-400 shrink-0" />
        <div class="truncate">
          <h3 class="font-semibold text-sm truncate">{file.name}</h3>
          <p class="text-xs text-slate-400">{formatBytes(file.size)}</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button
          onclick={handleDownload}
          class="flex items-center gap-2 px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer"
        >
          <Download class="w-4 h-4" />
          Download
        </button>

        <button
          onclick={close}
          class="p-2 hover:bg-slate-800 rounded-xl text-slate-400 hover:text-white transition cursor-pointer"
        >
          <X class="w-5 h-5" />
        </button>
      </div>
    </div>

    <!-- Preview canvas -->
    <div class="flex-1 flex items-center justify-center p-6 overflow-hidden">
      {#if isLoading}
        <div class="flex flex-col items-center gap-3 text-slate-400">
          <div class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          <span class="text-xs font-medium">Generating secure preview...</span>
        </div>
      {:else if errorMsg}
        <div class="text-center p-8 bg-slate-900/60 border border-slate-800 rounded-2xl max-w-md">
          <AlertCircle class="w-12 h-12 text-amber-500 mx-auto mb-3" />
          <p class="text-sm font-semibold text-white">Preview Unavailable</p>
          <p class="text-xs text-slate-400 mt-1 mb-4">{errorMsg}</p>
        </div>
      {:else if previewUrl}
        {@const category = getFileCategory(file.mimeType, file.name)}

        {#if category === 'image'}
          <img
            src={previewUrl}
            alt={file.name}
            class="max-h-[80vh] max-w-[90vw] object-contain rounded-lg shadow-2xl"
          />
        {:else if category === 'pdf'}
          <iframe
            src={previewUrl}
            title={file.name}
            class="w-full max-w-4xl h-[80vh] rounded-xl shadow-2xl bg-white"
          ></iframe>
        {:else if category === 'video'}
          <video controls src={previewUrl} class="max-h-[80vh] max-w-[90vw] rounded-xl shadow-2xl">
            <track kind="captions" />
          </video>
        {:else if category === 'audio'}
          <div class="p-8 bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl flex flex-col items-center gap-4">
            <FileText class="w-16 h-16 text-blue-400" />
            <h4 class="font-bold text-white text-base">{file.name}</h4>
            <audio controls src={previewUrl} class="w-80"></audio>
          </div>
        {:else if textContent !== null}
          <div class="w-full max-w-4xl h-[75vh] bg-slate-900 border border-slate-800 rounded-2xl p-6 overflow-auto text-xs font-mono text-slate-200 leading-relaxed">
            <pre>{textContent}</pre>
          </div>
        {:else}
          <div class="text-center p-8 bg-slate-900/60 border border-slate-800 rounded-2xl">
            <FileText class="w-16 h-16 text-slate-500 mx-auto mb-3" />
            <p class="text-sm font-semibold text-white">No inline preview available</p>
            <p class="text-xs text-slate-400 mt-1 mb-4">You can download the file to open it on your device.</p>
            <button
              onclick={handleDownload}
              class="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold cursor-pointer"
            >
              <Download class="w-4 h-4" />
              Download File
            </button>
          </div>
        {/if}
      {/if}
    </div>
  </div>
{/if}

