import { Link } from 'react-router-dom';
import { Card } from '../../components/Card.jsx';

/** Shown when the user's role may not open a page (403). */
export function ForbiddenPage() {
  return (
    <Card title="No access">
      <p>Your role does not have access to this page.</p>
      <p>
        <Link to="/">Back to the dashboard</Link>
      </p>
    </Card>
  );
}

/** Unknown route inside the app. */
export function NotFoundPage() {
  return (
    <Card title="Page not found">
      <p>
        <Link to="/">Back to the dashboard</Link>
      </p>
    </Card>
  );
}
