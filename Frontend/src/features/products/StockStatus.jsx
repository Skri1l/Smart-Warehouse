import { Badge } from '../../components/Badge.jsx';

/**
 * Status of a product summary from core/stock.js `summariseProducts`.
 * @param {{ summary: { quantity: number, isLow: boolean } }} props
 */
export function StockStatus({ summary }) {
  if (summary.quantity === 0) return <Badge tone="danger">Out of stock</Badge>;
  if (summary.isLow) return <Badge tone="danger">Low</Badge>;
  return <Badge tone="ok">OK</Badge>;
}
