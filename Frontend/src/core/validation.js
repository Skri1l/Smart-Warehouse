import { daysUntil } from './stock.js';

/**
 * Client-side form rules. Each validator returns `{ field: message }` for the fields
 * that fail; an empty object means the form may be submitted. The backend validates
 * again (`@Valid` DTOs) and its 400 message is shown at the top of the form.
 */

/** Same format the shelf agent accepts in `device.toml`. */
export const DEVICE_KEY_PATTERN = /^[a-z0-9][a-z0-9-]{2,63}$/;

/**
 * @param {{ username: string, password: string }} values
 * @returns {Record<string, string>}
 */
export function validateLogin({ username, password }) {
  const errors = {};
  if (!username.trim()) errors.username = 'Enter your username.';
  if (!password) errors.password = 'Enter your password.';
  return errors;
}

/**
 * @param {{ deviceKey: string, shelfId: string }} values
 * @returns {Record<string, string>}
 */
export function validateDevice({ deviceKey, shelfId }) {
  const errors = {};
  if (!DEVICE_KEY_PATTERN.test(deviceKey)) {
    errors.deviceKey = 'Use 3–64 lowercase letters, digits and dashes.';
  }
  if (!shelfId) errors.shelfId = 'Choose a shelf.';
  return errors;
}

/**
 * @param {{ quantity: string, requiredBy: string, reason: string }} values
 * @returns {Record<string, string>}
 */
export function validateRecommendationChange({ quantity, requiredBy, reason }) {
  const errors = {};
  const parsed = Number(quantity);
  if (quantity.trim() === '' || !Number.isInteger(parsed) || parsed <= 0) {
    errors.quantity = 'Enter a whole number above 0.';
  }
  if (!requiredBy) errors.requiredBy = 'Choose a date.';
  else if (daysUntil(requiredBy) < 0) errors.requiredBy = 'The date cannot be in the past.';
  if (!reason.trim()) errors.reason = 'Explain the change.';
  return errors;
}

/** @param {Record<string, string>} errors */
export const isValid = (errors) => Object.keys(errors).length === 0;
