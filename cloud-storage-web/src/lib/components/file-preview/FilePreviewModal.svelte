<script lang="ts">
  import { selectionStore } from '$lib/stores/selection.store';
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { getFileCategory, isTextPreviewable } from '$lib/utils/file-type';
  import { X, Download, FileText, Share2, Info, AlertCircle, FileSpreadsheet, Printer, ExternalLink, Image } from 'lucide-svelte';
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

        // ONLY fetch text if the file is an actual text/code file (NEVER for binary/xlsx/pdf)
        if (isTextPreviewable(file.mimeType, file.name)) {
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
  <div class="fixed inset-0 bg-black/80 backdrop-blur-xs z-50 flex flex-col justify-between animate-in fade-in duration-150">
    <!-- Top toolbar -->
    <div class="h-13 px-4 sm:px-6 bg-[#09090b] border-b border-zinc-800/80 flex items-center justify-between text-white shrink-0">
      <div class="flex items-center gap-2.5 min-w-0">
        <div class="w-7 h-7 rounded-lg bg-zinc-850 flex items-center justify-center shrink-0">
          <FileText class="w-4 h-4 text-zinc-300" />
        </div>
        <div class="truncate">
          <h3 class="font-medium text-xs text-zinc-100 truncate">{file.name}</h3>
          <p class="text-[10px] text-zinc-400">{formatBytes(file.size)}</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button
          onclick={handleDownload}
          class="flex items-center gap-1.5 px-3 py-1.5 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer"
        >
          <Download class="w-3.5 h-3.5" />
          <span>Download</span>
        </button>

        <button
          onclick={close}
          class="p-1.5 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-white transition cursor-pointer"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Preview canvas -->
    <div class="flex-1 flex items-center justify-center p-4 sm:p-6 overflow-hidden">
      {#if isLoading}
        <div class="flex flex-col items-center gap-3 text-zinc-400">
          <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
          <span class="text-xs font-medium">Generating secure preview...</span>
        </div>
      {:else if errorMsg}
        <div class="text-center p-6 bg-[#121214] border border-zinc-800 rounded-xl max-w-sm">
          <AlertCircle class="w-10 h-10 text-amber-500 mx-auto mb-3" />
          <p class="text-xs font-semibold text-white">Preview Unavailable</p>
          <p class="text-[11px] text-zinc-400 mt-1 mb-4">{errorMsg}</p>
        </div>
      {:else if previewUrl}
        {@const category = getFileCategory(file.mimeType, file.name)}

        {#if category === 'image'}
          <img
            src={previewUrl}
            alt={file.name}
            class="max-h-[80vh] max-w-[90vw] object-contain rounded-xl shadow-2xl border border-zinc-800"
          />
        {:else if category === 'pdf'}
          <div class="w-full max-w-5xl h-[85vh] flex flex-col bg-[#121214] rounded-xl shadow-2xl overflow-hidden border border-zinc-800/80">
            <!-- PDF control bar -->
            <div class="h-9 px-4 bg-zinc-900 border-b border-zinc-800 flex items-center justify-between text-xs text-zinc-300">
              <span class="font-medium flex items-center gap-2 text-xs">
                <FileText class="w-3.5 h-3.5 text-rose-400" />
                PDF Document Viewer
              </span>
              <button
                onclick={() => window.open(previewUrl, '_blank')}
                class="flex items-center gap-1.5 px-2.5 py-1 bg-zinc-800 hover:bg-zinc-700 rounded-md text-white text-[11px] font-medium transition cursor-pointer"
                title="Open in Full Window"
              >
                <Printer class="w-3 h-3" />
                Print / Full View (Ctrl+P)
              </button>
            </div>
            <iframe
              src={previewUrl}
              title={file.name}
              class="w-full flex-1 bg-white"
            ></iframe>
          </div>
        {:else if category === 'spreadsheet'}
          <div class="text-center p-6 bg-[#121214] border border-zinc-800/80 rounded-xl max-w-sm shadow-2xl">
            <div class="w-12 h-12 bg-emerald-950/40 text-emerald-400 rounded-xl flex items-center justify-center mx-auto mb-3 border border-emerald-900/60">
              <FileSpreadsheet class="w-6 h-6" />
            </div>
            <h4 class="font-semibold text-white text-sm mb-1">{file.name}</h4>
            <p class="text-xs text-zinc-400 mb-5">Spreadsheet Document ({formatBytes(file.size)})</p>
            <div class="flex flex-col gap-2">
              <button
                onclick={handleDownload}
                class="inline-flex items-center justify-center gap-2 px-3 py-2 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer"
              >
                <Download class="w-3.5 h-3.5" />
                Download Spreadsheet
              </button>
              <button
                onclick={() => window.open(`https://docs.google.com/viewer?url=${encodeURIComponent(previewUrl || '')}`, '_blank')}
                class="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 bg-zinc-850 hover:bg-zinc-800 text-zinc-300 rounded-lg text-xs font-medium transition cursor-pointer"
              >
                <ExternalLink class="w-3.5 h-3.5" />
                Open with Google Docs Viewer
              </button>
            </div>
          </div>
        {:else if category === 'document'}
          <div class="text-center p-6 bg-[#121214] border border-zinc-800/80 rounded-xl max-w-sm shadow-2xl">
            <div class="w-12 h-12 bg-zinc-800 text-zinc-300 rounded-xl flex items-center justify-center mx-auto mb-3">
              <FileText class="w-6 h-6" />
            </div>
            <h4 class="font-semibold text-white text-sm mb-1">{file.name}</h4>
            <p class="text-xs text-zinc-400 mb-5">Document ({formatBytes(file.size)})</p>
            <div class="flex flex-col gap-2">
              <button
                onclick={handleDownload}
                class="inline-flex items-center justify-center gap-2 px-3 py-2 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer"
              >
                <Download class="w-3.5 h-3.5" />
                Download Document
              </button>
              <button
                onclick={() => window.open(`https://docs.google.com/viewer?url=${encodeURIComponent(previewUrl || '')}`, '_blank')}
                class="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 bg-zinc-850 hover:bg-zinc-800 text-zinc-300 rounded-lg text-xs font-medium transition cursor-pointer"
              >
                <ExternalLink class="w-3.5 h-3.5" />
                Open with Google Docs Viewer
              </button>
            </div>
          </div>
        {:else if category === 'video'}
          <video controls src={previewUrl} class="max-h-[80vh] max-w-[90vw] rounded-xl shadow-2xl border border-zinc-800">
            <track kind="captions" />
          </video>
        {:else if category === 'audio'}
          <div class="p-6 bg-[#121214] border border-zinc-800/80 rounded-xl shadow-2xl flex flex-col items-center gap-3">
            <FileText class="w-12 h-12 text-zinc-400" />
            <h4 class="font-semibold text-white text-sm">{file.name}</h4>
            <audio controls src={previewUrl} class="w-80"></audio>
          </div>
        {:else if textContent !== null}
          <div class="w-full max-w-4xl h-[75vh] bg-[#121214] border border-zinc-800/80 rounded-xl p-5 overflow-auto text-xs font-mono text-zinc-300 leading-relaxed">
            <pre>{textContent}</pre>
          </div>
        {:else}
          <div class="text-center p-6 bg-[#121214] border border-zinc-800/80 rounded-xl max-w-sm">
            <FileText class="w-12 h-12 text-zinc-500 mx-auto mb-2" />
            <p class="text-xs font-semibold text-white">No inline preview available</p>
            <p class="text-[11px] text-zinc-400 mt-1 mb-4">Download the file to view it on your device.</p>
            <button
              onclick={handleDownload}
              class="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold cursor-pointer"
            >
              <Download class="w-3.5 h-3.5" />
              Download File
            </button>
          </div>
        {/if}
      {/if}
    </div>
  </div>
{/if}
