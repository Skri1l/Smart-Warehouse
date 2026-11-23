import { Link } from 'react-router-dom';
import styles from './ShelvesPage.module.css';
import { useShelves } from './useShelves.js';
import { Badge } from '../../components/Badge.jsx';
import { PageHeader } from '../../components/Card.jsx';
import { Icon } from '../../components/Icon.jsx';
import { StateView } from '../../components/StateView.jsx';
import { formatDate, formatPercent, formatRelative } from '../../core/format.js';
import { ExpiryBadge } from '../products/StockStatus.jsx';

const LEVEL_BADGES = {
  empty: { tone: 'danger', text: 'Empty' },
  low: { tone: 'warn', text: 'Low' },
  ok: { tone: 'ok', text: 'OK' },
  full: { tone: 'info', text: 'Full' },
};

function ShelfCard({ view }) {
  const { shelf, product, mixedProducts, batches, quantity, fillRatio, level, device } = view;
  const inactive = shelf.status === 'INACTIVE';
  const mixed = mixedProducts.length > 0;
  let badge = LEVEL_BADGES[level];
  if (inactive) badge = { tone: 'neutral', text: 'Inactive' };
  if (mixed) badge = { tone: 'danger', text: 'Mixed' };
  const showBatches = batches.length > 1 || batches.some((batch) => batch.expiry !== 'ok');

  return (
    <article className={`${styles.shelf} ${inactive ? styles.inactive : ''}`}>
      <header className={styles.shelfHeader}>
        <h3>Shelf {shelf.label}</h3>
        <Badge tone={badge.tone}>{badge.text}</Badge>
      </header>

      {product ? (
        <Link to={`/products/${product.id}`} className={styles.product}>
          <span>{product.name}</span>
          <span className={styles.sku}>{product.sku}</span>
        </Link>
      ) : null}
      {!product && !mixed ? <p className={styles.muted}>No product on this shelf.</p> : null}
      {mixed ? (
        <div className={styles.mixed} role="alert">
          <p>
            <strong>{mixedProducts.length} products on one shelf.</strong> The camera only counts
            items, so it cannot tell them apart and the count below is unreliable. Keep one product
            per shelf.
          </p>
          <ul>
            {mixedProducts.map((item) => (
              <li key={item.id}>
                <Link to={`/products/${item.id}`}>{item.name}</Link>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      <div
        className={styles.meter}
        role="meter"
        aria-label={`Shelf ${shelf.label} fill level`}
        aria-valuemin={0}
        aria-valuemax={shelf.capacity}
        aria-valuenow={Math.min(quantity, shelf.capacity)}
        aria-valuetext={`${quantity} of ${shelf.capacity} units, ${formatPercent(fillRatio)}`}
      >
        <span
          className={`${styles.meterFill} ${styles[level]}`}
          style={{ width: `${fillRatio * 100}%` }}
        />
      </div>
      <p className={styles.capacity}>
        {quantity} / {shelf.capacity} pcs · {formatPercent(fillRatio)}
      </p>

      {showBatches ? (
        <ul className={styles.items} aria-label={`Batches on shelf ${shelf.label}`}>
          {batches.map((batch) => (
            <li key={batch.stockId}>
              <span>
                {batch.quantity} pcs · until {formatDate(batch.expirationDate)}
              </span>
              <ExpiryBadge expiry={batch.expiry} />
            </li>
          ))}
        </ul>
      ) : null}

      <footer className={styles.device}>
        <Icon name="camera" size={16} />
        {device ? (
          <span>
            {device.deviceKey} · {device.status === 'ACTIVE' ? 'seen ' : 'inactive, seen '}
            {formatRelative(device.lastSeenAt)}
          </span>
        ) : (
          <span>No camera connected</span>
        )}
      </footer>
    </article>
  );
}

/** Racks and shelves with what is on them and how full they are (Milestone 5 shelf/stock view). */
export default function ShelvesPage() {
  const { racks, warehouse, error, loading, reload } = useShelves();
  return (
    <>
      <PageHeader title="Shelves" subtitle={warehouse ? warehouse.name : undefined} />
      <StateView
        loading={loading}
        error={error}
        onRetry={reload}
        empty={racks.length === 0}
        emptyText="No racks configured yet."
      >
        {racks.map(({ rack, shelves }) => (
          <section key={rack.id} className={styles.rack} aria-labelledby={`rack-${rack.id}`}>
            <h2 id={`rack-${rack.id}`}>Rack {rack.label}</h2>
            <div className={styles.grid}>
              {shelves.map((view) => (
                <ShelfCard key={view.shelf.id} view={view} />
              ))}
            </div>
          </section>
        ))}
      </StateView>
    </>
  );
}
