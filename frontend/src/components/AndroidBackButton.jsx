import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { isNativeApp } from '../platform.js';

/**
 * Makes the Android back button behave like users expect:
 * close an open dialog -> close the side menu -> go back a page -> leave the app.
 */
export default function AndroidBackButton() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!isNativeApp) return undefined;
    let listener;
    let cancelled = false;
    import('@capacitor/app').then(async ({ App }) => {
      const handle = await App.addListener('backButton', ({ canGoBack }) => {
        if (document.querySelector('.modal.show')) {
          // Our Modal component closes itself on Escape.
          document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
        } else if (document.querySelector('.sidebar-scrim')) {
          document.querySelector('.sidebar-scrim').click();
        } else if (canGoBack) {
          navigate(-1);
        } else {
          App.exitApp();
        }
      });
      if (cancelled) handle.remove(); else listener = handle;
    }).catch(() => { /* plugin unavailable: the default back behaviour still works */ });
    return () => { cancelled = true; listener?.remove(); };
  }, [navigate]);

  return null;
}
