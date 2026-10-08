/**
 * Downtime Log — Google Sheets receiver.
 *
 * SETUP (one time, by a supervisor):
 * 1. Create (or open) a Google Sheet for the log. Name the file (and/or
 *    the Drive folder it lives in) "Unit K Downtime" so everyone can find
 *    it in Drive — renaming or moving the file afterwards is always safe,
 *    this script writes to whichever Sheet it's bound to.
 * 2. Extensions > Apps Script. Delete any placeholder code and paste this whole file in.
 * 3. Click Deploy > New deployment.
 *    - Type: "Web app"
 *    - Execute as: "Me"
 *    - Who has access: "Anyone" (this lets the Android phones post to it
 *      without each operator needing a Google login)
 * 4. Click Deploy, authorize it, and copy the "Web app URL" it gives you
 *    (ends in /exec).
 * 5. Paste that URL into the app's Settings screen on every phone.
 *
 * The first entry ever posted will create a header row automatically.
 *
 * MULTIPLE PHONES AT ONCE: every operator's phone posts straight to this
 * same Sheet — that's already the simplest reliable way to collect from
 * many phones without running a server. A short lock below (LockService)
 * makes sure two phones submitting in the same instant don't collide;
 * each request just waits its turn (normally a few milliseconds) before
 * its row is appended.
 */

const SHEET_NAME = "Downtime Log";
const LOCK_TIMEOUT_MS = 10000;

const HEADERS = [
  "Date", "Time", "Operator", "Line", "Category", "Equipment",
  "Fault", "Note", "Duration (min)", "Start", "End", "Received At"
];

function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
  } catch (lockErr) {
    return jsonResponse({ status: "error", message: "Server busy, please retry." });
  }

  try {
    const data = JSON.parse(e.postData.contents);
    const sheet = getOrCreateSheet();

    sheet.appendRow([
      data.date || "",
      data.time || "",
      data.operator || "",
      data.line || "",
      data.station || "",
      data.part || "",
      data.reason || "",
      data.note || "",
      data.durationMinutes === null || data.durationMinutes === undefined ? "" : data.durationMinutes,
      data.startTime || "",
      data.endTime || "",
      new Date()
    ]);

    return jsonResponse({ status: "ok" });
  } catch (err) {
    return jsonResponse({ status: "error", message: err.message });
  } finally {
    lock.releaseLock();
  }
}

function doGet(e) {
  // Simple health check — open the /exec URL in a browser to confirm it's live.
  return jsonResponse({ status: "ok", message: "Downtime Log endpoint is running." });
}

function getOrCreateSheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SHEET_NAME);
  }
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(HEADERS);
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
