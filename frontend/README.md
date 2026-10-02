# Frontend — React app

React 18 · Vite 5 · Bootstrap 5 · Bootstrap Icons · Axios · React Router 6 · Recharts

## Run

```bash
cp .env.example .env     # VITE_API_BASE_URL=http://localhost:8080/api
npm install
npm run dev              # http://localhost:5173
npm run build            # production build in dist/
```

The backend must be running (see `../backend/README.md`).

## Android app

```bash
npm run android:build    # vite build + copy into android/
npm run android:open     # open in Android Studio
npm run android:assets   # regenerate icons/splash from assets/
```

Needs Node 22+ and Android Studio. See `../docs/Android-App.md`.

## Code map

| Folder / file | What is in it |
|---|---|
| `src/api/client.js` | Axios instance: adds the JWT, handles 401 (log out), turns errors into readable messages, CSV download. |
| `src/context/AuthContext.jsx` | Logged-in user, login / register / logout, role helpers (`isAdmin`, `isStaff`, ...). |
| `src/context/ToastContext.jsx` | Success / error pop-up messages. |
| `src/hooks/useApi.js` | `useApi(url, params)` loads data and reloads when params change; `useDebounced` for search boxes. |
| `src/components/` | Layout (sidebar + top bar), route protection, shared UI (badges, due-date stamp, pagination, modal), charts, member picker, payment dialog. |
| `src/pages/public/` | Login, register, forgot password, public catalogue. |
| `src/pages/staff/` | Admin and librarian pages: dashboards, books, copies, members, issue, return / renew, fines, payments, reservations, reports, settings, audit logs. |
| `src/pages/member/` | Member dashboard, my books, reservations, fines, payments, notifications, profile. |
| `src/platform.js` | `isNativeApp`: true inside the Android app. |
| `src/components/ServerSettings.jsx`, `AndroidBackButton.jsx` | App-only: backend address setting, Android back button. |
| `capacitor.config.json`, `android/` | Android app settings and the Android Studio project. |
| `src/styles.css` | The library look: colours, typography, due-date stamp, receipt-style fine slip. |

Routes and role access are defined in `src/App.jsx`. Hiding a page in the UI is only for convenience — the backend checks the role on every request.
