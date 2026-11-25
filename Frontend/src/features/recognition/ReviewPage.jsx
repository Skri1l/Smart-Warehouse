import styles from './ReviewPage.module.css';
import { useReview } from './useReview.js';
import { Badge } from '../../components/Badge.jsx';
import { Button } from '../../components/Button.jsx';
import { Card, PageHeader } from '../../components/Card.jsx';
import { DataTable } from '../../components/DataTable.jsx';
import { StateView } from '../../components/StateView.jsx';
import { formatDateTime, formatDelta, formatPercent, humanize } from '../../core/format.js';
import { REVIEW_CONFIDENCE } from '../../core/stock.js';

const STATUS_TONES = { CONFIRMED: 'ok', REJECTED: 'neutral', FAILED: 'danger', DETECTED: 'warn' };

const HISTORY_COLUMNS = [
  { key: 'product', header: 'Product', primary: true, render: (row) => row.productName },
  { key: 'when', header: 'Captured', render: (row) => formatDateTime(row.capturedAt) },
  { key: 'shelf', header: 'Shelf', render: (row) => row.shelfLabel },
  { key: 'delta', header: 'Change', align: 'end', render: (row) => formatDelta(row.quantityDelta) },
  {
    key: 'confidence',
    header: 'Confidence',
    align: 'end',
    render: (row) => formatPercent(row.confidence),
  },
  {
    key: 'status',
    header: 'Result',
    render: (row) => <Badge tone={STATUS_TONES[row.status]}>{humanize(row.status)}</Badge>,
  },
];

/** Manual review of low-confidence shelf recognitions before they change stock (FR-08). */
export default function ReviewPage() {
  const { pending, history, error, loading, reload, decide, busyId, actionError } = useReview();

  return (
    <>
      <PageHeader
        title="Recognition review"
        subtitle={`Results below ${formatPercent(REVIEW_CONFIDENCE)} confidence wait here instead of changing stock.`}
      />
      <StateView loading={loading} error={error} onRetry={reload}>
        {actionError ? (
          <p className={styles.actionError} role="alert">
            {actionError.message}
          </p>
        ) : null}
        <section aria-labelledby="pending-title" className={styles.section}>
          <h2 id="pending-title">Waiting for review ({pending.length})</h2>
          {pending.length === 0 ? (
            <Card>
              <p className={styles.muted}>Nothing to review. New uncertain photos appear here.</p>
            </Card>
          ) : (
            <ul className={styles.queue}>
              {pending.map((item) => (
                <li key={item.id} className={styles.item}>
                  <div className={styles.photo} aria-hidden="true">
                    <span>Shelf {item.shelfLabel}</span>
                  </div>
                  <div className={styles.itemBody}>
                    <h3>{item.productName}</h3>
                    <dl className={styles.facts}>
                      <div>
                        <dt>Shelf</dt>
                        <dd>{item.shelfLabel}</dd>
                      </div>
                      <div>
                        <dt>Change</dt>
                        <dd>{formatDelta(item.quantityDelta)} pcs</dd>
                      </div>
                      <div>
                        <dt>Detected</dt>
                        <dd>{item.detectedQuantity} pcs</dd>
                      </div>
                      <div>
                        <dt>Confidence</dt>
                        <dd>
                          <Badge tone="warn">{formatPercent(item.confidence)}</Badge>
                        </dd>
                      </div>
                    </dl>
                    <p className={styles.muted}>Captured {formatDateTime(item.capturedAt)}</p>
                    <div className={styles.actions}>
                      <Button
                        variant="primary"
                        icon="check"
                        busy={busyId === `${item.id}`}
                        disabled={busyId !== null}
                        onClick={() => decide(item.id, true)}
                      >
                        Confirm
                      </Button>
                      <Button
                        variant="danger"
                        icon="close"
                        disabled={busyId !== null}
                        onClick={() => decide(item.id, false)}
                      >
                        Reject
                      </Button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
        <Card title="Recent results" flush>
          <DataTable
            caption="Recent recognition results"
            columns={HISTORY_COLUMNS}
            rows={history}
            rowKey={(row) => row.id}
            pageSize={10}
          />
        </Card>
      </StateView>
    </>
  );
}
