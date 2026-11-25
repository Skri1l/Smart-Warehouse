import { useEffect, useState } from 'react';
import styles from './DataTable.module.css';
import { Button } from './Button.jsx';
import { BREAKPOINTS, useMediaQuery } from '../hooks/useMediaQuery.js';

export const DEFAULT_PAGE_SIZE = 20;

/**
 * @template T
 * @typedef {object} Column
 * @property {string} key Unique column key.
 * @property {string} header Column title (also the label in card view).
 * @property {(row: T) => import('react').ReactNode} render Cell content.
 * @property {boolean} [primary] Shown as the card title below `md`.
 * @property {boolean} [secondary] Hidden in card view (keeps cards short on phones).
 * @property {'start' | 'end'} [align] Text alignment in table view.
 */

/**
 * Table on tablets/desktops, card list on phones (CODE_STYLE_FRONTEND "Responsive"),
 * with client-side paging because backend list endpoints are not paginated yet.
 * @template T
 * @param {{ columns: Column<T>[], rows: T[], rowKey: (row: T) => string | number,
 *   caption: string, pageSize?: number }} props
 */
export function DataTable({ columns, rows, rowKey, caption, pageSize = DEFAULT_PAGE_SIZE }) {
  const isWide = useMediaQuery(`(min-width: ${BREAKPOINTS.md}px)`);
  const [page, setPage] = useState(0);
  const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));

  useEffect(() => {
    if (page > pageCount - 1) setPage(pageCount - 1);
  }, [page, pageCount]);

  const visible = rows.slice(page * pageSize, (page + 1) * pageSize);
  const primary = columns.find((column) => column.primary) ?? columns[0];

  return (
    <div>
      {isWide ? (
        <div className={styles.scroll}>
          <table className={styles.table}>
            <caption className="visually-hidden">{caption}</caption>
            <thead>
              <tr>
                {columns.map((column) => (
                  <th
                    key={column.key}
                    scope="col"
                    className={column.align === 'end' ? styles.end : undefined}
                  >
                    {column.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visible.map((row) => (
                <tr key={rowKey(row)}>
                  {columns.map((column) => (
                    <td
                      key={column.key}
                      className={column.align === 'end' ? styles.end : undefined}
                    >
                      {column.render(row)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <ul className={styles.cards} aria-label={caption}>
          {visible.map((row) => (
            <li key={rowKey(row)} className={styles.card}>
              <div className={styles.cardTitle}>{primary.render(row)}</div>
              <dl className={styles.fields}>
                {columns
                  .filter((column) => column !== primary && !column.secondary)
                  .map((column) => (
                    <div key={column.key} className={styles.field}>
                      <dt>{column.header}</dt>
                      <dd>{column.render(row)}</dd>
                    </div>
                  ))}
              </dl>
            </li>
          ))}
        </ul>
      )}
      {pageCount > 1 ? (
        <nav className={styles.pager} aria-label={`${caption} pages`}>
          <Button variant="ghost" onClick={() => setPage(page - 1)} disabled={page === 0}>
            Previous
          </Button>
          <span aria-live="polite">
            Page {page + 1} of {pageCount}
          </span>
          <Button
            variant="ghost"
            onClick={() => setPage(page + 1)}
            disabled={page >= pageCount - 1}
          >
            Next
          </Button>
        </nav>
      ) : null}
    </div>
  );
}
