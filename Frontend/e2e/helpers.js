import { expect } from '@playwright/test';

/** Sign in through the real login form (mock users: worker, manager, admin; password = name). */
export async function signIn(page, username) {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(username);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
}

/** Skip the form: the mock backend trusts this session cookie (like a real `HttpOnly` one). */
export async function useSession(context, username, baseURL) {
  await context.addCookies([{ name: 'sw_session', value: username, url: baseURL }]);
}
