import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import { Loading } from './components/Common.jsx';
import AndroidBackButton from './components/AndroidBackButton.jsx';

import Login from './pages/public/Login.jsx';
import Register from './pages/public/Register.jsx';
import ForgotPassword from './pages/public/ForgotPassword.jsx';
import Catalogue from './pages/public/Catalogue.jsx';

import AdminDashboard from './pages/staff/AdminDashboard.jsx';
import LibrarianDashboard from './pages/staff/LibrarianDashboard.jsx';
import Books from './pages/staff/Books.jsx';
import BookDetail from './pages/staff/BookDetail.jsx';
import Copies from './pages/staff/Copies.jsx';
import LookupPage from './pages/staff/LookupPage.jsx';
import Members from './pages/staff/Members.jsx';
import MemberDetail from './pages/staff/MemberDetail.jsx';
import Librarians from './pages/staff/Librarians.jsx';
import Borrowings from './pages/staff/Borrowings.jsx';
import IssueBook from './pages/staff/IssueBook.jsx';
import ReturnBook from './pages/staff/ReturnBook.jsx';
import Fines from './pages/staff/Fines.jsx';
import Payments from './pages/staff/Payments.jsx';
import Reservations from './pages/staff/Reservations.jsx';
import Reports from './pages/staff/Reports.jsx';
import Settings from './pages/staff/Settings.jsx';
import AuditLogs from './pages/staff/AuditLogs.jsx';

import MemberDashboard from './pages/member/MemberDashboard.jsx';
import MyBooks from './pages/member/MyBooks.jsx';
import MyReservations from './pages/member/MyReservations.jsx';
import MyFines from './pages/member/MyFines.jsx';
import MyPayments from './pages/member/MyPayments.jsx';
import Notifications from './pages/member/Notifications.jsx';
import Profile from './pages/member/Profile.jsx';

const STAFF = ['ADMIN', 'LIBRARIAN'];

function Dashboard() {
  const { user } = useAuth();
  if (user.role === 'ADMIN') return <AdminDashboard />;
  if (user.role === 'LIBRARIAN') return <LibrarianDashboard />;
  return <MemberDashboard />;
}

function Home() {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return <Navigate to={user ? '/dashboard' : '/catalogue'} replace />;
}

export default function App() {
  return (
    <>
    <AndroidBackButton />
    <Routes>
      {/* Public */}
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/catalogue" element={<Catalogue publicView />} />

      {/* Logged in (any role) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />

          {/* Admin + librarian */}
          <Route element={<ProtectedRoute roles={STAFF} />}>
            <Route path="/books" element={<Books />} />
            <Route path="/books/:id" element={<BookDetail />} />
            <Route path="/copies" element={<Copies />} />
            <Route path="/members" element={<Members />} />
            <Route path="/members/:id" element={<MemberDetail />} />
            <Route path="/borrowings" element={<Borrowings />} />
            <Route path="/issue" element={<IssueBook />} />
            <Route path="/return" element={<ReturnBook mode="return" />} />
            <Route path="/renew" element={<ReturnBook mode="renew" />} />
            <Route path="/fines" element={<Fines />} />
            <Route path="/payments" element={<Payments />} />
            <Route path="/reservations" element={<Reservations />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/authors" element={<LookupPage kind="authors" />} />
            <Route path="/publishers" element={<LookupPage kind="publishers" />} />
            <Route path="/categories" element={<LookupPage kind="categories" />} />
          </Route>

          {/* Admin only */}
          <Route element={<ProtectedRoute roles={['ADMIN']} />}>
            <Route path="/librarians" element={<Librarians />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/audit-logs" element={<AuditLogs />} />
          </Route>

          {/* Member only */}
          <Route element={<ProtectedRoute roles={['MEMBER']} />}>
            <Route path="/browse" element={<Catalogue />} />
            <Route path="/my-books" element={<MyBooks />} />
            <Route path="/my-reservations" element={<MyReservations />} />
            <Route path="/my-fines" element={<MyFines />} />
            <Route path="/my-payments" element={<MyPayments />} />
            <Route path="/notifications" element={<Notifications />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<div className="container py-5"><h1 className="h4">Page not found</h1><a href="/">Go to the start page</a></div>} />
    </Routes>
    </>
  );
}
