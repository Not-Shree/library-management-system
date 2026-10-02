import { Link } from 'react-router-dom';
import { isNativeApp } from '../../platform.js';
import ServerSettings from '../../components/ServerSettings.jsx';

/** Two-column frame for login / register: a date-due slip on the left, the form on the right. */
export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="auth-page">
      <section className="auth-art d-none d-lg-flex" aria-hidden="true">
        <div>
          <div className="auth-brand"><i className="bi bi-book-half" /> College Library</div>
          <p className="auth-tagline">Issue, return and fines — calculated for you, every time.</p>
        </div>
        <div className="due-slip">
          <div className="due-slip-head">Date due</div>
          <ul>
            <li><span>12 AUG</span></li>
            <li><span>03 SEP</span></li>
            <li className="stamp-late"><span>19 SEP</span><em>5 days late · ₹25</em></li>
            <li><span>09 OCT</span></li>
            <li className="blank" />
            <li className="blank" />
          </ul>
        </div>
        <Link to="/catalogue" className="auth-art-link">Browse the catalogue without logging in</Link>
      </section>
      <section className="auth-form">
        <div className="auth-form-inner">
          <div className="d-lg-none auth-brand-sm mb-4"><i className="bi bi-book-half" /> College Library</div>
          <h1 className="page-title mb-1">{title}</h1>
          {subtitle && <p className="text-body-secondary mb-4">{subtitle}</p>}
          {children}
          <div className="d-lg-none text-center mt-4"><Link to="/catalogue" className="small">Browse the catalogue without logging in</Link></div>
          {isNativeApp && <ServerSettings />}
        </div>
      </section>
    </div>
  );
}
