const API_BASE = '/api/v1';

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public details?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function apiRequest<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_BASE}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const defaultHeaders: Record<string, string> = {
    'Accept': 'application/json',
  };

  if (!(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  const res = await fetch(url, {
    ...options,
    credentials: 'include', // Sends HttpOnly session cookie
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  });

  const contentType = res.headers.get('content-type') || '';
  let data: any;

  if (contentType.includes('application/json')) {
    data = await res.json();
  } else {
    data = await res.text();
  }

  if (!res.ok) {
    const errorMsg = data?.error?.message || data?.message || res.statusText || 'Request failed';
    const errorCode = data?.error?.code || 'API_ERROR';
    throw new ApiError(res.status, errorCode, errorMsg, data?.error?.details);
  }

  return data?.data !== undefined ? data.data : data;
}

