import { useState } from 'react';
import axios from 'axios';
import { DEFAULT_API_BASE, getApiBase, setApiBase } from '../api/client.js';
import { useToast } from '../context/ToastContext.jsx';
import { Field, Modal } from './Common.jsx';

/**
 * Android app only (shown on the login and register screens).
 * Lets the user point the app at the computer or server that runs the backend.
 */
export default function ServerSettings() {
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState(getApiBase());
  const [status, setStatus] = useState(null);   // null | 'testing' | 'ok' | 'fail'
  const [current, setCurrent] = useState(getApiBase());

  const clean = url.trim().replace(/\/+$/, '');

  const test = async () => {
    setStatus('testing');
    try { await axios.get(`${clean}/categories`, { timeout: 90000 }); setStatus('ok'); }
    catch { setStatus('fail'); }
  };

  const save = () => {
    setApiBase(clean);
    setCurrent(getApiBase());
    setOpen(false);
    toast.success('Server address saved');
  };

  return (
    <>
      <div className="server-line small text-body-secondary mt-4">
        <i className="bi bi-hdd-network me-1" aria-hidden="true" />
        Server: <code>{current}</code>
        <button type="button" className="btn btn-link btn-sm p-0 ms-2 align-baseline" onClick={() => { setUrl(current); setStatus(null); setOpen(true); }}>Change</button>
      </div>

      <Modal show={open} title="Server address" onClose={() => setOpen(false)}
        footer={<>
          <button className="btn btn-light" onClick={() => setOpen(false)}>Cancel</button>
          <button className="btn btn-outline-primary" onClick={test} disabled={!clean || status === 'testing'}>
            {status === 'testing' && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}Test connection
          </button>
          <button className="btn btn-primary" onClick={save} disabled={!/^https?:\/\/.+/.test(clean)}>Save</button>
        </>}>
        <Field label="Backend API address" name="apiBase" className="col-12"
          hint="Must end with /api. The backend must be running and reachable from this phone.">
          <input id="apiBase" className="form-control" value={url} onChange={(e) => { setUrl(e.target.value); setStatus(null); }}
            inputMode="url" autoCapitalize="off" autoCorrect="off" spellCheck={false} />
        </Field>
        {status === 'ok' && <div className="alert alert-success small mt-3 mb-0">Connected. The library server answered.</div>}
        {status === 'fail' && <div className="alert alert-danger small mt-3 mb-0">No answer from {clean}. Check the address, that the backend is running, and that the phone is on the same Wi-Fi as the computer.</div>}
        <div className="small text-body-secondary mt-3">
          <div className="fw-semibold mb-1">Which address?</div>
          <div><strong>Android emulator</strong> (backend on this computer): <code>http://10.0.2.2:8080/api</code></div>
          <div><strong>Real phone</strong> on the same Wi-Fi: <code>http://&lt;your computer's IP&gt;:8080/api</code>, e.g. <code>http://192.168.1.5:8080/api</code></div>
          <div><strong>Deployed backend</strong>: <code>https://library-api.onrender.com/api</code></div>
          <button type="button" className="btn btn-link btn-sm p-0 mt-2" onClick={() => { setUrl(DEFAULT_API_BASE); setStatus(null); }}>Reset to default</button>
        </div>
      </Modal>
    </>
  );
}
