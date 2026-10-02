import { useApi } from '../../hooks/useApi.js';
import { ErrorAlert, Loading, PageHeader, StatCard } from '../../components/Common.jsx';
import { CategoryPie, FineCollectionChart, IssuedReturnedChart, RankingChart } from '../../components/Charts.jsx';
import { money } from '../../utils/format.js';

export default function AdminDashboard() {
  const { data: d, loading, error, reload } = useApi('/dashboard/admin');
  if (loading && !d) return <Loading />;
  if (error) return <ErrorAlert message={error} onRetry={reload} />;

  return (
    <>
      <PageHeader title="Dashboard" subtitle="The whole library at a glance." />
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-4 col-xl-2"><StatCard label="Titles" value={d.totalBooks} icon="book" to="/books" /></div>
        <div className="col-6 col-md-4 col-xl-2"><StatCard label="Physical copies" value={d.totalCopies} icon="stack" to="/copies" /></div>
        <div className="col-6 col-md-4 col-xl-2"><StatCard label="On the shelf" value={d.availableCopies} icon="check2-circle" tone="good" /></div>
        <div className="col-6 col-md-4 col-xl-2"><StatCard label="Borrowed" value={d.borrowedCopies} icon="box-arrow-up-right" tone="info" to="/borrowings" /></div>
        <div className="col-6 col-md-4 col-xl-2"><StatCard label="Overdue" value={d.overdueBooks} icon="alarm" tone={d.overdueBooks ? 'bad' : 'default'} to="/borrowings?overdue=true" /></div>
        <div className="col-6 col-md-4 col-xl-2"><StatCard label="Active reservations" value={d.activeReservations} icon="bookmark-star" tone="warn" to="/reservations" /></div>
        <div className="col-6 col-md-4 col-xl-3"><StatCard label="Members" value={d.totalMembers} icon="people" to="/members" /></div>
        <div className="col-6 col-md-4 col-xl-3"><StatCard label="Active members" value={d.activeMembers} icon="person-check" tone="good" /></div>
        <div className="col-6 col-md-6 col-xl-3"><StatCard label="Outstanding fines" value={money(d.outstandingFines)} icon="hourglass-split" tone="warn" to="/fines" /></div>
        <div className="col-6 col-md-6 col-xl-3"><StatCard label="Fines collected" value={money(d.finesCollected)} icon="cash-stack" tone="good" to="/payments" /></div>
      </div>
      <div className="row g-3">
        <div className="col-lg-7"><IssuedReturnedChart data={d.monthly} /></div>
        <div className="col-lg-5"><FineCollectionChart data={d.monthly} /></div>
        <div className="col-lg-6"><RankingChart title="Most borrowed books (last 12 months)" data={d.mostBorrowedBooks} valueLabel="Loans" /></div>
        <div className="col-lg-6"><RankingChart title="Most active members (last 12 months)" data={d.mostActiveMembers} valueLabel="Books borrowed" /></div>
        <div className="col-lg-6"><CategoryPie data={d.booksByCategory} /></div>
      </div>
    </>
  );
}
