import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp, signInAs } from '../../../tests/renderApp.jsx';
import { db } from '../../mocks/db.js';

describe('ReviewPage', () => {
  it('shows the result as confirmed and updates stock when a reviewer confirms it', async () => {
    signInAs('worker');
    renderApp('/review');
    const user = userEvent.setup();

    const heading = await screen.findByRole('heading', { name: /Waiting for review \(3\)/ });
    expect(heading).toBeInTheDocument();
    const pasta = screen.getByRole('heading', { name: 'Pasta 500g' }).closest('li');
    const before = db.shelfStock.find((row) => row.id === 6).quantity;

    await user.click(within(pasta).getByRole('button', { name: 'Confirm' }));

    expect(
      await screen.findByRole('heading', { name: /Waiting for review \(2\)/ }),
    ).toBeInTheDocument();
    expect(db.shelfStock.find((row) => row.id === 6).quantity).toBe(before - 3);
  });
});
