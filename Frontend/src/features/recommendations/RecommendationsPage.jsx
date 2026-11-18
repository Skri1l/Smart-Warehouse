import { useState } from 'react';
import styles from './RecommendationsPage.module.css';
import { useRecommendations } from './useRecommendations.js';
import { Badge } from '../../components/Badge.jsx';
import { Button } from '../../components/Button.jsx';
import { PageHeader } from '../../components/Card.jsx';
import { Field } from '../../components/Field.jsx';
import { Modal } from '../../components/Modal.jsx';
import { StateView } from '../../components/StateView.jsx';
import { formatDate, formatRelative, humanize } from '../../core/format.js';
import { daysUntil } from '../../core/stock.js';

const STATUS_TONES = {
  NEW: 'info',
  APPROVED: 'ok',
  ORDERED: 'ok',
  COMPLETED: 'neutral',
  REJECTED: 'neutral',
};

function EditDialog({ row, busy, error, onSave, onClose }) {
  const [quantity, setQuantity] = useState(String(row.recommendedQuantity));
  const [requiredBy, setRequiredBy] = useState(row.requiredBy);
  const [reason, setReason] = useState(row.reason);
  const [errors, setErrors] = useState({});

  const handleSubmit = (event) => {
    event.preventDefault();
    const next = {};
    const parsed = Number(quantity);
    if (!Number.isInteger(parsed) || parsed <= 0) next.quantity = 'Enter a whole number above 0.';
    if (!requiredBy) next.requiredBy = 'Choose a date.';
    else if (daysUntil(requiredBy) < 0) next.requiredBy = 'The date cannot be in the past.';
    if (!reason.trim()) next.reason = 'Explain the change.';
    setErrors(next);
    if (Object.keys(next).length === 0) {
      onSave({ recommendedQuantity: parsed, requiredBy, reason: reason.trim() });
    }
  };

  return (
    <Modal
      title={`Modify: ${row.productName}`}
      onClose={onClose}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" form="edit-recommendation" variant="primary" busy={busy}>
            Save
          </Button>
        </>
      }
    >
      <form id="edit-recommendation" className={styles.form} onSubmit={handleSubmit} noValidate>
        {error ? (
          <p className={styles.formError} role="alert">
            {error.message}
          </p>
        ) : null}
        <Field
          label="Quantity, pcs"
          type="number"
          inputMode="numeric"
          min="1"
          value={quantity}
          onChange={(event) => setQuantity(event.target.value)}
          error={errors.quantity}
        />
        <Field
          label="Required by"
          type="date"
          value={requiredBy}
          onChange={(event) => setRequiredBy(event.target.value)}
          error={errors.requiredBy}
        />
        <Field
          as="textarea"
          label="Reason"
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          error={errors.reason}
        />
      </form>
    </Modal>
  );
}

/** Manager decides on purchase recommendations: approve, reject or modify (FR-16). */
export default function RecommendationsPage() {
  const {
    rows,
    error,
    loading,
    reload,
    busyKey,
    actionError,
    clearActionError,
    approve,
    reject,
    modify,
  } = useRecommendations();
  const [editing, setEditing] = useState(null);

  const closeEditor = () => {
    clearActionError();
    setEditing(null);
  };

  const newCount = rows.filter((row) => row.status === 'NEW').length;

  return (
    <>
      <PageHeader
        title="Purchase recommendations"
        subtitle={`${newCount} waiting for a decision. Calculated from stock, demand plans and shelf life.`}
      />
      {actionError && !editing ? (
        <p className={styles.formError} role="alert">
          {actionError.message}
        </p>
      ) : null}
      <StateView
        loading={loading}
        error={error}
        onRetry={reload}
        empty={rows.length === 0}
        emptyText="No recommendations yet."
      >
        <ul className={styles.list}>
          {rows.map((row) => {
            const days = daysUntil(row.requiredBy);
            return (
              <li key={row.id} className={styles.item}>
                <div className={styles.head}>
                  <div>
                    <h2 className={styles.title}>{row.productName}</h2>
                    <span className={styles.muted}>{row.sku}</span>
                  </div>
                  <Badge tone={STATUS_TONES[row.status]}>{humanize(row.status)}</Badge>
                </div>
                <dl className={styles.facts}>
                  <div>
                    <dt>Quantity</dt>
                    <dd>{row.recommendedQuantity} pcs</dd>
                  </div>
                  <div>
                    <dt>Required by</dt>
                    <dd>
                      {formatDate(row.requiredBy)}
                      {row.status === 'NEW' && days >= 0 && days <= 3 ? (
                        <>
                          {' '}
                          <Badge tone="warn">in {days} d</Badge>
                        </>
                      ) : null}
                    </dd>
                  </div>
                  <div>
                    <dt>Created</dt>
                    <dd>{formatRelative(row.createdAt)}</dd>
                  </div>
                </dl>
                <p className={styles.reason}>{row.reason}</p>
                {row.status === 'NEW' ? (
                  <div className={styles.actions}>
                    <Button
                      variant="primary"
                      icon="check"
                      busy={busyKey === `approve-${row.id}`}
                      disabled={busyKey !== null}
                      onClick={() => approve(row.id)}
                    >
                      Approve
                    </Button>
                    <Button icon="edit" disabled={busyKey !== null} onClick={() => setEditing(row)}>
                      Modify
                    </Button>
                    <Button
                      variant="danger"
                      icon="close"
                      busy={busyKey === `reject-${row.id}`}
                      disabled={busyKey !== null}
                      onClick={() => reject(row.id)}
                    >
                      Reject
                    </Button>
                  </div>
                ) : null}
              </li>
            );
          })}
        </ul>
      </StateView>
      {editing ? (
        <EditDialog
          row={editing}
          busy={busyKey === `edit-${editing.id}`}
          error={actionError}
          onClose={closeEditor}
          onSave={async (changes) => {
            if (await modify(editing, changes)) setEditing(null);
          }}
        />
      ) : null}
    </>
  );
}
