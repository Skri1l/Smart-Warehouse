import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '../../../tests/renderApp.jsx';

describe('LoginPage', () => {
  it('shows field errors when the form is submitted empty', async () => {
    renderApp('/');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Sign in' }));
    expect(screen.getByText('Enter your username.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
  });

  it('shows an error when login fails', async () => {
    renderApp('/');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Username'), 'worker');
    await user.type(screen.getByLabelText('Password'), 'wrong');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Wrong username or password.');
  });

  it('shows the dashboard with role-based navigation after a successful login', async () => {
    renderApp('/');
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText('Username'), 'worker');
    await user.type(screen.getByLabelText('Password'), 'worker');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Purchasing/ })).not.toBeInTheDocument();
  });
});
