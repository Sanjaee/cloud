import { Elysia } from 'elysia';

export const requestId = new Elysia({ name: 'middleware:request-id' })
  .onRequest(({ request, set }) => {
    const id = request.headers.get('x-request-id') || crypto.randomUUID();
    set.headers['x-request-id'] = id;
  });

