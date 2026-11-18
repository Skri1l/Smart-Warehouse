import { BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import styles from './DashboardPage.module.css';
import { formatShortDate } from '../../core/format.js';
import { BREAKPOINTS, useMediaQuery } from '../../hooks/useMediaQuery.js';

Chart.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend);

/**
 * Units received vs. removed per day.
 * @param {{ data: { date: string, incoming: number, outgoing: number }[] }} props
 */
export function MovementsChart({ data }) {
  const isWide = useMediaQuery(`(min-width: ${BREAKPOINTS.sm}px)`);
  const chartData = {
    labels: data.map((day) => formatShortDate(day.date)),
    datasets: [
      { label: 'Received', data: data.map((day) => day.incoming), backgroundColor: '#16a34a' },
      { label: 'Removed', data: data.map((day) => day.outgoing), backgroundColor: '#2563eb' },
    ],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom' } },
    scales: {
      x: { ticks: { maxTicksLimit: isWide ? 7 : 4 }, grid: { display: false } },
      y: { beginAtZero: true, ticks: { maxTicksLimit: 5, precision: 0 } },
    },
  };
  const total = data.reduce((sum, day) => sum + day.incoming + day.outgoing, 0);
  return (
    <div className={styles.chart}>
      <Bar
        data={chartData}
        options={options}
        role="img"
        aria-label={`Bar chart of units received and removed per day over the last ${data.length} days, ${total} units in total.`}
      />
    </div>
  );
}
