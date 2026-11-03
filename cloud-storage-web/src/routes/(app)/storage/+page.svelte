<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { formatBytes } from '$lib/utils/file-size';
  import { Database, HardDrive, Users, CheckCircle, Shield, FileCheck } from 'lucide-svelte';
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

<div class="space-y-8 max-w-4xl">
  <div>
    <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
      <Database class="w-7 h-7 text-blue-600" />
      Storage & Quotas
    </h1>
    <p class="text-xs text-slate-500 mt-1">
      Centralized Cloudflare R2 object storage usage and accounting breakdown.
    </p>
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if quota}
    <!-- Main Storage Card -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8 shadow-xs">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-6">
        <div>
          <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Total Cloud Usage</span>
          <div class="flex items-baseline gap-2 mt-1">
            <h2 class="text-3xl font-extrabold text-slate-900 dark:text-white font-mono">{quota.formattedUsed}</h2>
            <span class="text-sm font-semibold text-slate-500">of {quota.formattedQuota}</span>
          </div>
        </div>

        <div class="flex items-center gap-2 px-4 py-2 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-2xl text-xs font-bold border border-blue-200 dark:border-blue-900 self-start md:self-auto">
          <Shield class="w-4 h-4" />
          <span>Active Plan: 10 GB Included</span>
        </div>
      </div>

      <!-- Meter Bar -->
      <div class="w-full h-4 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden p-0.5 mb-3">
        <div
          class="h-full bg-gradient-to-r from-blue-600 to-indigo-600 rounded-full transition-all duration-700"
          style="width: {Math.min(100, Math.max(2, quota.percentage))}%"
        ></div>
      </div>

      <div class="flex justify-between text-xs text-slate-500 font-medium">
        <span>{quota.percentage}% used</span>
        <span>{quota.formattedAvailable} available</span>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs flex items-center gap-4">
        <div class="w-12 h-12 rounded-2xl bg-blue-100 dark:bg-blue-950/60 text-blue-600 flex items-center justify-center shrink-0">
          <HardDrive class="w-6 h-6" />
        </div>
        <div>
          <span class="text-[11px] font-bold uppercase text-slate-400">Physical Objects in R2</span>
          <p class="text-xl font-bold text-slate-900 dark:text-white font-mono">{quota.physicalObjectCount}</p>
        </div>
      </div>

      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs flex items-center gap-4">
        <div class="w-12 h-12 rounded-2xl bg-indigo-100 dark:bg-indigo-950/60 text-indigo-600 flex items-center justify-center shrink-0">
          <FileCheck class="w-6 h-6" />
        </div>
        <div>
          <span class="text-[11px] font-bold uppercase text-slate-400">Logical Files Owned</span>
          <p class="text-xl font-bold text-slate-900 dark:text-white font-mono">{quota.fileCount}</p>
        </div>
      </div>

      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-xs flex items-center gap-4">
        <div class="w-12 h-12 rounded-2xl bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 flex items-center justify-center shrink-0">
          <Users class="w-6 h-6" />
        </div>
        <div>
          <span class="text-[11px] font-bold uppercase text-slate-400">Shared References (0 bytes)</span>
          <p class="text-xl font-bold text-slate-900 dark:text-white font-mono">{quota.sharedFileCount}</p>
        </div>
      </div>
    </div>

    <!-- Storage Policy Callout -->
    <div class="p-6 bg-slate-100/70 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 rounded-3xl text-xs space-y-2 text-slate-600 dark:text-slate-400 leading-relaxed">
      <h4 class="font-bold text-slate-900 dark:text-slate-100 text-sm flex items-center gap-2">
        <CheckCircle class="w-4 h-4 text-emerald-500" />
        How Storage Accounting Works
      </h4>
      <p>
        • <strong class="text-slate-800 dark:text-slate-200">Owner Pays:</strong> Storage quota is only consumed by the original owner of a physical file stored in Cloudflare R2.
      </p>
      <p>
        • <strong class="text-slate-800 dark:text-slate-200">Zero Duplication Sharing:</strong> When a file is shared with you or when you add a shortcut to My Drive, it consumes <span class="font-bold text-emerald-600">0 bytes</span> of your personal storage quota.
      </p>
      <p>
        • <strong class="text-slate-800 dark:text-slate-200">Safe Deletion:</strong> Removing a shortcut or revoking shared access never deletes the original physical object from R2.
      </p>
    </div>
  {/if}
</div>

