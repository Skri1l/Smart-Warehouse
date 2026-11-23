import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { server } from '../../../tests/server.js';
import { renderApp, signInAs } from '../../../tests/renderApp.jsx';
import { db } from '../../mocks/db.js';

describe('ShelvesPage', () => {
  it('shows a single product per shelf in the seeded warehouse', async () => {
    signInAs('worker');
    renderApp('/shelves');
    const shelf = (await screen.findByRole('heading', { name: 'Shelf B-1' })).closest('article');
    expect(within(shelf).getByRole('link', { name: /Rice 1kg/ })).toBeInTheDocument();
    expect(screen.queryByText(/products on one shelf/)).not.toBeInTheDocument();
  });

  it('shows a warning when the backend reports two products on one shelf', async () => {
    server.use(
      http.get('/api/v1/shelf-stock', () =>
        HttpResponse.json([...db.shelfStock, { ...db.shelfStock[4], id: 99, productId: 8 }]),
      ),
    );
    signInAs('worker');
    renderApp('/shelves');
    const shelf = (await screen.findByRole('heading', { name: 'Shelf B-1' })).closest('article');
    expect(within(shelf).getByRole('alert')).toHaveTextContent('2 products on one shelf.');
    expect(within(shelf).getByText('Mixed')).toBeInTheDocument();
  });
});
