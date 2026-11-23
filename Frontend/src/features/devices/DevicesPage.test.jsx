import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp, signInAs } from '../../../tests/renderApp.jsx';

async function openRegisterDialog() {
  signInAs('admin');
  renderApp('/devices');
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: 'Register camera' }));
  return { user, dialog: await screen.findByRole('dialog') };
}

describe('DevicesPage', () => {
  it('rejects an invalid device key without calling the server', async () => {
    const { user, dialog } = await openRegisterDialog();
    await user.type(within(dialog).getByLabelText('Device key'), 'X!');
    await user.click(screen.getByRole('button', { name: 'Register' }));
    expect(
      within(dialog).getByText('Use 3–64 lowercase letters, digits and dashes.'),
    ).toBeInTheDocument();
    expect(within(dialog).getByLabelText('Device key')).toHaveAttribute('aria-invalid', 'true');
  });

  it('registers a camera on the free shelf and lists it', async () => {
    const { user, dialog } = await openRegisterDialog();
    await user.type(within(dialog).getByLabelText('Device key'), 'shelf-new-cam');
    await user.click(screen.getByRole('button', { name: 'Register' }));
    expect(await screen.findByText('shelf-new-cam')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('keeps the dialog open and shows the server error on a duplicate key', async () => {
    const { user, dialog } = await openRegisterDialog();
    const existingKey = (await screen.findAllByText(/^shelf-.+-cam$/))[0].textContent;
    await user.type(within(dialog).getByLabelText('Device key'), existingKey);
    await user.click(screen.getByRole('button', { name: 'Register' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(/already exists/);
  });
});
