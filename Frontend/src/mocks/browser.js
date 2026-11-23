import { setupWorker } from 'msw/browser';
import { handlers, setMockLatency } from './handlers.js';
import { simulateCameraUpload } from './db.js';

/** How often a simulated shelf camera "uploads" a photo while the mock runs. */
const CAMERA_INTERVAL_MS = 15000;

/**
 * Start the in-browser mock backend. Only imported when `VITE_API_MOCK=true`,
 * so MSW is never part of the production bundle.
 */
export async function startMockBackend() {
  setMockLatency(200);
  const worker = setupWorker(...handlers);
  await worker.start({
    onUnhandledRequest: ({ url }, print) => {
      if (new URL(url).pathname.startsWith('/api/')) print.warning();
    },
  });
  window.setInterval(() => {
    if (!document.hidden) simulateCameraUpload();
  }, CAMERA_INTERVAL_MS);
}
