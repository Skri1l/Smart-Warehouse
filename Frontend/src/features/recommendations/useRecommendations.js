import { useCallback } from 'react';
import {
  approveRecommendation,
  listRecommendations,
  rejectRecommendation,
  updateRecommendation,
} from '../../api/recommendations.js';
import { listProducts } from '../../api/products.js';
import { useResource } from '../../hooks/useResource.js';
import { useMutation } from '../../hooks/useMutation.js';

const STATUS_ORDER = { NEW: 0, APPROVED: 1, ORDERED: 2, COMPLETED: 3, REJECTED: 4 };

/** Purchase recommendations with approve / reject / modify actions (FR-15, FR-16). */
export function useRecommendations() {
  const loader = useCallback(async () => {
    const [recommendations, products] = await Promise.all([listRecommendations(), listProducts()]);
    return { recommendations, products };
  }, []);
  const { data, error, loading, reload } = useResource(loader);
  const mutation = useMutation();

  const rows = data
    ? data.recommendations
        .map((item) => {
          const product = data.products.find((p) => p.id === item.productId);
          return { ...item, productName: product?.name ?? `#${item.productId}`, sku: product?.sku };
        })
        .sort(
          (a, b) =>
            STATUS_ORDER[a.status] - STATUS_ORDER[b.status] ||
            a.requiredBy.localeCompare(b.requiredBy),
        )
    : [];

  const act = async (key, action) => {
    const ok = await mutation.run(key, action);
    if (ok) await reload();
    return ok;
  };

  return {
    rows,
    error: data ? null : error,
    loading: loading && !data,
    reload,
    busyKey: mutation.busyKey,
    actionError: mutation.error,
    clearActionError: mutation.clearError,
    approve: (id) => act(`approve-${id}`, () => approveRecommendation(id)),
    reject: (id) => act(`reject-${id}`, () => rejectRecommendation(id)),
    /** @param {object} row @param {{ recommendedQuantity: number, requiredBy: string, reason: string }} changes */
    modify: (row, changes) =>
      act(`edit-${row.id}`, () =>
        updateRecommendation(row.id, { productId: row.productId, ...changes }),
      ),
  };
}
