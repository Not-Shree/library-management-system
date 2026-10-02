import { Link } from 'react-router-dom';
import { useApi } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, StatusBadge } from '../../components/Common.jsx';
import { formatDate, money } from '../../utils/format.js';

export default function MyFines() {
  const { data, loading, error, reload } = useApi('/me/fines');
  const settings = useApi('/settings').data;
  const owed = (data || []).reduce((s, f) => s + Number(f.outstandingAmount), 0);

  return (
    <>
      <PageHeader title="My fines" subtitle={settings ? `Late returns cost ${money(settings.finePerDay)} per day, up to ${money(settings.maxFinePerBook)} per book.` : undefined}
        actions={<Link to="/my-payments" className="btn btn-outline-primary">Payment history</Link>} />
      {owed > 0 && (
        <div className="alert alert-warning">
          You owe <strong>{money(owed)}</strong>. Pay at the library counter by cash, UPI or card — online payment is not available.
        </div>
      )}
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.length === 0 ? <EmptyState icon="emoji-smile" title="No fines — thank you for returning books on time!" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Book</th><th>Due</th><th>Returned</th><th>Days late</th><th className="text-end">Fine</th><th className="text-end">Paid</th><th className="text-end">To pay</th><th>Status</th></tr></thead>
              <tbody>{data?.map((f) => (
                <tr key={f.id}>
                  <td className="cell-title">{f.bookTitle}</td><td>{formatDate(f.dueDate)}</td><td>{formatDate(f.returnDate)}</td>
                  <td>{f.overdueDays} × {money(f.finePerDay)}</td><td className="text-end">{money(f.amount)}</td><td className="text-end">{money(f.paidAmount)}</td>
                  <td className="text-end fw-semibold">{money(f.outstandingAmount)}</td><td><StatusBadge status={f.status} /></td>
                </tr>))}</tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
