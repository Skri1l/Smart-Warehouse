import { Link, useSearchParams } from 'react-router-dom';
import styles from './ProductsPage.module.css';
import { useProducts } from './useProducts.js';
import { StockStatus } from './StockStatus.jsx';
import { Card, PageHeader } from '../../components/Card.jsx';
import { DataTable } from '../../components/DataTable.jsx';
import { Field } from '../../components/Field.jsx';
import { StateView } from '../../components/StateView.jsx';

const FILTERS = [
  { value: 'all', label: 'All products' },
  { value: 'low', label: 'Low stock' },
];

const COLUMNS = [
  {
    key: 'product',
    header: 'Product',
    primary: true,
    render: (row) => (
      <Link to={`/products/${row.product.id}`} className={styles.product}>
        <span>{row.product.name}</span>
        <span className={styles.sku}>{row.product.sku}</span>
      </Link>
    ),
  },
  { key: 'quantity', header: 'Quantity', align: 'end', render: (row) => `${row.quantity} pcs` },
  {
    key: 'shelves',
    header: 'Shelves',
    render: (row) => row.locations.map((location) => location.shelfLabel).join(', ') || '—',
  },
  { key: 'status', header: 'Status', render: (row) => <StockStatus summary={row} /> },
];

/** Product catalogue with current stock and where it is (FR-03, FR-10). */
export default function ProductsPage() {
  const [params, setParams] = useSearchParams();
  const query = params.get('q') ?? '';
  const filter = FILTERS.some((item) => item.value === params.get('filter'))
    ? params.get('filter')
    : 'all';
  const { rows, total, error, loading, reload } = useProducts({ query, filter });

  const update = (key, value) => {
    const next = new URLSearchParams(params);
    if (value && value !== 'all') next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  return (
    <>
      <PageHeader title="Products" subtitle={`${total} products in the catalogue`} />
      <Card flush>
        <div className={styles.filters}>
          <Field
            label="Search"
            type="search"
            placeholder="Name or SKU"
            value={query}
            onChange={(event) => update('q', event.target.value)}
          />
          <Field
            as="select"
            label="Show"
            value={filter}
            onChange={(event) => update('filter', event.target.value)}
          >
            {FILTERS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </Field>
        </div>
        <StateView
          loading={loading}
          error={error}
          onRetry={reload}
          empty={rows.length === 0}
          emptyText="No products match your search."
        >
          <DataTable
            caption="Products"
            columns={COLUMNS}
            rows={rows}
            rowKey={(row) => row.product.id}
          />
        </StateView>
      </Card>
    </>
  );
}
