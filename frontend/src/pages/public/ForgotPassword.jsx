import { Link } from 'react-router-dom';
import AuthShell from './AuthShell.jsx';

/**
 * Self-service reset by email is NOT implemented (it would need an SMTP server).
 * Instead, staff reset passwords from the admin panel: Members -> member -> "Reset password".
 */
export default function ForgotPassword() {
  return (
    <AuthShell title="Forgot your password?" subtitle="Password resets are done at the library counter.">
      <ol className="reset-steps">
        <li>Visit the circulation desk with your college ID card.</li>
        <li>The librarian opens your member record and chooses <strong>Reset password</strong>.</li>
        <li>Log in with the temporary password, then change it from <strong>Profile</strong>.</li>
      </ol>
      <div className="alert alert-secondary small">
        Staff accounts are reset by an administrator from the <strong>Librarians</strong> page.
        Email-based reset links are not part of this version, so the system works without any email service.
      </div>
      <Link to="/login" className="btn btn-primary w-100">Back to log in</Link>
    </AuthShell>
  );
}
