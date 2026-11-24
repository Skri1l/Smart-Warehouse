import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './styles/tokens.css';
import './styles/global.css';
import { App } from './App.jsx';
import { ROUTER_FUTURE } from './routerFuture.js';
import { AuthProvider } from './hooks/useAuth.jsx';
import { CrashScreen, ErrorBoundary } from './components/ErrorBoundary.jsx';
import { installGlobalErrorHandlers, reportError } from './core/errors.js';

const root = createRoot(document.getElementById('root'));
// '/' locally; '/Smart-Warehouse/app/' in the GitHub Pages demo (vite --base).
const ROUTER_BASENAME = import.meta.env.BASE_URL.replace(/\/$/, '');
const reload = () => window.location.reload();

async function bootstrap() {
  installGlobalErrorHandlers();
  try {
    if (import.meta.env.VITE_API_MOCK === 'true') {
      // Dynamic import keeps MSW and the mock data out of the production bundle.
      const { startMockBackend } = await import('./mocks/browser.js');
      await startMockBackend();
    }
  } catch (error) {
    // Without this the user would see a blank page (e.g. a browser that blocks service workers).
    reportError(error, { source: 'bootstrap' });
    root.render(
      <CrashScreen
        onRetry={reload}
        message="The app could not start. Reload the page or try another browser."
      />,
    );
    return;
  }
  root.render(
    <StrictMode>
      <ErrorBoundary>
        <BrowserRouter future={ROUTER_FUTURE} basename={ROUTER_BASENAME}>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </ErrorBoundary>
    </StrictMode>,
  );
}

bootstrap();
