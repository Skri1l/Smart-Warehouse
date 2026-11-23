# Frontend

Responsive browser UI for Smart Warehouse: React 18 + Vite, plain JavaScript with JSDoc. Runs on phones, tablets and desktops. Decisions: [ADR-0012](../Documentation/adr/0012-browser-ui-javascript.md); conventions: [CODE_STYLE_FRONTEND.md](../Documentation/CODE_STYLE_FRONTEND.md).

## Run

Node.js 20+.

```bash
cd Frontend
npm ci
npm run dev        # http://localhost:5173, the whole backend is mocked in the browser
npm run dev:api    # real backend: Vite proxies /api to http://localhost:8080
```

In mock mode sign in as `worker`, `manager` or `admin`; the password is the same word. Roles see different navigation (Purchasing for manager/admin, Devices for admin only).

| Command                           | What it does                                                   |
| --------------------------------- | -------------------------------------------------------------- |
| `npm run lint`                    | ESLint (react, hooks, jsx-a11y)                                |
| `npm run format` / `format:check` | Prettier                                                       |
| `npm test`                        | Vitest + React Testing Library, against the same mock handlers |
| `npm run build`                   | Production build into `dist/` (no mock code included)          |
| `npm run licenses`                | Fails on any non-permissive runtime dependency                 |

## Screens

| Route                        | Screen                                                                                  | Requirement         |
| ---------------------------- | --------------------------------------------------------------------------------------- | ------------------- |
| `/login`                     | Sign-in                                                                                 | FR-01               |
| `/`                          | Dashboard: KPIs, 7-day movement chart, low stock, recent movements, new recommendations | FR-17               |
| `/shelves`                   | Racks and shelves: the shelf's product, fill level, batches by expiry, camera status    | M5 shelf/stock view |
| `/products`, `/products/:id` | Catalogue with stock, search and filters; product locations and history                 | FR-03, FR-09, FR-10 |
| `/review`                    | Confirm / reject low-confidence recognitions before they change stock                   | FR-08               |
| `/movements`                 | Stock history filtered by type and source                                               | FR-09               |
| `/recommendations`           | Approve, reject or modify purchase recommendations (manager, admin)                     | FR-15, FR-16        |
| `/devices`                   | Register shelf cameras, activate / deactivate (admin)                                   | FR-04               |

Data views poll every 15 s while the tab is visible.

## Mocked backend

Until the backend is ready, `npm run dev` runs a mock of the whole REST protocol inside the browser with [MSW](https://mswjs.io/) (`VITE_API_MOCK=true` in `.env.development`). The UI code is identical in both modes: `src/api/` calls `fetch('/api/v1/...')` and MSW answers instead of the network.

- `src/mocks/handlers.js` — one handler per Spring controller method the UI uses: same path, verb, status code and response body as the `*Controller.java` / `*ResponseDto.java` in `Backend/`. List endpoints return plain arrays, as the backend does today. Errors use `{ status, error, message }`.
- `src/mocks/db.js` — in-memory seed data (one warehouse, 3 racks, 9 shelves with one product each, 8 products, devices, movements, recognitions, recommendations). Mutations work and reset on reload. Every 15 s a simulated camera "uploads" a photo: confident results change stock, uncertain ones land in the review queue.
- Tests (`tests/server.js`) use the same handlers via `msw/node`.

### Where the UI is ahead of the backend

These are assumptions the backend has to match (or the UI adapts) before `npm run dev:api` works end-to-end:

1. **`/api/v1` prefix.** Controllers are mapped at the root (`/products`, …); API_GUIDELINES.md requires `/api/v1/...`.
2. **Auth endpoints** (`src/api/auth.js`): `POST /api/v1/auth/login` → `200 { id, username, email, role }` + `HttpOnly` session cookie, `GET /api/v1/auth/me` → current user or `401`, `POST /api/v1/auth/logout` → `204`. Role checks: recommendations actions need `MANAGER`/`ADMIN`, device registration needs `ADMIN`.
3. **Error code.** `ErrorResponse` has only `status` and `message`; the UI also reads the stable `error` code from API_GUIDELINES.md when present.
4. **No pagination yet.** Lists are paged on the client (`DataTable`); switch to `?page=&size=` once the backend wraps lists.
5. **"Low stock" and "expiring soon"** are UI constants in `src/core/stock.js` (below 15 pcs, within 7 days); the backend has no per-product minimum yet.
6. **Camera images** are not served to the browser yet; the review screen shows a placeholder instead of the shelf photo.
7. **One product per shelf.** The camera only counts items (Vision returns a count), so it cannot tell products apart. The UI treats a shelf as one product with possibly several batches (expiry dates). `shelf-stock` still allows several products per shelf; when the backend returns that, the shelf card and the dashboard flag it as a setup problem instead of summing the counts.

## Layout

See the target structure in [CODE_STYLE_FRONTEND.md](../Documentation/CODE_STYLE_FRONTEND.md#project-structure): `src/features/*` per business area (page + data hook + CSS module), `src/components/` for domain-free UI, `src/api/` as the only place that knows REST paths, `src/core/` for pure logic, `src/mocks/` for the mocked backend.
