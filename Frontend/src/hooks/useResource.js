import { useCallback, useEffect, useRef, useState } from 'react';

/** Default polling interval for live data (CODE_STYLE_FRONTEND: >= 10 s). */
export const POLL_INTERVAL_MS = 15000;

/**
 * Load async data with explicit loading / error / success state, optional polling that
 * pauses while the tab is hidden, and a `reload` for after mutations.
 *
 * @template T
 * @param {() => Promise<T>} loader Must be stable (module function or `useCallback`).
 * @param {{ pollMs?: number }} [options]
 * @returns {{ data: T | undefined, error: Error | null, loading: boolean, reload: () => Promise<void> }}
 */
export function useResource(loader, { pollMs } = {}) {
  const [data, setData] = useState(/** @type {T | undefined} */ (undefined));
  const [error, setError] = useState(/** @type {Error | null} */ (null));
  const [loading, setLoading] = useState(true);
  const mounted = useRef(true);

  const reload = useCallback(async () => {
    try {
      const result = await loader();
      if (!mounted.current) return;
      setData(result);
      setError(null);
    } catch (err) {
      if (mounted.current) setError(err);
    } finally {
      if (mounted.current) setLoading(false);
    }
  }, [loader]);

  useEffect(() => {
    mounted.current = true;
    setLoading(true);
    reload();
    return () => {
      mounted.current = false;
    };
  }, [reload]);

  useEffect(() => {
    if (!pollMs) return undefined;
    const timer = window.setInterval(() => {
      if (!document.hidden) reload();
    }, pollMs);
    const handleVisibility = () => {
      if (!document.hidden) reload();
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, [pollMs, reload]);

  return { data, error, loading, reload };
}
