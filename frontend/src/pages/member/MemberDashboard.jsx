import { Link } from 'react-router-dom';
import { useApi } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, StatCard } from '../../components/Common.jsx';
import BorrowingTable from '../../components/BorrowingTable.jsx';
import { money } from '../../utils/format.js';

export default function MemberDashboard() {
  const { data: d, loading, error, reload } = useApi('/dashboard/member');
  if (loading && !d) return <Loading />;
  if (error) return <ErrorAlert message={error} onRetry={reload} />;
  const firstName = d.memberName.split(' ')[0];

  return (
    <>
      <PageHeader title={`Hello, ${firstName}`} subtitle={`Member ID ${d.memberCode}`}
        actions={<Link to="/browse" className="btn btn-primary"><i className="bi bi-search me-1" />Browse books</Link>} />
      {d.readyReservations > 0 && (
        <div className="alert alert-warning d-flex align-items-center gap-2">
          <i className="bi bi-bookmark-check-fill" aria-hidden="true" />
          <span>{d.readyReservations} reserved book{d.readyReservations > 1 ? 's are' : ' is'} waiting for you at the desk. <Link to="/my-reservations">See details</Link></span>
        </div>
      )}
      {d.overdueCount > 0 && (
        <div className="alert alert-danger d-flex align-items-center gap-2">
          <i className="bi bi-alarm-fill" aria-hidden="true" />
          <span>You have {d.overdueCount} overdue book{d.overdueCount > 1 ? 's' : ''}. The fine grows every day until you return {d.overdueCount > 1 ? 'them' : 'it'}.</span>
        </div>
      )}
      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3"><StatCard label="Borrowed" value={`${d.currentBorrowed} / ${d.borrowLimit}`} icon="journal-bookmark" to="/my-books" note={`You can borrow ${Math.max(0, d.borrowLimit - d.currentBorrowed)} more`} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Overdue" value={d.overdueCount} icon="alarm" tone={d.overdueCount ? 'bad' : 'good'} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Fines to pay" value={money(d.outstandingFine)} icon="cash-coin" tone={Number(d.outstandingFine) > 0 ? 'warn' : 'good'} to="/my-fines"
          note={Number(d.accruingFine) > 0 ? `+ ${money(d.accruingFine)} building up on overdue books` : 'Pay at the library counter'} /></div>
        <div className="col-6 col-lg-3"><StatCard label="Reservations" value={d.activeReservations} icon="bookmark-star" to="/my-reservations" /></div>
      </div>
      <div className="panel">
        <h2 className="panel-title">Books you have now</h2>
        {d.currentBooks.length === 0 ? <EmptyState icon="book" title="You have no books right now"><Link to="/browse">Find something to read</Link></EmptyState>
          : <BorrowingTable rows={d.currentBooks} showMember={false} compact />}
      </div>
    </>
  );
}
