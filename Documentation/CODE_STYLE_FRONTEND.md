# Frontend Code Style (JavaScript / React Browser UI)

The frontend is a **responsive single-page web application** that runs in the browser on phones, tablets and desktops. It is built with **React 18 + Vite** in plain JavaScript and talks to the backend exclusively over its REST API. It is served by the reverse proxy of the hosted deployment on the same origin as `/api`. See [ADR-0012](adr/0012-browser-ui-javascript.md) and [ADR-0011](adr/0011-hosted-server-deployment.md); [ADR-0006](adr/0006-frontend-pyside6-desktop.md) (PySide6 desktop) is superseded.

Base style: [Airbnb-style](https://github.com/airbnb/javascript) modern JavaScript, mechanically enforced by **ESLint** (`eslint-plugin-react`, `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`) and **Prettier** (formatting). Configuration lives in `Frontend/eslint.config.js` and `Frontend/.prettierrc`.

```bash
cd Frontend
npm ci                 # install exact versions from package-lock.json
npm run dev            # Vite dev server, proxies /api to http://localhost:8080
npm run lint           # ESLint
npm run format         # Prettier --write
npm test               # Vitest + React Testing Library
npm run build          # production build into dist/
```

> The Vite project has not been scaffolded yet; `Frontend/` still contains the retired PySide6 client. The structure below is the target layout.

## Project structure

One Vite app, organised by feature, with shared layers next to it:

```text
Frontend/
├── index.html
├── src/
│   ├── main.jsx               Entry point: mounts <App/>, router, providers
│   ├── App.jsx                Routes and the responsive app shell (header, nav)
│   ├── features/              One folder per business area
│   │   ├── stock/
│   │   │   ├── StockPage.jsx
│   │   │   ├── StockTable.jsx
│   │   │   ├── StockPage.module.css
│   │   │   ├── useStock.js     Data hook for this feature
│   │   │   └── StockPage.test.jsx
│   │   ├── products/  shelves/  recommendations/  auth/ ...
│   ├── components/            Reusable presentational components (Button, DataTable, Modal)
│   ├── api/                   The only place that knows REST paths
│   │   ├── http.js            fetch wrapper: base URL, JSON, errors, credentials
│   │   └── products.js        One module per backend resource
│   ├── core/                  Pure logic: formatting, validation, calculations (no React, no fetch)
│   ├── hooks/                 Shared hooks (useAuth, useMediaQuery, ...)
│   └── styles/
│       ├── tokens.css         Colors, spacing, font sizes, breakpoints as CSS custom properties
│       └── global.css         Reset and base element styles
├── public/                    Static assets copied as-is (favicon, manifest)
├── tests/                     Test setup and shared fixtures / API mocks
├── package.json
├── package-lock.json          Committed
├── vite.config.js
└── eslint.config.js
```

`dist/` and `node_modules/` are build output and are never committed.

## Naming

| Element | Convention | Example |
|---|---|---|
| Component file and component | `PascalCase.jsx` | `StockTable.jsx`, `function StockTable()` |
| Hook | `camelCase`, starts with `use` | `useStock.js`, `useMediaQuery` |
| Other modules | `camelCase.js` | `http.js`, `formatQuantity.js` |
| Functions / variables | `camelCase` | `loadCurrentStock` |
| Constants | `UPPER_SNAKE_CASE` | `DEFAULT_PAGE_SIZE` |
| CSS Module file | same name as component + `.module.css` | `StockTable.module.css` |
| CSS classes | `camelCase` (accessed as `styles.rowLow`) | `.rowLow` |
| Event handler props / functions | `onX` props, `handleX` implementations | `onSave={handleSave}` |
| Test file | `<Module>.test.jsx` next to the module | `StockTable.test.jsx` |
| Test name | `it('shows <result> when <condition>')` | `it('shows an error when login fails')` |

## JavaScript rules

- ES modules only, `const` by default, `let` when reassigned, never `var`.
- Function components and hooks only; no class components.
- Named exports; a default export only where a tool requires it (lazy-loaded route pages).
- `async`/`await` instead of `.then()` chains; every awaited API call has error handling.
- Exported functions, hooks and non-trivial components have **JSDoc** with parameter and return types:

```js
/**
 * Estimate stock remaining for a product on a given date.
 * @param {number} currentQuantity Quantity on the shelves today.
 * @param {number} dailyDemand Planned consumption per day.
 * @param {number} days Days until the target date.
 * @returns {number} Projected quantity; negative means a shortage.
 */
export function projectStock(currentQuantity, dailyDemand, days) {
  return currentQuantity - dailyDemand * days;
}
```

- No commented-out code, no `console.log` in committed code (ESLint `no-console` as error, except `console.error` inside the error boundary).

## Layering rules

- **Pages and components** (`features/*`, `components/`) render UI and handle user events. They never call `fetch` and never contain business calculations.
- **Hooks** (`useStock`, `useAuth`) hold loading/error/data state and call `api/` and `core/`. A component gets everything it needs from one hook call.
- **`api/`** is the only place that knows endpoint paths, query parameters and response shapes, written against the backend's OpenAPI spec (`/v3/api-docs`, see [API_GUIDELINES.md](API_GUIDELINES.md)), not guessed. It turns the backend's `ErrorResponse` into a thrown `ApiError` with `status` and `message`.
- **`core/`** has zero React and zero HTTP imports: pure functions, the cheapest code to unit test.
- Components in `components/` know nothing about the business domain (no product/shelf specifics).

## Responsive (adaptive) UI rules

The UI must be fully usable on a phone held by a warehouse worker and on a manager's wide monitor. These rules are mandatory for every screen.

### Breakpoints

Mobile-first: base styles target the smallest screen, larger screens are added with `min-width` media queries. Only these breakpoints are used (defined once in `styles/tokens.css`):

| Name | Min width | Typical device | Layout |
|---|---|---|---|
| `xs` (base) | 0 | Phone, portrait (target 360 px) | Single column, bottom or burger navigation |
| `sm` | 600 px | Phone landscape, small tablet | Single column, wider cards |
| `md` | 900 px | Tablet, small laptop | Side navigation appears, two-column forms |
| `lg` | 1200 px | Desktop | Dashboard grid, full tables |
| `xl` | 1600 px | Wide monitor | Content width capped, no stretching |

### Layout

- Use CSS Grid and Flexbox; no fixed pixel widths on containers, no absolute positioning for layout.
- No horizontal page scroll at any width from 360 px up. Only a table container may scroll horizontally, and only if it cannot be reflowed.
- Wide tables become a **card list** below `md` (each row a card with label/value pairs), or hide secondary columns. Decide per table; the primary columns (product, shelf, quantity, status) are always visible.
- Content max width at `xl` is capped (e.g. 1440 px) and centred.
- Side navigation at `md`+; on smaller screens a collapsible menu or bottom tab bar with at most five entries.
- Dialogs become full-screen sheets below `sm`.
- Charts resize with their container (`maintainAspectRatio: false` inside a sized wrapper) and stay readable at 360 px: fewer ticks, legend below the chart.

### Sizing and touch

- Spacing, font sizes and radii come from tokens in `tokens.css`; no magic numbers in component CSS.
- Font sizes in `rem`; base 16 px, never smaller than 14 px for body text. Text must stay usable at 200 % browser zoom.
- Touch targets at least **44 × 44 px**, with at least 8 px between neighbouring targets.
- Everything that has a hover effect also works by tap and keyboard; hover is never the only way to reveal an action.
- Images and icons scale (`max-width: 100%`, SVG icons); no text inside images.
- Use proper input types (`type="number"`, `inputmode="numeric"`, `type="search"`) so phones show the right keyboard.

### Verification

- Every PR that changes a screen is checked at **360 px, 768 px and 1440 px** width (browser dev tools device mode), in portrait and landscape for phone size. Screenshots at 360 px and 1440 px go in the PR description.
- Component tests that depend on layout use a mocked `matchMedia`; visual layout itself is checked manually.

## Accessibility

- Semantic HTML first (`<button>`, `<nav>`, `<table>`, `<label>`); `div` with `onClick` is not allowed (`jsx-a11y` enforces this).
- Every form field has a visible label; errors are shown next to the field as text, not by color alone.
- Color contrast at least WCAG AA (4.5:1 for normal text). Stock status (low / ok / expired) uses an icon or text in addition to color.
- All actions reachable by keyboard with a visible focus outline.

## State and data fetching

- Server data lives in feature hooks; global state is limited to the authenticated user and their role (React Context). No Redux unless a real need appears (requires an ADR).
- Every data view handles **loading, empty, error and success** states explicitly.
- Lists from the API are paginated per [API_GUIDELINES.md](API_GUIDELINES.md); the UI never loads unbounded collections.
- Live data (stock after a shelf photo) is refreshed by polling at a moderate interval (≥ 10 s) while the page is visible; pause polling when `document.hidden`.

## Authentication & authorization

- The session credential is never written to `localStorage`, `sessionStorage` or a readable cookie. The preferred mechanism is an `HttpOnly`, `Secure`, `SameSite=Strict` cookie set by the backend; `http.js` sends requests with `credentials: 'same-origin'`.
- A `401` from any request sends the user to the login page; a `403` shows a "no access" message.
- Navigation and buttons are shown or hidden by the user's role for UX only. The backend is the only authority on permissions (see [API_GUIDELINES.md](API_GUIDELINES.md)).
- Never render server-provided strings as HTML (`dangerouslySetInnerHTML` is forbidden).

## Configuration

- The API is always called with a relative base path (`/api/v1`), because UI and API share one origin in production (ADR-0011). The dev server proxies it in `vite.config.js`.
- Build-time settings use Vite env variables prefixed `VITE_`; they end up in the public bundle, so **no secrets** ever go there.

## Dependency licensing

The project is proprietary (see [LICENSE](../LICENSE)); every dependency must be usable in closed-source software:

- Allowed: MIT, BSD, Apache-2.0, ISC.
- Rejected: GPL, AGPL, and any "non-commercial" or source-available licenses.
- Before adding a dependency, check its license, prefer small well-maintained packages, and add it to [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). CI runs `license-checker --onlyAllow` to catch violations.
- `package-lock.json` is committed; CI installs with `npm ci`.

## Testing

- **Vitest** + **React Testing Library** + `@testing-library/user-event`. Query elements the way a user finds them (`getByRole`, `getByLabelText`), not by CSS class.
- `core/` is tested with plain unit tests.
- `api/` and hooks are tested against mocked HTTP (MSW, `msw`), never against a live backend. End-to-end tests against a deployed stack belong to Milestone 8 integration testing.

## Build and deployment

- `npm run build` produces static files in `dist/`; the reverse proxy from [ADR-0011](adr/0011-hosted-server-deployment.md) serves them and falls back to `index.html` for client-side routes.
- File names in `dist/assets/` are content-hashed and can be cached forever; `index.html` is served with `no-cache`.
- Route pages are lazy-loaded (`React.lazy`) so a phone on a slow connection downloads only the screen it opens.

## Formatting reference

Prettier handles line length (100), quotes (single), semicolons (on), trailing commas and indentation (2 spaces) automatically. If `npm run lint` and `prettier --check .` pass, the formatting is correct — don't hand-tune it further.
