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
  const mime = mimeType.toLowerCase();
  const ext = filename.split('.').pop()?.toLowerCase() || '';

  if (mime.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) {
    return 'image';
  }
  if (mime.startsWith('video/') || ['mp4', 'webm', 'mov', 'avi', 'mkv'].includes(ext)) {
    return 'video';
  }
  if (mime.startsWith('audio/') || ['mp3', 'wav', 'ogg', 'm4a', 'flac'].includes(ext)) {
    return 'audio';
  }
  if (mime === 'application/pdf' || ext === 'pdf') {
    return 'pdf';
  }
  if (
    mime.includes('word') ||
    mime.includes('document') ||
    ['doc', 'docx', 'txt', 'rtf', 'md'].includes(ext)
  ) {
    return 'document';
  }
  if (
    mime.includes('sheet') ||
    mime.includes('excel') ||
    mime.includes('csv') ||
    ['xls', 'xlsx', 'csv'].includes(ext)
  ) {
    return 'spreadsheet';
  }
  if (
    mime.includes('presentation') ||
    mime.includes('powerpoint') ||
    ['ppt', 'pptx'].includes(ext)
  ) {
    return 'presentation';
  }
  if (
    mime.includes('zip') ||
    mime.includes('compressed') ||
    mime.includes('tar') ||
    ['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)
  ) {
    return 'archive';
  }
  if (
    ['json', 'js', 'ts', 'jsx', 'tsx', 'html', 'css', 'py', 'go', 'rs', 'c', 'cpp', 'java'].includes(ext)
  ) {
    return 'code';
  }

  return 'generic';
}

export function isPreviewable(mimeType: string, filename: string): boolean {
  const cat = getFileCategory(mimeType, filename);
  return ['image', 'video', 'audio', 'pdf', 'document', 'code'].includes(cat);
}

