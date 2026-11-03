<script lang="ts">
  import { selectionStore, type BatchResourceItem } from '$lib/stores/selection.store';
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
      publicLinkUrl = `${window.location.origin}/share/${res.token}`;
    } catch {}
  }

  async function handleSearch(query: string) {
    recipientInput = query;
    if (query.trim().length >= 2) {
      try {
        const res = await apiRequest(`/users/search?q=${encodeURIComponent(query)}`);
        searchResults = res.users || [];
      } catch {
        searchResults = [];
      }
    } else {
      searchResults = [];
    }
  }

  async function handleShare() {
    if (!recipientInput.trim()) return;
    isSharing = true;
    error = '';
    message = '';

    try {
      if (batchResources && batchResources.length > 0) {
        // Batch sharing
        const items = batchResources.map((r) => ({
          resourceType: r.type,
          resourceId: r.item.id,
        }));

        const res = await apiRequest('/shares/batch', {
          method: 'POST',
          body: JSON.stringify({
            items,
            recipientEmailOrUsername: recipientInput.trim(),
            permission: selectedPermission,
            message: shareMessage || undefined,
          }),
        });

        message = res.message || `Successfully shared ${items.length} items`;
        recipientInput = '';
        shareMessage = '';
        searchResults = [];
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
  <div class="fixed inset-0 bg-slate-950/60 backdrop-blur-xs z-50 flex items-center justify-center p-4 animate-in fade-in duration-150">
    <div class="w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden">
      <!-- Header -->
      <div class="px-6 py-5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-2xl bg-blue-50 dark:bg-blue-950/60 flex items-center justify-center text-blue-600 shrink-0">
            {#if batchResources}
              <Layers class="w-5 h-5" />
            {:else}
              <UserPlus class="w-5 h-5" />
            {/if}
          </div>
          <div>
            <h3 class="font-bold text-base text-slate-900 dark:text-white">
              {#if batchResources}
                Share {batchResources.length} Selected Items
              {:else}
                Share "{resource?.item.name}"
              {/if}
            </h3>
            <p class="text-xs text-slate-500 mt-0.5">
              {#if batchResources}
                Grant simultaneous collaborative access to all selected files & folders.
              {:else}
                Native sharing: collaborators access the same Cloudflare R2 object with zero duplicate storage.
              {/if}
            </p>
          </div>
        </div>

        <button onclick={close} class="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 cursor-pointer">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Tabs -->
      <div class="flex border-b border-slate-100 dark:border-slate-800 px-6">
        <button
          onclick={() => (activeTab = 'users')}
          class="py-3 px-2 text-xs font-semibold flex items-center gap-2 border-b-2 transition cursor-pointer {activeTab === 'users'
            ? 'border-blue-600 text-blue-600 dark:text-blue-400'
            : 'border-transparent text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'}"
        >
          <UserPlus class="w-4 h-4" />
          Share with people
        </button>

        <button
          onclick={() => (activeTab = 'link')}
          class="py-3 px-2 text-xs font-semibold flex items-center gap-2 border-b-2 transition cursor-pointer {activeTab === 'link'
            ? 'border-blue-600 text-blue-600 dark:text-blue-400'
            : 'border-transparent text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'}"
        >
          <Link class="w-4 h-4" />
          Public share link
        </button>
      </div>

      <!-- Tab 1: Share with Users -->
      {#if activeTab === 'users'}
        <div class="p-6 space-y-5">
          {#if error}
            <div class="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-xl text-xs text-red-700 dark:text-red-300 flex items-center gap-2">
              <AlertCircle class="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          {/if}

          {#if message}
            <div class="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 rounded-xl text-xs text-emerald-700 dark:text-emerald-300 flex items-center gap-2">
              <Check class="w-4 h-4 shrink-0" />
              <span>{message}</span>
            </div>
          {/if}

          <!-- User input & permission -->
          <div class="space-y-3">
            <div class="flex gap-2">
              <div class="relative flex-1">
                <input
                  type="text"
                  value={recipientInput}
                  oninput={(e) => handleSearch((e.target as HTMLInputElement).value)}
                  placeholder="Enter email or username..."
                  class="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                />

                <!-- Autocomplete Dropdown -->
                {#if searchResults.length > 0}
                  <div class="absolute left-0 right-0 top-full mt-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl shadow-xl overflow-hidden z-20 divide-y divide-slate-100 dark:divide-slate-700">
                    {#each searchResults as u}
                      <button
                        onclick={() => {
                          recipientInput = u.email;
                          searchResults = [];
                        }}
                        class="w-full text-left px-4 py-2 hover:bg-slate-50 dark:hover:bg-slate-700/60 text-xs flex flex-col cursor-pointer"
                      >
                        <span class="font-semibold text-slate-900 dark:text-white">{u.username}</span>
                        <span class="text-slate-500 text-[11px]">{u.email}</span>
                      </button>
                    {/each}
                  </div>
                {/if}
              </div>

              <!-- Permission Level Selector -->
              <select
                bind:value={selectedPermission}
                class="px-3 py-2.5 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
              >
                <option value="viewer">Viewer</option>
                <option value="commenter">Commenter</option>
                <option value="editor">Editor</option>
              </select>
            </div>

            <textarea
              bind:value={shareMessage}
              rows="2"
              placeholder="Add an optional message..."
              class="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white resize-none"
            ></textarea>

            <button
              onclick={handleShare}
              disabled={isSharing || !recipientInput.trim()}
              class="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition disabled:opacity-50 cursor-pointer"
            >
              {isSharing ? 'Sharing...' : 'Share Access'}
            </button>
          </div>

          <!-- Active Collaborators List -->
          <div>
            <h4 class="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2.5 flex items-center gap-1.5">
              <Users class="w-3.5 h-3.5" />
              People with access
            </h4>

            <div class="space-y-2 max-h-48 overflow-y-auto">
              <!-- Owner item -->
              <div class="flex items-center justify-between p-2.5 bg-slate-50 dark:bg-slate-800/50 rounded-xl text-xs">
                <div>
                  <p class="font-semibold text-slate-900 dark:text-white">You</p>
                  <p class="text-[11px] text-slate-400">Owner</p>
                </div>
                <span class="text-[11px] font-medium text-slate-500 bg-slate-200 dark:bg-slate-700 px-2 py-0.5 rounded-md">
                  Owner
                </span>
              </div>

              {#each collaborators as c}
                <div class="flex items-center justify-between p-2.5 bg-slate-50 dark:bg-slate-800/50 rounded-xl text-xs">
                  <div>
                    <p class="font-semibold text-slate-900 dark:text-white">{c.username}</p>
                    <p class="text-[11px] text-slate-400">{c.email}</p>
                  </div>

                  <div class="flex items-center gap-2">
                    <span class="text-[11px] font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950 px-2 py-0.5 rounded-md capitalize">
                      {c.permission}
                    </span>

                    <button
                      onclick={() => revokeAccess(c.userId)}
                      class="p-1 hover:bg-red-50 dark:hover:bg-red-950 text-slate-400 hover:text-red-600 rounded cursor-pointer"
                      title="Revoke access"
                    >
                      <Trash2 class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              {/each}
            </div>
          </div>
        </div>
      {:else}
        <!-- Tab 2: Public Share Link -->
        <div class="p-6 space-y-4">
          <p class="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
            Anyone with this link can view and download this file directly via Cloudflare R2 short-lived presigned URLs.
          </p>

          <div class="flex items-center gap-2">
            <input
              type="text"
              readonly
              value={publicLinkUrl}
              class="flex-1 px-4 py-2 bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-xl text-xs text-slate-700 dark:text-slate-300 font-mono select-all"
            />

            <button
              onclick={copyPublicLink}
              class="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold shadow-md transition cursor-pointer shrink-0"
            >
              {#if linkCopied}
                <Check class="w-4 h-4" />
                Copied!
              {:else}
                <Copy class="w-4 h-4" />
                Copy Link
              {/if}
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>
{/if}

