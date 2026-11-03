export type FileCategory =
  | 'image'
  | 'video'
  | 'audio'
  | 'pdf'
  | 'document'
  | 'spreadsheet'
  | 'presentation'
  | 'archive'
  | 'code'
  | 'generic';

export function getFileCategory(mimeType: string, filename: string): FileCategory {
  const mime = (mimeType || '').toLowerCase();
  const ext = filename.split('.').pop()?.toLowerCase() || '';

  // 1. Image
  if (mime.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp', 'ico'].includes(ext)) {
    return 'image';
  }

  // 2. Video
  if (mime.startsWith('video/') || ['mp4', 'webm', 'mov', 'avi', 'mkv', 'flv'].includes(ext)) {
    return 'video';
  }

  // 3. Audio
  if (mime.startsWith('audio/') || ['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac'].includes(ext)) {
    return 'audio';
  }

  // 4. PDF
  if (mime === 'application/pdf' || ext === 'pdf') {
    return 'pdf';
  }

  // 5. Spreadsheet (Check BEFORE document to prevent 'officedocument.spreadsheetml' matching document)
  if (
    ['xls', 'xlsx', 'csv'].includes(ext) ||
    mime.includes('spreadsheet') ||
    mime.includes('excel') ||
    mime.includes('csv')
  ) {
    return 'spreadsheet';
  }

  // 6. Presentation (Check BEFORE document)
  if (
    ['ppt', 'pptx'].includes(ext) ||
    mime.includes('presentation') ||
    mime.includes('powerpoint')
  ) {
    return 'presentation';
  }

  // 7. Word Document
  if (
    ['doc', 'docx', 'odt', 'rtf'].includes(ext) ||
    mime.includes('wordprocessing') ||
    mime.includes('msword')
  ) {
    return 'document';
  }

  // 8. Archives
  if (
    ['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(ext) ||
    mime.includes('zip') ||
    mime.includes('compressed') ||
    mime.includes('tar')
  ) {
    return 'archive';
  }

  // 9. Code & Text (Plain text files that are safe to render as text)
  if (
    [
      'txt', 'md', 'json', 'log', 'xml', 'yaml', 'yml', 'js', 'ts', 'jsx', 'tsx',
      'html', 'css', 'py', 'go', 'rs', 'c', 'cpp', 'java', 'sql', 'sh', 'env'
    ].includes(ext) ||
    mime.startsWith('text/') ||
    mime === 'application/json'
  ) {
    return 'code';
  }

  return 'generic';
}

export function isTextPreviewable(mimeType: string, filename: string): boolean {
  const mime = (mimeType || '').toLowerCase();
  const ext = filename.split('.').pop()?.toLowerCase() || '';

  // Never treat rich office documents or PDFs as plain text
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'zip', 'rar', '7z', 'exe', 'bin'].includes(ext)) {
    return false;
  }

  if (mime.startsWith('text/') && !['text/rtf'].includes(mime)) return true;
  return [
    'txt', 'md', 'json', 'log', 'xml', 'yaml', 'yml', 'js', 'ts', 'jsx', 'tsx',
    'html', 'css', 'py', 'go', 'rs', 'c', 'cpp', 'java', 'sql', 'sh', 'env'
  ].includes(ext);
}

export function isPreviewable(mimeType: string, filename: string): boolean {
  const cat = getFileCategory(mimeType, filename);
  return ['image', 'video', 'audio', 'pdf', 'code', 'spreadsheet', 'document'].includes(cat);
}

