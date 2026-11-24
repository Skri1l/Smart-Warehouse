import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import styles from './LoginPage.module.css';
import { Button } from '../../components/Button.jsx';
import { Field } from '../../components/Field.jsx';
import { useAuth } from '../../hooks/useAuth.jsx';
import { isValid, validateLogin } from '../../core/validation.js';

const IS_MOCK = import.meta.env.VITE_API_MOCK === 'true';

/** Sign-in form (FR-01). */
export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState(/** @type {Record<string, string>} */ ({}));
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={location.state?.from ?? '/'} replace />;

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validateLogin({ username, password });
    setErrors(nextErrors);
    if (!isValid(nextErrors)) return;

    setSubmitting(true);
    try {
      await login(username.trim(), password);
      navigate(location.state?.from ?? '/', { replace: true });
    } catch (error) {
      setErrors({
        form: error.status === 401 ? 'Wrong username or password.' : error.message,
      });
      setSubmitting(false);
    }
  };

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={handleSubmit} noValidate>
        <div className={styles.brand}>
          <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="" width="40" height="40" />
          <h1>Smart Warehouse</h1>
        </div>
        <p className={styles.lead}>Sign in to see stock on your shelves.</p>
        {errors.form ? (
          <p className={styles.formError} role="alert">
            {errors.form}
          </p>
        ) : null}
        <Field
          label="Username"
          name="username"
          autoComplete="username"
          autoCapitalize="none"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          error={errors.username}
        />
        <Field
          label="Password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={errors.password}
        />
        <Button type="submit" variant="primary" busy={submitting}>
          Sign in
        </Button>
        {IS_MOCK ? (
          <p className={styles.hint}>
            Demo mode: sign in as <code>worker</code>, <code>manager</code> or <code>admin</code>{' '}
            with the same word as the password.
          </p>
        ) : null}
      </form>
    </main>
  );
}
