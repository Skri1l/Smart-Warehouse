import { http } from './http.js';

/** @returns {Promise<import('./types.js').ShelfStock[]>} */
export const listShelfStock = () => http.get('/shelf-stock');

/** @param {number} productId @returns {Promise<import('./types.js').ShelfStock[]>} */
export const listStockByProduct = (productId) => http.get(`/shelf-stock/product/${productId}`);

/** @returns {Promise<import('./types.js').StockMovement[]>} */
export const listStockMovements = () => http.get('/stock-movements');

/** @param {number} productId @returns {Promise<import('./types.js').StockMovement[]>} */
export const listMovementsByProduct = (productId) =>
  http.get(`/stock-movements/product/${productId}`);
