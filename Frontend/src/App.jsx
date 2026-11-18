import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AppShell } from './AppShell.jsx';
import { StateView } from './components/StateView.jsx';
import { hasRole, useAuth } from './hooks/useAuth.jsx';
import { LoginPage } from './features/auth/LoginPage.jsx';
import { ForbiddenPage, NotFoundPage } from './features/errors/ErrorPages.jsx';

const DashboardPage = lazy(() => import('./features/dashboard/DashboardPage.jsx'));
const ShelvesPage = lazy(() => import('./features/shelves/ShelvesPage.jsx'));
const ProductsPage = lazy(() => import('./features/products/ProductsPage.jsx'));
const ProductDetailsPage = lazy(() => import('./features/products/ProductDetailsPage.jsx'));
const ReviewPage = lazy(() => import('./features/recognition/ReviewPage.jsx'));
const MovementsPage = lazy(() => import('./features/movements/MovementsPage.jsx'));
const RecommendationsPage = lazy(
  () => import('./features/recommendations/RecommendationsPage.jsx'),
);
const DevicesPage = lazy(() => import('./features/devices/DevicesPage.jsx'));

/** Redirects to /login when signed out; shows "no access" when the role does not match. */
function RequireAuth({ roles, children }) {
  const { user, status } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <StateView loading error={null} />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (!hasRole(user, roles)) return <ForbiddenPage />;
  return children;
}

function page(element, roles) {
  return (
    <RequireAuth roles={roles}>
      <Suspense fallback={<StateView loading error={null} />}>{element}</Suspense>
    </RequireAuth>
  );
}

/** Routes of the application. */
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={page(<DashboardPage />)} />
        <Route path="shelves" element={page(<ShelvesPage />)} />
        <Route path="products" element={page(<ProductsPage />)} />
        <Route path="products/:productId" element={page(<ProductDetailsPage />)} />
        <Route path="review" element={page(<ReviewPage />)} />
        <Route path="movements" element={page(<MovementsPage />)} />
        <Route
          path="recommendations"
          element={page(<RecommendationsPage />, ['MANAGER', 'ADMIN'])}
        />
        <Route path="devices" element={page(<DevicesPage />, ['ADMIN'])} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
