<script lang="ts">
  import { apiRequest } from '$lib/api/client';
  import { formatDate } from '$lib/utils/format-date';
  import { Bell, CheckCheck, Trash2 } from 'lucide-svelte';
  import { onMount } from 'svelte';
  import type { NotificationItem } from '$lib/api/types';

  let notifications = $state<NotificationItem[]>([]);
  let unreadCount = $state(0);
  let isLoading = $state(true);

  async function loadNotifications() {
    isLoading = true;
    try {
      const res = await apiRequest('/notifications');
      notifications = res.notifications || [];
      unreadCount = res.unreadCount || 0;
    } finally {
      isLoading = false;
    }
  }

  async function markAllRead() {
    try {
      await apiRequest('/notifications/read-all', { method: 'POST' });
      unreadCount = 0;
      notifications = notifications.map((n) => ({ ...n, readAt: new Date().toISOString() }));
    } catch {}
  }

  onMount(() => {
    loadNotifications();
  });
</script>

<div class="space-y-6 max-w-3xl">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2.5">
        <Bell class="w-7 h-7 text-blue-600" />
        Notifications
      </h1>
      <p class="text-xs text-slate-500 mt-1">In-app notifications for shares and account security.</p>
    </div>

    {#if unreadCount > 0}
      <button
        onclick={markAllRead}
        class="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 rounded-xl text-xs font-semibold transition cursor-pointer"
      >
        <CheckCheck class="w-4 h-4" />
        Mark all as read
      </button>
    {/if}
  </div>

  {#if isLoading}
    <div class="flex items-center justify-center py-20">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>
  {:else if notifications.length === 0}
    <div class="text-center py-20 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-8">
      <Bell class="w-12 h-12 text-slate-400 mx-auto mb-3" />
      <h3 class="font-bold text-slate-800 dark:text-slate-200 text-sm">No notifications</h3>
      <p class="text-xs text-slate-500 mt-1">You are all caught up!</p>
    </div>
  {:else}
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl divide-y divide-slate-100 dark:divide-slate-800 overflow-hidden shadow-xs">
      {#each notifications as notif}
        <div class="p-4 flex items-start gap-4 hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
          <div class="w-2.5 h-2.5 rounded-full mt-1.5 shrink-0 {notif.readAt ? 'bg-transparent' : 'bg-blue-600'}"></div>
          <div class="flex-1">
            <p class="text-xs font-medium text-slate-800 dark:text-slate-200 leading-relaxed">{notif.message}</p>
            <span class="text-[11px] text-slate-400 mt-1 block">{formatDate(notif.createdAt)}</span>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>

