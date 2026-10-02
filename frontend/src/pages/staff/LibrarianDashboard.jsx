import { Link } from 'react-router-dom';
import { useApi } from '../../hooks/useApi.js';
import { EmptyState, ErrorAlert, Loading, PageHeader, StatCard } from '../../components/Common.jsx';
import { IssuedReturnedChart } from '../../components/Charts.jsx';
import BorrowingTable from '../../components/BorrowingTable.jsx';
import { money } from '../../utils/format.js';

export default function LibrarianDashboard() {
  const { data: d, loading, error, reload } = useApi('/dashboard/librarian');
  if (loading && !d) return <Loading />;
  if (error) return <ErrorAlert message={error} onRetry={reload} />;

  return (
    <>
      <PageHeader title="Circulation desk" subtitle="Today's work and what needs attention."
        actions={<>
          <Link to="/issue" className="btn btn-primary"><i className="bi bi-box-arrow-up-right me-1" />Issue a book</Link>
          <Link to="/return" className="btn btn-outline-primary"><i className="bi bi-box-arrow-in-down-left me-1" />Return a book</Link>
        </>} />
      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3"><StatCard label="Issued today" value={d.issuedToday} icon="box-arrow-up-right" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Returned today" value={d.returnedToday} icon="box-arrow-in-down-left" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Overdue" value={d.overdueBooks} icon="alarm" tone={d.overdueBooks ? 'bad' : 'default'} to="/borrowings?overdue=true" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Due in the next few days" value={d.dueSoon} icon="calendar-event" tone="warn" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Waiting reservations" value={d.waitingReservations} icon="hourglass" to="/reservations?status=WAITING" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Ready for pickup" value={d.readyForPickup} icon="bookmark-check" tone="warn" to="/reservations?status=AVAILABLE" note="Copies held on the reservation shelf" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Outstanding fines" value={money(d.outstandingFines)} icon="hourglass-split" to="/fines" /></div>
        <div className="col-6 col-lg-3"><StatCard label="Collected today" value={money(d.collectedToday)} icon="cash-coin" tone="good" /></div>
      </div>
      <div className="row g-3">
        <div className="col-xl-7">
          <div className="panel h-100">
            <div className="d-flex justify-content-between align-items-center">
              <h2 className="panel-title">Longest overdue</h2>
              <Link to="/borrowings?overdue=true" className="small">See all overdue</Link>
            </div>
            {d.overdueList.length === 0 ? <EmptyState icon="emoji-smile" title="Nothing is overdue" /> : <BorrowingTable rows={d.overdueList} compact />}
          </div>
        </div>
        <div className="col-xl-5"><IssuedReturnedChart data={d.monthly} /></div>
      </div>
    </>
  );
}
