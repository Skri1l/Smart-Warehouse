import { useId } from 'react';
import styles from './Field.module.css';

/**
 * Labelled form control with an error message as text below it.
 * Pass `as="select"` with `<option>` children for a dropdown, `as="textarea"` for multi-line.
 * @param {{ label: string, error?: string, hint?: string, as?: 'input' | 'select' | 'textarea',
 *   children?: import('react').ReactNode } & Record<string, any>} props
 */
export function Field({ label, error, hint, as: Control = 'input', children, ...controlProps }) {
  const id = useId();
  const describedBy = [error ? `${id}-error` : null, hint ? `${id}-hint` : null]
    .filter(Boolean)
    .join(' ');
  return (
    <div className={styles.field}>
      <label htmlFor={id} className={styles.label}>
        {label}
      </label>
      <Control
        id={id}
        className={`${styles.control} ${error ? styles.invalid : ''}`}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy || undefined}
        {...controlProps}
      >
        {children}
      </Control>
      {hint ? (
        <p id={`${id}-hint`} className={styles.hint}>
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={`${id}-error`} className={styles.error}>
          {error}
        </p>
      ) : null}
    </div>
  );
}
