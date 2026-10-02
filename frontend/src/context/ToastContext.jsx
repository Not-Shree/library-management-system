import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const ToastContext = createContext(null);

/** Small toast messages in the bottom-right corner (Bootstrap toast styling, no Bootstrap JS). */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);

  const show = useCallback((message, variant = 'success') => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, message, variant }]);
    setTimeout(() => remove(id), variant === 'danger' ? 7000 : 4000);
  }, [remove]);

  const api = useMemo(() => ({
    success: (m) => show(m, 'success'),
    error: (m) => show(m, 'danger'),
    info: (m) => show(m, 'primary'),
    warning: (m) => show(m, 'warning'),
  }), [show]);

  const icons = { success: 'check-circle-fill', danger: 'exclamation-octagon-fill', primary: 'info-circle-fill', warning: 'exclamation-triangle-fill' };

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-container position-fixed bottom-0 end-0 p-3" style={{ zIndex: 1100 }} aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast show align-items-center border-0 text-bg-${t.variant}`} role="status">
            <div className="d-flex">
              <div className="toast-body d-flex gap-2 align-items-start">
                <i className={`bi bi-${icons[t.variant]} mt-1`} aria-hidden="true" />
                <span>{t.message}</span>
              </div>
              <button type="button" className={`btn-close ${t.variant === 'warning' ? '' : 'btn-close-white'} me-2 m-auto`}
                aria-label="Close" onClick={() => remove(t.id)} />
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
