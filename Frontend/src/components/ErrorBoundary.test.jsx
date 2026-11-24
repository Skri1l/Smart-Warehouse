import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorBoundary } from './ErrorBoundary.jsx';

const crash = { on: true };
function Fragile() {
  if (crash.on) throw new Error('boom');
  return <p>Screen content</p>;
}

describe('ErrorBoundary', () => {
  afterEach(() => {
    crash.on = true;
    vi.restoreAllMocks();
  });

  it('shows a fallback instead of a blank page and reports the error', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    render(
      <ErrorBoundary>
        <Fragile />
      </ErrorBoundary>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong');
    expect(consoleError).toHaveBeenCalledWith(
      '[smart-warehouse]',
      expect.objectContaining({ message: 'boom' }),
      expect.objectContaining({ componentStack: expect.any(String) }),
    );
  });

  it('renders the children again after "Try again" once the cause is gone', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    render(
      <ErrorBoundary>
        <Fragile />
      </ErrorBoundary>,
    );
    crash.on = false;
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }));
    expect(screen.getByText('Screen content')).toBeInTheDocument();
  });

  it('clears the error when resetKey changes (navigating to another route)', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const { rerender } = render(
      <ErrorBoundary resetKey="/a">
        <Fragile />
      </ErrorBoundary>,
    );
    crash.on = false;
    rerender(
      <ErrorBoundary resetKey="/b">
        <Fragile />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Screen content')).toBeInTheDocument();
  });
});
