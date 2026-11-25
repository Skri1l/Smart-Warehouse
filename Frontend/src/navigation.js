/**
 * App sections. `roles` hides an entry for other users (UX only — the backend enforces access).
 * The first four visible entries go into the phone bottom bar, the rest under "More".
 * @type {{ path: string, label: string, icon: string, roles?: import('./api/types.js').UserRole[] }[]}
 */
export const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', icon: 'dashboard' },
  { path: '/shelves', label: 'Shelves', icon: 'shelves' },
  { path: '/products', label: 'Products', icon: 'products' },
  { path: '/review', label: 'Review', icon: 'review' },
  { path: '/movements', label: 'Movements', icon: 'movements' },
  {
    path: '/recommendations',
    label: 'Purchasing',
    icon: 'recommendations',
    roles: ['MANAGER', 'ADMIN'],
  },
  { path: '/devices', label: 'Devices', icon: 'devices', roles: ['ADMIN'] },
];
