/*
 * In-memory database behind the mocked backend. Row shapes match the backend Response DTOs
 * one to one (see src/api/types.js). State lives in memory and resets on page reload.
 */

/** Demo accounts for the mock (documented in Frontend/README.md). Password = username. */
export const MOCK_USERS = [
  { id: 1, username: 'worker', email: 'worker@demo.local', role: 'WORKER', status: 'ACTIVE' },
  { id: 2, username: 'manager', email: 'manager@demo.local', role: 'MANAGER', status: 'ACTIVE' },
  { id: 3, username: 'admin', email: 'admin@demo.local', role: 'ADMIN', status: 'ACTIVE' },
];

const DAY = 24 * 60 * 60 * 1000;

/** Small deterministic PRNG so the seed looks the same on every load. */
function createRandom(seed) {
  let state = seed;
  return () => {
    state = (state * 1664525 + 1013904223) % 4294967296;
    return state / 4294967296;
  };
}

/**
 * Format a Date the way Jackson serialises `LocalDateTime` (no zone).
 * @param {Date} date
 * @returns {string}
 */
export function toLocalDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  );
}

/** @param {Date} date @returns {string} `LocalDate` string. */
export function toLocalDate(date) {
  return toLocalDateTime(date).slice(0, 10);
}

function seed() {
  const random = createRandom(42);
  const now = Date.now();

  const warehouses = [{ id: 1, name: 'Main warehouse', address: 'Minsk, Industrialnaya 12' }];
  const racks = [
    { id: 1, label: 'A', warehouseId: 1 },
    { id: 2, label: 'B', warehouseId: 1 },
    { id: 3, label: 'C', warehouseId: 1 },
  ];
  const shelves = [
    { id: 1, label: 'A-1', capacity: 60, status: 'ACTIVE', rackId: 1 },
    { id: 2, label: 'A-2', capacity: 60, status: 'ACTIVE', rackId: 1 },
    { id: 3, label: 'A-3', capacity: 40, status: 'ACTIVE', rackId: 1 },
    { id: 4, label: 'B-1', capacity: 80, status: 'ACTIVE', rackId: 2 },
    { id: 5, label: 'B-2', capacity: 80, status: 'ACTIVE', rackId: 2 },
    { id: 6, label: 'B-3', capacity: 50, status: 'INACTIVE', rackId: 2 },
    { id: 7, label: 'C-1', capacity: 100, status: 'ACTIVE', rackId: 3 },
    { id: 8, label: 'C-2', capacity: 100, status: 'ACTIVE', rackId: 3 },
  ];
  const products = [
    { id: 1, sku: 'SKU-1001', name: 'Milk 1L', description: 'Pasteurised milk, 3.2 %' },
    { id: 2, sku: 'SKU-1002', name: 'Butter 200g', description: 'Unsalted butter' },
    { id: 3, sku: 'SKU-2001', name: 'Rice 1kg', description: 'Long grain rice' },
    { id: 4, sku: 'SKU-2002', name: 'Pasta 500g', description: 'Durum wheat spaghetti' },
    { id: 5, sku: 'SKU-3001', name: 'Apple juice 1L', description: null },
    { id: 6, sku: 'SKU-3002', name: 'Mineral water 1.5L', description: 'Sparkling' },
    { id: 7, sku: 'SKU-4001', name: 'Canned tuna 185g', description: 'In olive oil' },
    { id: 8, sku: 'SKU-4002', name: 'Tomato paste 140g', description: null },
  ];
  const day = (offset) => toLocalDate(new Date(now + offset * DAY));
  const shelfStock = [
    { id: 1, shelfId: 1, productId: 1, quantity: 38, expirationDate: day(6), status: 'ACTIVE' },
    { id: 2, shelfId: 1, productId: 2, quantity: 6, expirationDate: day(20), status: 'ACTIVE' },
    { id: 3, shelfId: 2, productId: 5, quantity: 44, expirationDate: day(120), status: 'ACTIVE' },
    { id: 4, shelfId: 3, productId: 1, quantity: 3, expirationDate: day(-1), status: 'EXPIRED' },
    { id: 5, shelfId: 4, productId: 3, quantity: 72, expirationDate: day(300), status: 'ACTIVE' },
    { id: 6, shelfId: 5, productId: 4, quantity: 11, expirationDate: day(240), status: 'ACTIVE' },
    { id: 7, shelfId: 5, productId: 8, quantity: 9, expirationDate: day(180), status: 'ACTIVE' },
    { id: 8, shelfId: 7, productId: 6, quantity: 85, expirationDate: day(365), status: 'ACTIVE' },
    { id: 9, shelfId: 8, productId: 7, quantity: 24, expirationDate: day(500), status: 'ACTIVE' },
    { id: 10, shelfId: 8, productId: 3, quantity: 18, expirationDate: day(280), status: 'ACTIVE' },
  ];
  const devices = shelves
    .filter((shelf) => shelf.id !== 6)
    .map((shelf, index) => ({
      id: index + 1,
      deviceKey: `shelf-${shelf.label.toLowerCase()}-cam`,
      status: shelf.id === 3 ? 'INACTIVE' : 'ACTIVE',
      lastSeenAt: toLocalDateTime(new Date(now - (shelf.id === 3 ? 3 * DAY : random() * 600000))),
      shelfId: shelf.id,
    }));

  const types = ['REMOVAL', 'REMOVAL', 'REMOVAL', 'RECEIPT', 'ADJUSTMENT', 'WRITE_OFF'];
  const stockMovements = [];
  for (let i = 0; i < 60; i += 1) {
    const stock = shelfStock[Math.floor(random() * shelfStock.length)];
    const type = types[Math.floor(random() * types.length)];
    const amount = 1 + Math.floor(random() * (type === 'RECEIPT' ? 30 : 6));
    stockMovements.push({
      id: i + 1,
      productId: stock.productId,
      shelfId: stock.shelfId,
      quantityDelta: type === 'RECEIPT' ? amount : -amount,
      type,
      source: type === 'REMOVAL' ? 'CAMERA' : type === 'RECEIPT' ? 'MANUAL' : 'SYSTEM',
      createdAt: toLocalDateTime(new Date(now - random() * 7 * DAY)),
    });
  }
  stockMovements.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  stockMovements.forEach((movement, index) => {
    movement.id = index + 1;
  });

  const imageEvents = [];
  const recognitionResults = [];
  const recognitionSeed = [
    { stockId: 1, delta: -2, confidence: 0.94, status: 'CONFIRMED' },
    { stockId: 5, delta: -4, confidence: 0.91, status: 'CONFIRMED' },
    { stockId: 6, delta: -3, confidence: 0.47, status: 'DETECTED' },
    { stockId: 9, delta: 12, confidence: 0.38, status: 'DETECTED' },
    { stockId: 2, delta: -1, confidence: 0.55, status: 'DETECTED' },
    { stockId: 8, delta: 0, confidence: 0.12, status: 'FAILED' },
    { stockId: 3, delta: -5, confidence: 0.62, status: 'REJECTED' },
  ];
  recognitionSeed.forEach((item, index) => {
    const stock = shelfStock.find((row) => row.id === item.stockId);
    const device = devices.find((row) => row.shelfId === stock.shelfId);
    const capturedAt = new Date(now - (index + 1) * 47 * 60000);
    imageEvents.push({
      id: index + 1,
      deviceId: device.id,
      shelfId: stock.shelfId,
      imagePath: `/images/${device.deviceKey}/${capturedAt.getTime()}.jpg`,
      capturedAt: toLocalDateTime(capturedAt),
      receivedAt: toLocalDateTime(new Date(capturedAt.getTime() + 2000)),
      processingStatus: item.status === 'FAILED' ? 'FAILED' : 'PROCESSED',
    });
    recognitionResults.push({
      id: index + 1,
      imageEventId: index + 1,
      productId: stock.productId,
      detectedQuantity: Math.max(0, stock.quantity + (item.status === 'DETECTED' ? item.delta : 0)),
      quantityDelta: item.delta,
      confidence: item.confidence,
      status: item.status,
    });
  });

  const recommendations = [
    {
      id: 1,
      productId: 2,
      recommendedQuantity: 40,
      requiredBy: day(4),
      reason: 'Projected stock 6 pcs is below planned demand of 30 pcs for the next 7 days.',
      status: 'NEW',
      createdAt: toLocalDateTime(new Date(now - 2 * 3600000)),
    },
    {
      id: 2,
      productId: 4,
      recommendedQuantity: 60,
      requiredBy: day(7),
      reason: 'Average consumption 5 pcs/day; stock of 11 pcs lasts about 2 days.',
      status: 'NEW',
      createdAt: toLocalDateTime(new Date(now - 5 * 3600000)),
    },
    {
      id: 3,
      productId: 1,
      recommendedQuantity: 24,
      requiredBy: day(3),
      reason: 'Shelf life is 7 days: ordering a smaller batch closer to the demand date.',
      status: 'NEW',
      createdAt: toLocalDateTime(new Date(now - 26 * 3600000)),
    },
    {
      id: 4,
      productId: 8,
      recommendedQuantity: 30,
      requiredBy: day(10),
      reason: 'Planned demand of 35 pcs on the required date.',
      status: 'APPROVED',
      createdAt: toLocalDateTime(new Date(now - 3 * DAY)),
    },
    {
      id: 5,
      productId: 3,
      recommendedQuantity: 100,
      requiredBy: day(-2),
      reason: 'Monthly restock.',
      status: 'COMPLETED',
      createdAt: toLocalDateTime(new Date(now - 9 * DAY)),
    },
  ];

  return {
    warehouses,
    racks,
    shelves,
    products,
    shelfStock,
    devices,
    stockMovements,
    imageEvents,
    recognitionResults,
    recommendations,
  };
}

/** The live tables. Handlers read and mutate these arrays directly. */
export const db = seed();

/** Restore the seed state (used between tests). */
export function resetDb() {
  Object.assign(db, seed());
}

/**
 * Next id for a table, like a database sequence.
 * @param {Array<{ id: number }>} table
 * @returns {number}
 */
export function nextId(table) {
  return table.reduce((max, row) => Math.max(max, row.id), 0) + 1;
}

/**
 * Apply a confirmed stock change: update the shelf balance and log a movement (FR-07, FR-09).
 * @param {{ shelfId: number, productId: number, quantityDelta: number, source: string }} change
 */
export function applyStockChange({ shelfId, productId, quantityDelta, source }) {
  const stock = db.shelfStock.find((row) => row.shelfId === shelfId && row.productId === productId);
  if (stock) stock.quantity = Math.max(0, stock.quantity + quantityDelta);
  db.stockMovements.push({
    id: nextId(db.stockMovements),
    productId,
    shelfId,
    quantityDelta,
    type: quantityDelta >= 0 ? 'RECEIPT' : 'REMOVAL',
    source,
    createdAt: toLocalDateTime(new Date()),
  });
}

/**
 * Simulate one shelf photo arriving from a device, as the real pipeline would:
 * image event -> recognition result -> stock update if confident, manual review otherwise.
 * @param {() => number} [random]
 */
export function simulateCameraUpload(random = Math.random) {
  const activeDevices = db.devices.filter((device) => device.status === 'ACTIVE');
  const candidates = db.shelfStock.filter(
    (row) => row.status === 'ACTIVE' && activeDevices.some((d) => d.shelfId === row.shelfId),
  );
  if (candidates.length === 0) return;
  const stock = candidates[Math.floor(random() * candidates.length)];
  const device = activeDevices.find((d) => d.shelfId === stock.shelfId);
  const now = new Date();
  const delta = stock.quantity > 0 ? -(1 + Math.floor(random() * Math.min(3, stock.quantity))) : 0;
  const confidence = Math.round((0.3 + random() * 0.69) * 100) / 100;
  const confident = confidence >= 0.6;

  device.lastSeenAt = toLocalDateTime(now);
  const imageEventId = nextId(db.imageEvents);
  db.imageEvents.push({
    id: imageEventId,
    deviceId: device.id,
    shelfId: stock.shelfId,
    imagePath: `/images/${device.deviceKey}/${now.getTime()}.jpg`,
    capturedAt: toLocalDateTime(new Date(now.getTime() - 1500)),
    receivedAt: toLocalDateTime(now),
    processingStatus: 'PROCESSED',
  });
  db.recognitionResults.push({
    id: nextId(db.recognitionResults),
    imageEventId,
    productId: stock.productId,
    detectedQuantity: stock.quantity + delta,
    quantityDelta: delta,
    confidence,
    status: confident ? 'CONFIRMED' : 'DETECTED',
  });
  if (confident && delta !== 0) {
    applyStockChange({
      shelfId: stock.shelfId,
      productId: stock.productId,
      quantityDelta: delta,
      source: 'CAMERA',
    });
  }
}
