import { Badge } from '../../components/Badge.jsx';

/**
 * Status of a product summary from core/stock.js `summariseProducts`.
 * @param {{ summary: { quantity: number, isLow: boolean, expiresSoon: boolean, expiredQuantity: number } }} props
 */
export function StockStatus({ summary }) {
  if (summary.quantity === 0) return <Badge tone="danger">Out of stock</Badge>;
  if (summary.isLow) return <Badge tone="danger">Low</Badge>;
  if (summary.expiredQuantity > 0) return <Badge tone="warn">Has expired</Badge>;
  if (summary.expiresSoon) return <Badge tone="warn">Expires soon</Badge>;
  return <Badge tone="ok">OK</Badge>;
}

/**
 * Expiry state of a single shelf-stock row.
 * @param {{ expiry: 'expired' | 'soon' | 'ok' }} props
 */
export function ExpiryBadge({ expiry }) {
  if (expiry === 'expired') return <Badge tone="danger">Expired</Badge>;
  if (expiry === 'soon') return <Badge tone="warn">Soon</Badge>;
  return <Badge tone="ok">OK</Badge>;
}
