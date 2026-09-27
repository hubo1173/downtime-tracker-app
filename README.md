# Downtime Log

A simple Android app for logging line downtime and issues (labelers, depositors,
fish-knife depositors, scissors, utensils, and material/prep supply problems)
straight from the floor, on each operator's phone. Entries sync to a shared
Google Sheet automatically.

## What it does

- **Log Issue / Downtime** — pick the line (1–6), the station (Depositor, Fish
  Knife Depositor, Labeler, Scissors, Utensils/Scoop, Material Supply, Other),
  and, for a Labeler, which part (roller, transfer belt, sponge wheel, blade/glue
  build-up, settings/changeover) or, for Material Supply, what happened (late
  delivery, not enough made, wrong material, ran out). Tap a quick reason chip
  or write your own.
- **Start Downtime Timer** — starts the clock now; the Home screen shows it
  running live with a Stop button. Duration is calculated automatically.
- **Save Entry (no timer)** — for logging something after the fact, with an
  optional minutes-lost field.
- **Works offline** — every entry saves to the phone immediately. It syncs to
  the Google Sheet in the background whenever there's a connection, and
  retries automatically; nothing is lost if the line has bad signal.
- **History** — every entry on the phone, with sync status.

## One-time setup (do this first)

### 1. Connect the Google Sheet
1. Create a Google Sheet (or use an existing one) for the team's downtime log.
2. In the Sheet: **Extensions → Apps Script**.
3. Delete any placeholder code, and paste in the contents of `apps-script/Code.gs`
   (included in this project).
4. **Deploy → New deployment → Web app**.
   - Execute as: **Me**
   - Who has access: **Anyone** (so phones can post without a Google login)
5. Click **Deploy**, approve the permissions, and copy the **Web app URL**
   (it ends in `/exec`).
6. Open the URL once in a browser — you should see `{"status":"ok", ...}`.
   A "Downtime Log" tab with headers will appear in the Sheet the first time
   a real entry comes in.

### 2. Build the app
1. Open this folder (`DowntimeTracker/`) in **Android Studio** (Iguana or newer).
   Let Gradle sync — it will download the Android/Kotlin/Compose dependencies.
2. Build an APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or
   run `./gradlew assembleDebug` from a terminal. The APK lands in
   `app/build/outputs/apk/debug/app-debug.apk`.
3. Install it on each operator's phone (sideload the APK, or connect the
   phone via USB and hit Run in Android Studio; for wider rollout, use your
   MDM or upload as an internal-testing release on the Play Console).

### 3. Configure each phone
Open the app → the gear icon (Settings) → enter:
- **Your name** (so entries are attributed to the operator)
- **Google Sheet link** — paste the same Web App URL from step 1 on every phone

That's it — the whole team points at the same Sheet.

## Project layout

```
app/src/main/java/com/foodpack/downtime/
  data/     Room database, entities, DAO, the line/station/reason lists
            (Constants.kt — edit this file to add lines, stations, parts,
            or reasons as your process changes), settings storage
  sync/     Uploads entries to the Google Sheet, background retry via WorkManager
  ui/       Compose screens: Home, Log Entry, History, Settings
apps-script/Code.gs   Paste into Google Sheets' Apps Script editor
```

## Extending it

Everything specific to your line — the list of lines, stations, labeler parts,
material sub-issues, and the quick-tap reason chips — lives in one file:
`app/src/main/java/com/foodpack/downtime/data/Constants.kt`. Add a new line,
a new failure mode, or a new station there and it shows up in the app; no
other file needs to change.

You mentioned more detail is coming — when it does, the two places most
likely to need editing are that `Constants.kt` file (for new categories) and
`apps-script/Code.gs`'s `HEADERS` list (if you want new columns in the Sheet).
