import { useCallback } from 'react';
import { loadStockSnapshot } from '../../api/snapshot.js';
import { getProduct } from '../../api/products.js';
import { listMovementsByProduct, listStockByProduct } from '../../api/stock.js';
import { listShelves } from '../../api/warehouses.js';
import { POLL_INTERVAL_MS, useResource } from '../../hooks/useResource.js';
import { summariseProducts } from '../../core/stock.js';

/**
 * @typedef {'all' | 'low'} ProductFilter
 */

/**
 * Product list with stock summary, filtered by text and status.
 * @param {{ query: string, filter: ProductFilter }} criteria
 */
export function useProducts({ query, filter }) {
  const { data, error, loading, reload } = useResource(loadStockSnapshot, {
    pollMs: POLL_INTERVAL_MS,
  });
  const summaries = data ? summariseProducts(data.products, data.stock, data.shelves) : [];
  const text = query.trim().toLowerCase();
  const rows = summaries.filter((item) => {
    if (filter === 'low' && !item.isLow) return false;
    if (!text) return true;
    return (
      item.product.name.toLowerCase().includes(text) ||
      item.product.sku.toLowerCase().includes(text)
    );
  });
  return { rows, total: summaries.length, error, loading: loading && !data, reload };
}

/**
 * One product with its shelf locations (FR-10) and movement history (FR-09).
 * @param {number} productId
 */
export function useProductDetails(productId) {
  const loader = useCallback(async () => {
    const [product, stock, movements, shelves] = await Promise.all([
      getProduct(productId),
      listStockByProduct(productId),
      listMovementsByProduct(productId),
      listShelves(),
    ]);
    const [summary] = summariseProducts([product], stock, shelves);
    const shelfById = new Map(shelves.map((shelf) => [shelf.id, shelf]));
    return {
      summary,
      movements: [...movements]
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        .map((movement) => ({
          ...movement,
          shelfLabel: shelfById.get(movement.shelfId)?.label ?? `#${movement.shelfId}`,
        })),
    };
  }, [productId]);
  return useResource(loader, { pollMs: POLL_INTERVAL_MS });
}
