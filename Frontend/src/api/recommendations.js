import { http } from './http.js';

/** @typedef {import('./types.js').Recommendation} Recommendation */

/** @returns {Promise<Recommendation[]>} */
export const listRecommendations = () => http.get('/replenishment-recommendations');

/** @param {number} id */
export const approveRecommendation = (id) =>
  http.patch(`/replenishment-recommendations/${id}/approve`);

/** @param {number} id */
export const rejectRecommendation = (id) =>
  http.patch(`/replenishment-recommendations/${id}/reject`);

/**
 * Modify a recommendation before approving it (FR-16).
 * @param {number} id
 * @param {{ productId: number, recommendedQuantity: number, requiredBy: string, reason: string }} body
 * @returns {Promise<Recommendation>}
 */
export const updateRecommendation = (id, body) =>
  http.put(`/replenishment-recommendations/${id}`, body);
