import { useCallback } from 'react';
import { listStockMovements } from '../../api/stock.js';
import { listProducts } from '../../api/products.js';
import { listShelves } from '../../api/warehouses.js';
import { POLL_INTERVAL_MS, useResource } from '../../hooks/useResource.js';
import { indexById } from '../../core/stock.js';

/**
 * Stock movements with product and shelf names, newest first, optionally filtered.
 * @param {{ type: string, source: string }} filters Empty string = no filter.
 */
export function useMovements({ type, source }) {
  const loader = useCallback(async () => {
    const [movements, products, shelves] = await Promise.all([
      listStockMovements(),
      listProducts(),
      listShelves(),
    ]);
    return { movements, products, shelves };
  }, []);
  const { data, error, loading, reload } = useResource(loader, { pollMs: POLL_INTERVAL_MS });
  if (!data) return { rows: [], error, loading, reload };
  const { productById, shelfById } = indexById(data);
  const rows = data.movements
    .filter((row) => (!type || row.type === type) && (!source || row.source === source))
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .map((row) => ({
      ...row,
      productName: productById.get(row.productId)?.name ?? `#${row.productId}`,
      shelfLabel: shelfById.get(row.shelfId)?.label ?? `#${row.shelfId}`,
    }));
  return { rows, error: null, loading: false, reload };
}
