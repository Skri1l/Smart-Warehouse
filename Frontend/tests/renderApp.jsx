import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../src/App.jsx';
import { AuthProvider } from '../src/hooks/useAuth.jsx';
import { ROUTER_FUTURE } from '../src/routerFuture.js';

/**
 * Render the whole app at a route, talking to the MSW mock backend.
 * @param {string} [route]
 */
export function renderApp(route = '/') {
  return render(
    <MemoryRouter initialEntries={[route]} future={ROUTER_FUTURE}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}

/** Pretend the backend already issued a session cookie for this mock user. */
export function signInAs(username) {
  document.cookie = `sw_session=${username}; path=/`;
}
