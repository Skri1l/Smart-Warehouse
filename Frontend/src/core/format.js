/* Display formatting. Pure functions, no React. */

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
});
const dateFormat = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});
const shortDateFormat = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short' });

/** Parse a backend `LocalDate` / `LocalDateTime` string as local time. */
function parse(value) {
  return value.length === 10 ? new Date(`${value}T00:00:00`) : new Date(value);
}

/** @param {string | null} value `LocalDateTime` @returns {string} */
export const formatDateTime = (value) => (value ? dateTimeFormat.format(parse(value)) : '—');

/** @param {string | null} value `LocalDate` @returns {string} */
export const formatDate = (value) => (value ? dateFormat.format(parse(value)) : '—');

/** @param {string} value `LocalDate` @returns {string} e.g. "18 Nov" */
export const formatShortDate = (value) => shortDateFormat.format(parse(value));

/**
 * Relative time like "5 min ago".
 * @param {string | null} value `LocalDateTime`
 * @param {Date} [now]
 * @returns {string}
 */
export function formatRelative(value, now = new Date()) {
  if (!value) return 'never';
  const minutes = Math.round((now.getTime() - parse(value).getTime()) / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} h ago`;
  return `${Math.round(hours / 24)} d ago`;
}

/** @param {number} delta @returns {string} "+3", "−2", "0" */
export const formatDelta = (delta) => (delta > 0 ? `+${delta}` : delta < 0 ? `−${-delta}` : '0');

/** @param {number} ratio 0..1 @returns {string} "42 %" */
export const formatPercent = (ratio) => `${Math.round(ratio * 100)} %`;

/**
 * Turn an enum constant into readable text: "TRANSFER_IN" -> "Transfer in".
 * @param {string} value
 * @returns {string}
 */
export function humanize(value) {
  const text = value.toLowerCase().replace(/_/g, ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}
