import { useState } from 'react';
import styles from '../recommendations/RecommendationsPage.module.css';
import { useDevices } from './useDevices.js';
import { Badge } from '../../components/Badge.jsx';
import { Button } from '../../components/Button.jsx';
import { Card, PageHeader } from '../../components/Card.jsx';
import { DataTable } from '../../components/DataTable.jsx';
import { Field } from '../../components/Field.jsx';
import { Modal } from '../../components/Modal.jsx';
import { StateView } from '../../components/StateView.jsx';
import { formatRelative } from '../../core/format.js';
import { isValid, validateDevice } from '../../core/validation.js';

function RegisterDialog({ shelves, busy, error, onSave, onClose }) {
  const [deviceKey, setDeviceKey] = useState('');
  const [shelfId, setShelfId] = useState(shelves[0] ? String(shelves[0].id) : '');
  const [errors, setErrors] = useState({});

  const handleSubmit = (event) => {
    event.preventDefault();
    const next = validateDevice({ deviceKey, shelfId });
    setErrors(next);
    if (isValid(next)) onSave({ deviceKey, shelfId: Number(shelfId) });
  };

  return (
    <Modal
      title="Register a shelf camera"
      onClose={onClose}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" form="register-device" variant="primary" busy={busy}>
            Register
          </Button>
        </>
      }
    >
      <form id="register-device" className={styles.form} onSubmit={handleSubmit} noValidate>
        {error ? (
          <p className={styles.formError} role="alert">
            {error.message}
          </p>
        ) : null}
        <Field
          label="Device key"
          autoCapitalize="none"
          placeholder="shelf-b3-cam"
          hint="The same key goes into the shelf agent config (device.toml)."
          value={deviceKey}
          onChange={(event) => setDeviceKey(event.target.value.trim())}
          error={errors.deviceKey}
        />
        <Field
          as="select"
          label="Shelf"
          value={shelfId}
          onChange={(event) => setShelfId(event.target.value)}
          error={errors.shelfId}
          disabled={shelves.length === 0}
        >
          {shelves.length === 0 ? <option value="">Every shelf already has a camera</option> : null}
          {shelves.map((shelf) => (
            <option key={shelf.id} value={shelf.id}>
              Shelf {shelf.label}
            </option>
          ))}
        </Field>
      </form>
    </Modal>
  );
}

/** Admin view of shelf cameras and their heartbeat. */
export default function DevicesPage() {
  const devices = useDevices();
  const [registering, setRegistering] = useState(false);
  const { busyKey } = devices;

  const columns = [
    { key: 'key', header: 'Device', primary: true, render: (row) => <code>{row.deviceKey}</code> },
    { key: 'shelf', header: 'Shelf', render: (row) => row.shelfLabel },
    { key: 'seen', header: 'Last seen', render: (row) => formatRelative(row.lastSeenAt) },
    {
      key: 'status',
      header: 'Status',
      render: (row) =>
        row.status === 'ACTIVE' ? <Badge tone="ok">Active</Badge> : <Badge>Inactive</Badge>,
    },
    {
      key: 'actions',
      header: 'Actions',
      align: 'end',
      render: (row) => (
        <Button
          variant={row.status === 'ACTIVE' ? 'danger' : 'secondary'}
          busy={busyKey === `toggle-${row.id}`}
          disabled={busyKey !== null}
          onClick={() => devices.toggle(row)}
        >
          {row.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
        </Button>
      ),
    },
  ];

  const close = () => {
    devices.clearActionError();
    setRegistering(false);
  };

  return (
    <>
      <PageHeader
        title="Devices"
        subtitle="Cameras that photograph shelves and push images to the server."
        actions={
          <Button variant="primary" icon="plus" onClick={() => setRegistering(true)}>
            Register camera
          </Button>
        }
      />
      {devices.actionError && !registering ? (
        <p className={styles.formError} role="alert">
          {devices.actionError.message}
        </p>
      ) : null}
      <Card flush>
        <StateView
          loading={devices.loading}
          error={devices.error}
          onRetry={devices.reload}
          empty={devices.rows.length === 0}
          emptyText="No cameras registered yet."
        >
          <DataTable
            caption="Devices"
            columns={columns}
            rows={devices.rows}
            rowKey={(row) => row.id}
          />
        </StateView>
      </Card>
      {registering ? (
        <RegisterDialog
          shelves={devices.freeShelves}
          busy={busyKey === 'register'}
          error={devices.actionError}
          onClose={close}
          onSave={async (device) => {
            if (await devices.register(device)) setRegistering(false);
          }}
        />
      ) : null}
    </>
  );
}
