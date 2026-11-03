import { createStore } from 'zustand/vanilla';
import { apiRequest } from '$lib/api/client';

export type UploadStatus =
  | 'queued'
  | 'compressing'
  | 'awaiting_upload'
  | 'uploading'
  | 'verifying'
  | 'completed'
  | 'failed'
  | 'cancelled';

export interface UploadItem {
  id: string;
  file: File;
  processedBlob: Blob;
  name: string;
  size: number;
  originalSize: number;
  mimeType: string;
  folderId: string | null;
  status: UploadStatus;
  progress: number; // 0 - 100
  bytesUploaded: number;
  speedBytesPerSec: number;
  timeRemainingSec: number;
  error?: string;
  isCompressed?: boolean;
  savingsPercentage?: number;
  xhr?: XMLHttpRequest;
}

interface UploadStoreState {
  items: UploadItem[];
  isOpen: boolean;
  isMinimized: boolean;
  compressionMode: 'original' | 'balanced' | 'high';
  maxConcurrent: number;
  activeUploadCount: number;

  // Actions
  addFiles: (files: FileList | File[], targetFolderId?: string | null) => void;
  setCompressionMode: (mode: 'original' | 'balanced' | 'high') => void;
  toggleOpen: () => void;
  toggleMinimized: () => void;
  cancelUpload: (id: string) => void;
  retryUpload: (id: string) => void;
  clearCompleted: () => void;
  processQueue: () => void;
}

export const uploadStore = createStore<UploadStoreState>((set, get) => ({
  items: [],
  isOpen: false,
  isMinimized: false,
  compressionMode: 'balanced',
  maxConcurrent: 3,
  activeUploadCount: 0,

  setCompressionMode: (mode) => set({ compressionMode: mode }),
  toggleOpen: () => set((state) => ({ isOpen: !state.isOpen })),
  toggleMinimized: () => set((state) => ({ isMinimized: !state.isMinimized })),

  addFiles: (fileList, targetFolderId = null) => {
    const newItems: UploadItem[] = Array.from(fileList).map((file) => ({
      id: crypto.randomUUID(),
      file,
      processedBlob: file,
      name: file.name,
      size: file.size,
      originalSize: file.size,
      mimeType: file.type || 'application/octet-stream',
      folderId: targetFolderId,
      status: 'queued',
      progress: 0,
      bytesUploaded: 0,
      speedBytesPerSec: 0,
      timeRemainingSec: 0,
    }));

    set((state) => ({
      items: [...state.items, ...newItems],
      isOpen: true,
      isMinimized: false,
    }));

    get().processQueue();
  },

  cancelUpload: (id: string) => {
    const item = get().items.find((i) => i.id === id);
    if (item && item.xhr) {
      item.xhr.abort();
    }
    set((state) => ({
      items: state.items.map((i) =>
        i.id === id ? { ...i, status: 'cancelled', progress: 0 } : i
      ),
      activeUploadCount: Math.max(0, state.activeUploadCount - 1),
    }));
    get().processQueue();
  },

  retryUpload: (id: string) => {
    set((state) => ({
      items: state.items.map((i) =>
        i.id === id ? { ...i, status: 'queued', progress: 0, error: undefined } : i
      ),
    }));
    get().processQueue();
  },

  clearCompleted: () => {
    set((state) => ({
      items: state.items.filter((i) => i.status !== 'completed'),
    }));
  },

  processQueue: async () => {
    const { items, maxConcurrent, activeUploadCount } = get();
    if (activeUploadCount >= maxConcurrent) return;

    const nextItem = items.find((i) => i.status === 'queued');
    if (!nextItem) return;

    set((state) => ({
      activeUploadCount: state.activeUploadCount + 1,
      items: state.items.map((i) =>
        i.id === nextItem.id ? { ...i, status: 'uploading' } : i
      ),
    }));

    try {
      // Strategy:
      // For files <= 4MB: Use direct API upload (ultra-reliable, avoids cross-origin CORS limitations)
      // For files > 4MB: Use presigned Cloudflare R2 direct PUT upload
      if (nextItem.size <= 4 * 1024 * 1024) {
        const formData = new FormData();
        formData.append('file', nextItem.file);
        if (nextItem.folderId) {
          formData.append('folderId', nextItem.folderId);
        }

        await new Promise<void>((resolve, reject) => {
          const xhr = new XMLHttpRequest();
          const startTime = Date.now();

          xhr.open('POST', '/api/v1/uploads/direct', true);
          xhr.withCredentials = true;

          xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
              const percent = Math.round((event.loaded / event.total) * 100);
              const elapsedSec = (Date.now() - startTime) / 1000;
              const speed = elapsedSec > 0 ? event.loaded / elapsedSec : 0;
              const remainingBytes = event.total - event.loaded;
              const timeRemaining = speed > 0 ? Math.round(remainingBytes / speed) : 0;

              set((state) => ({
                items: state.items.map((i) =>
                  i.id === nextItem.id
                    ? {
                        ...i,
                        progress: percent,
                        bytesUploaded: event.loaded,
                        speedBytesPerSec: speed,
                        timeRemainingSec: timeRemaining,
                        xhr,
                      }
                    : i
                ),
              }));
            }
          };

          xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
              resolve();
            } else {
              try {
                const data = JSON.parse(xhr.responseText);
                reject(new Error(data?.error?.message || data?.message || `Upload failed (${xhr.status})`));
              } catch {
                reject(new Error(`Upload failed with status ${xhr.status}`));
              }
            }
          };

          xhr.onerror = () => reject(new Error('Network error during file upload'));
          xhr.onabort = () => reject(new Error('Upload cancelled'));

          xhr.send(formData);
        });
      } else {
        // Files > 4MB: Presigned Direct R2 Upload
        // 1. Request upload session from backend
        const initData = await apiRequest('/uploads/initiate', {
          method: 'POST',
          body: JSON.stringify({
            filename: nextItem.name,
            mimeType: nextItem.mimeType,
            size: nextItem.size,
            folderId: nextItem.folderId,
          }),
        });

        const { uploadSessionId, uploadUrl } = initData;

        // 2. Direct upload to Cloudflare R2 via XMLHttpRequest for live byte progress
        await new Promise<void>((resolve, reject) => {
          const xhr = new XMLHttpRequest();
          const startTime = Date.now();

          xhr.open('PUT', uploadUrl, true);
          xhr.setRequestHeader('Content-Type', nextItem.mimeType);

          xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
              const percent = Math.round((event.loaded / event.total) * 100);
              const elapsedSec = (Date.now() - startTime) / 1000;
              const speed = elapsedSec > 0 ? event.loaded / elapsedSec : 0;
              const remainingBytes = event.total - event.loaded;
              const timeRemaining = speed > 0 ? Math.round(remainingBytes / speed) : 0;

              set((state) => ({
                items: state.items.map((i) =>
                  i.id === nextItem.id
                    ? {
                        ...i,
                        progress: percent,
                        bytesUploaded: event.loaded,
                        speedBytesPerSec: speed,
                        timeRemainingSec: timeRemaining,
                        xhr,
                      }
                    : i
                ),
              }));
            }
          };

          xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
              resolve();
            } else {
              reject(new Error(`R2 storage upload failed with status ${xhr.status}`));
            }
          };

          xhr.onerror = () => reject(new Error('Network error during R2 direct transfer'));
          xhr.onabort = () => reject(new Error('Upload cancelled'));

          xhr.send(nextItem.file);
        });

        // 3. Complete and verify with backend
        set((state) => ({
          items: state.items.map((i) =>
            i.id === nextItem.id ? { ...i, status: 'verifying', progress: 99 } : i
          ),
        }));

        await apiRequest('/uploads/complete', {
          method: 'POST',
          body: JSON.stringify({ uploadSessionId }),
        });
      }

      set((state) => ({
        items: state.items.map((i) =>
          i.id === nextItem.id
            ? { ...i, status: 'completed', progress: 100, bytesUploaded: i.size }
            : i
        ),
        activeUploadCount: Math.max(0, state.activeUploadCount - 1),
      }));

      // Notify drive view to refresh immediately
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('file-uploaded'));
      }
    } catch (err: any) {
      set((state) => ({
        items: state.items.map((i) =>
          i.id === nextItem.id
            ? { ...i, status: 'failed', error: err.message || 'Upload failed' }
            : i
        ),
        activeUploadCount: Math.max(0, state.activeUploadCount - 1),
      }));
    } finally {
      // Continue next queued file
      get().processQueue();
    }
  },
}));

