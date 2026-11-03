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
		variant = "default",
		size = "default",
		...restProps
	}: AlertDialogPrimitive.ActionProps & {
		variant?: ButtonVariant;
		size?: ButtonSize;
	} = $props();
</script>

<AlertDialogPrimitive.Action
	bind:ref
	data-slot="alert-dialog-action"
	class={cn(
		"inline-flex items-center justify-center rounded-lg text-xs font-semibold px-4 py-2 transition cursor-pointer active:scale-[0.99] disabled:opacity-50",
		variant === "destructive"
			? "bg-red-600 hover:bg-red-700 text-white shadow-xs"
			: "bg-white text-zinc-950 hover:bg-zinc-200 shadow-xs",
		className
	)}
	{...restProps}
/>
