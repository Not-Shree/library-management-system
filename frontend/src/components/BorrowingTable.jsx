import { Link } from 'react-router-dom';
import { DueStamp, StatusBadge } from './Common.jsx';
import { formatDate, money } from '../utils/format.js';

/** Reusable borrowing table. `actions(b)` can render buttons in the last column. */
export default function BorrowingTable({ rows, showMember = true, actions, compact }) {
  return (
    <div className="table-responsive">
      <table className={`table align-middle ${compact ? 'table-sm' : ''} mb-0`}>
        <thead>
          <tr>
            {showMember && <th>Member</th>}
            <th>Book</th>
            <th>Copy</th>
            <th>Issued</th>
            <th>Due</th>
            <th>Returned</th>
            <th>Status</th>
            <th className="text-end">Fine</th>
            {actions && <th className="text-end"><span className="visually-hidden">Actions</span></th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((b) => (
            <tr key={b.id}>
              {showMember && (
                <td>
                  <Link to={`/members/${b.memberId}`} className="fw-semibold">{b.memberName}</Link>
                  <div className="small text-body-secondary">{b.memberCode}</div>
                </td>
              )}
              <td className="cell-title">{b.bookTitle}</td>
              <td><code>{b.copyCode}</code></td>
              <td className="text-nowrap">{formatDate(b.issueDate)}</td>
              <td><DueStamp date={b.dueDate} returned={b.status === 'RETURNED'} /></td>
              <td className="text-nowrap">{formatDate(b.returnDate)}</td>
              <td>
                <StatusBadge status={b.displayStatus} />
                {b.renewalCount > 0 && <div className="small text-body-secondary">Renewed {b.renewalCount}×</div>}
              </td>
              <td className="text-end text-nowrap">
                {b.fineAmount != null && <><span className="fw-semibold">{money(b.fineAmount)}</span> <StatusBadge status={b.fineStatus} /></>}
                {b.estimatedFine != null && <span className="text-danger" title="If returned today">{money(b.estimatedFine)} so far</span>}
                {b.fineAmount == null && b.estimatedFine == null && <span className="text-body-tertiary">—</span>}
              </td>
              {actions && <td className="text-end text-nowrap">{actions(b)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
