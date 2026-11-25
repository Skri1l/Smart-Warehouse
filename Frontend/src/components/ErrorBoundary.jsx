import { Component } from 'react';
import styles from './StateView.module.css';
import { Button } from './Button.jsx';
import { Icon } from './Icon.jsx';
import { reportError } from '../core/errors.js';

/** App root: '/' locally, '/<repo>/app/' in the GitHub Pages demo. */
const HOME_URL = import.meta.env.BASE_URL;

/**
 * Default screen for a crashed part of the UI. Plain `<a>` instead of a router link,
 * because the root boundary sits outside the router.
 * @param {{ onRetry: () => void, message?: string }} props
 */
export function CrashScreen({
  onRetry,
  message = 'Something went wrong on this screen. The error has been logged.',
}) {
  return (
    <div className={`${styles.state} ${styles.error}`} role="alert">
      <Icon name="alert" size={28} />
      <p>{message}</p>
      <Button icon="refresh" onClick={onRetry}>
        Try again
      </Button>
      <a href={HOME_URL}>Back to the dashboard</a>
    </div>
  );
}

/**
 * Catches render errors in its subtree, reports them and shows a fallback instead of
 * a blank page. Change `resetKey` (for example the route) to clear the error.
 * @extends {Component<{ resetKey?: unknown, fallback?: (retry: () => void) => import('react').ReactNode,
 *   children: import('react').ReactNode }, { error: Error | null }>}
 */
export class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    reportError(error, { componentStack: info.componentStack });
  }

  componentDidUpdate(prevProps) {
    if (this.state.error && prevProps.resetKey !== this.props.resetKey) {
      this.setState({ error: null });
    }
  }

  retry = () => this.setState({ error: null });

  render() {
    if (!this.state.error) return this.props.children;
    return this.props.fallback ? (
      this.props.fallback(this.retry)
    ) : (
      <CrashScreen onRetry={this.retry} />
    );
  }
}
