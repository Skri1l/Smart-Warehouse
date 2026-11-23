import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import styles from './AppShell.module.css';
import { Icon } from './components/Icon.jsx';
import { Button } from './components/Button.jsx';
import { Modal } from './components/Modal.jsx';
import { ErrorBoundary } from './components/ErrorBoundary.jsx';
import { hasRole, useAuth } from './hooks/useAuth.jsx';
import { BREAKPOINTS, useMediaQuery } from './hooks/useMediaQuery.js';
import { humanize } from './core/format.js';
import { NAV_ITEMS } from './navigation.js';

const BOTTOM_BAR_SLOTS = 4;
const IS_MOCK = import.meta.env.VITE_API_MOCK === 'true';

/** Responsive app frame: header, side navigation (md+) or bottom tab bar (phones). */
export function AppShell() {
  const { user, logout } = useAuth();
  const isWide = useMediaQuery(`(min-width: ${BREAKPOINTS.md}px)`);
  const location = useLocation();
  const [moreOpen, setMoreOpen] = useState(false);

  const items = NAV_ITEMS.filter((item) => hasRole(user, item.roles));
  const needsMore = items.length > BOTTOM_BAR_SLOTS + 1;
  const barItems = needsMore ? items.slice(0, BOTTOM_BAR_SLOTS) : items;
  const moreItems = needsMore ? items.slice(BOTTOM_BAR_SLOTS) : [];
  const moreActive = moreItems.some((item) => location.pathname.startsWith(item.path));

  const navLink = (item, className) => (
    <NavLink
      key={item.path}
      to={item.path}
      end={item.path === '/'}
      className={({ isActive }) => `${className} ${isActive ? styles.active : ''}`}
      onClick={() => setMoreOpen(false)}
    >
      <Icon name={item.icon} />
      <span>{item.label}</span>
    </NavLink>
  );

  return (
    <div className={styles.shell}>
      <a href="#main" className={styles.skip}>
        Skip to content
      </a>
      <header className={styles.header}>
        <div className={styles.brand}>
          <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="" width="28" height="28" />
          <span>Smart Warehouse</span>
          {IS_MOCK ? <span className={styles.mock}>Demo data</span> : null}
        </div>
        <div className={styles.user}>
          <span className={styles.userText}>
            <strong>{user.username}</strong>
            <span>{humanize(user.role)}</span>
          </span>
          <Button variant="ghost" icon="logout" onClick={logout} aria-label="Sign out">
            {isWide ? 'Sign out' : null}
          </Button>
        </div>
      </header>

      {isWide ? (
        <nav className={styles.sidebar} aria-label="Main">
          {items.map((item) => navLink(item, styles.sideLink))}
        </nav>
      ) : null}

      <main id="main" className={styles.main}>
        <div className={styles.content}>
          {/* A crash in one screen keeps the navigation usable; moving to another route clears it. */}
          <ErrorBoundary resetKey={location.pathname}>
            <Outlet />
          </ErrorBoundary>
        </div>
      </main>

      {!isWide ? (
        <nav className={styles.bottomBar} aria-label="Main">
          {barItems.map((item) => navLink(item, styles.tab))}
          {needsMore ? (
            <button
              type="button"
              className={`${styles.tab} ${moreActive ? styles.active : ''}`}
              onClick={() => setMoreOpen(true)}
              aria-haspopup="dialog"
            >
              <Icon name="more" />
              <span>More</span>
            </button>
          ) : null}
        </nav>
      ) : null}

      {moreOpen ? (
        <Modal title="More" onClose={() => setMoreOpen(false)}>
          <div className={styles.moreList}>
            {moreItems.map((item) => navLink(item, styles.sideLink))}
          </div>
        </Modal>
      ) : null}
    </div>
  );
}
