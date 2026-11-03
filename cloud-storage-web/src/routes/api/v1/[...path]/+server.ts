import type { RequestHandler } from './$types';

const API_BACKEND = 'https://cloud-storage-api-tau.vercel.app';

export const fallback: RequestHandler = async ({ request, params, url }) => {
  const targetUrl = `${API_BACKEND}/api/v1/${params.path || ''}${url.search}`;

  const headers = new Headers(request.headers);
  headers.delete('host');

  let body: Blob | undefined;
  if (!['GET', 'HEAD'].includes(request.method)) {
    try {
      body = await request.blob();
    } catch {
      body = undefined;
    }
  }

  const response = await fetch(targetUrl, {
    method: request.method,
    headers,
    body,
    redirect: 'manual',
  });

  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
  });
};
