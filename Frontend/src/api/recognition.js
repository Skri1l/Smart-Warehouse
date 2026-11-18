import { http } from './http.js';

/** @returns {Promise<import('./types.js').RecognitionResult[]>} */
export const listRecognitionResults = () => http.get('/recognition-results');

/** @returns {Promise<import('./types.js').ImageEvent[]>} */
export const listImageEvents = () => http.get('/image-events');

/** @param {number} id */
export const confirmRecognition = (id) => http.patch(`/recognition-results/${id}/confirm`);

/** @param {number} id */
export const rejectRecognition = (id) => http.patch(`/recognition-results/${id}/reject`);
