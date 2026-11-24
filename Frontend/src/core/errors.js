/**
 * Single place where unexpected errors end up: render crashes (ErrorBoundary),
 * uncaught exceptions and unhandled promise rejections. Expected API errors
 * (`ApiError`) are shown next to the action that caused them and are not reported here.
 */

/**
 * Report an unexpected error. Today it goes to the browser console; a remote
 * collector (Sentry or similar) would be called from here as well.
 * @param {unknown} error
 * @param {Record<string, unknown>} [context]
 */
export function reportError(error, context = {}) {
  // eslint-disable-next-line no-console
  console.error('[smart-warehouse]', error, context);
}

/**
 * Install window-level handlers for errors that escape React and async code.
 * @returns {() => void} Removes the handlers (used by tests).
 */
export function installGlobalErrorHandlers() {
  const onError = (event) => reportError(event.error ?? event.message, { source: 'window.error' });
  const onRejection = (event) => {
    // Aborted fetches (page left before the response) are not errors.
    if (event.reason?.name === 'AbortError') return;
    reportError(event.reason, { source: 'unhandledrejection' });
  };
  window.addEventListener('error', onError);
  window.addEventListener('unhandledrejection', onRejection);
  return () => {
    window.removeEventListener('error', onError);
    window.removeEventListener('unhandledrejection', onRejection);
  };
}
