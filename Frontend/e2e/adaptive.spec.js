import { expect, test } from '@playwright/test';
import { useSession } from './helpers.js';

/**
 * Adaptive UI checks on every screen at phone, tablet and desktop sizes, in both
 * orientations and at 200% browser zoom (a 1280px window at 200% lays out like 640px).
 * See Documentation/design/DESIGN_SYSTEM.md.
 */
const VIEWPORTS = [
  { name: 'phone portrait', width: 375, height: 812, touch: true },
  { name: 'phone landscape', width: 812, height: 375, touch: true },
  { name: 'tablet portrait', width: 768, height: 1024, touch: true },
  { name: 'tablet landscape', width: 1024, height: 768, touch: true },
  { name: 'desktop', width: 1280, height: 800, touch: false },
  { name: 'desktop at 200% zoom', width: 640, height: 400, touch: false },
];

const SCREENS = [
  { path: '/login', heading: 'Smart Warehouse', signedIn: false },
  { path: '/', heading: 'Dashboard' },
  { path: '/shelves', heading: 'Shelves' },
  { path: '/products', heading: 'Products' },
  { path: '/review', heading: 'Recognition review' },
  { path: '/movements', heading: 'Stock movements' },
  { path: '/recommendations', heading: 'Purchase recommendations' },
  { path: '/devices', heading: 'Devices' },
];

/** Runs in the page: returns a list of human-readable problems. */
function auditPage({ touch, signedIn }) {
  const problems = [];
  const root = document.documentElement;
  if (root.scrollWidth > root.clientWidth + 1) {
    problems.push(`page scrolls sideways: ${root.scrollWidth}px content in ${root.clientWidth}px`);
  }

  const isVisible = (el) => {
    const style = getComputedStyle(el);
    const box = el.getBoundingClientRect();
    return (
      style.visibility !== 'hidden' && style.display !== 'none' && box.width > 0 && box.height > 0
    );
  };
  const label = (el) =>
    `<${el.tagName.toLowerCase()}> "${(el.innerText || el.getAttribute('aria-label') || '').trim().slice(0, 30)}"`;
  const inFixedLayer = (el) => {
    for (let node = el; node; node = node.parentElement) {
      const position = getComputedStyle(node).position;
      if (position === 'fixed' || position === 'sticky') return true;
    }
    return false;
  };

  if (signedIn && ![...document.querySelectorAll('nav[aria-label="Main"]')].some(isVisible)) {
    problems.push('main navigation is not visible');
  }

  const interactive = [
    ...document.querySelectorAll(
      'a[href], button, input:not([type=hidden]), select, textarea, [role=button]',
    ),
  ].filter(isVisible);

  if (touch) {
    for (const el of interactive) {
      const box = el.getBoundingClientRect();
      if (box.width < 44 || box.height < 44) {
        problems.push(
          `touch target ${Math.round(box.width)}x${Math.round(box.height)}px: ${label(el)}`,
        );
      }
    }
  }

  // Overlap between controls in normal flow (fixed/sticky bars legitimately float above content).
  const inFlow = interactive.filter((el) => !inFixedLayer(el));
  for (let i = 0; i < inFlow.length; i += 1) {
    for (let j = i + 1; j < inFlow.length; j += 1) {
      const [a, b] = [inFlow[i], inFlow[j]];
      if (a.contains(b) || b.contains(a)) continue;
      const x = a.getBoundingClientRect();
      const y = b.getBoundingClientRect();
      const w = Math.min(x.right, y.right) - Math.max(x.left, y.left);
      const h = Math.min(x.bottom, y.bottom) - Math.max(x.top, y.top);
      if (w > 2 && h > 2) problems.push(`overlap: ${label(a)} and ${label(b)}`);
    }
  }

  // WCAG 2.1 AA text contrast against the nearest opaque background.
  const rgb = (value) => (value.match(/[\d.]+/g) ?? []).map(Number);
  const luminance = ([r, g, b]) => {
    const channel = (v) => {
      const c = v / 255;
      return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
    };
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
  };
  const background = (el) => {
    for (let node = el; node; node = node.parentElement) {
      const color = rgb(getComputedStyle(node).backgroundColor);
      if (color.length === 3 || (color.length === 4 && color[3] > 0.5)) return color;
    }
    return [255, 255, 255];
  };
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  while (walker.nextNode()) {
    const text = walker.currentNode.textContent.trim();
    const el = walker.currentNode.parentElement;
    if (!text || !el || !isVisible(el) || el.closest('.visually-hidden, canvas')) continue;
    const style = getComputedStyle(el);
    const [fg, bg] = [luminance(rgb(style.color)), luminance(background(el))];
    const ratio = (Math.max(fg, bg) + 0.05) / (Math.min(fg, bg) + 0.05);
    const size = parseFloat(style.fontSize);
    const large = size >= 24 || (Number(style.fontWeight) >= 700 && size >= 18.66);
    if (ratio < (large ? 3 : 4.5)) {
      problems.push(`contrast ${ratio.toFixed(2)}:1 for "${text.slice(0, 30)}"`);
    }
  }
  return [...new Set(problems)];
}

for (const viewport of VIEWPORTS) {
  test.describe(viewport.name, () => {
    test.use({
      viewport: { width: viewport.width, height: viewport.height },
      hasTouch: viewport.touch,
    });

    for (const screen of SCREENS) {
      test(`${screen.path} has no layout or accessibility problems`, async ({
        page,
        context,
        baseURL,
      }) => {
        const signedIn = screen.signedIn !== false;
        if (signedIn) await useSession(context, 'admin', baseURL);
        await page.goto(screen.path);
        await expect(page.getByRole('heading', { level: 1, name: screen.heading })).toBeVisible();
        await expect(page.getByRole('status')).toHaveCount(0); // data loaded
        const problems = await page.evaluate(auditPage, { touch: viewport.touch, signedIn });
        expect(problems).toEqual([]);
      });
    }
  });
}
