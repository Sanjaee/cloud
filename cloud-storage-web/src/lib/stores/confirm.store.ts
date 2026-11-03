import { createStore } from 'zustand/vanilla';

export interface ConfirmDialogOptions {
  title?: string;
  description: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'destructive' | 'default';
}

interface ConfirmState {
  isOpen: boolean;
  title: string;
  description: string;
  confirmText: string;
  cancelText: string;
  variant: 'destructive' | 'default';
  resolve?: (value: boolean) => void;
  open: (options: ConfirmDialogOptions) => Promise<boolean>;
  confirm: () => void;
  cancel: () => void;
}

export const confirmStore = createStore<ConfirmState>((set, get) => ({
  isOpen: false,
  title: 'Konfirmasi Tindakan',
  description: 'Apakah Anda yakin ingin melanjutkan tindakan ini?',
  confirmText: 'Lanjutkan',
  cancelText: 'Batal',
  variant: 'default',
  resolve: undefined,

  open: (options) => {
    return new Promise<boolean>((resolve) => {
      set({
        isOpen: true,
        title: options.title || (options.variant === 'destructive' ? 'Konfirmasi Tindakan' : 'Konfirmasi'),
        description: options.description,
        confirmText: options.confirmText || (options.variant === 'destructive' ? 'Lanjutkan' : 'Konfirmasi'),
        cancelText: options.cancelText || 'Batal',
        variant: options.variant || 'default',
        resolve,
      });
    });
  },

  confirm: () => {
    const { resolve } = get();
    set({ isOpen: false, resolve: undefined });
    if (resolve) resolve(true);
  },

  cancel: () => {
    const { resolve } = get();
    set({ isOpen: false, resolve: undefined });
    if (resolve) resolve(false);
  },
}));

/**
 * Universal async drop-in replacement for window.confirm()
 * Usage:
 * if (!await confirmDialog({ description: 'Hapus item ini?', variant: 'destructive' })) return;
 */
export async function confirmDialog(options: string | ConfirmDialogOptions): Promise<boolean> {
  const opts: ConfirmDialogOptions = typeof options === 'string'
    ? { description: options }
    : options;
  return confirmStore.getState().open(opts);
}

