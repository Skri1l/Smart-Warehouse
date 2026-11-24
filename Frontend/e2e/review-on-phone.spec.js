import { expect, test } from '@playwright/test';
import { signIn } from './helpers.js';

test.use({ viewport: { width: 375, height: 812 }, hasTouch: true, isMobile: true });

test('worker signs in on a phone and confirms an uncertain recognition', async ({ page }) => {
  await test.step('validation stops an empty login', async () => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('Enter your username.')).toBeVisible();
    await expect(page.getByText('Enter your password.')).toBeVisible();
  });

  await signIn(page, 'worker');

  await test.step('open Review from the bottom tab bar', async () => {
    const nav = page.getByRole('navigation', { name: 'Main' });
    await nav.getByRole('link', { name: 'Review' }).click();
    await expect(page).toHaveURL(/\/review$/);
    await expect(page.getByRole('heading', { name: /Waiting for review \(3\)/ })).toBeVisible();
  });

  await test.step('confirm "Pasta 500g"', async () => {
    const item = page
      .getByRole('listitem')
      .filter({ has: page.getByRole('heading', { name: 'Pasta 500g' }) });
    await item.getByRole('button', { name: 'Confirm' }).click();
    await expect(page.getByRole('heading', { name: 'Pasta 500g', level: 3 })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: /Waiting for review \(2\)/ })).toBeVisible();
  });
});
