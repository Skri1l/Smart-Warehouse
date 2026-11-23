import { useCallback, useState } from 'react';

/**
 * Run user-triggered API actions, tracking which one is in flight and the last error.
 * @returns {{ run: (key: string, action: () => Promise<unknown>) => Promise<boolean>,
 *   busyKey: string | null, error: Error | null, clearError: () => void }}
 */
export function useMutation() {
  const [busyKey, setBusyKey] = useState(/** @type {string | null} */ (null));
  const [error, setError] = useState(/** @type {Error | null} */ (null));

  const run = useCallback(async (key, action) => {
    setBusyKey(key);
    setError(null);
    try {
      await action();
      return true;
    } catch (err) {
      setError(err);
      return false;
    } finally {
      setBusyKey(null);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);
  return { run, busyKey, error, clearError };
}
