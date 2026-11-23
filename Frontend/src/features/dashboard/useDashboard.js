import { useCallback } from 'react';
import { listStockMovements } from '../../api/stock.js';
import { listRecognitionResults } from '../../api/recognition.js';
import { listRecommendations } from '../../api/recommendations.js';
import { POLL_INTERVAL_MS, useResource } from '../../hooks/useResource.js';
import { hasRole, useAuth } from '../../hooks/useAuth.jsx';
import { loadStockSnapshot } from '../../api/snapshot.js';
import {
  findMixedShelves,
  indexById,
  movementsPerDay,
  pendingReview,
  summariseProducts,
} from '../../core/stock.js';

const RECENT_MOVEMENTS = 8;
const CHART_DAYS = 7;

/** Dashboard data (FR-17): KPIs, low stock, recent movements, review queue, recommendations. */
export function useDashboard() {
  const { user } = useAuth();
  const isManager = hasRole(user, ['MANAGER', 'ADMIN']);

  const loader = useCallback(async () => {
    const [snapshot, movements, recognition, recommendations] = await Promise.all([
      loadStockSnapshot(),
      listStockMovements(),
      listRecognitionResults(),
      isManager ? listRecommendations() : Promise.resolve([]),
    ]);
    return { snapshot, movements, recognition, recommendations };
  }, [isManager]);

  const { data, error, loading, reload } = useResource(loader, { pollMs: POLL_INTERVAL_MS });
  if (!data) return { data: null, error, loading, reload, isManager };

  const { snapshot, movements, recognition, recommendations } = data;
  const { productById, shelfById } = indexById(snapshot);
  const summaries = summariseProducts(snapshot.products, snapshot.stock, snapshot.shelves);
  const pending = pendingReview(recognition);

  return {
    loading,
    error,
    reload,
    isManager,
    data: {
      totalUnits: summaries.reduce((sum, item) => sum + item.quantity, 0),
      productCount: snapshot.products.length,
      lowStock: summaries.filter((item) => item.isLow).sort((a, b) => a.quantity - b.quantity),
      expiringSoon: summaries.filter((item) => item.expiresSoon || item.expiredQuantity > 0),
      pendingReviewCount: pending.length,
      mixedShelves: findMixedShelves(snapshot.stock, snapshot.shelves),
      recognitionErrors: recognition.filter((item) => item.status === 'FAILED').length,
      recentMovements: [...movements]
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        .slice(0, RECENT_MOVEMENTS)
        .map((movement) => ({
          ...movement,
          productName: productById.get(movement.productId)?.name ?? `#${movement.productId}`,
          shelfLabel: shelfById.get(movement.shelfId)?.label ?? `#${movement.shelfId}`,
        })),
      chart: movementsPerDay(movements, CHART_DAYS),
      newRecommendations: recommendations
        .filter((item) => item.status === 'NEW')
        .map((item) => ({ ...item, productName: productById.get(item.productId)?.name ?? '' })),
    },
  };
}
