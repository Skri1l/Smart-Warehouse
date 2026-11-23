import { describe, expect, it, vi } from 'vitest';
import { HttpResponse, http as mswHttp } from 'msw';
import { server } from '../../tests/server.js';
import { ApiError, onUnauthorized, request } from './http.js';

describe('request', () => {
  it('shows the backend message and code in ApiError when the response is not ok', async () => {
    server.use(
      mswHttp.get('/api/v1/boom', () =>
        HttpResponse.json(
          { status: 409, error: 'CONFLICT', message: 'Already exists' },
          { status: 409 },
        ),
      ),
    );
    const error = await request('GET', '/boom').catch((err) => err);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 409, code: 'CONFLICT', message: 'Already exists' });
  });

  it('notifies listeners when the session has expired (401)', async () => {
    const listener = vi.fn();
    const unsubscribe = onUnauthorized(listener);
    await request('GET', '/products').catch(() => {});
    unsubscribe();
    expect(listener).toHaveBeenCalledOnce();
  });
});
