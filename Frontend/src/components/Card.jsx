import styles from './Card.module.css';

/**
 * Surface with an optional header row (title + actions).
 * @param {{ title?: import('react').ReactNode, actions?: import('react').ReactNode,
 *   flush?: boolean, className?: string, children: import('react').ReactNode }} props
 */
export function Card({ title, actions, flush = false, className, children }) {
  return (
    <section className={[styles.card, className].filter(Boolean).join(' ')}>
      {title || actions ? (
        <header className={styles.header}>
          {title ? <h2 className={styles.title}>{title}</h2> : <span />}
          {actions ? <div className={styles.actions}>{actions}</div> : null}
        </header>
      ) : null}
      <div className={flush ? undefined : styles.body}>{children}</div>
    </section>
  );
}

/**
 * Page title row with optional actions; wraps under the title on narrow screens.
 * @param {{ title: string, subtitle?: string, actions?: import('react').ReactNode }} props
 */
export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className={styles.pageHeader}>
      <div>
        <h1>{title}</h1>
        {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
      </div>
      {actions ? <div className={styles.actions}>{actions}</div> : null}
    </div>
  );
}
