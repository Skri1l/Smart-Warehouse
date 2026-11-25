import styles from './StateView.module.css';
import { Button } from './Button.jsx';
import { Icon } from './Icon.jsx';

/**
 * Renders the loading / error / empty states of a data view, or `children` on success.
 * @param {{ loading: boolean, error: Error | null, empty?: boolean, emptyText?: string,
 *   onRetry?: () => void, children: import('react').ReactNode }} props
 */
export function StateView({
  loading,
  error,
  empty = false,
  emptyText = 'Nothing here yet.',
  onRetry,
  children,
}) {
  if (loading) {
    return (
      <div className={styles.state} role="status">
        <span className={styles.spinner} aria-hidden="true" />
        <span>Loading…</span>
      </div>
    );
  }
  if (error) {
    return (
      <div className={`${styles.state} ${styles.error}`} role="alert">
        <Icon name="alert" size={28} />
        <p>{error.message || 'Something went wrong.'}</p>
        {onRetry ? (
          <Button icon="refresh" onClick={onRetry}>
            Try again
          </Button>
        ) : null}
      </div>
    );
  }
  if (empty) {
    return (
      <div className={styles.state}>
        <Icon name="box" size={28} />
        <p>{emptyText}</p>
      </div>
    );
  }
  return children;
}
