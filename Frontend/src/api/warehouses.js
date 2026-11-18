import { http } from './http.js';

/** @returns {Promise<import('./types.js').Warehouse[]>} */
export const listWarehouses = () => http.get('/warehouses');

/** @returns {Promise<import('./types.js').Rack[]>} */
export const listRacks = () => http.get('/racks');

/** @returns {Promise<import('./types.js').Shelf[]>} */
export const listShelves = () => http.get('/shelves');
