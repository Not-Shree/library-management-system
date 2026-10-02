import { useCallback, useEffect, useRef, useState } from 'react';
import api, { errorMessage } from '../api/client.js';

/**
 * Loads data from a GET endpoint and reloads when params change.
 * Returns { data, loading, error, reload }.
 */
export function useApi(url, params = {}, { enabled = true } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);
  const key = JSON.stringify(params);
  const requestId = useRef(0);

  const load = useCallback(() => {
    if (!enabled || !url) return;
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    api.get(url, { params: JSON.parse(key) })
      .then((res) => { if (id === requestId.current) setData(res.data); })
      .catch((e) => { if (id === requestId.current) setError(errorMessage(e)); })
      .finally(() => { if (id === requestId.current) setLoading(false); });
  }, [url, key, enabled]);

  useEffect(() => { load(); }, [load]);

  return { data, loading, error, reload: load };
}

/** Delays a fast-changing value (e.g. search box) so we don't call the API on every key press. */
export function useDebounced(value, delay = 350) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}
