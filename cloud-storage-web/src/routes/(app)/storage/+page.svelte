<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { Database, HardDrive, Users, CheckCircle, Shield, FileCheck, Cloud } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { QuotaInfo } from '$lib/api/types';

  let quota = $state<QuotaInfo | null>(null);
  let isLoading = $state(true);

  async function loadQuota() {
    isLoading = true;
    try {
      quota = await apiRequest('/quotas');
    } finally {
      isLoading = false;
    }
  }

  onMount(() => {
    loadQuota();
  });
</script>

<div class="space-y-6 max-w-4xl">
  <div>
    <h1 class="text-xl font-bold text-white flex items-center gap-2">
      <Database class="w-5 h-5 text-zinc-400" />
      Storage & Quotas
    </h1>
    <p class="text-xs text-zinc-400 mt-0.5">
      Cloudflare R2 storage usage and account quota limits.
    </p>
  </div>

  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-20 space-y-3">
      <div class="w-7 h-7 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
      <p class="text-xs text-zinc-400 font-medium">Calculating storage usage...</p>
    </div>
  {:else if quota}
    <!-- Main Storage Card -->
    <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-6 shadow-xs">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
        <div>
          <span class="text-[11px] font-semibold uppercase tracking-wider text-zinc-500">Total Cloud Usage</span>
          <div class="flex items-baseline gap-2 mt-1">
            <h2 class="text-2xl font-bold text-white font-mono">{quota.formattedUsed}</h2>
            <span class="text-xs text-zinc-400">of {quota.formattedQuota}</span>
          </div>
        </div>

        <div class="flex items-center gap-1.5 px-3 py-1 bg-zinc-800/80 text-zinc-300 rounded-lg text-xs font-medium border border-zinc-700/60 self-start sm:self-auto">
          <Cloud class="w-3.5 h-3.5 text-zinc-400" />
          <span>10 GB Free Tier</span>
        </div>
      </div>

      <!-- Meter Bar -->
      <div class="w-full h-2 bg-zinc-800 rounded-full overflow-hidden mb-2">
        <div
          class="h-full bg-white rounded-full transition-all duration-500"
          style="width: {Math.min(100, Math.max(2, quota.percentage))}%"
        ></div>
      </div>

      <div class="flex justify-between text-xs text-zinc-400 font-medium">
        <span>{quota.percentage}% used</span>
        <span>{quota.formattedAvailable} available</span>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
      <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-4 shadow-xs flex items-center gap-3">
        <div class="w-9 h-9 rounded-lg bg-zinc-800 text-zinc-300 flex items-center justify-center shrink-0">
          <HardDrive class="w-4 h-4" />
        </div>
        <div>
          <span class="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">R2 Objects</span>
          <p class="text-lg font-bold text-white font-mono">{quota.physicalObjectCount}</p>
        </div>
      </div>

      <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-4 shadow-xs flex items-center gap-3">
        <div class="w-9 h-9 rounded-lg bg-zinc-800 text-zinc-300 flex items-center justify-center shrink-0">
          <FileCheck class="w-4 h-4" />
        </div>
        <div>
          <span class="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">File Records</span>
          <p class="text-lg font-bold text-white font-mono">{quota.fileCount}</p>
        </div>
      </div>

      <div class="bg-[#121214] border border-zinc-800/80 rounded-xl p-4 shadow-xs flex items-center gap-3">
        <div class="w-9 h-9 rounded-lg bg-zinc-800 text-emerald-400 flex items-center justify-center shrink-0">
          <Shield class="w-4 h-4" />
        </div>
        <div>
          <span class="text-[10px] font-semibold uppercase tracking-wider text-zinc-500">Quota Status</span>
          <p class="text-xs font-semibold text-emerald-400 mt-0.5">Healthy & Active</p>
        </div>
      </div>
    </div>
  {/if}
</div>
