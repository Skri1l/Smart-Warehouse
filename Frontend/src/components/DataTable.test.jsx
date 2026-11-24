import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DataTable } from './DataTable.jsx';

const columns = [
  { key: 'name', header: 'Product', primary: true, render: (row) => row.name },
  { key: 'qty', header: 'Quantity', render: (row) => `${row.qty} pcs` },
  { key: 'note', header: 'Note', secondary: true, render: (row) => row.note },
];
const rows = Array.from({ length: 5 }, (_, i) => ({
  id: i + 1,
  name: `Product ${i + 1}`,
  qty: i * 10,
  note: `note ${i + 1}`,
}));

function setViewportWide(wide) {
  vi.spyOn(window, 'matchMedia').mockImplementation((query) => ({
    matches: wide,
    media: query,
    addEventListener() {},
    removeEventListener() {},
  }));
}

const renderTable = (props = {}) =>
  render(
    <DataTable caption="Stock" columns={columns} rows={rows} rowKey={(row) => row.id} {...props} />,
  );

describe('DataTable', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders a real table with column headers on tablets and desktops', () => {
    setViewportWide(true);
    renderTable();
    expect(screen.getByRole('table', { name: 'Stock' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Note' })).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(rows.length + 1);
  });

  it('renders cards without secondary columns on phones', () => {
    setViewportWide(false);
    renderTable();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'Stock' });
    expect(list.children).toHaveLength(rows.length);
    expect(screen.getAllByText('Quantity')).toHaveLength(rows.length);
    expect(screen.queryByText('note 1')).not.toBeInTheDocument();
  });

  it('pages the rows on the client', async () => {
    setViewportWide(true);
    renderTable({ pageSize: 2 });
    const user = userEvent.setup();
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByText('Product 3')).toBeInTheDocument();
    expect(screen.queryByText('Product 1')).not.toBeInTheDocument();
  });
});
