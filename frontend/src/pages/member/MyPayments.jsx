import { useApi } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, StatusBadge } from '../../components/Common.jsx';
import { formatDateTime, money } from '../../utils/format.js';

export default function MyPayments() {
  const { data, loading, error, reload } = useApi('/me/payments');
  return (
    <>
      <PageHeader title="Payment history" subtitle="Payments recorded by the library for your fines." />
      <ErrorAlert message={error} onRetry={reload} />
      <div className="panel p-0">
        {loading && !data ? <Loading /> : data?.length === 0 ? <EmptyState icon="receipt" title="No payments yet" /> : (
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead><tr><th>Date</th><th>Book</th><th>Method</th><th>Reference</th><th className="text-end">Paid</th><th className="text-end">Still due</th><th>Status</th></tr></thead>
              <tbody>{data?.map((p) => (
                <tr key={p.id}>
                  <td className="text-nowrap">{formatDateTime(p.paymentDate)}</td><td className="cell-title">{p.bookTitle}</td>
                  <td>{p.paymentMethod}</td><td className="small">{p.referenceNumber || '—'}</td>
                  <td className="text-end fw-semibold">{money(p.paidAmount)}</td><td className="text-end">{money(p.remainingAmount)}</td>
                  <td><StatusBadge status={p.paymentStatus} /></td>
                </tr>))}</tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
