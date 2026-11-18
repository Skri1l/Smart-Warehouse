/** Relative base path: UI and API share one origin (ADR-0011). */
export const API_BASE = '/api/v1';

/** Error thrown for every non-2xx response and for network failures. */
export class ApiError extends Error {
  /**
   * @param {number} status HTTP status, 0 for network errors.
   * @param {string} message Human-readable message (do not parse it).
   * @param {string} [code] Stable machine-readable code from `ErrorResponse.error`.
   */
  constructor(status, message, code) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code ?? defaultCode(status);
  }
}

function defaultCode(status) {
  if (status === 0) return 'NETWORK_ERROR';
  if (status === 401) return 'UNAUTHORIZED';
  if (status === 403) return 'FORBIDDEN';
  if (status === 404) return 'NOT_FOUND';
  if (status === 409) return 'CONFLICT';
  if (status === 400) return 'VALIDATION_ERROR';
  return 'SERVER_ERROR';
}

/** @type {Set<(error: ApiError) => void>} */
const unauthorizedListeners = new Set();

/**
 * Subscribe to 401 responses (the auth provider sends the user to the login page).
 * @param {(error: ApiError) => void} listener
 * @returns {() => void} Unsubscribe function.
 */
export function onUnauthorized(listener) {
  unauthorizedListeners.add(listener);
  return () => unauthorizedListeners.delete(listener);
}

/**
 * Build a query string, skipping empty values.
 * @param {Record<string, string | number | boolean | undefined | null>} [params]
 * @returns {string}
 */
function toQuery(params) {
  if (!params) return '';
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.append(key, String(value));
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

/**
 * Low-level request against the backend REST API.
 * @param {string} method HTTP verb.
 * @param {string} path Path relative to `/api/v1`, starting with `/`.
 * @param {{ body?: unknown, query?: Record<string, any>, signal?: AbortSignal }} [options]
 * @returns {Promise<any>} Parsed JSON body, or `null` for empty responses.
 */
export async function request(method, path, { body, query, signal } = {}) {
  let response;
  try {
    // Same-origin absolute URL (also lets tests run fetch outside a real browser).
    response = await fetch(new URL(`${API_BASE}${path}${toQuery(query)}`, window.location.origin), {
      method,
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError(0, 'The server cannot be reached. Check your connection.');
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    // Backend ErrorResponse: { status, message } today; { error, message, ... } per API_GUIDELINES.
    const error = new ApiError(
      response.status,
      data?.message || response.statusText || 'Request failed',
      data?.error,
    );
    if (response.status === 401) unauthorizedListeners.forEach((listener) => listener(error));
    throw error;
  }
  return data;
}

export const http = {
  /** @param {string} path @param {object} [options] */
  get: (path, options) => request('GET', path, options),
  /** @param {string} path @param {unknown} [body] */
  post: (path, body) => request('POST', path, { body }),
  /** @param {string} path @param {unknown} [body] */
  put: (path, body) => request('PUT', path, { body }),
  /** @param {string} path @param {unknown} [body] */
  patch: (path, body) => request('PATCH', path, { body }),
  /** @param {string} path */
  delete: (path) => request('DELETE', path),
};
