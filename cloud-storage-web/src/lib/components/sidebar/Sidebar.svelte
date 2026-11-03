<script lang="ts">
  import { page } from '$app/state';
  import { formatBytes } from '$lib/utils/file-size';
  import {
    HardDrive,
    Users,
    Share2,
    Clock,
    Star,
    Trash2,
    Database,
    Plus,
    FolderPlus,
    UploadCloud,
    Shield,
  } from 'lucide-svelte';
  import { uploadStore } from '$lib/stores/upload.store';
  import { selectionStore } from '$lib/stores/selection.store';

  interface Props {
    quota?: {
      usedBytes: number;
      quotaBytes: number;
      percentage: number;
    } | null;
    onNewFolder?: () => void;
  }

  let { quota, onNewFolder }: Props = $props();

  let fileInput: HTMLInputElement;

  function handleFileSelect(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const activeFolder = selectionStore.getState().activeFolderId;
      uploadStore.getState().addFiles(input.files, activeFolder);
      input.value = '';
    }
  }

  const navItems = [
    { href: '/drive', label: 'My Drive', icon: HardDrive },
    { href: '/shared/with-me', label: 'Shared with me', icon: Users },
    { href: '/shared/by-me', label: 'Shared by me', icon: Share2 },
    { href: '/recent', label: 'Recent', icon: Clock },
    { href: '/starred', label: 'Starred', icon: Star },
    { href: '/trash', label: 'Trash', icon: Trash2 },
  ];
</script>

<input
  bind:this={fileInput}
  type="file"
  multiple
  class="hidden"
  onchange={handleFileSelect}
/>

<aside class="w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col h-full shrink-0 select-none">
  <!-- New Action Button Dropdown -->
  <div class="p-4">
    <div class="relative group">
      <button
        onclick={() => fileInput.click()}
        class="w-full flex items-center justify-center gap-2.5 px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-semibold text-sm shadow-lg shadow-blue-500/25 transition cursor-pointer active:scale-[0.98]"
      >
        <Plus class="w-5 h-5 stroke-[2.5]" />
        <span>Upload Files</span>
      </button>

      {#if onNewFolder}
        <button
          onclick={onNewFolder}
          class="w-full mt-2 flex items-center justify-center gap-2 px-3 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 rounded-xl text-xs font-semibold transition cursor-pointer"
        >
          <FolderPlus class="w-4 h-4" />
          <span>New Folder</span>
        </button>
      {/if}
    </div>
  </div>

  <!-- Navigation Links -->
  <nav class="flex-1 px-3 space-y-1 overflow-y-auto">
    {#each navItems as item}
      {@const isActive = page.url.pathname === item.href}
      <a
        href={item.href}
        class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition {isActive
          ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 font-semibold shadow-xs'
          : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-slate-200'}"
      >
        <item.icon class="w-4 h-4 shrink-0 {isActive ? 'text-blue-600 dark:text-blue-400' : 'text-slate-400'}" />
        <span>{item.label}</span>
      </a>
    {/each}

    <div class="pt-4 mt-4 border-t border-slate-200 dark:border-slate-800">
      <a
        href="/settings/security"
        class="flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/60 transition {page.url.pathname.includes('/settings') ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400' : ''}"
      >
        <Shield class="w-4 h-4 text-slate-400" />
        <span>Security & Devices</span>
      </a>
    </div>
  </nav>

  <!-- Storage Meter -->
  <div class="p-4 border-t border-slate-200 dark:border-slate-800">
    <a href="/storage" class="block group">
      <div class="flex items-center justify-between text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
        <div class="flex items-center gap-1.5">
          <Database class="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
          <span>Storage</span>
        </div>
        <span class="text-slate-500 font-mono text-[11px]">{quota?.percentage ?? 0}%</span>
      </div>

      <div class="w-full h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden mb-2">
        <div
          class="h-full bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full transition-all duration-500"
          style="width: {Math.min(100, Math.max(3, quota?.percentage ?? 0))}%"
        ></div>
      </div>

      <p class="text-[11px] text-slate-500 dark:text-slate-400">
        {formatBytes(quota?.usedBytes ?? 0)} of {formatBytes(quota?.quotaBytes ?? 10737418240)} used
      </p>
    </a>
  </div>
</aside>

