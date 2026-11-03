<script lang="ts">
  import { page } from '$app/state';
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { formatDate } from '$lib/utils/format-date';
  import { Cloud, Download, Lock, FileText, Folder, AlertCircle, ArrowRight } from 'lucide-svelte';
  import { onMount } from 'svelte';

  let token = $derived(page.params.token);
  let password = $state('');
  let passwordRequired = $state(false);
  let resourceData = $state<any>(null);
  let error = $state('');
  let isLoading = $state(true);

  async function accessLink(pwd?: string) {
    isLoading = true;
    error = '';
    try {
      const res = await apiRequest(`/public-links/access/${token}`, {
        method: 'POST',
        body: JSON.stringify({ password: pwd || undefined }),
      });

      if (res.passwordRequired) {
        passwordRequired = true;
        isLoading = false;
        return;
      }

      resourceData = res;
      passwordRequired = false;
    } catch (err: any) {
      error = err.message || 'Unable to access shared resource';
    } finally {
      isLoading = false;
    }
  }

  function handlePasswordSubmit(e: Event) {
    e.preventDefault();
    accessLink(password);
  }

  onMount(() => {
    accessLink();
  });
</script>

<div class="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col items-center justify-center p-6">
  <!-- Header -->
  <div class="flex items-center gap-2.5 mb-8">
    <div class="w-10 h-10 bg-blue-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-blue-500/25">
      <Cloud class="w-6 h-6" />
    </div>
    <span class="font-bold text-xl text-slate-900 dark:text-white tracking-tight">REA Cloud Storage</span>
  </div>

  <div class="w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-slate-200 dark:border-slate-800 p-8">
    {#if isLoading}
      <div class="flex flex-col items-center justify-center py-12 gap-3">
        <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
        <p class="text-xs text-slate-500">Checking link authorization...</p>
      </div>
    {:else if error}
      <div class="text-center py-6">
        <div class="w-12 h-12 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-3">
          <AlertCircle class="w-6 h-6" />
        </div>
        <h3 class="font-bold text-slate-900 dark:text-white text-base">Unable to Open Link</h3>
        <p class="text-xs text-slate-500 mt-1 mb-6 leading-relaxed">{error}</p>
        <a href="/login" class="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl text-xs font-semibold">
          Sign In to REA Storage
        </a>
      </div>
    {:else if passwordRequired}
      <!-- Password Protected Form -->
      <div class="text-center mb-6">
        <div class="w-12 h-12 rounded-2xl bg-blue-100 text-blue-600 flex items-center justify-center mx-auto mb-3">
          <Lock class="w-6 h-6" />
        </div>
        <h3 class="font-bold text-slate-900 dark:text-white text-base">This link is password-protected</h3>
        <p class="text-xs text-slate-500 mt-1">Please enter the password set by the owner to proceed.</p>
      </div>

      <form onsubmit={handlePasswordSubmit} class="space-y-4">
        <div>
          <input
            type="password"
            bind:value={password}
            required
            placeholder="Enter link password"
            class="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white text-center"
          />
        </div>

        <button
          type="submit"
          class="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
        >
          Unlock Resource
          <ArrowRight class="w-4 h-4" />
        </button>
      </form>
    {:else if resourceData}
      <!-- Authorized Resource View -->
      <div class="space-y-6">
        <div class="flex items-center gap-4">
          <div class="w-14 h-14 rounded-2xl bg-blue-50 dark:bg-blue-950 text-blue-600 flex items-center justify-center shrink-0">
            {#if resourceData.resourceType === 'file'}
              <FileText class="w-7 h-7" />
            {:else}
              <Folder class="w-7 h-7 fill-current" />
            {/if}
          </div>

          <div class="truncate">
            <h2 class="font-bold text-lg text-slate-900 dark:text-white truncate">
              {resourceData.resourceType === 'file' ? resourceData.file.name : resourceData.folder.name}
            </h2>
            <p class="text-xs text-slate-500 mt-0.5">
              {#if resourceData.resourceType === 'file'}
                {formatBytes(resourceData.file.size)} • Uploaded {formatDate(resourceData.file.createdAt)}
              {:else}
                Folder containing {resourceData.files?.length || 0} files
              {/if}
            </p>
          </div>
        </div>

        {#if resourceData.resourceType === 'file' && resourceData.allowDownload && resourceData.downloadUrl}
          <a
            href={resourceData.downloadUrl}
            target="_blank"
            rel="noopener noreferrer"
            class="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl text-sm font-semibold shadow-lg shadow-blue-500/25 transition flex items-center justify-center gap-2 cursor-pointer"
          >
            <Download class="w-4 h-4" />
            Download Original File from R2
          </a>
        {/if}

        {#if resourceData.resourceType === 'folder'}
          <div class="border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden divide-y divide-slate-100 dark:divide-slate-800 max-h-60 overflow-y-auto">
            {#each resourceData.files || [] as f}
              <div class="p-3 flex items-center justify-between text-xs">
                <span class="font-semibold text-slate-900 dark:text-white truncate">{f.name}</span>
                <span class="text-slate-500">{formatBytes(f.size)}</span>
              </div>
            {/each}
          </div>
        {/if}
      </div>
    {/if}
  </div>
</div>

