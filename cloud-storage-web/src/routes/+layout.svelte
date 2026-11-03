<script lang="ts">
  import '../app.css';
  import { QueryClient, QueryClientProvider } from '@tanstack/svelte-query';
  import GlobalConfirmDialog from '$lib/components/dialogs/GlobalConfirmDialog.svelte';

  let { children } = $props();

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 1000 * 30,
        refetchOnWindowFocus: false,
        retry: 1,
      },
    },
  });
</script>

<QueryClientProvider client={queryClient}>
  {@render children()}
  <GlobalConfirmDialog />
</QueryClientProvider>

