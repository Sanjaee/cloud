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
    Shield,
    ChevronDown,
    Cloud,
    X,
    PanelLeftClose,
  } from 'lucide-svelte';
  import { uploadStore } from '$lib/stores/upload.store';
  import { selectionStore } from '$lib/stores/selection.store';
  import { authStore } from '$lib/stores/auth.store';

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
  let user = $derived(authStore.getState().user);

  let isSidebarOpen = $state(false);
  let isSidebarCollapsed = $state(false);

  selectionStore.subscribe((s) => {
    isSidebarOpen = s.isSidebarOpen;
    isSidebarCollapsed = s.isSidebarCollapsed;
  });

  function handleFileSelect(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const activeFolder = selectionStore.getState().activeFolderId;
      uploadStore.getState().addFiles(input.files, activeFolder);
      input.value = '';
    }
  }

  const mainNavItems = [
    { href: '/drive', label: 'My Drive', icon: HardDrive },
    { href: '/recent', label: 'Recent', icon: Clock },
    { href: '/starred', label: 'Starred', icon: Star },
    { href: '/trash', label: 'Trash', icon: Trash2 },
  ];

  const sharingNavItems = [
    { href: '/shared/with-me', label: 'Shared with me', icon: Users },
    { href: '/shared/by-me', label: 'Shared by me', icon: Share2 },
  ];

  function closeMobileSidebar() {
    selectionStore.getState().setSidebarOpen(false);
  }
</script>

<!-- Hidden File Picker -->
<input
  bind:this={fileInput}
  type="file"
  multiple
  class="hidden"
  onchange={handleFileSelect}
/>

<!-- Mobile Backdrop Overlay -->
{#if isSidebarOpen}
  <div
    role="button"
    tabindex="0"
    onclick={closeMobileSidebar}
    onkeydown={(e) => e.key === 'Escape' && closeMobileSidebar()}
    class="fixed inset-0 bg-black/60 backdrop-blur-xs z-40 lg:hidden transition-opacity duration-200"
  ></div>
{/if}

<!-- Sidebar Container (Responsive: Drawer on Mobile, Collapsible on Desktop) -->
<aside
  class="fixed lg:static top-0 bottom-0 left-0 z-50 flex flex-col h-full bg-[#09090b] text-zinc-200 border-r border-zinc-800/80 transition-all duration-200 select-none {isSidebarOpen
    ? 'translate-x-0 w-64'
    : '-translate-x-full lg:translate-x-0'} {isSidebarCollapsed ? 'lg:w-[68px]' : 'lg:w-60'}"
>
  <!-- Workspace Header (shadcn style Acme Inc header) -->
  <div class="h-14 flex items-center justify-between px-3.5 border-b border-zinc-800/80 shrink-0">
    <a
      href="/drive"
      class="flex items-center gap-2.5 font-semibold text-sm text-white hover:text-zinc-200 transition min-w-0"
    >
      <div class="w-8 h-8 rounded-lg bg-white text-zinc-900 flex items-center justify-center shrink-0 shadow-sm font-bold text-xs">
        <Cloud class="w-4 h-4 fill-zinc-900" />
      </div>
      {#if !isSidebarCollapsed}
        <div class="flex flex-col min-w-0 truncate">
          <span class="text-xs font-bold text-white tracking-tight truncate">REA Storage</span>
          <span class="text-[10px] text-zinc-400 truncate">Enterprise Cloud</span>
        </div>
      {/if}
    </a>

    <!-- Mobile Close Button -->
    <button
      onclick={closeMobileSidebar}
      class="p-1 rounded-md text-zinc-400 hover:text-white hover:bg-zinc-800 lg:hidden cursor-pointer"
    >
      <X class="w-4 h-4" />
    </button>
  </div>

  <!-- Primary Action Buttons (shadcn Quick Create pill style) -->
  <div class="p-3 space-y-1.5 shrink-0">
    <button
      onclick={() => fileInput.click()}
      class="w-full flex items-center justify-center gap-2 px-3 py-2 bg-white text-zinc-900 hover:bg-zinc-100 rounded-lg font-semibold text-xs shadow-sm transition cursor-pointer active:scale-[0.98] {isSidebarCollapsed
        ? 'px-0'
        : ''}"
      title="Upload Files"
    >
      <Plus class="w-4 h-4 stroke-[2.5]" />
      {#if !isSidebarCollapsed}
        <span>Upload Files</span>
      {/if}
    </button>

    {#if onNewFolder && !isSidebarCollapsed}
      <button
        onclick={onNewFolder}
        class="w-full flex items-center justify-center gap-1.5 px-3 py-1.5 bg-zinc-900 hover:bg-zinc-800/80 text-zinc-300 hover:text-white border border-zinc-800 rounded-lg text-xs font-medium transition cursor-pointer"
      >
        <FolderPlus class="w-3.5 h-3.5 text-zinc-400" />
        <span>New Folder</span>
      </button>
    {/if}
  </div>

  <!-- Navigation Groups -->
  <nav class="flex-1 px-2.5 py-1 space-y-4 overflow-y-auto scrollbar-none text-xs">
    <!-- Platform Section -->
    <div>
      {#if !isSidebarCollapsed}
        <p class="px-2 mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-zinc-500">
          Platform
        </p>
      {/if}
      <div class="space-y-0.5">
        {#each mainNavItems as item}
          {@const isActive = page.url.pathname === item.href}
          <a
            href={item.href}
            onclick={closeMobileSidebar}
            class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-lg font-medium transition {isActive
              ? 'bg-zinc-800 text-white font-semibold'
              : 'text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/50'} {isSidebarCollapsed ? 'justify-center px-0' : ''}"
            title={item.label}
          >
            <item.icon class="w-4 h-4 shrink-0 {isActive ? 'text-white' : 'text-zinc-400'}" />
            {#if !isSidebarCollapsed}
              <span class="truncate">{item.label}</span>
            {/if}
          </a>
        {/each}
      </div>
    </div>

    <!-- Sharing Section -->
    <div>
      {#if !isSidebarCollapsed}
        <p class="px-2 mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-zinc-500">
          Sharing
        </p>
      {/if}
      <div class="space-y-0.5">
        {#each sharingNavItems as item}
          {@const isActive = page.url.pathname === item.href}
          <a
            href={item.href}
            onclick={closeMobileSidebar}
            class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-lg font-medium transition {isActive
              ? 'bg-zinc-800 text-white font-semibold'
              : 'text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/50'} {isSidebarCollapsed ? 'justify-center px-0' : ''}"
            title={item.label}
          >
            <item.icon class="w-4 h-4 shrink-0 {isActive ? 'text-white' : 'text-zinc-400'}" />
            {#if !isSidebarCollapsed}
              <span class="truncate">{item.label}</span>
            {/if}
          </a>
        {/each}
      </div>
    </div>

    <!-- Settings Section -->
    <div>
      {#if !isSidebarCollapsed}
        <p class="px-2 mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-zinc-500">
          Settings
        </p>
      {/if}
      <div class="space-y-0.5">
        <a
          href="/settings/security"
          onclick={closeMobileSidebar}
          class="flex items-center gap-2.5 px-2.5 py-1.5 rounded-lg font-medium transition {page.url.pathname.includes('/settings')
            ? 'bg-zinc-800 text-white font-semibold'
            : 'text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/50'} {isSidebarCollapsed ? 'justify-center px-0' : ''}"
          title="Security & Devices"
        >
          <Shield class="w-4 h-4 shrink-0 text-zinc-400" />
          {#if !isSidebarCollapsed}
            <span class="truncate">Security & 2FA</span>
          {/if}
        </a>
      </div>
    </div>
  </nav>

  <!-- Storage Meter (Minimalist shadcn progress) -->
  <div class="p-3 border-t border-zinc-800/80 shrink-0">
    <a
      href="/storage"
      onclick={closeMobileSidebar}
      class="block group p-2 rounded-lg hover:bg-zinc-850/60 transition"
      title="Storage Quota"
    >
      <div class="flex items-center justify-between text-xs font-medium text-zinc-300 mb-1.5">
        <div class="flex items-center gap-1.5">
          <Database class="w-3.5 h-3.5 text-zinc-400 group-hover:text-white transition" />
          {#if !isSidebarCollapsed}
            <span class="text-[11px] font-medium">Storage</span>
          {/if}
        </div>
        {#if !isSidebarCollapsed}
          <span class="text-zinc-400 font-mono text-[10px]">{quota?.percentage ?? 0}%</span>
        {/if}
      </div>

      <div class="w-full h-1.5 bg-zinc-800 rounded-full overflow-hidden mb-1.5">
        <div
          class="h-full bg-white rounded-full transition-all duration-500"
          style="width: {Math.min(100, Math.max(3, quota?.percentage ?? 0))}%"
        ></div>
      </div>

      {#if !isSidebarCollapsed}
        <p class="text-[10px] text-zinc-500 truncate">
          {formatBytes(quota?.usedBytes ?? 0)} of {formatBytes(quota?.quotaBytes ?? 10737418240)}
        </p>
      {/if}
    </a>
  </div>
</aside>
