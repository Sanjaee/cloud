<script lang="ts">
	import { AlertDialog as AlertDialogPrimitive } from "bits-ui";
	import {
		buttonVariants,
		type ButtonVariant,
		type ButtonSize,
	} from "$lib/components/ui/button/index.js";
	import { cn } from "$lib/utils.js";

	let {
		ref = $bindable(null),
		class: className,
		variant = "outline",
		size = "default",
		...restProps
	}: AlertDialogPrimitive.CancelProps & {
		variant?: ButtonVariant;
		size?: ButtonSize;
	} = $props();
</script>

<AlertDialogPrimitive.Cancel
	bind:ref
	data-slot="alert-dialog-cancel"
	class={cn(
		"inline-flex items-center justify-center rounded-lg text-xs font-medium px-4 py-2 border border-zinc-800 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 hover:text-white transition cursor-pointer active:scale-[0.99]",
		className
	)}
	{...restProps}
/>
