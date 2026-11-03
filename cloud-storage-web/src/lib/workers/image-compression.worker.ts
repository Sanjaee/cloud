// Web Worker for client-side non-blocking image compression

export interface CompressionRequest {
  id: string;
  file: File;
  mode: 'original' | 'balanced' | 'high';
}

export interface CompressionResponse {
  id: string;
  compressedBlob: Blob;
  originalSize: number;
  compressedSize: number;
  savingsPercentage: number;
  isCompressed: boolean;
  mimeType: string;
}

self.onmessage = async (e: MessageEvent<CompressionRequest>) => {
  const { id, file, mode } = e.data;

  // If original mode or non-image, return file directly
  if (mode === 'original' || !file.type.startsWith('image/') || file.type === 'image/svg+xml') {
    self.postMessage({
      id,
      compressedBlob: file,
      originalSize: file.size,
      compressedSize: file.size,
      savingsPercentage: 0,
      isCompressed: false,
      mimeType: file.type,
    } as CompressionResponse);
    return;
  }

  try {
    const bitmap = await createImageBitmap(file);
    const { width, height } = bitmap;

    let targetWidth = width;
    let targetHeight = height;
    let quality = 0.82;

    if (mode === 'high') {
      quality = 0.65;
      const maxDim = 1920;
      if (width > maxDim || height > maxDim) {
        if (width > height) {
          targetWidth = maxDim;
          targetHeight = Math.round((height * maxDim) / width);
        } else {
          targetHeight = maxDim;
          targetWidth = Math.round((width * maxDim) / height);
        }
      }
    } else if (mode === 'balanced') {
      quality = 0.82;
      const maxDim = 2560;
      if (width > maxDim || height > maxDim) {
        if (width > height) {
          targetWidth = maxDim;
          targetHeight = Math.round((height * maxDim) / width);
        } else {
          targetHeight = maxDim;
          targetWidth = Math.round((width * maxDim) / height);
        }
      }
    }

    // Use OffscreenCanvas in Web Worker
    const offscreen = new OffscreenCanvas(targetWidth, targetHeight);
    const ctx = offscreen.getContext('2d');
    if (!ctx) throw new Error('Canvas context unavailable');

    ctx.drawImage(bitmap, 0, 0, targetWidth, targetHeight);
    bitmap.close();

    // Convert to modern WebP
    const compressedBlob = await offscreen.convertToBlob({
      type: 'image/webp',
      quality,
    });

    // Fallback: If compressed blob is larger than original, keep original
    if (compressedBlob.size >= file.size) {
      self.postMessage({
        id,
        compressedBlob: file,
        originalSize: file.size,
        compressedSize: file.size,
        savingsPercentage: 0,
        isCompressed: false,
        mimeType: file.type,
      } as CompressionResponse);
      return;
    }

    const savingsPercentage = Math.round(
      ((file.size - compressedBlob.size) / file.size) * 100
    );

    self.postMessage({
      id,
      compressedBlob,
      originalSize: file.size,
      compressedSize: compressedBlob.size,
      savingsPercentage,
      isCompressed: true,
      mimeType: 'image/webp',
    } as CompressionResponse);
  } catch (error) {
    // Error fallback to original file
    self.postMessage({
      id,
      compressedBlob: file,
      originalSize: file.size,
      compressedSize: file.size,
      savingsPercentage: 0,
      isCompressed: false,
      mimeType: file.type,
    } as CompressionResponse);
  }
};

