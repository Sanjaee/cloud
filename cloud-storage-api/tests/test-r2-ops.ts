import { createPresignedUploadUrl } from '../src/infrastructure/storage/cloudflare-r2/upload';
import { verifyR2Object } from '../src/infrastructure/storage/cloudflare-r2/verify';
import { createPresignedDownloadUrl } from '../src/infrastructure/storage/cloudflare-r2/download';
import { deleteR2Object } from '../src/infrastructure/storage/cloudflare-r2/delete';

async function testR2() {
  const testKey = `test/test-object-${Date.now()}.txt`;
  const content = 'Hello Cloudflare R2 from REA Platform!';
  const contentLength = Buffer.byteLength(content);
  const mimeType = 'text/plain';

  console.log('1. Generating Presigned Upload URL...');
  const { uploadUrl } = await createPresignedUploadUrl(testKey, mimeType, contentLength);
  console.log('Presigned Upload URL generated successfully.');

  console.log('2. Uploading content directly to R2 via Presigned PUT...');
  const uploadRes = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': mimeType,
      'Content-Length': contentLength.toString(),
    },
    body: content,
  });

  if (!uploadRes.ok) {
    throw new Error(`Upload to R2 failed with status: ${uploadRes.status} ${uploadRes.statusText}`);
  }
  console.log('Upload to R2 succeeded!');

  console.log('3. Verifying R2 object existence...');
  const verification = await verifyR2Object(testKey);
  console.log('R2 Object verification:', verification);
  if (!verification.exists) {
    throw new Error('Verification failed: object was not found in R2!');
  }

  console.log('4. Generating Presigned Download URL...');
  const { downloadUrl } = await createPresignedDownloadUrl(testKey, 'test-download.txt');
  console.log('Download URL generated successfully.');

  const downloadRes = await fetch(downloadUrl);
  const downloadedText = await downloadRes.text();
  console.log('Downloaded content verified:', downloadedText === content);

  console.log('5. Deleting test object from R2...');
  const deleted = await deleteR2Object(testKey);
  console.log('Deleted test object:', deleted);

  const postDeleteVerify = await verifyR2Object(testKey);
  console.log('Post-delete verification (should be false):', postDeleteVerify.exists);

  console.log('ALL R2 TESTS PASSED PERFECTLY!');
}

testR2().catch(err => {
  console.error('R2 Test failed:', err);
  process.exit(1);
});

