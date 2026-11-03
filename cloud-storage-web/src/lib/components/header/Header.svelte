<script lang="ts">
  import { authStore } from '$lib/stores/auth.store';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import {
    Search,
    LayoutGrid,
    List,
    Bell,
    LogOut,
    Shield,
    CheckCheck,
    Folder,
    FileText,
    ArrowUpRight,
    X,
    PanelLeft,
    ChevronRight,
  } from 'lucide-svelte';
  import { apiRequest } from '$lib/api/client';
  import { onMount } from 'svelte';
  import type { NotificationItem, FileItem, FolderItem } from '$lib/api/types';

  let user = $derived(authStore.getState().user);
  let viewMode = $state('grid');
  let notificationsOpen = $state(false);
  let profileOpen = $state(false);
  let notifications = $state<NotificationItem[]>([]);
  let unreadCount = $state(0);
  let searchQuery = $state('');

  let searchResults = $state<{ files: FileItem[]; folders: FolderItem[] }>({ files: [], folders: [] });
  let isSearching = $state(false);
  let searchDropdownOpen = $state(false);
  let searchDebounceTimer: any = null;

  selectionStore.subscribe((s) => {
    viewMode = s.viewMode;
  });

  function handleToggleSidebar() {
    if (typeof window !== 'undefined' && window.innerWidth < 1024) {
      selectionStore.getState().toggleSidebar();
    } else {
      selectionStore.getState().toggleSidebarCollapse();
    }
  }

  function handleSearchInput(e: Event) {
    clearTimeout(searchDebounceTimer);
    const q = searchQuery.trim();
    if (!q) {
      searchResults = { files: [], folders: [] };
      searchDropdownOpen = false;
      return;
    }

    searchDebounceTimer = setTimeout(async () => {
      isSearching = true;
      try {
        const res = await apiRequest(`/files/search?q=${encodeURIComponent(q)}&limit=6`);
        searchResults = {
          files: res.files || [],
          folders: res.folders || [],
        };
        searchDropdownOpen = true;
      } catch {
        searchResults = { files: [], folders: [] };
      } finally {
        isSearching = false;
      }
    }, 220);
  }

  function handleSearchKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      const q = searchQuery.trim();
      if (q) {
        searchDropdownOpen = false;
        window.location.href = `/search?q=${encodeURIComponent(q)}`;
      }
    } else if (e.key === 'Escape') {
      searchDropdownOpen = false;
    }
  }

  function goToFolder(folderId: string | null) {
    searchDropdownOpen = false;
    if (folderId) {
      window.location.href = `/drive?folder=${folderId}`;
    } else {
      window.location.href = '/drive';
    }
  }

  async function loadNotifications() {
    try {
      const res = await apiRequest('/notifications');
      notifications = res.notifications || [];
      unreadCount = res.unreadCount || 0;
    } catch {}
  }

  async function markAllNotificationsRead() {
    try {
      await apiRequest('/notifications/read-all', { method: 'POST' });
      unreadCount = 0;
      notifications = notifications.map((n) => ({ ...n, readAt: new Date().toISOString() }));
    } catch {}
  }

  function onGlobalClick(e: MouseEvent) {
    const target = e.target as HTMLElement;
    if (!target.closest('.search-container')) {
      searchDropdownOpen = false;
    }
    if (!target.closest('.profile-container')) {
      profileOpen = false;
    }
    if (!target.closest('.notifications-container')) {
      notificationsOpen = false;
    }
  }

  onMount(() => {
    loadNotifications();
    window.addEventListener('click', onGlobalClick);
    return () => {
      window.removeEventListener('click', onGlobalClick);
    };
  });
</script>

<header class="h-13 bg-[#09090b] border-b border-zinc-800/80 px-3 sm:px-4 flex items-center justify-between shrink-0 z-30 select-none">
  <!-- Left Side: Sidebar Trigger & Breadcrumb -->
  <div class="flex items-center gap-2 sm:gap-3 min-w-0">
    <button
      type="button"
      onclick={handleToggleSidebar}
      class="p-1.5 text-zinc-400 hover:text-white hover:bg-zinc-800/80 rounded-md transition cursor-pointer"
      title="Toggle Sidebar"
    >
      <PanelLeft class="w-4 h-4" />
    </button>

    <div class="h-4 w-px bg-zinc-800 shrink-0"></div>

    <div class="flex items-center gap-1.5 text-xs text-zinc-300 font-medium truncate">
      <a href="/drive" class="hover:text-white transition">My Drive</a>
    </div>
  </div>

  <!-- Center/Search: Compact Search Bar -->
  <div class="flex-1 max-w-md mx-3 relative search-container">
    <div class="relative w-full">
      <Search class="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 pointer-events-none" />
      <input
        type="text"
        bind:value={searchQuery}
        oninput={handleSearchInput}
        onkeydown={handleSearchKeyDown}
        onfocus={() => { if (searchQuery.trim()) searchDropdownOpen = true; }}
        placeholder="Search files and folders..."
        class="w-full h-8 pl-9 pr-8 bg-zinc-900/90 hover:bg-zinc-900 focus:bg-zinc-900 border border-zinc-800/80 focus:border-zinc-700 rounded-lg text-xs focus:outline-none text-zinc-200 placeholder:text-zinc-500 transition"
      />
      {#if searchQuery}
        <button
          onclick={() => { searchQuery = ''; searchDropdownOpen = false; }}
          class="absolute right-2.5 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition p-0.5 cursor-pointer"
        >
          <X class="w-3 h-3" />
        </button>
      {/if}
    </div>

    <!-- Live Search Dropdown -->
    {#if searchDropdownOpen && searchQuery.trim()}
      <div class="absolute left-0 right-0 top-full mt-1.5 bg-[#09090b] border border-zinc-800 rounded-xl shadow-2xl overflow-hidden z-50 divide-y divide-zinc-800/80 animate-in fade-in zoom-in-95 duration-100">
        {#if isSearching}
          <div class="p-3 text-center text-xs text-zinc-400 flex items-center justify-center gap-2">
            <div class="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            <span>Searching...</span>
          </div>
        {:else if searchResults.files.length === 0 && searchResults.folders.length === 0}
          <div class="p-3 text-center text-xs text-zinc-400">
            No matching items for "{searchQuery}"
          </div>
        {:else}
          <!-- Folders -->
          {#if searchResults.folders.length > 0}
            <div class="p-1.5">
              <span class="px-2 text-[10px] font-semibold uppercase tracking-wider text-zinc-500">Folders</span>
              {#each searchResults.folders as fol}
                <button
                  onclick={() => goToFolder(fol.id)}
                  class="w-full flex items-center justify-between p-1.5 rounded-lg hover:bg-zinc-800/60 transition text-left cursor-pointer group"
                >
                  <div class="flex items-center gap-2 min-w-0">
                    <Folder class="w-3.5 h-3.5 text-amber-500 shrink-0 fill-amber-500/20" />
                    <div class="truncate">
                      <p class="text-xs font-medium text-zinc-200 truncate">{fol.name}</p>
                      <p class="text-[10px] text-zinc-500 truncate">{fol.parentPath || 'My Drive'}</p>
                    </div>
                  </div>
                  <ArrowUpRight class="w-3 h-3 text-zinc-500 group-hover:text-white shrink-0" />
                </button>
              {/each}
            </div>
          {/if}

          <!-- Files -->
          {#if searchResults.files.length > 0}
            <div class="p-1.5">
              <span class="px-2 text-[10px] font-semibold uppercase tracking-wider text-zinc-500">Files</span>
              {#each searchResults.files as f}
                <div class="w-full flex items-center justify-between p-1.5 rounded-lg hover:bg-zinc-800/60 transition group">
                  <button
                    onclick={() => {
                      searchDropdownOpen = false;
                      selectionStore.getState().setPreviewFile(f);
                    }}
                    class="flex items-center gap-2 min-w-0 flex-1 text-left cursor-pointer"
                  >
                    <FileText class="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                    <div class="truncate flex-1">
                      <p class="text-xs font-medium text-zinc-200 truncate group-hover:text-white transition">{f.name}</p>
                      <p class="text-[10px] text-zinc-500 truncate flex items-center gap-1">
                        <span>📁 {f.folderPath || 'My Drive'}</span>
                        <span>•</span>
                        <span>{formatBytes(f.size)}</span>
                      </p>
                    </div>
                  </button>

                  <button
                    onclick={(e) => {
                      e.stopPropagation();
                      goToFolder(f.folderId);
                    }}
                    class="shrink-0 text-[10px] font-medium px-2 py-0.5 rounded bg-zinc-800 hover:bg-zinc-700 text-zinc-300 hover:text-white transition cursor-pointer ml-2"
                  >
                    Open
                  </button>
                </div>
              {/each}
            </div>
          {/if}

          <!-- View all footer -->
          <button
            onclick={() => {
              searchDropdownOpen = false;
              window.location.href = `/search?q=${encodeURIComponent(searchQuery)}`;
            }}
            class="w-full py-2 px-3 bg-zinc-900/60 text-center text-xs font-medium text-zinc-300 hover:text-white hover:bg-zinc-800/80 transition cursor-pointer"
          >
            View all results for "{searchQuery}" →
          </button>
        {/if}
      </div>
    {/if}
  </div>

  <!-- Right Side: View Switcher, Notifications & Profile -->
  <div class="flex items-center gap-1.5 sm:gap-2 shrink-0">
    <!-- View Switcher (shadcn style segmented pill) -->
    <div class="flex items-center bg-zinc-900 p-0.5 rounded-lg border border-zinc-800">
      <button
        onclick={() => selectionStore.getState().setViewMode('grid')}
        class="p-1 rounded-md text-xs transition cursor-pointer {viewMode === 'grid'
          ? 'bg-zinc-800 text-white shadow-xs'
          : 'text-zinc-400 hover:text-white'}"
        title="Grid View"
      >
        <LayoutGrid class="w-3.5 h-3.5" />
      </button>
      <button
        onclick={() => selectionStore.getState().setViewMode('table')}
        class="p-1 rounded-md text-xs transition cursor-pointer {viewMode === 'table'
          ? 'bg-zinc-800 text-white shadow-xs'
          : 'text-zinc-400 hover:text-white'}"
        title="Table View"
      >
        <List class="w-3.5 h-3.5" />
      </button>
    </div>

    <!-- Notifications Bell -->
    <div class="relative notifications-container">
      <button
        onclick={() => (notificationsOpen = !notificationsOpen)}
        class="relative p-1.5 text-zinc-400 hover:text-white hover:bg-zinc-800/80 rounded-md transition cursor-pointer"
        title="Notifications"
      >
        <Bell class="w-4 h-4" />
        {#if unreadCount > 0}
          <span class="absolute top-1 right-1 w-1.5 h-1.5 bg-blue-500 rounded-full"></span>
        {/if}
      </button>

      {#if notificationsOpen}
        <div class="absolute right-0 mt-2 w-80 bg-[#09090b] rounded-xl shadow-2xl border border-zinc-800 py-2.5 z-50">
          <div class="flex items-center justify-between px-3.5 pb-2 border-b border-zinc-800">
            <span class="font-semibold text-xs text-white">Notifications</span>
            {#if unreadCount > 0}
              <button
                onclick={markAllNotificationsRead}
                class="text-[11px] text-zinc-400 hover:text-white flex items-center gap-1 cursor-pointer"
              >
                <CheckCheck class="w-3 h-3" />
                Mark all read
              </button>
            {/if}
          </div>

          <div class="max-h-72 overflow-y-auto divide-y divide-zinc-800/80">
            {#if notifications.length === 0}
              <p class="text-xs text-zinc-500 text-center py-6">No notifications</p>
            {:else}
              {#each notifications as notif}
                <div class="p-3 text-xs hover:bg-zinc-850/60 transition">
                  <p class="text-zinc-300 font-medium leading-relaxed">{notif.message}</p>
                  <span class="text-[10px] text-zinc-500 mt-1 block">
                    {new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              {/each}
            {/if}
          </div>
        </div>
      {/if}
    </div>

    <!-- User Profile Dropdown -->
    <div class="relative profile-container">
      <button
        onclick={() => (profileOpen = !profileOpen)}
        class="flex items-center gap-2 p-1 hover:bg-zinc-800/80 rounded-lg transition cursor-pointer"
      >
        <div class="w-6 h-6 rounded-md bg-zinc-800 border border-zinc-700 text-white font-semibold text-[10px] flex items-center justify-center">
          {user?.username?.substring(0, 2).toUpperCase() || 'US'}
        </div>
      </button>

      {#if profileOpen}
        <div class="absolute right-0 mt-2 w-56 bg-[#09090b] rounded-xl shadow-2xl border border-zinc-800 py-2 z-50 text-xs">
          <div class="px-3 pb-2 border-b border-zinc-800">
            <p class="font-semibold text-white truncate">{user?.username}</p>
            <p class="text-[11px] text-zinc-400 truncate">{user?.email}</p>
          </div>

          <div class="py-1">
            <a
              href="/settings/security"
              class="flex items-center gap-2 px-3 py-1.5 text-zinc-300 hover:bg-zinc-800 hover:text-white transition"
            >
              <Shield class="w-3.5 h-3.5 text-zinc-400" />
              Security Settings
            </a>
          </div>

          <div class="pt-1 border-t border-zinc-800">
            <button
              onclick={() => authStore.getState().logout()}
              class="w-full flex items-center gap-2 px-3 py-1.5 text-red-400 hover:bg-red-950/30 transition cursor-pointer"
            >
              <LogOut class="w-3.5 h-3.5" />
              Sign Out
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>
</header>
