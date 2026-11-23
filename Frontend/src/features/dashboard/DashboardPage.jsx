import { Link } from 'react-router-dom';
import styles from './DashboardPage.module.css';
import { useDashboard } from './useDashboard.js';
import { MovementsChart } from './MovementsChart.jsx';
import { Badge } from '../../components/Badge.jsx';
import { Card, PageHeader } from '../../components/Card.jsx';
import { Icon } from '../../components/Icon.jsx';
import { StateView } from '../../components/StateView.jsx';
import { formatDate, formatDelta, formatRelative, humanize } from '../../core/format.js';
import { LOW_STOCK_THRESHOLD } from '../../core/stock.js';

function Stat({ icon, label, value, tone, to }) {
  const content = (
    <>
      <span className={`${styles.statIcon} ${styles[tone] ?? ''}`}>
        <Icon name={icon} />
      </span>
      <span className={styles.statText}>
        <span className={styles.statValue}>{value}</span>
        <span className={styles.statLabel}>{label}</span>
      </span>
    </>
  );
  return to ? (
    <Link to={to} className={styles.stat}>
      {content}
    </Link>
  ) : (
    <div className={styles.stat}>{content}</div>
  );
}

/** Overview for the shift: what is low, what changed, what needs attention (FR-17). */
export default function DashboardPage() {
  const { data, error, loading, reload, isManager } = useDashboard();

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Live view of the warehouse, refreshed every 15 s." />
      <StateView loading={loading && !data} error={data ? null : error} onRetry={reload}>
        {data ? (
          <div className={styles.grid}>
            <div className={styles.stats}>
              <Stat icon="box" label="Units on shelves" value={data.totalUnits} />
              <Stat
                icon="alert"
                label="Low-stock products"
                value={data.lowStock.length}
                tone={data.lowStock.length ? 'danger' : 'ok'}
                to="/products?filter=low"
              />
              <Stat
                icon="review"
                label="Awaiting review"
                value={data.pendingReviewCount}
                tone={data.pendingReviewCount ? 'warn' : 'ok'}
                to="/review"
              />
              <Stat
                icon="clock"
                label="Expiring or expired"
                value={data.expiringSoon.length}
                tone={data.expiringSoon.length ? 'warn' : 'ok'}
                to="/products?filter=expiring"
              />
            </div>

            <Card title="Stock movements, last 7 days" className={styles.chartCard}>
              <MovementsChart data={data.chart} />
            </Card>

            <Card
              title={`Low stock (below ${LOW_STOCK_THRESHOLD})`}
              actions={<Link to="/products?filter=low">All products</Link>}
              flush
            >
              {data.lowStock.length === 0 ? (
                <p className={styles.empty}>All products are above the threshold.</p>
              ) : (
                <ul className={styles.list}>
                  {data.lowStock.map((item) => (
                    <li key={item.product.id}>
                      <Link to={`/products/${item.product.id}`} className={styles.listMain}>
                        <strong>{item.product.name}</strong>
                        <span>{item.product.sku}</span>
                      </Link>
                      <Badge tone={item.quantity === 0 ? 'danger' : 'warn'}>
                        {item.quantity} pcs
                      </Badge>
                    </li>
                  ))}
                </ul>
              )}
            </Card>

            <Card
              title="Recent movements"
              actions={<Link to="/movements">History</Link>}
              flush
              className={styles.wide}
            >
              <ul className={styles.list}>
                {data.recentMovements.map((movement) => (
                  <li key={movement.id}>
                    <span className={styles.listMain}>
                      <strong>{movement.productName}</strong>
                      <span>
                        Shelf {movement.shelfLabel} · {humanize(movement.type)} ·{' '}
                        {humanize(movement.source)} · {formatRelative(movement.createdAt)}
                      </span>
                    </span>
                    <Badge tone={movement.quantityDelta >= 0 ? 'ok' : 'info'}>
                      {formatDelta(movement.quantityDelta)}
                    </Badge>
                  </li>
                ))}
              </ul>
            </Card>

            {isManager ? (
              <Card
                title="New purchase recommendations"
                actions={<Link to="/recommendations">Open</Link>}
                flush
              >
                {data.newRecommendations.length === 0 ? (
                  <p className={styles.empty}>Nothing to decide right now.</p>
                ) : (
                  <ul className={styles.list}>
                    {data.newRecommendations.map((item) => (
                      <li key={item.id}>
                        <span className={styles.listMain}>
                          <strong>{item.productName}</strong>
                          <span>By {formatDate(item.requiredBy)}</span>
                        </span>
                        <Badge tone="info">{item.recommendedQuantity} pcs</Badge>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            ) : null}

            {data.mixedShelves.length > 0 ? (
              <Card title="Shelf setup problems" actions={<Link to="/shelves">Shelves</Link>}>
                <p>
                  {data.mixedShelves.map((item) => `Shelf ${item.shelf.label}`).join(', ')}{' '}
                  {data.mixedShelves.length === 1 ? 'holds' : 'hold'} more than one product. The
                  camera only counts items, so these counts are unreliable.
                </p>
              </Card>
            ) : null}

            {data.recognitionErrors > 0 ? (
              <Card title="Recognition errors">
                <p>
                  {data.recognitionErrors} shelf photo(s) could not be recognised.{' '}
                  <Link to="/review">See details</Link>
                </p>
              </Card>
            ) : null}
          </div>
        ) : null}
      </StateView>
    </>
  );
}
