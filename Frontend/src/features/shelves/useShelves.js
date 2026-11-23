import { useCallback } from 'react';
import { loadStockSnapshot } from '../../api/snapshot.js';
import { listWarehouses } from '../../api/warehouses.js';
import { POLL_INTERVAL_MS, useResource } from '../../hooks/useResource.js';
import { groupShelvesByRack } from '../../core/stock.js';

/** Racks with their shelves, contents, fill level and camera, refreshed by polling. */
export function useShelves() {
  const loader = useCallback(async () => {
    const [snapshot, warehouses] = await Promise.all([loadStockSnapshot(), listWarehouses()]);
    return { snapshot, warehouses };
  }, []);
  const { data, error, loading, reload } = useResource(loader, { pollMs: POLL_INTERVAL_MS });
  return {
    racks: data ? groupShelvesByRack(data.snapshot) : [],
    warehouse: data?.warehouses[0] ?? null,
    error: data ? null : error,
    loading: loading && !data,
    reload,
  };
}
