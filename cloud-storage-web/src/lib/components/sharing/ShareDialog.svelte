<script lang="ts">
  import { selectionStore, type BatchResourceItem } from '$lib/stores/selection.store';
  import { authStore } from '$lib/stores/auth.store';
  import { apiRequest } from '$lib/api/client';
  import {
    X,
    UserPlus,
    Link,
    Copy,
    Check,
    Trash2,
    Shield,
    Users,
    AlertCircle,
    Layers,
  } from 'lucide-svelte';
  import type { CollaboratorInfo } from '$lib/api/types';

  let resource = $state<{ type: 'file' | 'folder'; item: any } | null>(null);
  let batchResources = $state<BatchResourceItem[] | null>(null);
  let activeTab = $state<'users' | 'link'>('users');
  let recipientInput = $state('');
  let searchResults = $state<{ id: string; email: string; username: string }[]>([]);
  let selectedPermission = $state<'viewer' | 'commenter' | 'editor'>('viewer');
  let shareMessage = $state('');
  let collaborators = $state<CollaboratorInfo[]>([]);
  let isLoading = $state(false);
  let isSharing = $state(false);
  let message = $state('');
  let error = $state('');

  // Public link state
  let publicLinkUrl = $state('');
  let publicLinkPassword = $state('');
  let linkCopied = $state(false);

  selectionStore.subscribe(async (s) => {
    resource = s.sharingResource;
    batchResources = s.batchSharingResources;
    if (resource && !batchResources) {
      loadCollaborators();
      loadOrCreatePublicLink();
    }
  });

  async function loadCollaborators() {
    if (!resource) return;
    try {
      const res = await apiRequest(`/shares/${resource.type}/${resource.item.id}/collaborators`);
      collaborators = res.collaborators || [];
    } catch {}
  }

  async function loadOrCreatePublicLink() {
    if (!resource) return;
    try {
      const res = await apiRequest('/public-links', {
        method: 'POST',
        body: JSON.stringify({
          resourceType: resource.type,
          resourceId: resource.item.id,
          permission: 'viewer',
        }),
      });
      publicLinkUrl = `${window.location.origin}/share/${res.publicLink.token}`;
    } catch {}
  }

  async function handleSearch(q: string) {
    recipientInput = q;
    if (q.length < 2) {
      searchResults = [];
      return;
    }
    try {
      const res = await apiRequest(`/users/search?q=${encodeURIComponent(q)}`);
      const myId = authStore.getState().user?.id;
      searchResults = (res.users || []).filter((u: any) => u.id !== myId);
    } catch {}
  }

  async function handleShare() {
    if (!recipientInput.trim()) return;

    // Validate not sharing with self
    const me = authStore.getState().user;
    const target = recipientInput.trim().toLowerCase();
    if (me && (target === me.email?.toLowerCase() || target === me.username?.toLowerCase())) {
      error = 'Anda tidak dapat membagikan item kepada akun Anda sendiri';
      return;
    }

    isSharing = true;
    error = '';
    message = '';

    try {
      if (batchResources && batchResources.length > 0) {
        // Multi-resource batch sharing
        const fileIds = batchResources.filter((r) => r.type === 'file').map((r) => r.item.id);
        const folderIds = batchResources.filter((r) => r.type === 'folder').map((r) => r.item.id);
        const items = batchResources.map((r) => ({
          resourceType: r.type,
          resourceId: r.item.id,
        }));

        const res = await apiRequest('/shares/batch', {
          method: 'POST',
          body: JSON.stringify({
            items,
            fileIds,
            folderIds,
            recipientEmailOrUsername: recipientInput.trim(),
            permission: selectedPermission,
            message: shareMessage || undefined,
          }),
        });

        message = res.message || `Shared ${batchResources.length} items successfully`;
        setTimeout(() => close(), 1500);
      } else if (resource) {
        // Single resource sharing
        const res = await apiRequest('/shares', {
          method: 'POST',
          body: JSON.stringify({
            resourceType: resource.type,
            resourceId: resource.item.id,
            recipientEmailOrUsername: recipientInput.trim(),
            permission: selectedPermission,
            message: shareMessage || undefined,
          }),
        });

        message = res.message || 'Shared successfully';
        recipientInput = '';
        shareMessage = '';
        searchResults = [];
        await loadCollaborators();
      }
    } catch (err: any) {
      error = err.message || 'Failed to share';
    } finally {
      isSharing = false;
    }
  }

  async function revokeAccess(userId: string) {
    if (!resource) return;
    try {
      await apiRequest(`/shares/${resource.type}/${resource.item.id}/collaborators/${userId}`, {
        method: 'DELETE',
      });
      await loadCollaborators();
    } catch {}
  }

  function copyPublicLink() {
    navigator.clipboard.writeText(publicLinkUrl);
    linkCopied = true;
    setTimeout(() => (linkCopied = false), 2000);
  }

  function close() {
    selectionStore.getState().setSharingResource(null);
    selectionStore.getState().setBatchSharingResources(null);
  }
</script>

{#if resource || (batchResources && batchResources.length > 0)}
  <div class="fixed inset-0 bg-black/70 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
    <div class="w-full max-w-md bg-[#121214] rounded-xl shadow-2xl border border-zinc-800/80 overflow-hidden">
      <!-- Header -->
      <div class="px-5 py-4 border-b border-zinc-800/80 flex items-center justify-between">
        <div class="flex items-center gap-2.5 min-w-0">
          <div class="w-8 h-8 rounded-lg bg-zinc-800 text-zinc-300 flex items-center justify-center shrink-0">
            {#if batchResources}
              <Layers class="w-4 h-4" />
            {:else}
              <UserPlus class="w-4 h-4" />
            {/if}
          </div>
          <div class="truncate">
            <h3 class="font-semibold text-sm text-white truncate">
              {#if batchResources}
                Share {batchResources.length} Items
              {:else}
                Share "{resource?.item.name}"
              {/if}
            </h3>
            <p class="text-[11px] text-zinc-400 truncate">
              {#if batchResources}
                Simultaneous collaborator access
              {:else}
                Native Cloudflare R2 zero-copy sharing
              {/if}
            </p>
          </div>
        </div>

        <button onclick={close} class="p-1 hover:bg-zinc-800 rounded-md text-zinc-400 hover:text-white cursor-pointer">
          <X class="w-4 h-4" />
        </button>
      </div>

      <!-- Tabs -->
      <div class="flex border-b border-zinc-800/80 px-5 text-xs">
        <button
          onclick={() => (activeTab = 'users')}
          class="py-2.5 px-2 font-medium flex items-center gap-1.5 border-b-2 transition cursor-pointer {activeTab === 'users'
            ? 'border-white text-white font-semibold'
            : 'border-transparent text-zinc-400 hover:text-zinc-200'}"
        >
          <UserPlus class="w-3.5 h-3.5" />
          Collaborators
        </button>

        {#if !batchResources}
          <button
            onclick={() => (activeTab = 'link')}
            class="py-2.5 px-2 font-medium flex items-center gap-1.5 border-b-2 transition cursor-pointer {activeTab === 'link'
              ? 'border-white text-white font-semibold'
              : 'border-transparent text-zinc-400 hover:text-zinc-200'}"
          >
            <Link class="w-3.5 h-3.5" />
            Public Link
          </button>
        {/if}
      </div>

      <!-- Tab 1: Share with Users -->
      {#if activeTab === 'users'}
        <div class="p-5 space-y-4">
          {#if error}
            <div class="p-2.5 bg-red-950/40 border border-red-900/60 rounded-lg text-xs text-red-300 flex items-center gap-2">
              <AlertCircle class="w-3.5 h-3.5 shrink-0" />
              <span>{error}</span>
            </div>
          {/if}

          {#if message}
            <div class="p-2.5 bg-emerald-950/40 border border-emerald-900/60 rounded-lg text-xs text-emerald-300 flex items-center gap-2">
              <Check class="w-3.5 h-3.5 shrink-0" />
              <span>{message}</span>
            </div>
          {/if}

          <!-- User input & permission -->
          <div class="space-y-2.5">
            <div class="flex gap-2">
              <div class="relative flex-1">
                <input
                  type="text"
                  value={recipientInput}
                  oninput={(e) => handleSearch((e.target as HTMLInputElement).value)}
                  placeholder="Email or username..."
                  class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs focus:outline-none focus:border-zinc-700 text-white placeholder:text-zinc-500"
                />

                <!-- Autocomplete Dropdown -->
                {#if searchResults.length > 0}
                  <div class="absolute left-0 right-0 top-full mt-1 bg-[#121214] border border-zinc-800 rounded-lg shadow-xl overflow-hidden z-20 divide-y divide-zinc-800/80">
                    {#each searchResults as u}
                      <button
                        onclick={() => {
                          recipientInput = u.email;
                          searchResults = [];
                        }}
                        class="w-full text-left px-3 py-1.5 hover:bg-zinc-800 text-xs flex flex-col cursor-pointer"
                      >
                        <span class="font-medium text-white">{u.username}</span>
                        <span class="text-zinc-400 text-[10px]">{u.email}</span>
                      </button>
                    {/each}
                  </div>
                {/if}
              </div>

              <!-- Permission Level Selector -->
              <select
                bind:value={selectedPermission}
                class="px-2.5 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs font-medium text-zinc-300 focus:outline-none focus:border-zinc-700 cursor-pointer"
              >
                <option value="viewer">Viewer</option>
                <option value="commenter">Commenter</option>
                <option value="editor">Editor</option>
              </select>
            </div>

            <textarea
              bind:value={shareMessage}
              rows="2"
              placeholder="Optional message..."
              class="w-full px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs focus:outline-none focus:border-zinc-700 text-white placeholder:text-zinc-500 resize-none"
            ></textarea>

            <button
              onclick={handleShare}
              disabled={isSharing || !recipientInput.trim()}
              class="w-full py-2 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition disabled:opacity-50 cursor-pointer active:scale-95"
            >
              {isSharing ? 'Sharing...' : 'Share Access'}
            </button>
          </div>

          <!-- Active Collaborators List (for single item) -->
          {#if !batchResources}
            <div>
              <h4 class="text-[10px] font-semibold uppercase tracking-wider text-zinc-500 mb-2 flex items-center gap-1.5">
                <Users class="w-3 h-3" />
                Collaborators
              </h4>

              <div class="space-y-1.5 max-h-36 overflow-y-auto">
                <div class="flex items-center justify-between p-2 bg-zinc-900/60 rounded-lg text-xs">
                  <div>
                    <p class="font-medium text-white">You</p>
                    <p class="text-[10px] text-zinc-400">Owner</p>
                  </div>
                  <span class="text-[10px] font-medium text-zinc-400 bg-zinc-800 px-1.5 py-0.5 rounded">
                    Owner
                  </span>
                </div>

                {#each collaborators as c}
                  <div class="flex items-center justify-between p-2 bg-zinc-900/60 rounded-lg text-xs">
                    <div>
                      <p class="font-medium text-white">{c.username}</p>
                      <p class="text-[10px] text-zinc-400">{c.email}</p>
                    </div>

                    <div class="flex items-center gap-1.5">
                      <span class="text-[10px] font-medium text-zinc-300 bg-zinc-800 px-1.5 py-0.5 rounded capitalize">
                        {c.permission}
                      </span>

                      <button
                        onclick={() => revokeAccess(c.userId)}
                        class="p-1 hover:bg-red-950/60 text-zinc-400 hover:text-red-400 rounded transition cursor-pointer"
                        title="Revoke access"
                      >
                        <Trash2 class="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                {/each}
              </div>
            </div>
          {/if}
        </div>
      {:else}
        <!-- Tab 2: Public Share Link -->
        <div class="p-5 space-y-3">
          <p class="text-xs text-zinc-400 leading-relaxed">
            Anyone with this link can view and download this file directly from Cloudflare R2 storage.
          </p>

          <div class="flex items-center gap-2">
            <input
              type="text"
              readonly
              value={publicLinkUrl}
              class="flex-1 px-3 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-xs text-zinc-300 font-mono select-all focus:outline-none"
            />

            <button
              onclick={copyPublicLink}
              class="flex items-center gap-1.5 px-3 py-2 bg-white text-zinc-950 hover:bg-zinc-200 rounded-lg text-xs font-semibold shadow-xs transition cursor-pointer shrink-0"
            >
              {#if linkCopied}
                <Check class="w-3.5 h-3.5" />
                Copied
              {:else}
                <Copy class="w-3.5 h-3.5" />
                Copy
              {/if}
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>
{/if}
