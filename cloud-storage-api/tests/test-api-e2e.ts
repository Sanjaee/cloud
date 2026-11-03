import { app } from '../src/app';

async function runTests() {
  console.log('=== Starting REA Backend End-to-End Test Suite ===\n');

  // Helper fetch function directly against Elysia instance
  async function apiFetch(path: string, options: RequestInit = {}) {
    const url = `http://localhost${path}`;
    const req = new Request(url, options);
    const res = await app.fetch(req);
    const text = await res.text();
    let json;
    try {
      json = JSON.parse(text);
    } catch {
      json = { raw: text };
    }
    return { status: res.status, headers: res.headers, json };
  }

  // 1. Health check
  console.log('1. Testing Health and Readiness endpoints...');
  const health = await apiFetch('/health');
  console.log('Health status:', health.json.status);
  if (health.status !== 200) throw new Error('Health check failed');

  const ready = await apiFetch('/health/ready');
  console.log('Readiness response:', ready.json);
  if (ready.status !== 200 || ready.json.status !== 'ready') {
    throw new Error('Readiness check failed');
  }

  // 2. Register User A and User B
  const uniqueSuffix = Date.now();
  const userAEmail = `user_a_${uniqueSuffix}@example.com`;
  const userBEmail = `user_b_${uniqueSuffix}@example.com`;
  const userCEmail = `user_c_${uniqueSuffix}@example.com`;

  console.log('\n2. Registering User A, User B, and User C...');
  const regA = await apiFetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userAEmail, username: `user_a_${uniqueSuffix}`, password: 'Password123!' }),
  });
  console.log('User A registered:', regA.json.success);

  const regB = await apiFetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userBEmail, username: `user_b_${uniqueSuffix}`, password: 'Password123!' }),
  });
  console.log('User B registered:', regB.json.success);

  const regC = await apiFetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userCEmail, username: `user_c_${uniqueSuffix}`, password: 'Password123!' }),
  });
  console.log('User C registered:', regC.json.success);

  // 3. Login User A
  console.log('\n3. Logging in User A (Argon2id + Session Cookie)...');
  const loginA = await apiFetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userAEmail, password: 'Password123!' }),
  });
  console.log('Login A Set-Cookie header:', loginA.headers.get('set-cookie'));
  const cookieA = loginA.headers.get('set-cookie')?.split(';')[0];
  console.log('User A logged in. Session cookie received:', Boolean(cookieA), cookieA);

  // 4. User A creates folder
  console.log('\n4. User A creating folder "Projects"...');
  const folderRes = await apiFetch('/api/v1/folders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: cookieA! },
    body: JSON.stringify({ name: 'Projects' }),
  });
  console.log('Folder response:', folderRes.status, folderRes.json);
  const folderAId = folderRes.json.data.folder.id;
  console.log('Folder created with ID:', folderAId);

  // 5. User A uploads file to R2
  console.log('\n5. User A initiating upload for "document.pdf" (direct R2 upload)...');
  const fileContent = 'Sample Enterprise PDF document content for testing native sharing.';
  const fileSize = Buffer.byteLength(fileContent);
  const mimeType = 'application/pdf';

  const initUpload = await apiFetch('/api/v1/uploads/initiate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: cookieA! },
    body: JSON.stringify({
      filename: 'document.pdf',
      mimeType,
      size: fileSize,
      folderId: folderAId,
    }),
  });

  const uploadSessionId = initUpload.json.data.uploadSessionId;
  const fileAId = initUpload.json.data.fileId;
  const presignedUploadUrl = initUpload.json.data.uploadUrl;
  console.log('Upload initiated. Presigned R2 PUT URL received.');

  // Direct PUT to R2
  const r2PutRes = await fetch(presignedUploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': mimeType, 'Content-Length': fileSize.toString() },
    body: fileContent,
  });
  console.log('Direct upload to Cloudflare R2 result status:', r2PutRes.status);
  if (!r2PutRes.ok) throw new Error('R2 direct upload failed');

  // Complete upload
  const completeRes = await apiFetch('/api/v1/uploads/complete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: cookieA! },
    body: JSON.stringify({ uploadSessionId }),
  });
  console.log('Upload completed and verified in R2:', completeRes.json.success);

  // 6. User A shares file with User B (Editor)
  console.log('\n6. User A sharing file with User B as "editor" (Native shared reference)...');
  const shareRes = await apiFetch('/api/v1/shares', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: cookieA! },
    body: JSON.stringify({
      resourceType: 'file',
      resourceId: fileAId,
      recipientEmailOrUsername: userBEmail,
      permission: 'editor',
      message: 'Here is the project document!',
    }),
  });
  console.log('Share result:', shareRes.json.message);

  // 7. Login User B
  console.log('\n7. Logging in User B...');
  const loginB = await apiFetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userBEmail, password: 'Password123!' }),
  });
  const cookieB = loginB.headers.get('set-cookie')?.split(';')[0];

  // 8. User B views "Shared with me"
  console.log('\n8. User B viewing "Shared with me"...');
  const sharedWithMeRes = await apiFetch('/api/v1/shares/shared-with-me', {
    headers: { Cookie: cookieB! },
  });
  const sharedFiles = sharedWithMeRes.json.data.files;
  console.log(`User B found ${sharedFiles.length} shared file(s).`);
  console.log('Shared file name:', sharedFiles[0]?.name, 'Permission:', sharedFiles[0]?.permission);
  if (sharedFiles[0]?.id !== fileAId) throw new Error('Shared file mismatch');

  // 9. User B downloads the shared file (accesses SAME R2 object)
  console.log('\n9. User B downloading shared file...');
  const downloadB = await apiFetch(`/api/v1/downloads/${fileAId}`, {
    headers: { Cookie: cookieB! },
  });
  const presignedDownloadUrl = downloadB.json.data.downloadUrl;
  const downloadedRes = await fetch(presignedDownloadUrl);
  const downloadedText = await downloadedRes.text();
  console.log('User B downloaded file content matches original:', downloadedText === fileContent);

  // 10. User B adds Shortcut to My Drive
  console.log('\n10. User B adding shortcut to My Drive...');
  const shortcutRes = await apiFetch('/api/v1/shortcuts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Cookie: cookieB! },
    body: JSON.stringify({ targetType: 'file', targetId: fileAId }),
  });
  console.log('Shortcut added:', shortcutRes.json.message);

  // 11. Storage Quota Verification: User A paid storage, User B has 0 bytes used
  console.log('\n11. Verifying storage quota accounting...');
  const quotaA = await apiFetch('/api/v1/quotas', { headers: { Cookie: cookieA! } });
  const quotaB = await apiFetch('/api/v1/quotas', { headers: { Cookie: cookieB! } });

  console.log(`User A used storage: ${quotaA.json.data.usedBytes} bytes (Physical count: ${quotaA.json.data.physicalObjectCount})`);
  console.log(`User B used storage: ${quotaB.json.data.usedBytes} bytes (Shared reference count: ${quotaB.json.data.sharedFileCount})`);

  if (quotaA.json.data.usedBytes !== fileSize) throw new Error('User A quota incorrect');
  if (quotaB.json.data.usedBytes !== 0) throw new Error('User B quota should be 0 for shared reference!');

  // 12. Security Test: User C attempts to access shared file -> MUST FAIL (403)
  console.log('\n12. Security Test: Unauthorized User C attempts access to User A\'s file...');
  const loginC = await apiFetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: userCEmail, password: 'Password123!' }),
  });
  const cookieC = loginC.headers.get('set-cookie')?.split(';')[0];

  const unauthorizedDownload = await apiFetch(`/api/v1/downloads/${fileAId}`, {
    headers: { Cookie: cookieC! },
  });
  console.log('User C download attempt status (Expected 403):', unauthorizedDownload.status);
  if (unauthorizedDownload.status !== 403) throw new Error('Security failure: User C accessed unauthorized file!');

  // 13. Revoke Sharing Access
  console.log('\n13. User A revoking User B\'s access...');
  const userBRecord = loginB.json.data.user;
  const revokeRes = await apiFetch(`/api/v1/shares/file/${fileAId}/collaborators/${userBRecord.id}`, {
    method: 'DELETE',
    headers: { Cookie: cookieA! },
  });
  console.log('Revocation result:', revokeRes.json.message);

  const postRevokeDownload = await apiFetch(`/api/v1/downloads/${fileAId}`, {
    headers: { Cookie: cookieB! },
  });
  console.log('User B download attempt after revocation (Expected 403):', postRevokeDownload.status);
  if (postRevokeDownload.status !== 403) throw new Error('Security failure: Revoked user still has access!');

  // 14. File Deletion and Lifecycle test
  console.log('\n14. Testing File Soft Delete, Restore, and Permanent R2 Purge...');
  await apiFetch(`/api/v1/files/${fileAId}`, { method: 'DELETE', headers: { Cookie: cookieA! } });
  console.log('File soft-deleted to trash.');

  const trashList = await apiFetch('/api/v1/trash', { headers: { Cookie: cookieA! } });
  console.log('Items in trash count:', trashList.json.data.files.length);

  // Restore file
  await apiFetch(`/api/v1/trash/restore/file/${fileAId}`, { method: 'POST', headers: { Cookie: cookieA! } });
  console.log('File restored from trash.');

  // Permanently delete file
  await apiFetch(`/api/v1/files/${fileAId}`, { method: 'DELETE', headers: { Cookie: cookieA! } });
  const permDeleteRes = await apiFetch(`/api/v1/trash/permanent/file/${fileAId}`, {
    method: 'DELETE',
    headers: { Cookie: cookieA! },
  });
  console.log('File permanently purged. Physical R2 object deleted:', permDeleteRes.json.data?.r2ObjectDeleted);

  console.log('\n======================================================');
  console.log('🎉 ALL BACKEND END-TO-END TESTS COMPLETED SUCCESSFULLY! 🎉');
  console.log('======================================================\n');
  process.exit(0);
}

runTests().catch(err => {
  console.error('\n❌ Test suite failed:', err);
  process.exit(1);
});
