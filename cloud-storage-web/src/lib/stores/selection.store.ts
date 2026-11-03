import { createStore } from 'zustand/vanilla';
import type { FileItem, FolderItem } from '$lib/api/types';

interface SelectionStoreState {
  selectedFileIds: Set<string>;
  selectedFolderIds: Set<string>;
  viewMode: 'grid' | 'table';
  previewFile: FileItem | null;
  sharingResource: { type: 'file' | 'folder'; item: FileItem | FolderItem } | null;

  setViewMode: (mode: 'grid' | 'table') => void;
  toggleFileSelection: (id: string, multi?: boolean) => void;
  toggleFolderSelection: (id: string, multi?: boolean) => void;
  clearSelection: () => void;
  setPreviewFile: (file: FileItem | null) => void;
  setSharingResource: (res: { type: 'file' | 'folder'; item: FileItem | FolderItem } | null) => void;
}

export const selectionStore = createStore<SelectionStoreState>((set) => ({
  selectedFileIds: new Set(),
  selectedFolderIds: new Set(),
  viewMode: 'grid',
  previewFile: null,
  sharingResource: null,

  setViewMode: (mode) => set({ viewMode: mode }),

  toggleFileSelection: (id, multi = false) =>
    set((state) => {
      const next = multi ? new Set(state.selectedFileIds) : new Set<string>();
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return { selectedFileIds: next, selectedFolderIds: multi ? state.selectedFolderIds : new Set() };
    }),

  toggleFolderSelection: (id, multi = false) =>
    set((state) => {
      const next = multi ? new Set(state.selectedFolderIds) : new Set<string>();
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return { selectedFolderIds: next, selectedFileIds: multi ? state.selectedFileIds : new Set() };
    }),

  clearSelection: () => set({ selectedFileIds: new Set(), selectedFolderIds: new Set() }),

  setPreviewFile: (file) => set({ previewFile: file }),

  setSharingResource: (res) => set({ sharingResource: res }),
}));

