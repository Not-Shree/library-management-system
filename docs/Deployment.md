# Deployment (free): Neon + Render

| Part | Where | Free-tier limits worth knowing |
|---|---|---|
| Database | **Neon** (PostgreSQL) | 0.5 GB storage per project, a monthly compute allowance; the database pauses when idle and wakes on the next query |
| Backend API | **Render** web service (Docker) | Sleeps after 15 minutes with no traffic; the next request takes about a minute to wake it |
| Frontend | **Render** static site | Always on |

Free tiers change; check each provider's pricing page before you rely on them. This setup suits a demo or viva, not a library with real users.

**Before deploying:** run the project locally once (README section 11). If it starts on your machine, the Docker build on Render will behave the same way.

---

## 1. Put the code on GitHub

```bash
cd library-management-system
git init
git add .
git commit -m "Library Management System"
```

Create an empty repository on github.com (no README), then:

```bash
git remote add origin https://github.com/<your-username>/library-management-system.git
git branch -M main
git push -u origin main
```

`.gitignore` already keeps `.env`, `node_modules` and build output out of the repository. Check that no `.env` file appears on GitHub.

## 2. Create the database on Neon

1. Sign up at **neon.com** and create a project. Pick the **Singapore** region (closest to India and to the Render region below).
2. On the project dashboard, open **Connect**. Switch **Connection pooling off** so the host name does **not** contain `-pooler`. Copy the connection string. It looks like:
   ```
   postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   ```
3. Load the tables and demo data (schema first), using the `psql` you installed for local development:
   ```bash
   psql "postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require" -f database/schema.sql
   psql "postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require" -f database/seed.sql
   ```
   No `psql`? Open Neon's **SQL Editor**, paste the contents of `schema.sql`, run it, then do the same with `seed.sql`.
4. Split the connection string into the three values the backend needs:

   | Variable | Value (from the example above) |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://ep-cool-name-123456.ap-southeast-1.aws.neon.tech/neondb?sslmode=require` |
   | `DB_USERNAME` | `neondb_owner` |
   | `DB_PASSWORD` | `AbC123xyz` |

   `DB_URL` must start with `jdbc:postgresql://` and must **not** contain the user name and password. If Neon's string ends with `&channel_binding=require`, leave that part out.

## 3. Deploy the backend and frontend on Render

1. Sign up at **render.com** with your GitHub account.
2. **New → Blueprint** → choose your repository. Render reads `render.yaml` and shows two services, `library-api` and `library-web`.
3. Render asks for the values marked `sync: false`. Fill in:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: from step 2.4.
   - `CORS_ALLOWED_ORIGINS`: `https://library-web.onrender.com,https://localhost` (the second one lets the Android app connect)
   - `VITE_API_BASE_URL`: `https://library-api.onrender.com/api`

   `JWT_SECRET` is generated for you.
4. Click **Apply**. The backend's first build takes several minutes (Maven downloads everything); the frontend takes about one minute.

**If Render gave your services different URLs** (it adds a suffix when a name is taken, e.g. `library-api-x7k2.onrender.com`), update the two URL variables to the real addresses:
- `CORS_ALLOWED_ORIGINS` on **library-api**
- `VITE_API_BASE_URL` on **library-web**, then **Manual Deploy → Deploy latest commit**. The frontend reads this value while it is being *built*, so it must be rebuilt after any change.

## 4. Check it works

1. Open `https://library-api.onrender.com/api/categories` — you should see a JSON list of 5 categories. This proves the backend is running and can reach Neon.
2. Open `https://library-web.onrender.com` and log in with a demo account.
3. Swagger UI: `https://library-api.onrender.com/swagger-ui.html`.

## 5. Secure it — do this immediately

The demo accounts from `seed.sql` have well-known passwords, and the site is now public.

1. Log in as **admin** → **Profile** → change the password.
2. **Librarians** → reset the `librarian` password.
3. **Members** → Riya Patil → **Reset password** for `student`.

Never commit real passwords or the Neon connection string to GitHub; they belong only in Render's **Environment** settings.

## 6. Updating the site

Every `git push` to `main` redeploys both services automatically.

---

## Things that are normal on the free tier

- **First visit is slow.** If nobody used the site for 15 minutes, the backend is asleep; the first request waits about a minute while it wakes (the app waits up to 90 seconds before showing an error). Before a demo or viva, open the site 2–3 minutes early.
- **Daily reminders** (due soon, overdue) run whenever the backend starts, and at 08:00 if it is awake then.
- **Neon also pauses when idle**, which adds a second or two to the first query.

## Troubleshooting

| Symptom | Where to look / fix |
|---|---|
| Backend deploy fails during `mvn` | Render → library-api → **Logs**. A compile error here would also appear locally with `mvn package`. |
| Logs show `JWT_SECRET is missing` | The variable was deleted; add any random 32+ character value under **Environment**. |
| Logs show `password authentication failed` or `Connection refused` | Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` against step 2.4. |
| Logs show `relation "users" does not exist` | `schema.sql` was not loaded into the Neon database. |
| Logs show `OutOfMemoryError` or the service keeps restarting | The free 512 MB instance is too small at that moment; redeploy, or move to a paid instance. |
| Site loads but login shows "Cannot reach the server" | `VITE_API_BASE_URL` is wrong, or the frontend was not rebuilt after changing it. |
| Browser console shows a CORS error | `CORS_ALLOWED_ORIGINS` does not exactly match the frontend URL (https, no trailing slash). |
| Refreshing a page like `/dashboard` gives "Not found" | The rewrite rule is missing: library-web → **Redirects/Rewrites** → add `/*` → `/index.html` (Rewrite). |
