import { describe, expect, it } from 'vitest';
import {
  daysUntil,
  expiryState,
  fillLevel,
  findMixedShelves,
  groupShelvesByRack,
  movementsPerDay,
  summariseProducts,
} from './stock.js';

const TODAY = new Date(2026, 10, 18);

describe('daysUntil', () => {
  it('shows a positive number for future dates and negative for past dates', () => {
    expect(daysUntil('2026-11-20', TODAY)).toBe(2);
    expect(daysUntil('2026-11-18', TODAY)).toBe(0);
    expect(daysUntil('2026-11-17', TODAY)).toBe(-1);
  });
});

describe('expiryState', () => {
  it('shows expired when the backend marks the row EXPIRED even with a future date', () => {
    expect(expiryState({ expirationDate: '2027-01-01', status: 'EXPIRED' }, TODAY)).toBe('expired');
  });

  it('shows soon when the date is within the warning window', () => {
    expect(expiryState({ expirationDate: '2026-11-25', status: 'ACTIVE' }, TODAY)).toBe('soon');
    expect(expiryState({ expirationDate: '2026-11-26', status: 'ACTIVE' }, TODAY)).toBe('ok');
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
  const products = [{ id: 1, sku: 'A', name: 'Milk', description: null }];
  const shelves = [
    { id: 10, label: 'A-1', capacity: 50, status: 'ACTIVE', rackId: 1 },
    { id: 11, label: 'A-2', capacity: 50, status: 'ACTIVE', rackId: 1 },
  ];

  it('excludes expired stock from the usable quantity', () => {
    const stock = [
      {
        id: 1,
        shelfId: 10,
        productId: 1,
        quantity: 12,
        expirationDate: '2026-12-30',
        status: 'ACTIVE',
      },
      {
        id: 2,
        shelfId: 11,
        productId: 1,
        quantity: 5,
        expirationDate: '2026-11-10',
        status: 'ACTIVE',
      },
    ];
    const [summary] = summariseProducts(products, stock, shelves, TODAY);
    expect(summary.quantity).toBe(12);
    expect(summary.expiredQuantity).toBe(5);
    expect(summary.isLow).toBe(true);
    expect(summary.locations.map((l) => l.shelfLabel)).toEqual(['A-1', 'A-2']);
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

describe('groupShelvesByRack', () => {
  const racks = [{ id: 1, label: 'A', warehouseId: 1 }];
  const shelves = [
    { id: 10, label: 'A-1', capacity: 50, status: 'ACTIVE', rackId: 1 },
    { id: 11, label: 'A-2', capacity: 50, status: 'ACTIVE', rackId: 1 },
  ];
  const products = [
    { id: 1, sku: 'M', name: 'Milk', description: null },
    { id: 2, sku: 'B', name: 'Butter', description: null },
  ];
  const row = (id, shelfId, productId, quantity, expirationDate) => ({
    id,
    shelfId,
    productId,
    quantity,
    expirationDate,
    status: 'ACTIVE',
  });

  it('shows one product with its batches oldest expiry first when a shelf holds one product', () => {
    const stock = [row(1, 10, 1, 20, '2026-12-30'), row(2, 10, 1, 5, '2026-11-20')];
    const [{ shelves: views }] = groupShelvesByRack(
      { racks, shelves, stock, products, devices: [] },
      TODAY,
    );
    expect(views[0].product.name).toBe('Milk');
    expect(views[0].mixedProducts).toEqual([]);
    expect(views[0].quantity).toBe(25);
    expect(views[0].batches.map((b) => b.quantity)).toEqual([5, 20]);
    expect(views[1].product).toBeNull();
  });

  it('shows the shelf as mixed instead of picking a product when it holds two products', () => {
    const stock = [row(1, 10, 1, 20, '2026-12-30'), row(2, 10, 2, 5, '2026-12-30')];
    const [{ shelves: views }] = groupShelvesByRack(
      { racks, shelves, stock, products, devices: [] },
      TODAY,
    );
    expect(views[0].product).toBeNull();
    expect(views[0].mixedProducts.map((p) => p.name)).toEqual(['Milk', 'Butter']);
    expect(findMixedShelves(stock, shelves)).toEqual([{ shelf: shelves[0], productCount: 2 }]);
  });
});
