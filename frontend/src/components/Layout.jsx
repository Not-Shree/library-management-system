import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import api from '../api/client.js';

// Sidebar entries per role. Each role only sees the pages it is allowed to use.
const NAV = {
  ADMIN: [
    { section: 'Overview' },
    { to: '/dashboard', icon: 'speedometer2', label: 'Dashboard' },
    { section: 'Circulation' },
    { to: '/issue', icon: 'box-arrow-up-right', label: 'Issue book' },
    { to: '/return', icon: 'box-arrow-in-down-left', label: 'Returns' },
    { to: '/renew', icon: 'arrow-repeat', label: 'Renew book' },
    { to: '/borrowings', icon: 'journal-text', label: 'Borrowings' },
    { to: '/reservations', icon: 'bookmark-star', label: 'Reservations' },
    { section: 'Fines' },
    { to: '/fines', icon: 'cash-coin', label: 'Fines' },
    { to: '/payments', icon: 'receipt', label: 'Payments' },
    { section: 'Catalogue' },
    { to: '/books', icon: 'book', label: 'Books' },
    { to: '/copies', icon: 'upc-scan', label: 'Book copies' },
    { to: '/authors', icon: 'person-lines-fill', label: 'Authors' },
    { to: '/publishers', icon: 'building', label: 'Publishers' },
    { to: '/categories', icon: 'tags', label: 'Categories' },
    { section: 'People' },
    { to: '/members', icon: 'people', label: 'Members' },
    { to: '/librarians', icon: 'person-badge', label: 'Librarians' },
    { section: 'Administration' },
    { to: '/reports', icon: 'bar-chart-line', label: 'Reports' },
    { to: '/settings', icon: 'sliders', label: 'Settings' },
    { to: '/audit-logs', icon: 'shield-check', label: 'Audit logs' },
  ],
  LIBRARIAN: [
    { to: '/dashboard', icon: 'speedometer2', label: 'Dashboard' },
    { section: 'Circulation desk' },
    { to: '/issue', icon: 'box-arrow-up-right', label: 'Issue book' },
    { to: '/return', icon: 'box-arrow-in-down-left', label: 'Return book' },
    { to: '/renew', icon: 'arrow-repeat', label: 'Renew book' },
    { to: '/borrowings', icon: 'journal-text', label: 'Borrowings' },
    { to: '/reservations', icon: 'bookmark-star', label: 'Reservations' },
    { to: '/fines', icon: 'cash-coin', label: 'Fines' },
    { section: 'Library' },
    { to: '/books', icon: 'book', label: 'Books' },
    { to: '/copies', icon: 'upc-scan', label: 'Book copies' },
    { to: '/members', icon: 'people', label: 'Members' },
    { to: '/reports', icon: 'bar-chart-line', label: 'Reports' },
  ],
  MEMBER: [
    { to: '/dashboard', icon: 'house', label: 'My library' },
    { to: '/browse', icon: 'search', label: 'Browse books' },
    { to: '/my-books', icon: 'journal-bookmark', label: 'My borrowed books' },
    { to: '/my-reservations', icon: 'bookmark-star', label: 'My reservations' },
    { to: '/my-fines', icon: 'cash-coin', label: 'My fines' },
    { to: '/my-payments', icon: 'receipt', label: 'Payment history' },
    { to: '/notifications', icon: 'bell', label: 'Notifications' },
    { to: '/profile', icon: 'person-circle', label: 'Profile' },
  ],
};

export default function Layout() {
  const { user, logout, isMember } = useAuth();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const location = useLocation();

  useEffect(() => setOpen(false), [location.pathname]);

  // Members see an unread-notification count in the top bar.
  useEffect(() => {
    if (!isMember) return;
    api.get('/me/notifications/unread-count').then((r) => setUnread(r.data.count)).catch(() => {});
  }, [isMember, location.pathname]);

  const items = NAV[user.role] || [];
  const roleLabel = { ADMIN: 'Administrator', LIBRARIAN: 'Librarian', MEMBER: 'Member' }[user.role];

  return (
    <div className="app-shell">
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Main navigation">
        <Link to="/dashboard" className="brand">
          <i className="bi bi-book-half" aria-hidden="true" />
          <span>College Library</span>
        </Link>
        <nav className="sidebar-nav">
          {items.map((item) => item.section
            ? <div key={item.section} className="nav-section">{item.section}</div>
            : (
              <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
                <i className={`bi bi-${item.icon}`} aria-hidden="true" />
                <span>{item.label}</span>
                {item.to === '/notifications' && unread > 0 && <span className="badge rounded-pill text-bg-danger ms-auto">{unread}</span>}
              </NavLink>
            ))}
        </nav>
      </aside>
      {open && <div className="sidebar-scrim" onClick={() => setOpen(false)} aria-hidden="true" />}

      <div className="main-area">
        <header className="topbar">
          <button className="btn btn-link text-reset d-lg-none px-1" onClick={() => setOpen(!open)} aria-label="Open menu">
            <i className="bi bi-list fs-4" />
          </button>
          <div className="ms-auto d-flex align-items-center gap-3">
            {isMember && (
              <Link to="/notifications" className="btn btn-link text-reset position-relative p-1" aria-label={`${unread} unread notifications`}>
                <i className="bi bi-bell fs-5" />
                {unread > 0 && <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill text-bg-danger">{unread}</span>}
              </Link>
            )}
            <div className="text-end lh-sm d-none d-sm-block">
              <div className="fw-semibold">{user.fullName}</div>
              <div className="small text-body-secondary">{roleLabel}</div>
            </div>
            <div className="dropdown-lite">
              <Link to="/profile" className="avatar" aria-label="Profile">{user.fullName?.[0] ?? '?'}</Link>
            </div>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => logout()}>
              <i className="bi bi-box-arrow-right me-1" aria-hidden="true" />Log out
            </button>
          </div>
        </header>
        <main className="content" id="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
