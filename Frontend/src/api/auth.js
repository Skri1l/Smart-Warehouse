import { http } from './http.js';

/*
 * Auth endpoints do not exist in the backend yet (Milestone 5). This is the contract the
 * UI is built against and the mock implements. The session is an HttpOnly cookie set by
 * the backend (ADR-0012); the UI never sees or stores a token.
 *
 *   POST /auth/login  { username, password }  -> 200 CurrentUser + Set-Cookie | 401
 *   GET  /auth/me                             -> 200 CurrentUser | 401
 *   POST /auth/logout                         -> 204, clears the cookie
 */

/**
 * @param {string} username
 * @param {string} password
 * @returns {Promise<import('./types.js').CurrentUser>}
 */
export function login(username, password) {
  return http.post('/auth/login', { username, password });
}

/** @returns {Promise<import('./types.js').CurrentUser>} */
export function fetchCurrentUser() {
  return http.get('/auth/me');
}

/** @returns {Promise<void>} */
export function logout() {
  return http.post('/auth/logout');
}
