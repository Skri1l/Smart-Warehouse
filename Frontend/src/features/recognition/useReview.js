import { useCallback } from 'react';
import {
  confirmRecognition,
  listImageEvents,
  listRecognitionResults,
  rejectRecognition,
} from '../../api/recognition.js';
import { listProducts } from '../../api/products.js';
import { listShelves } from '../../api/warehouses.js';
import { POLL_INTERVAL_MS, useResource } from '../../hooks/useResource.js';
import { useMutation } from '../../hooks/useMutation.js';
import { indexById } from '../../core/stock.js';

const HISTORY_SIZE = 30;

/** Recognition results waiting for review (FR-08) plus recent automatic decisions. */
export function useReview() {
  const loader = useCallback(async () => {
    const [results, events, products, shelves] = await Promise.all([
      listRecognitionResults(),
      listImageEvents(),
      listProducts(),
      listShelves(),
    ]);
    return { results, events, products, shelves };
  }, []);
  const { data, error, loading, reload } = useResource(loader, { pollMs: POLL_INTERVAL_MS });
  const mutation = useMutation();

  const decide = async (id, accept) => {
    const ok = await mutation.run(`${id}`, () =>
      accept ? confirmRecognition(id) : rejectRecognition(id),
    );
    if (ok) await reload();
  };

  let pending = [];
  let history = [];
  if (data) {
    const { productById, shelfById } = indexById(data);
    const eventById = new Map(data.events.map((event) => [event.id, event]));
    const rows = data.results
      .map((result) => {
        const event = eventById.get(result.imageEventId);
        return {
          ...result,
          productName: productById.get(result.productId)?.name ?? `#${result.productId}`,
          shelfLabel: event ? (shelfById.get(event.shelfId)?.label ?? `#${event.shelfId}`) : '—',
          capturedAt: event?.capturedAt ?? null,
        };
      })
      .sort((a, b) => (b.capturedAt ?? '').localeCompare(a.capturedAt ?? ''));
    pending = rows.filter((row) => row.status === 'DETECTED');
    history = rows.filter((row) => row.status !== 'DETECTED').slice(0, HISTORY_SIZE);
  }

  return {
    pending,
    history,
    error: data ? null : error,
    loading: loading && !data,
    reload,
    decide,
    busyId: mutation.busyKey,
    actionError: mutation.error,
  };
}
