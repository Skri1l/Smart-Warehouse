/*
 * Pure stock calculations: no React, no HTTP. Input rows are backend DTOs (src/api/types.js).
 */

/** A product is "low" when its usable quantity across all shelves is below this. */
export const LOW_STOCK_THRESHOLD = 15;
/** A shelf is "low" when filled below this share of its capacity. */
export const LOW_FILL_RATIO = 0.25;
/** Stock expiring within this many days is flagged. */
export const EXPIRY_WARNING_DAYS = 7;
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
 * Expiry state of a shelf-stock row.
 * @param {{ expirationDate: string, status: string }} row
 * @param {Date} [today]
 * @returns {'expired' | 'soon' | 'ok'}
 */
export function expiryState(row, today = new Date()) {
  const days = daysUntil(row.expirationDate, today);
  if (row.status === 'EXPIRED' || days < 0) return 'expired';
  if (days <= EXPIRY_WARNING_DAYS) return 'soon';
  return 'ok';
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
 * Stock summary per product: usable quantity (expired stock excluded), locations, nearest expiry.
 * @param {import('../api/types.js').Product[]} products
 * @param {import('../api/types.js').ShelfStock[]} stock
 * @param {import('../api/types.js').Shelf[]} shelves
 * @param {Date} [today]
 */
export function summariseProducts(products, stock, shelves, today = new Date()) {
  const shelfById = new Map(shelves.map((shelf) => [shelf.id, shelf]));
  return products.map((product) => {
    const rows = stock.filter((row) => row.productId === product.id);
    const usable = rows.filter((row) => expiryState(row, today) !== 'expired');
    const quantity = usable.reduce((sum, row) => sum + row.quantity, 0);
    const expiredQuantity = rows
      .filter((row) => expiryState(row, today) === 'expired')
      .reduce((sum, row) => sum + row.quantity, 0);
    const nearestExpiry = usable
      .map((row) => row.expirationDate)
      .sort()
      .at(0);
    return {
      product,
      quantity,
      expiredQuantity,
      isLow: quantity < LOW_STOCK_THRESHOLD,
      nearestExpiry: nearestExpiry ?? null,
      expiresSoon: nearestExpiry ? daysUntil(nearestExpiry, today) <= EXPIRY_WARNING_DAYS : false,
      locations: rows.map((row) => ({
        stockId: row.id,
        shelfLabel: shelfById.get(row.shelfId)?.label ?? `#${row.shelfId}`,
        quantity: row.quantity,
        expirationDate: row.expirationDate,
        expiry: expiryState(row, today),
      })),
    };
  });
}

/**
 * Shelves grouped by rack with their contents and fill level.
 * @param {{ racks: import('../api/types.js').Rack[], shelves: import('../api/types.js').Shelf[],
 *   stock: import('../api/types.js').ShelfStock[], products: import('../api/types.js').Product[],
 *   devices: import('../api/types.js').Device[] }} data
 * @param {Date} [today]
 */
export function groupShelvesByRack(
  { racks, shelves, stock, products, devices },
  today = new Date(),
) {
  const productById = new Map(products.map((product) => [product.id, product]));
  return racks.map((rack) => ({
    rack,
    shelves: shelves
      .filter((shelf) => shelf.rackId === rack.id)
      .sort((a, b) => a.label.localeCompare(b.label))
      .map((shelf) => {
        const items = stock
          .filter((row) => row.shelfId === shelf.id)
          .map((row) => ({
            stockId: row.id,
            product: productById.get(row.productId),
            quantity: row.quantity,
            expirationDate: row.expirationDate,
            expiry: expiryState(row, today),
          }));
        const quantity = items.reduce((sum, item) => sum + item.quantity, 0);
        return {
          shelf,
          items,
          quantity,
          fillRatio: shelf.capacity > 0 ? Math.min(1, quantity / shelf.capacity) : 0,
          level: fillLevel(quantity, shelf.capacity),
          device: devices.find((device) => device.shelfId === shelf.id) ?? null,
        };
      }),
  }));
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
