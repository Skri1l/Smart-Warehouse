import styles from './Button.module.css';
import { Icon } from './Icon.jsx';

/**
 * @param {{ variant?: 'primary' | 'secondary' | 'danger' | 'ghost', icon?: string,
 *   busy?: boolean, type?: 'button' | 'submit', children?: import('react').ReactNode }
 *   & import('react').ButtonHTMLAttributes<HTMLButtonElement>} props
 */
export function Button({
  variant = 'secondary',
  icon,
  busy = false,
  type = 'button',
  className,
  children,
  disabled,
  ...rest
}) {
  return (
    <button
      type={type}
      className={[styles.button, styles[variant], className].filter(Boolean).join(' ')}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      {...rest}
    >
      {busy ? <span className={styles.spinner} aria-hidden="true" /> : null}
      {!busy && icon ? <Icon name={icon} size={18} /> : null}
      {children ? <span>{children}</span> : null}
    </button>
  );
}
