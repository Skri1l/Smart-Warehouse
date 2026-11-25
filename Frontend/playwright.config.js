import { defineConfig, devices } from '@playwright/test';

const PORT = 5174;

/**
 * End-to-end tests run the real app in Chromium against the in-browser mock backend
 * (MSW), so they need no Java backend or database. Port 5174 keeps them clear of a
 * `npm run dev` already running on 5173.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  // The first page in each worker waits for the MSW service worker and Vite's cold compile.
  expect: { timeout: 15000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: `http://localhost:${PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: `npm run dev -- --port ${PORT} --strictPort`,
    url: `http://localhost:${PORT}`,
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
});
