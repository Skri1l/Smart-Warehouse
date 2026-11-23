import { Link, useParams } from 'react-router-dom';
import styles from './ProductsPage.module.css';
import { useProductDetails } from './useProducts.js';
import { StockStatus } from './StockStatus.jsx';
import { Badge } from '../../components/Badge.jsx';
import { Card, PageHeader } from '../../components/Card.jsx';
import { DataTable } from '../../components/DataTable.jsx';
import { StateView } from '../../components/StateView.jsx';
import { formatDateTime, formatDelta, humanize } from '../../core/format.js';

const LOCATION_COLUMNS = [
  { key: 'shelf', header: 'Shelf', primary: true, render: (row) => `Shelf ${row.shelfLabel}` },
  { key: 'quantity', header: 'Quantity', align: 'end', render: (row) => `${row.quantity} pcs` },
];

const MOVEMENT_COLUMNS = [
  { key: 'when', header: 'When', primary: true, render: (row) => formatDateTime(row.createdAt) },
  { key: 'shelf', header: 'Shelf', render: (row) => row.shelfLabel },
  { key: 'type', header: 'Type', render: (row) => humanize(row.type) },
  { key: 'source', header: 'Source', render: (row) => humanize(row.source) },
  {
    key: 'delta',
    header: 'Change',
    align: 'end',
    render: (row) => (
      <Badge tone={row.quantityDelta >= 0 ? 'ok' : 'info'}>{formatDelta(row.quantityDelta)}</Badge>
    ),
  },
];

/** Where a product is stored and how its stock changed (FR-09, FR-10). */
export default function ProductDetailsPage() {
  const productId = Number(useParams().productId);
  const { data, error, loading, reload } = useProductDetails(productId);
  const summary = data?.summary;

  return (
    <>
      <Link to="/products" className={styles.back}>
        ← All products
      </Link>
      <StateView loading={loading && !data} error={data ? null : error} onRetry={reload}>
        {summary ? (
          <>
            <PageHeader title={summary.product.name} subtitle={summary.product.sku} />
            <div className={styles.details}>
              <Card title="Summary" className={styles.full}>
                <dl className={styles.facts}>
                  <div>
                    <dt>Quantity</dt>
                    <dd>{summary.quantity} pcs</dd>
                  </div>
                  <div>
                    <dt>Shelves</dt>
                    <dd>{summary.locations.length}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>
                      <StockStatus summary={summary} />
                    </dd>
                  </div>
                </dl>
                {summary.product.description ? <p>{summary.product.description}</p> : null}
              </Card>
              <Card title="Locations" flush>
                <StateView
                  loading={false}
                  error={null}
                  empty={summary.locations.length === 0}
                  emptyText="This product is not on any shelf."
                >
                  <DataTable
                    caption="Shelves holding this product"
                    columns={LOCATION_COLUMNS}
                    rows={summary.locations}
                    rowKey={(row) => row.shelfId}
                  />
                </StateView>
              </Card>
              <Card title="Movement history" flush>
                <StateView
                  loading={false}
                  error={null}
                  empty={data.movements.length === 0}
                  emptyText="No movements recorded."
                >
                  <DataTable
                    caption="Movement history"
                    columns={MOVEMENT_COLUMNS}
                    rows={data.movements}
                    rowKey={(row) => row.id}
                    pageSize={10}
                  />
                </StateView>
              </Card>
            </div>
          </>
        ) : null}
      </StateView>
    </>
  );
}
