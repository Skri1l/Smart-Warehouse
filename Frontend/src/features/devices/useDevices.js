import { useCallback } from 'react';
import { activateDevice, createDevice, deactivateDevice, listDevices } from '../../api/devices.js';
import { listShelves } from '../../api/warehouses.js';
import { POLL_INTERVAL_MS, useResource } from '../../hooks/useResource.js';
import { useMutation } from '../../hooks/useMutation.js';

/** Shelf cameras: list, register (FR-04), activate / deactivate. */
export function useDevices() {
  const loader = useCallback(async () => {
    const [devices, shelves] = await Promise.all([listDevices(), listShelves()]);
    return { devices, shelves };
  }, []);
  const { data, error, loading, reload } = useResource(loader, { pollMs: POLL_INTERVAL_MS });
  const mutation = useMutation();

  const shelfById = new Map((data?.shelves ?? []).map((shelf) => [shelf.id, shelf]));
  const rows = (data?.devices ?? []).map((device) => ({
    ...device,
    shelfLabel: shelfById.get(device.shelfId)?.label ?? `#${device.shelfId}`,
  }));
  const freeShelves = (data?.shelves ?? []).filter(
    (shelf) => !data.devices.some((device) => device.shelfId === shelf.id),
  );

  const act = async (key, action) => {
    const ok = await mutation.run(key, action);
    if (ok) await reload();
    return ok;
  };

  return {
    rows,
    freeShelves,
    error: data ? null : error,
    loading: loading && !data,
    reload,
    busyKey: mutation.busyKey,
    actionError: mutation.error,
    clearActionError: mutation.clearError,
    /** @param {{ deviceKey: string, shelfId: number }} device */
    register: (device) => act('register', () => createDevice(device)),
    toggle: (device) =>
      act(`toggle-${device.id}`, () =>
        device.status === 'ACTIVE' ? deactivateDevice(device.id) : activateDevice(device.id),
      ),
  };
}
