import { HttpResponse, delay, http } from 'msw';
import { MOCK_USERS, applyStockChange, db, nextId } from './db.js';

/*
 * Mock of the backend REST protocol. Every handler mirrors one Spring controller method
 * (same path, verb, status code and body) under `/api/v1`. Auth endpoints are the planned
 * contract from src/api/auth.js. Swap to the real backend with `npm run dev:api`.
 */

const API = '/api/v1';
const SESSION_COOKIE = 'sw_session';

let latencyMs = 0;

/** Artificial network latency for the browser mock (tests run with 0). */
export function setMockLatency(ms) {
  latencyMs = ms;
}

async function respond() {
  if (latencyMs > 0) await delay(latencyMs + Math.floor(Math.random() * latencyMs));
}

/**
 * Error body: `{ status, message }` is what GlobalExceptionHandler returns today;
 * `error` is the stable code API_GUIDELINES.md adds.
 */
function error(status, code, message) {
  return HttpResponse.json({ status, error: code, message }, { status });
}

const notFound = (what, id) => error(404, 'NOT_FOUND', `${what} with id ${id} not found`);

/** The user as the auth endpoints return it (no account status). */
function toCurrentUser({ id, username, email, role }) {
  return { id, username, email, role };
}

function currentUser(cookies) {
  return MOCK_USERS.find((user) => user.username === cookies[SESSION_COOKIE]) ?? null;
}

/**
 * Wrap a resolver with session and role checks, like Spring Security will do.
 * @param {(info: any, user: object) => any} resolver
 * @param {string[]} [roles] Allowed roles; any authenticated user when omitted.
 */
function secured(resolver, roles) {
  return async (info) => {
    await respond();
    const user = currentUser(info.cookies);
    if (!user) return error(401, 'UNAUTHORIZED', 'Authentication required');
    if (roles && !roles.includes(user.role)) {
      return error(403, 'FORBIDDEN', 'You do not have permission for this action');
    }
    return resolver(info, user);
  };
}

const MANAGERS = ['MANAGER', 'ADMIN'];
const ADMINS = ['ADMIN'];

/** GET list + GET by id for a plain table. */
function readable(path, table, label) {
  return [
    http.get(
      `${API}${path}`,
      secured(() => HttpResponse.json(db[table])),
    ),
    http.get(
      `${API}${path}/:id`,
      secured(({ params }) => {
        const row = db[table].find((item) => item.id === Number(params.id));
        return row ? HttpResponse.json(row) : notFound(label, params.id);
      }),
    ),
  ];
}

/** PATCH /{id}/<action> that sets a status field, returning 200 with an empty body. */
function statusAction(path, table, label, field, value, roles) {
  return http.patch(
    `${API}${path}/:id/${value.action}`,
    secured(({ params }) => {
      const row = db[table].find((item) => item.id === Number(params.id));
      if (!row) return notFound(label, params.id);
      row[field] = value.status;
      return new HttpResponse(null, { status: 200 });
    }, roles),
  );
}

const authHandlers = [
  http.post(`${API}/auth/login`, async ({ request }) => {
    await respond();
    const body = await request.json().catch(() => ({}));
    const user = MOCK_USERS.find((item) => item.username === body.username);
    if (!user || !body.password || body.password !== user.username) {
      return error(401, 'UNAUTHORIZED', 'Invalid username or password');
    }
    return HttpResponse.json(toCurrentUser(user), {
      headers: { 'Set-Cookie': `${SESSION_COOKIE}=${user.username}; Path=/; SameSite=Strict` },
    });
  }),
  http.get(
    `${API}/auth/me`,
    secured((info, user) => HttpResponse.json(toCurrentUser(user))),
  ),
  http.post(`${API}/auth/logout`, async () => {
    await respond();
    return new HttpResponse(null, {
      status: 204,
      headers: { 'Set-Cookie': `${SESSION_COOKIE}=; Path=/; Max-Age=0` },
    });
  }),
];

const productHandlers = [
  http.get(
    `${API}/products/search`,
    secured(({ request }) => {
      const name = (new URL(request.url).searchParams.get('name') ?? '').toLowerCase();
      return HttpResponse.json(db.products.filter((p) => p.name.toLowerCase().includes(name)));
    }),
  ),
  http.get(
    `${API}/products/sku/:sku`,
    secured(({ params }) => {
      const row = db.products.find((p) => p.sku === params.sku);
      return row ? HttpResponse.json(row) : error(404, 'NOT_FOUND', 'Product not found');
    }),
  ),
  ...readable('/products', 'products', 'Product'),
  http.post(
    `${API}/products`,
    secured(async ({ request }) => {
      const body = await request.json();
      if (!body.sku || !body.name)
        return error(400, 'VALIDATION_ERROR', 'SKU and name are required');
      if (db.products.some((p) => p.sku === body.sku)) {
        return error(409, 'CONFLICT', `Product with SKU ${body.sku} already exists`);
      }
      const id = nextId(db.products);
      db.products.push({
        id,
        sku: body.sku,
        name: body.name,
        description: body.description ?? null,
      });
      return HttpResponse.json(id, { status: 201 });
    }, ADMINS),
  ),
];

const stockHandlers = [
  http.get(
    `${API}/shelf-stock/shelf/:shelfId`,
    secured(({ params }) =>
      HttpResponse.json(db.shelfStock.filter((row) => row.shelfId === Number(params.shelfId))),
    ),
  ),
  http.get(
    `${API}/shelf-stock/product/:productId`,
    secured(({ params }) =>
      HttpResponse.json(db.shelfStock.filter((row) => row.productId === Number(params.productId))),
    ),
  ),
  ...readable('/shelf-stock', 'shelfStock', 'Shelf stock'),
  http.get(
    `${API}/stock-movements/product/:productId`,
    secured(({ params }) =>
      HttpResponse.json(
        db.stockMovements.filter((row) => row.productId === Number(params.productId)),
      ),
    ),
  ),
  http.get(
    `${API}/stock-movements/shelf/:shelfId`,
    secured(({ params }) =>
      HttpResponse.json(db.stockMovements.filter((row) => row.shelfId === Number(params.shelfId))),
    ),
  ),
  ...readable('/stock-movements', 'stockMovements', 'Stock movement'),
];

const structureHandlers = [
  ...readable('/warehouses', 'warehouses', 'Warehouse'),
  ...readable('/racks', 'racks', 'Rack'),
  ...readable('/shelves', 'shelves', 'Shelf'),
];

const recognitionHandlers = [
  http.get(
    `${API}/recognition-results/status/:status`,
    secured(({ params }) =>
      HttpResponse.json(db.recognitionResults.filter((row) => row.status === params.status)),
    ),
  ),
  ...readable('/recognition-results', 'recognitionResults', 'Recognition result'),
  ...readable('/image-events', 'imageEvents', 'Image event'),
  http.patch(
    `${API}/recognition-results/:id/confirm`,
    secured(({ params }) => {
      const row = db.recognitionResults.find((item) => item.id === Number(params.id));
      if (!row) return notFound('Recognition result', params.id);
      if (row.status !== 'DETECTED') {
        return error(409, 'CONFLICT', `Recognition result is already ${row.status}`);
      }
      row.status = 'CONFIRMED';
      const event = db.imageEvents.find((item) => item.id === row.imageEventId);
      if (event && row.quantityDelta !== 0) {
        applyStockChange({
          shelfId: event.shelfId,
          productId: row.productId,
          quantityDelta: row.quantityDelta,
          source: 'CAMERA',
        });
      }
      return new HttpResponse(null, { status: 200 });
    }),
  ),
  http.patch(
    `${API}/recognition-results/:id/reject`,
    secured(({ params }) => {
      const row = db.recognitionResults.find((item) => item.id === Number(params.id));
      if (!row) return notFound('Recognition result', params.id);
      if (row.status !== 'DETECTED') {
        return error(409, 'CONFLICT', `Recognition result is already ${row.status}`);
      }
      row.status = 'REJECTED';
      return new HttpResponse(null, { status: 200 });
    }),
  ),
];

const RECOMMENDATIONS = '/replenishment-recommendations';
const recommendationHandlers = [
  ...readable(RECOMMENDATIONS, 'recommendations', 'Replenishment recommendation'),
  http.put(
    `${API}${RECOMMENDATIONS}/:id`,
    secured(async ({ params, request }) => {
      const row = db.recommendations.find((item) => item.id === Number(params.id));
      if (!row) return notFound('Replenishment recommendation', params.id);
      const body = await request.json();
      if (!(body.recommendedQuantity > 0) || !body.requiredBy || !body.reason) {
        return error(400, 'VALIDATION_ERROR', 'Quantity, required-by date and reason are required');
      }
      Object.assign(row, {
        productId: body.productId,
        recommendedQuantity: body.recommendedQuantity,
        requiredBy: body.requiredBy,
        reason: body.reason,
      });
      return HttpResponse.json(row);
    }, MANAGERS),
  ),
  ...[
    { action: 'approve', status: 'APPROVED' },
    { action: 'reject', status: 'REJECTED' },
    { action: 'ordered', status: 'ORDERED' },
    { action: 'complete', status: 'COMPLETED' },
  ].map((value) =>
    statusAction(
      RECOMMENDATIONS,
      'recommendations',
      'Replenishment recommendation',
      'status',
      value,
      MANAGERS,
    ),
  ),
];

const deviceHandlers = [
  ...readable('/devices', 'devices', 'Device'),
  http.post(
    `${API}/devices`,
    secured(async ({ request }) => {
      const body = await request.json();
      if (!body.deviceKey || !body.shelfId) {
        return error(400, 'VALIDATION_ERROR', 'Device key and shelf are required');
      }
      if (db.devices.some((d) => d.deviceKey === body.deviceKey)) {
        return error(409, 'CONFLICT', `Device ${body.deviceKey} already exists`);
      }
      if (db.devices.some((d) => d.shelfId === Number(body.shelfId))) {
        return error(409, 'CONFLICT', 'This shelf already has a device');
      }
      const id = nextId(db.devices);
      db.devices.push({
        id,
        deviceKey: body.deviceKey,
        status: 'ACTIVE',
        lastSeenAt: null,
        shelfId: Number(body.shelfId),
      });
      return HttpResponse.json(id, { status: 201 });
    }, ADMINS),
  ),
  statusAction(
    '/devices',
    'devices',
    'Device',
    'status',
    { action: 'activate', status: 'ACTIVE' },
    ADMINS,
  ),
  statusAction(
    '/devices',
    'devices',
    'Device',
    'status',
    { action: 'deactivate', status: 'INACTIVE' },
    ADMINS,
  ),
];

export const handlers = [
  ...authHandlers,
  ...productHandlers,
  ...stockHandlers,
  ...structureHandlers,
  ...recognitionHandlers,
  ...recommendationHandlers,
  ...deviceHandlers,
];
