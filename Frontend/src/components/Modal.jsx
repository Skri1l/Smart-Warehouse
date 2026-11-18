import { useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import styles from './Modal.module.css';
import { Button } from './Button.jsx';

/**
 * Accessible dialog; a full-screen sheet below the `sm` breakpoint.
 * Closes on Escape and on the close button; focus moves into the dialog and back on close.
 * @param {{ title: string, onClose: () => void, children: import('react').ReactNode,
 *   footer?: import('react').ReactNode }} props
 */
export function Modal({ title, onClose, children, footer }) {
  const titleId = useId();
  const dialogRef = useRef(/** @type {HTMLDivElement | null} */ (null));

  useEffect(() => {
    const previous = document.activeElement;
    const focusable = dialogRef.current?.querySelector('input, select, textarea, button');
    (focusable ?? dialogRef.current)?.focus();
    const handleKey = (event) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, [onClose]);

  return createPortal(
    <div className={styles.backdrop}>
      <div
        ref={dialogRef}
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
      >
        <header className={styles.header}>
          <h2 id={titleId}>{title}</h2>
          <Button variant="ghost" icon="close" aria-label="Close" onClick={onClose} />
        </header>
        <div className={styles.body}>{children}</div>
        {footer ? <footer className={styles.footer}>{footer}</footer> : null}
      </div>
    </div>,
    document.body,
  );
}
