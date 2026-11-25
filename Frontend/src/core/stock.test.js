import { describe, expect, it } from 'vitest';
import {
  daysUntil,
  fillLevel,
  findEmptyShelves,
  findMixedShelves,
  groupShelvesByRack,
  movementsPerDay,
  summariseProducts,
} from './stock.js';

const TODAY = new Date(2026, 10, 18);

const shelves = [
  { id: 10, label: 'A-1', capacity: 50, status: 'ACTIVE', rackId: 1 },
  { id: 11, label: 'A-2', capacity: 50, status: 'ACTIVE', rackId: 1 },
  { id: 12, label: 'A-3', capacity: 50, status: 'INACTIVE', rackId: 1 },
];
const products = [
  { id: 1, sku: 'M', name: 'Milk', description: null },
  { id: 2, sku: 'B', name: 'Butter', description: null },
];
/** A shelf-stock row as the backend returns it; expiry is not recorded. */
const row = (id, shelfId, productId, quantity) => ({
  id,
  shelfId,
  productId,
  quantity,
  expirationDate: null,
  status: 'ACTIVE',
});

describe('daysUntil', () => {
  it('shows a positive number for future dates and negative for past dates', () => {
    expect(daysUntil('2026-11-20', TODAY)).toBe(2);
    expect(daysUntil('2026-11-18', TODAY)).toBe(0);
    expect(daysUntil('2026-11-17', TODAY)).toBe(-1);
  });
});

describe('fillLevel', () => {
  it('shows low when the shelf is filled below a quarter of its capacity', () => {
    expect(fillLevel(0, 40)).toBe('empty');
    expect(fillLevel(9, 40)).toBe('low');
    expect(fillLevel(10, 40)).toBe('ok');
    expect(fillLevel(40, 40)).toBe('full');
  });
});

describe('summariseProducts', () => {
  it('shows the total across shelves and one location per shelf', () => {
    const stock = [row(1, 10, 1, 8), row(2, 11, 1, 5), row(3, 10, 1, 1)];
    const [milk, butter] = summariseProducts(products, stock, shelves);
    expect(milk.quantity).toBe(14);
    expect(milk.isLow).toBe(true);
    expect(milk.locations).toEqual([
      { shelfId: 10, shelfLabel: 'A-1', quantity: 9 },
      { shelfId: 11, shelfLabel: 'A-2', quantity: 5 },
    ]);
    expect(butter.quantity).toBe(0);
    expect(butter.locations).toEqual([]);
  });
});

describe('groupShelvesByRack', () => {
  const racks = [{ id: 1, label: 'A', warehouseId: 1 }];

  it('shows one product and its total when a shelf holds one product', () => {
    const stock = [row(1, 10, 1, 20), row(2, 10, 1, 5)];
    const [{ shelves: views }] = groupShelvesByRack({
      racks,
      shelves,
      stock,
      products,
      devices: [],
    });
    expect(views[0].product.name).toBe('Milk');
    expect(views[0].mixedProducts).toEqual([]);
    expect(views[0].quantity).toBe(25);
    expect(views[1].product).toBeNull();
  });

  it('shows the shelf as mixed instead of picking a product when it holds two products', () => {
    const stock = [row(1, 10, 1, 20), row(2, 10, 2, 5)];
    const [{ shelves: views }] = groupShelvesByRack({
      racks,
      shelves,
      stock,
      products,
      devices: [],
    });
    expect(views[0].product).toBeNull();
    expect(views[0].mixedProducts.map((p) => p.name)).toEqual(['Milk', 'Butter']);
    expect(findMixedShelves(stock, shelves)).toEqual([{ shelf: shelves[0], productCount: 2 }]);
  });
});

describe('findEmptyShelves', () => {
  it('shows active shelves with nothing on them and skips inactive ones', () => {
    const stock = [row(1, 10, 1, 3), row(2, 11, 2, 0)];
    expect(findEmptyShelves(stock, shelves).map((shelf) => shelf.label)).toEqual(['A-2']);
  });
});

describe('movementsPerDay', () => {
  it('sums incoming and outgoing units into daily buckets', () => {
    const movements = [
      { quantityDelta: 10, createdAt: '2026-11-18T09:00:00' },
      { quantityDelta: -3, createdAt: '2026-11-18T10:00:00' },
      { quantityDelta: -2, createdAt: '2026-11-17T10:00:00' },
      { quantityDelta: -99, createdAt: '2026-10-01T10:00:00' },
    ];
    const days = movementsPerDay(movements, 2, TODAY);
    expect(days).toEqual([
      { date: '2026-11-17', incoming: 0, outgoing: 2 },
      { date: '2026-11-18', incoming: 10, outgoing: 3 },
    ]);
  });
});
