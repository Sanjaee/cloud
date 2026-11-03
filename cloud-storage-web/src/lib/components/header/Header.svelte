<script lang="ts">
  import { authStore } from '$lib/stores/auth.store';
  import { selectionStore } from '$lib/stores/selection.store';
  import { formatBytes } from '$lib/utils/file-size';
  import {
    Cloud,
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
    }, 250);
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
  }

  onMount(() => {
    loadNotifications();
    window.addEventListener('click', onGlobalClick);
  });
</script>

<header class="h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 flex items-center justify-between shrink-0 z-20">
  <!-- Left: Logo & Search -->
  <div class="flex items-center gap-8 flex-1 max-w-2xl">
    <a href="/drive" class="flex items-center gap-2.5 font-bold text-lg text-slate-900 dark:text-white shrink-0">
      <div class="w-9 h-9 bg-blue-600 rounded-xl flex items-center justify-center text-white shadow-md shadow-blue-500/20">
        <Cloud class="w-5 h-5" />
      </div>
      <span class="tracking-tight">REA Storage</span>
    </a>

    <!-- Search Bar -->
    <div class="relative w-full search-container">
      <Search class="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
      <input
        type="text"
        bind:value={searchQuery}
        oninput={handleSearchInput}
        onkeydown={handleSearchKeyDown}
        onfocus={() => { if (searchQuery.trim()) searchDropdownOpen = true; }}
        placeholder="Cari file dan folder..."
        class="w-full pl-10 pr-9 py-2 bg-slate-100/80 dark:bg-slate-800/60 border border-transparent focus:border-blue-500/50 focus:bg-white dark:focus:bg-slate-900 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 text-slate-900 dark:text-white transition"
      />
      {#if searchQuery}
        <button
          onclick={() => { searchQuery = ''; searchDropdownOpen = false; }}
          class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition p-0.5 cursor-pointer"
        >
          <X class="w-3.5 h-3.5" />
        </button>
      {/if}

      <!-- Live Search Results Dropdown -->
      {#if searchDropdownOpen && searchQuery.trim()}
        <div class="absolute left-0 right-0 top-full mt-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-xl overflow-hidden z-50 divide-y divide-slate-100 dark:divide-slate-800 animate-in fade-in zoom-in-95 duration-100">
          {#if isSearching}
            <div class="p-4 text-center text-xs text-slate-400 flex items-center justify-center gap-2">
              <div class="w-4 h-4 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              <span>Mencari di dalam semua folder...</span>
            </div>
          {:else if searchResults.files.length === 0 && searchResults.folders.length === 0}
            <div class="p-4 text-center text-xs text-slate-400">
              Tidak ada file atau folder yang cocok dengan "{searchQuery}"
            </div>
          {:else}
            <!-- Matching Folders -->
            {#if searchResults.folders.length > 0}
              <div class="p-2">
                <span class="px-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">Folders</span>
                {#each searchResults.folders as fol}
                  <button
                    onclick={() => goToFolder(fol.id)}
                    class="w-full flex items-center justify-between p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800/60 transition text-left cursor-pointer group"
                  >
                    <div class="flex items-center gap-2.5 min-w-0">
                      <Folder class="w-4 h-4 text-amber-500 shrink-0 fill-amber-500/20" />
                      <div class="truncate">
                        <p class="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">{fol.name}</p>
                        <p class="text-[10px] text-slate-400 truncate">Lokasi: {fol.parentPath || 'My Drive'}</p>
                      </div>
                    </div>
                    <ArrowUpRight class="w-3.5 h-3.5 text-slate-400 group-hover:text-blue-500 shrink-0" />
                  </button>
                {/each}
              </div>
            {/if}

            <!-- Matching Files -->
            {#if searchResults.files.length > 0}
              <div class="p-2">
                <span class="px-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">Files</span>
                {#each searchResults.files as f}
                  <div
                    class="w-full flex items-center justify-between p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800/60 transition text-left group"
                  >
                    <button
                      onclick={() => {
                        searchDropdownOpen = false;
                        selectionStore.getState().setPreviewFile(f);
                      }}
                      class="flex items-center gap-2.5 min-w-0 flex-1 text-left cursor-pointer"
                    >
                      <FileText class="w-4 h-4 text-blue-500 shrink-0" />
                      <div class="truncate flex-1">
                        <p class="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate group-hover:text-blue-600 transition">{f.name}</p>
                        <p class="text-[10px] text-slate-400 truncate flex items-center gap-1">
                          <span class="font-medium text-slate-500 dark:text-slate-400">📁 {f.folderPath || 'My Drive'}</span>
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
                      class="shrink-0 text-[10px] font-semibold px-2 py-1 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-blue-950 hover:text-blue-600 transition cursor-pointer ml-2"
                      title="Buka lokasi folder di My Drive"
                    >
                      Buka Folder
                    </button>
                  </div>
                {/each}
              </div>
            {/if}

            <!-- View All Footer -->
            <button
              onclick={() => {
                searchDropdownOpen = false;
                window.location.href = `/search?q=${encodeURIComponent(searchQuery)}`;
              }}
              class="w-full py-2.5 px-4 bg-slate-50 dark:bg-slate-800/40 text-center text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition cursor-pointer"
            >
              Lihat semua hasil untuk "{searchQuery}" →
            </button>
          {/if}
        </div>
      {/if}
    </div>
  </div>

  <!-- Right: View mode, Notifications, Profile -->
  <div class="flex items-center gap-3">
    <!-- View Switcher -->
    <div class="flex items-center bg-slate-100 dark:bg-slate-800 p-1 rounded-xl border border-slate-200 dark:border-slate-700">
      <button
        onclick={() => selectionStore.getState().setViewMode('grid')}
        class="p-1.5 rounded-lg text-xs transition cursor-pointer {viewMode === 'grid'
          ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-xs'
          : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'}"
        title="Grid View"
      >
        <LayoutGrid class="w-4 h-4" />
      </button>
      <button
        onclick={() => selectionStore.getState().setViewMode('table')}
        class="p-1.5 rounded-lg text-xs transition cursor-pointer {viewMode === 'table'
          ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-xs'
          : 'text-slate-500 hover:text-slate-900 dark:hover:text-white'}"
        title="List / Table View"
      >
        <List class="w-4 h-4" />
      </button>
    </div>

    <!-- Notifications Bell -->
    <div class="relative">
      <button
        onclick={() => (notificationsOpen = !notificationsOpen)}
        class="relative p-2 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
      >
        <Bell class="w-5 h-5" />
        {#if unreadCount > 0}
          <span class="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-600 rounded-full ring-2 ring-white dark:ring-slate-900"></span>
        {/if}
      </button>

      {#if notificationsOpen}
        <div class="absolute right-0 mt-2 w-80 bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 py-3 z-50">
          <div class="flex items-center justify-between px-4 pb-2 border-b border-slate-100 dark:border-slate-800">
            <span class="font-semibold text-sm text-slate-900 dark:text-white">Notifications</span>
            {#if unreadCount > 0}
              <button
                onclick={markAllNotificationsRead}
                class="text-xs text-blue-600 hover:underline flex items-center gap-1 cursor-pointer"
              >
                <CheckCheck class="w-3.5 h-3.5" />
                Mark all read
              </button>
            {/if}
          </div>

          <div class="max-h-72 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800">
            {#if notifications.length === 0}
              <p class="text-xs text-slate-500 text-center py-6">No notifications yet</p>
            {:else}
              {#each notifications as notif}
                <div class="p-3 text-xs hover:bg-slate-50 dark:hover:bg-slate-800/50 transition">
                  <p class="text-slate-800 dark:text-slate-200 font-medium leading-relaxed">{notif.message}</p>
                  <span class="text-[10px] text-slate-400 mt-1 block">
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
    <div class="relative">
      <button
        onclick={() => (profileOpen = !profileOpen)}
        class="flex items-center gap-2 p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition cursor-pointer"
      >
        <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-bold text-xs flex items-center justify-center shadow-xs">
          {user?.username?.substring(0, 2).toUpperCase() || 'US'}
        </div>
      </button>

      {#if profileOpen}
        <div class="absolute right-0 mt-2 w-64 bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 py-3 z-50">
          <div class="px-4 pb-3 border-b border-slate-100 dark:border-slate-800">
            <p class="font-semibold text-sm text-slate-900 dark:text-white truncate">{user?.username}</p>
            <p class="text-xs text-slate-500 truncate">{user?.email}</p>
          </div>

          <div class="py-1">
            <a
              href="/settings/security"
              class="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
            >
              <Shield class="w-4 h-4 text-slate-400" />
              Security Settings
            </a>
          </div>

          <div class="pt-1 border-t border-slate-100 dark:border-slate-800">
            <button
              onclick={() => authStore.getState().logout()}
              class="w-full flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 transition cursor-pointer"
            >
              <LogOut class="w-4 h-4" />
              Sign Out
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>
</header>

