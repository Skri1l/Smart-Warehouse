import { listProducts } from './products.js';
import { listRacks, listShelves } from './warehouses.js';
import { listShelfStock } from './stock.js';
import { listDevices } from './devices.js';

/**
 * Load everything needed to show stock with names: products, racks, shelves, balances and devices.
 * The backend has no aggregated endpoint yet, so the UI joins these lists itself (in core/).
 * @returns {Promise<{ products: import('./types.js').Product[], racks: import('./types.js').Rack[],
 *   shelves: import('./types.js').Shelf[], stock: import('./types.js').ShelfStock[],
 *   devices: import('./types.js').Device[] }>}
 */
export async function loadStockSnapshot() {
  const [products, racks, shelves, stock, devices] = await Promise.all([
    listProducts(),
    listRacks(),
    listShelves(),
    listShelfStock(),
    listDevices(),
  ]);
  return { products, racks, shelves, stock, devices };
}
