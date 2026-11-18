import { http } from './http.js';

/** @typedef {import('./types.js').Product} Product */

/** @returns {Promise<Product[]>} */
export const listProducts = () => http.get('/products');

/** @param {number} id @returns {Promise<Product>} */
export const getProduct = (id) => http.get(`/products/${id}`);

/**
 * @param {{ sku: string, name: string, description?: string }} product
 * @returns {Promise<number>} Id of the created product.
 */
export const createProduct = (product) => http.post('/products', product);
