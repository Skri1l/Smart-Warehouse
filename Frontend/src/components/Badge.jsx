import styles from './Badge.module.css';
import { Icon } from './Icon.jsx';

const TONE_ICONS = {
  ok: 'check',
  warn: 'clock',
  danger: 'alert',
  info: undefined,
  neutral: undefined,
};

/**
 * Status pill. Always carries text (and an icon for ok/warn/danger), never color alone.
 * @param {{ tone?: 'ok' | 'warn' | 'danger' | 'info' | 'neutral', children: import('react').ReactNode }} props
 */
export function Badge({ tone = 'neutral', children }) {
  const icon = TONE_ICONS[tone];
  return (
    <span className={`${styles.badge} ${styles[tone]}`}>
      {icon ? <Icon name={icon} size={14} /> : null}
      {children}
    </span>
  );
}
