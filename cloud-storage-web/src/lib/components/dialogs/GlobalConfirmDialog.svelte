<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { confirmStore } from '$lib/stores/confirm.store';
  import {
    AlertDialog,
    AlertDialogContent,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogCancel,
    AlertDialogAction,
  } from '$lib/components/ui/alert-dialog';
  import { AlertTriangle, HelpCircle } from 'lucide-svelte';

  let state = $state(confirmStore.getState());
  let unsubscribe: () => void;

  onMount(() => {
    unsubscribe = confirmStore.subscribe((s) => {
      state = s;
    });
  });

  onDestroy(() => {
    if (unsubscribe) unsubscribe();
  });
</script>

<AlertDialog
  open={state.isOpen}
  onOpenChange={(open) => {
    if (!open && state.isOpen) {
      confirmStore.getState().cancel();
    }
  }}
>
  <AlertDialogContent class="border-zinc-800 bg-[#121214] text-white">
    <AlertDialogHeader class="flex flex-col space-y-2 text-left">
      <div class="flex items-center gap-2">
        {#if state.variant === 'destructive'}
          <div class="p-1.5 rounded-lg bg-red-950/60 border border-red-900/50 text-red-400 shrink-0">
            <AlertTriangle class="w-4 h-4" />
          </div>
        {:else}
          <div class="p-1.5 rounded-lg bg-zinc-800 border border-zinc-700 text-zinc-300 shrink-0">
            <HelpCircle class="w-4 h-4" />
          </div>
        {/if}
        <AlertDialogTitle>{state.title}</AlertDialogTitle>
      </div>
      <AlertDialogDescription class="text-zinc-400 text-xs sm:text-sm">
        {state.description}
      </AlertDialogDescription>
    </AlertDialogHeader>

    <AlertDialogFooter>
      <AlertDialogCancel onclick={() => confirmStore.getState().cancel()}>
        {state.cancelText}
      </AlertDialogCancel>
      <AlertDialogAction
        variant={state.variant}
        onclick={() => confirmStore.getState().confirm()}
      >
        {state.confirmText}
      </AlertDialogAction>
    </AlertDialogFooter>
  </AlertDialogContent>
</AlertDialog>

