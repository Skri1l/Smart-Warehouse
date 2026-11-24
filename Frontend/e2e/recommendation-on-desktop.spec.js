import { expect, test } from '@playwright/test';
import { signIn } from './helpers.js';

test.use({ viewport: { width: 1280, height: 800 } });

test('manager modifies a purchase recommendation, with form validation', async ({ page }) => {
  await signIn(page, 'manager');

  await page
    .getByRole('navigation', { name: 'Main' })
    .getByRole('link', { name: 'Purchasing' })
    .click();
  await expect(page.getByRole('heading', { name: 'Purchase recommendations' })).toBeVisible();

  const card = page
    .getByRole('listitem')
    .filter({ has: page.getByRole('button', { name: 'Modify' }) })
    .first();
  const product = await card.getByRole('heading', { level: 2 }).textContent();
  await card.getByRole('button', { name: 'Modify' }).click();

  const dialog = page.getByRole('dialog', { name: `Modify: ${product}` });
  await expect(dialog).toBeVisible();

  await test.step('invalid quantity is rejected in the browser', async () => {
    await dialog.getByLabel('Quantity, pcs').fill('0');
    await dialog.getByLabel('Reason').fill('');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(dialog.getByText('Enter a whole number above 0.')).toBeVisible();
    await expect(dialog.getByText('Explain the change.')).toBeVisible();
    await expect(dialog.getByLabel('Quantity, pcs')).toHaveAttribute('aria-invalid', 'true');
  });

  await test.step('valid change is saved and shown on the card', async () => {
    await dialog.getByLabel('Quantity, pcs').fill('137');
    await dialog.getByLabel('Reason').fill('Promo week, supplier confirmed');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(dialog).toBeHidden();
    const updated = page
      .getByRole('listitem')
      .filter({ has: page.getByRole('heading', { name: product, level: 2 }) });
    await expect(updated.getByText('137 pcs')).toBeVisible();
  });
});
