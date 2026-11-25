# Design system and adaptive UI

The browser UI ([Frontend/](../../Frontend/README.md)) uses one set of design tokens on every device. Phone, tablet and desktop differ only in layout. Colours, type and components stay the same.

- **Mockups (Figma):** [Smart Warehouse — Adaptive UI](https://www.figma.com/design/2cT9dahpeFuGZmCCnzSU1X/Smart-Warehouse-%E2%80%94-Adaptive-UI) (view-only, no sign-in). Pages: `Foundations`, `Components`, `Screens` (sections `Mobile · 375`, `Tablet · 768 portrait`, `Desktop · 1440`, `Landscape · phone 812×375`, `Landscape · tablet 1024×768`)
- **Source of truth for tokens:** [`Frontend/src/styles/tokens.css`](../../Frontend/src/styles/tokens.css)
- **Same tokens for Figma:** [`figma-tokens.json`](figma-tokens.json) (Tokens Studio format, generated from `tokens.css`)

## Devices and layout

Mobile-first CSS with `min-width` breakpoints: `sm 600px · md 900px · lg 1200px · xl 1600px`.

| Device | Width | Main navigation | Content grid |
|---|---|---|---|
| Phone, portrait | < 600px | Bottom tab bar: 4 sections + "More" sheet | KPIs 2 per row, cards in 1 column, tables as card lists, dialogs as bottom sheets |
| Phone landscape / tablet portrait | 600–899px | Bottom tab bar | Filters side by side, centred dialogs |
| Tablet landscape / laptop | 900–1199px | Left sidebar (240px), sticky | KPIs 4 per row, cards in 2 columns, real tables |
| Desktop | ≥ 1200px | Left sidebar | Cards in 3 columns, content max 1440px |

- Layout uses CSS Grid / Flexbox with `fr`, `minmax()` and `auto-fill`. Spacing and type are in `rem`, so browser zoom and the OS text size scale the whole UI.
- Zoom to 200% on a 1280px window gives a 640px layout. It switches to the bottom tab bar, so nothing overlaps and the page never scrolls sideways.
- Tables become cards on narrow screens (`DataTable`). The fixed bottom bar always has matching padding under the content, so the last row stays reachable.
- Orientation changes need no special code: the layout follows the viewport width.

## Colour palette

| Token | Value | Use |
|---|---|---|
| `--color-bg` | `#f4f6fa` | Page background |
| `--color-surface` | `#ffffff` | Cards, header, navigation |
| `--color-surface-muted` | `#f0f2f7` | Hover, secondary areas |
| `--color-border` | `#d9dee8` | Borders, dividers |
| `--color-text` | `#172033` | Main text |
| `--color-text-muted` | `#556079` | Secondary text, labels |
| `--color-primary` / `-hover` / `-soft` | `#1d4ed8` / `#1e40af` / `#e0e8fc` | Actions, links, active nav |
| `--color-ok` / `-soft` | `#166534` / `#dcfce7` | In stock, confirmed |
| `--color-warn` / `-soft` | `#92400e` / `#fef3c7` | Low stock, needs review |
| `--color-danger` / `-soft` | `#b91c1c` / `#fee2e2` | Out of stock, errors, destructive actions |
| `--color-info` / `-soft` | `#1e40af` / `#dbeafe` | Neutral status |
| `--color-focus` | `#f59e0b` | 3px keyboard focus ring |

### Text contrast (WCAG 2.1 AA, needs 4.5:1)

Every text/background pair the UI uses:

| Text | Background | Ratio |
|---|---|---|
| text `#172033` | bg `#f4f6fa` | 15.04:1 |
| text `#172033` | surface `#ffffff` | 16.27:1 |
| text `#172033` | surface-muted `#f0f2f7` | 14.52:1 |
| text-muted `#556079` | bg `#f4f6fa` | 5.82:1 |
| text-muted `#556079` | surface `#ffffff` | 6.30:1 |
| text-muted `#556079` | surface-muted `#f0f2f7` | 5.62:1 |
| text-muted `#556079` | neutral-soft `#e9ecf2` | 5.32:1 |
| primary `#1d4ed8` | surface `#ffffff` | 6.70:1 |
| primary `#1d4ed8` | primary-soft `#e0e8fc` | 5.46:1 |
| on-primary `#ffffff` | primary `#1d4ed8` | 6.70:1 |
| on-primary `#ffffff` | primary-hover `#1e40af` | 8.72:1 |
| ok `#166534` | ok-soft `#dcfce7` | 6.49:1 |
| warn `#92400e` | warn-soft `#fef3c7` | 6.37:1 |
| danger `#b91c1c` | danger-soft `#fee2e2` | 5.30:1 |
| danger `#b91c1c` | surface `#ffffff` | 6.47:1 |
| info `#1e40af` | info-soft `#dbeafe` | 7.15:1 |

The e2e suite also checks the computed contrast of all visible text on every screen (see [Verification](#verification)).

## Typography

One family on every device: the system UI stack (`system-ui, Segoe UI, Roboto, …`). Mockups use **Inter** as the closest stand-in.

| Token | Size | Use |
|---|---|---|
| `--font-size-xxl` | 32px / 2rem | KPI numbers |
| `--font-size-xl` | 24px / 1.5rem | Page title (h1) |
| `--font-size-lg` | 18px / 1.125rem | Card title (h2) |
| `--font-size-md` | 16px / 1rem | Body text, inputs (prevents iOS zoom on focus) |
| `--font-size-sm` | 14px / 0.875rem | Labels, badges, buttons, table meta |

Line height: 1.5 for body text, 1.25 for headings. Weights: 400, 500, 600, 700.

## Spacing, radius, sizing

- Spacing scale: 4 · 8 · 12 · 16 · 24 · 32 · 48 px (`--space-1` … `--space-7`)
- Radius: 6 (controls) · 10 (cards) · 14 (sheets) · pill (badges)
- **Touch target: 44px.** Buttons, inputs, selects, nav items and table/card links all have `min-height: 44px`. On touch screens (`pointer: coarse`) plain text links also get a 44×44px hit area.

## Component library

Every component is used unchanged on all three device types. Figma components mirror these files:

| Component | Code | Variants |
|---|---|---|
| Button | [`Button.jsx`](../../Frontend/src/components/Button.jsx) | primary · secondary · danger · ghost; busy; disabled; icon |
| Badge | [`Badge.jsx`](../../Frontend/src/components/Badge.jsx) | ok · warn · danger · info · neutral |
| Card | [`Card.jsx`](../../Frontend/src/components/Card.jsx) | with title, with actions |
| Field | [`Field.jsx`](../../Frontend/src/components/Field.jsx) | text · password · number · select; with error |
| DataTable | [`DataTable.jsx`](../../Frontend/src/components/DataTable.jsx) | table (≥ 900px) · card list (< 900px); paging |
| Modal | [`Modal.jsx`](../../Frontend/src/components/Modal.jsx) | dialog (desktop) · bottom sheet (phone) |
| StateView | [`StateView.jsx`](../../Frontend/src/components/StateView.jsx) | loading · error · empty |
| Navigation | [`AppShell.jsx`](../../Frontend/src/AppShell.jsx) | sidebar · bottom tab bar · "More" sheet |
| Icon | [`Icon.jsx`](../../Frontend/src/components/Icon.jsx) | 20/24px line icons |

## Importing the tokens into Figma

1. Install the **Tokens Studio for Figma** plugin.
2. Plugin → **Tools → Load from file** → choose `Documentation/design/figma-tokens.json`.
3. **Styles & Variables → Export to Figma**: you get colour variables, text styles and spacing variables named like the CSS tokens.

## Verification

- `npm run test:e2e` (Playwright, [`Frontend/e2e/`](../../Frontend/e2e/)) opens every screen at phone, phone landscape, tablet, tablet landscape, desktop and desktop-at-200%-zoom sizes. It fails on horizontal scrolling, a missing main navigation, an interactive element smaller than 44px on touch sizes, or text below 4.5:1 contrast.
- Manual check: Chrome DevTools → Device toolbar (iPhone SE, iPad, Responsive 1280) and `Ctrl +` up to 200%.
