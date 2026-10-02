# Android App

The Android app is the same React frontend packaged as a native Android app with **Capacitor 8**. Every screen and feature from the website is in the app, and it talks to the same Spring Boot backend and PostgreSQL database. There is no second codebase to maintain: change the React code, rebuild, and both the website and the app are updated.

What the app adds on Android:

- **Real app icon and splash screen** (an open book on library green).
- **Android back button** closes a dialog, then the side menu, then goes back a page, and finally leaves the app.
- **Report CSV export** opens the Android share sheet: save to Files or Drive, or open in Google Sheets/Excel.
- **Server address setting** on the login screen, so one app works with the emulator, a real phone on Wi-Fi, or the deployed backend.

The Android project lives in `frontend/android/`; the app settings are in `frontend/capacitor.config.json`.

## Quickest: get the APK from GitHub (no Android Studio)

The repository includes a GitHub Actions workflow (`.github/workflows/android-apk.yml`) that builds the APK on GitHub's servers.

1. Push the project to GitHub (docs/Deployment.md, step 1).
2. *Optional but recommended:* if your backend is deployed, go to the repo's **Settings → Secrets and variables → Actions → Variables → New repository variable** and add `ANDROID_API_BASE_URL` = `https://library-api.onrender.com/api` (your real backend URL). The app then connects there out of the box. Without it, the app starts with the emulator address and you set the server on the login screen.
3. Open the **Actions** tab → **Build Android APK** → **Run workflow**. It also runs by itself on every push to `main` that changes the frontend. A build takes about 5–10 minutes.
4. When it shows a green tick, open **Releases → Android APK (latest)** and download `college-library.apk`. On a phone, open that page in the browser and tap the file. Each run also keeps a copy under the run's **Artifacts** for 30 days.
5. Android asks to allow installing apps from your browser or file manager — allow it. Google Play Protect may warn that the app is from an unknown developer; choose **Install anyway**.

Every build is signed with the same key and gets a higher version number, so a newer APK installs over the old one and keeps the app's saved server address. GitHub Actions is free for public repositories; private repositories get a monthly allowance of free minutes, and their release downloads need a GitHub login.

If a build fails, open the failed run, expand the red step and copy the error.

---

## 1. Install (once)

| Software | Notes |
|---|---|
| **Node.js 22 or newer** | Capacitor 8 requires 22+ (the website alone works with 18+). Check with `node -v`. |
| **Android Studio** (latest version) | From developer.android.com/studio. It includes the Java 21 runtime and the Android SDK the project needs. |

In Android Studio, open **Settings → Languages & Frameworks → Android SDK** and make sure **Android 16 (API 36)** is installed. The app itself runs on any phone with **Android 7.0 or newer**.

## 2. Start the backend

The app needs the backend running, exactly as for the website (README section 11):

```bash
cd backend
mvn spring-boot:run
```

`CORS_ALLOWED_ORIGINS` must include `https://localhost` — that is the address the app's pages are served from inside the phone. It is already in the default and in `backend/.env.example`; if you created your `.env` earlier, update that line to:

```
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://localhost
```

## 3. Build and open the app

```bash
cd frontend
npm install                 # always first: the Android project uses files from node_modules
npm run android:build       # builds the React app and copies it into the Android project
npm run android:open        # opens frontend/android in Android Studio
```

In Android Studio:

1. Wait for **Gradle sync** to finish (the first time it downloads a few hundred MB).
2. Pick a device in the toolbar: an emulator (**Device Manager → Create Virtual Device**, e.g. Pixel 8, API 36) or your phone (below).
3. Press **Run ▶**.

## 4. Point the app at the backend

Inside a phone, `localhost` means the phone itself, not your computer. Tap **Change** next to "Server" at the bottom of the login screen, enter the address, tap **Test connection**, then **Save**.

| Where the app runs | Server address |
|---|---|
| Android emulator, backend on the same computer | `http://10.0.2.2:8080/api` (the default — nothing to change) |
| Real phone on the same Wi-Fi as your computer | `http://<computer's IP>:8080/api`, e.g. `http://192.168.1.5:8080/api` |
| Any phone, backend deployed on Render | `https://library-api.onrender.com/api` |

**Finding your computer's IP:** Windows: run `ipconfig` and use the *IPv4 Address* of your Wi-Fi adapter. Mac: *System Settings → Wi-Fi → Details*. Linux: `hostname -I`.

**Windows firewall:** the first time, Windows may ask whether Java can accept connections — allow it on **Private networks**. If the phone still can't connect, allow inbound TCP port 8080 in *Windows Defender Firewall → Advanced settings → Inbound Rules*.

## 5. Run on your own phone

1. On the phone: **Settings → About phone →** tap **Build number** 7 times to unlock Developer options.
2. **Settings → System → Developer options →** turn on **USB debugging**.
3. Connect the phone by USB and accept the "Allow USB debugging?" prompt.
4. The phone appears in Android Studio's device list — press **Run ▶**.

## 6. Make an APK to share

**Build → Build App Bundle(s) / APK(s) → Build APK(s).** Android Studio shows a link to the file:
`frontend/android/app/build/outputs/apk/debug/app-debug.apk`. Or skip Android Studio and let GitHub build it (top of this page).

The APK is signed with a fixed *debug* key committed at `frontend/android/app/library-debug.keystore`. Debug keys are public by design, which is fine for demos — never use it for a Play Store release.

Send it to a phone (Drive, USB, email) and open it; Android asks to allow installing apps from that source. This *debug* APK is fine for demos, a viva, or classmates.

For the Play Store you need a signed release build: **Build → Generate Signed App Bundle / APK**, create a keystore, and keep the keystore file and passwords safe (losing them means you can never update the app). Before a public release, see the security note below.

## 7. After changing the code

```bash
cd frontend
npm run android:build
```

Then press **Run ▶** in Android Studio again. To change the icon or splash, edit the images in `frontend/assets/` and run `npm run android:assets`.

## Security note: plain HTTP

To reach a backend on your own computer (`http://...`), `capacitor.config.json` allows unencrypted HTTP (`"cleartext": true` and `"allowMixedContent": true`). That is right for development and demos.

For an app you publish that only talks to an **HTTPS** backend (such as Render), set both to `false`, then run `npm run android:build` again.

## What is not included

- **Push notifications** — notifications stay in-app (the bell); the app does not alert you when closed.
- **Offline use** — the app needs a connection to the backend.
- **Camera barcode scanning** — copy codes are typed, or entered with a USB/Bluetooth scanner that types like a keyboard.
- **iOS** — Capacitor supports it (`npx cap add ios`), but building needs a Mac with Xcode; it is not set up here.

## Troubleshooting

| Problem | Fix |
|---|---|
| `npm run android:build` fails with a Node version error | Install Node.js 22 or newer. |
| Gradle sync fails with *"Could not resolve project :capacitor-android"* or missing `node_modules` | Run `npm install` in `frontend`, then sync again. |
| Gradle sync complains about the Java version | Android Studio → *Settings → Build Tools → Gradle → Gradle JDK* → choose the bundled JDK (21). |
| Gradle asks to install a missing SDK platform | Click the link in the error, or install Android 16 (API 36) from the SDK Manager. |
| Login says *"Cannot reach the server at …"* | The address is wrong or the backend isn't running. Open `<address>/categories` in the phone's browser — it should show JSON. |
| Works on the emulator but not on a real phone | The phone must use your computer's IP (not 10.0.2.2), be on the same Wi-Fi, and the firewall must allow port 8080. |
| Requests fail with a CORS error (visible in Chrome at `chrome://inspect`) | Add `https://localhost` to `CORS_ALLOWED_ORIGINS` and restart the backend. |
| First login after a while takes a long time (deployed backend) | The free Render backend was asleep; it wakes in about a minute. |
| The app shows an old version of the screens | Run `npm run android:build` again before pressing Run. |

**Debugging:** with the app running on an emulator or USB-connected phone, open `chrome://inspect` in Chrome on your computer and click **inspect** under the app. You get the full browser DevTools (console, network) for the app.
