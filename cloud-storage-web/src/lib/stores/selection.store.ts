import { createStore } from 'zustand/vanilla';
import type { FileItem, FolderItem } from '$lib/api/types';

export interface BatchResourceItem {
  type: 'file' | 'folder';
  item: FileItem | FolderItem;
}

interface SelectionStoreState {
  selectedFileIds: Set<string>;
  selectedFolderIds: Set<string>;
  viewMode: 'grid' | 'table';
  previewFile: FileItem | null;
  sharingResource: { type: 'file' | 'folder'; item: FileItem | FolderItem } | null;
  batchSharingResources: BatchResourceItem[] | null;
  activeFolderId: string | null;
  isSidebarOpen: boolean; // Mobile sheet toggle
  isSidebarCollapsed: boolean; // Desktop rail toggle

  setViewMode: (mode: 'grid' | 'table') => void;
  toggleFileSelection: (id: string, multi?: boolean) => void;
  toggleFolderSelection: (id: string, multi?: boolean) => void;
  selectAll: (fileIds: string[], folderIds: string[]) => void;
  clearSelection: () => void;
  setPreviewFile: (file: FileItem | null) => void;
  setSharingResource: (res: { type: 'file' | 'folder'; item: FileItem | FolderItem } | null) => void;
  setBatchSharingResources: (items: BatchResourceItem[] | null) => void;
  setActiveFolderId: (folderId: string | null) => void;
  setSidebarOpen: (open: boolean) => void;
  toggleSidebar: () => void;
  toggleSidebarCollapse: () => void;
}

export const selectionStore = createStore<SelectionStoreState>((set) => ({
  selectedFileIds: new Set(),
  selectedFolderIds: new Set(),
  viewMode: 'grid',
  previewFile: null,
  sharingResource: null,
  batchSharingResources: null,
  activeFolderId: null,
  isSidebarOpen: false,
  isSidebarCollapsed: false,

  setViewMode: (mode) => set({ viewMode: mode }),

  toggleFileSelection: (id, multi = true) =>
    set((state) => {
      const next = multi ? new Set(state.selectedFileIds) : new Set<string>();
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return { selectedFileIds: next, selectedFolderIds: multi ? state.selectedFolderIds : new Set() };
    }),

  toggleFolderSelection: (id, multi = true) =>
    set((state) => {
      const next = multi ? new Set(state.selectedFolderIds) : new Set<string>();
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return { selectedFolderIds: next, selectedFileIds: multi ? state.selectedFileIds : new Set() };
    }),

  selectAll: (fileIds, folderIds) =>
    set({
      selectedFileIds: new Set(fileIds),
      selectedFolderIds: new Set(folderIds),
    }),

  clearSelection: () => set({ selectedFileIds: new Set(), selectedFolderIds: new Set() }),

  setPreviewFile: (file) => set({ previewFile: file }),

  setSharingResource: (res) => set({ sharingResource: res }),

  setBatchSharingResources: (items) => set({ batchSharingResources: items }),

  setActiveFolderId: (folderId) => set({ activeFolderId: folderId }),

  setSidebarOpen: (open) => set({ isSidebarOpen: open }),

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),

  toggleSidebarCollapse: () => set((state) => ({ isSidebarCollapsed: !state.isSidebarCollapsed })),
}));
