/*
 * Pure stock calculations: no React, no HTTP. Input rows are backend DTOs (src/api/types.js).
 */

/** A product is "low" when its quantity across all shelves is below this. */
export const LOW_STOCK_THRESHOLD = 15;
/** A shelf is "low" when filled below this share of its capacity. */
export const LOW_FILL_RATIO = 0.25;
/** Recognition results below this confidence need manual review (FR-08). */
export const REVIEW_CONFIDENCE = 0.6;

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Whole days from `today` to an ISO `YYYY-MM-DD` date (negative = in the past).
 * @param {string} isoDate
 * @param {Date} [today]
 * @returns {number}
 */
export function daysUntil(isoDate, today = new Date()) {
  const [year, month, day] = isoDate.split('-').map(Number);
  const target = Date.UTC(year, month - 1, day);
  const start = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((target - start) / DAY_MS);
}

/**
 * Fill level of a shelf from quantity and capacity.
 * @param {number} quantity
 * @param {number} capacity
 * @returns {'empty' | 'low' | 'ok' | 'full'}
 */
export function fillLevel(quantity, capacity) {
  if (quantity <= 0) return 'empty';
  if (capacity > 0 && quantity >= capacity) return 'full';
  if (capacity > 0 && quantity / capacity < LOW_FILL_RATIO) return 'low';
  return 'ok';
}

/**
 * Sum quantities per shelf. Expiry is not recorded, so several `shelf-stock` rows for the
 * same shelf are just parts of one count.
 * @param {import('../api/types.js').ShelfStock[]} rows
 * @returns {Map<number, number>} shelfId -> quantity
 */
function quantityByShelf(rows) {
  const totals = new Map();
  rows.forEach((row) => totals.set(row.shelfId, (totals.get(row.shelfId) ?? 0) + row.quantity));
  return totals;
}

/**
 * Stock summary per product: total quantity and the shelves holding it (FR-10).
 * @param {import('../api/types.js').Product[]} products
 * @param {import('../api/types.js').ShelfStock[]} stock
 * @param {import('../api/types.js').Shelf[]} shelves
 */
export function summariseProducts(products, stock, shelves) {
  const shelfById = new Map(shelves.map((shelf) => [shelf.id, shelf]));
  return products.map((product) => {
    const totals = quantityByShelf(stock.filter((row) => row.productId === product.id));
    const quantity = [...totals.values()].reduce((sum, value) => sum + value, 0);
    return {
      product,
      quantity,
      isLow: quantity < LOW_STOCK_THRESHOLD,
      locations: [...totals].map(([shelfId, shelfQuantity]) => ({
        shelfId,
        shelfLabel: shelfById.get(shelfId)?.label ?? `#${shelfId}`,
        quantity: shelfQuantity,
      })),
    };
  });
}

/**
 * Shelves grouped by rack. A shelf holds exactly one product: the camera only counts items
 * and cannot tell products apart. A shelf with more than one product is a setup error and
 * is reported as `mixedProducts` instead of being silently summed.
 * @param {{ racks: import('../api/types.js').Rack[], shelves: import('../api/types.js').Shelf[],
 *   stock: import('../api/types.js').ShelfStock[], products: import('../api/types.js').Product[],
 *   devices: import('../api/types.js').Device[] }} data
 */
export function groupShelvesByRack({ racks, shelves, stock, products, devices }) {
  const productById = new Map(products.map((product) => [product.id, product]));
  return racks.map((rack) => ({
    rack,
    shelves: shelves
      .filter((shelf) => shelf.rackId === rack.id)
      .sort((a, b) => a.label.localeCompare(b.label))
      .map((shelf) => {
        const rows = stock.filter((row) => row.shelfId === shelf.id);
        const productIds = [...new Set(rows.map((row) => row.productId))];
        const quantity = rows.reduce((sum, row) => sum + row.quantity, 0);
        return {
          shelf,
          product: productIds.length === 1 ? (productById.get(productIds[0]) ?? null) : null,
          mixedProducts:
            productIds.length > 1
              ? productIds.map((id) => productById.get(id) ?? { id, name: `#${id}`, sku: '' })
              : [],
          quantity,
          fillRatio: shelf.capacity > 0 ? Math.min(1, quantity / shelf.capacity) : 0,
          level: fillLevel(quantity, shelf.capacity),
          device: devices.find((device) => device.shelfId === shelf.id) ?? null,
        };
      }),
  }));
}

/**
 * Active shelves with nothing on them, as last counted by the camera.
 * @param {import('../api/types.js').ShelfStock[]} stock
 * @param {import('../api/types.js').Shelf[]} shelves
 * @returns {import('../api/types.js').Shelf[]}
 */
export function findEmptyShelves(stock, shelves) {
  const totals = quantityByShelf(stock);
  return shelves.filter((shelf) => shelf.status === 'ACTIVE' && !(totals.get(shelf.id) > 0));
}

/**
 * Units moved in and out per day for the last `days` days, oldest first.
 * @param {import('../api/types.js').StockMovement[]} movements
 * @param {number} days
 * @param {Date} [today]
 * @returns {{ date: string, incoming: number, outgoing: number }[]}
 */
export function movementsPerDay(movements, days, today = new Date()) {
  const buckets = [];
  for (let offset = days - 1; offset >= 0; offset -= 1) {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() - offset);
    const pad = (n) => String(n).padStart(2, '0');
    buckets.push({
      date: `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
      incoming: 0,
      outgoing: 0,
    });
  }
  const byDate = new Map(buckets.map((bucket) => [bucket.date, bucket]));
  movements.forEach((movement) => {
    const bucket = byDate.get(movement.createdAt.slice(0, 10));
    if (!bucket) return;
    if (movement.quantityDelta >= 0) bucket.incoming += movement.quantityDelta;
    else bucket.outgoing += -movement.quantityDelta;
  });
  return buckets;
}

/**
 * Recognition results waiting for a person to confirm or reject (FR-08).
 * @param {import('../api/types.js').RecognitionResult[]} results
 */
export function pendingReview(results) {
  return results.filter((result) => result.status === 'DETECTED');
}

/**
 * Lookup maps by id for rendering names next to ids.
 * @param {{ products: { id: number }[], shelves: { id: number }[] }} snapshot
 */
export function indexById({ products, shelves }) {
  return {
    productById: new Map(products.map((product) => [product.id, product])),
    shelfById: new Map(shelves.map((shelf) => [shelf.id, shelf])),
  };
}

/**
 * Shelves that hold more than one product. The camera cannot tell products apart, so stock
 * counts on these shelves are unreliable until the shelf is cleaned up.
 * @param {import('../api/types.js').ShelfStock[]} stock
 * @param {import('../api/types.js').Shelf[]} shelves
 * @returns {{ shelf: import('../api/types.js').Shelf, productCount: number }[]}
 */
export function findMixedShelves(stock, shelves) {
  return shelves
    .map((shelf) => ({
      shelf,
      productCount: new Set(
        stock.filter((row) => row.shelfId === shelf.id).map((row) => row.productId),
      ).size,
    }))
    .filter((item) => item.productCount > 1);
}
