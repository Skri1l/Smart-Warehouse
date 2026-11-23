import { http } from './http.js';

/** @returns {Promise<import('./types.js').Device[]>} */
export const listDevices = () => http.get('/devices');

/**
 * Register a shelf device (FR-04).
 * @param {{ deviceKey: string, shelfId: number }} device
 * @returns {Promise<number>} Id of the created device.
 */
export const createDevice = (device) => http.post('/devices', device);

/** @param {number} id */
export const activateDevice = (id) => http.patch(`/devices/${id}/activate`);

/** @param {number} id */
export const deactivateDevice = (id) => http.patch(`/devices/${id}/deactivate`);
