import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './styles/tokens.css';
import './styles/global.css';
import { App } from './App.jsx';
import { ROUTER_FUTURE } from './routerFuture.js';
import { AuthProvider } from './hooks/useAuth.jsx';

async function bootstrap() {
  if (import.meta.env.VITE_API_MOCK === 'true') {
    // Dynamic import keeps MSW and the mock data out of the production bundle.
    const { startMockBackend } = await import('./mocks/browser.js');
    await startMockBackend();
  }
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <BrowserRouter future={ROUTER_FUTURE}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </StrictMode>,
  );
}

bootstrap();
