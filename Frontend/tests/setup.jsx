import '@testing-library/jest-dom/vitest';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import { server } from './server.js';
import { resetDb } from '../src/mocks/db.js';

// jsdom has no canvas: render charts as a plain placeholder in tests.
vi.mock('react-chartjs-2', () => ({
  Bar: (props) => <div role="img" aria-label={props['aria-label']} />,
}));

if (!window.matchMedia) {
  window.matchMedia = (query) => ({
    matches: false,
    media: query,
    addEventListener() {},
    removeEventListener() {},
  });
}

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetDb();
  document.cookie = 'sw_session=; Max-Age=0; path=/';
});
afterAll(() => server.close());
