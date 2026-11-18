import { useState } from 'react';
import { Link } from 'react-router-dom';
import styles from '../products/ProductsPage.module.css';
import { Badge } from '../../components/Badge.jsx';
import { Card, PageHeader } from '../../components/Card.jsx';
import { DataTable } from '../../components/DataTable.jsx';
import { Field } from '../../components/Field.jsx';
import { StateView } from '../../components/StateView.jsx';
import { useMovements } from './useMovements.js';
import { formatDateTime, formatDelta, humanize } from '../../core/format.js';

const TYPES = ['RECEIPT', 'REMOVAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'WRITE_OFF', 'ADJUSTMENT'];
const SOURCES = ['CAMERA', 'MANUAL', 'SYSTEM'];

const COLUMNS = [
  {
    key: 'product',
    header: 'Product',
    primary: true,
    render: (row) => <Link to={`/products/${row.productId}`}>{row.productName}</Link>,
  },
  { key: 'when', header: 'When', render: (row) => formatDateTime(row.createdAt) },
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

/** Full stock history with time, product, shelf, change and source (FR-09). */
export default function MovementsPage() {
  const [type, setType] = useState('');
  const [source, setSource] = useState('');
  const { rows, error, loading, reload } = useMovements({ type, source });

  return (
    <>
      <PageHeader title="Stock movements" subtitle="Every change of stock, newest first." />
      <Card flush>
        <div className={styles.filters}>
          <Field as="select" label="Type" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="">All types</option>
            {TYPES.map((value) => (
              <option key={value} value={value}>
                {humanize(value)}
              </option>
            ))}
          </Field>
          <Field
            as="select"
            label="Source"
            value={source}
            onChange={(e) => setSource(e.target.value)}
          >
            <option value="">All sources</option>
            {SOURCES.map((value) => (
              <option key={value} value={value}>
                {humanize(value)}
              </option>
            ))}
          </Field>
        </div>
        <StateView
          loading={loading}
          error={error}
          onRetry={reload}
          empty={rows.length === 0}
          emptyText="No movements match the filters."
        >
          <DataTable
            caption="Stock movements"
            columns={COLUMNS}
            rows={rows}
            rowKey={(row) => row.id}
          />
        </StateView>
      </Card>
    </>
  );
}
