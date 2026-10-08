/**
 * Unit K Pulse — Google Sheets + Drive receiver.
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
 *
 * NOTES / REFERENCE FILES: the app's Notes section lets an operator
 * attach a reference file (Word/OpenDocument/PDF — a labeler settings
 * sheet, a scale calibration doc, etc.) to one of a few fixed folders.
 * No one signs into Google on their phone for this — the file is base64
 * -encoded, POSTed here just like a log entry, and this script drops it
 * into the matching Drive subfolder (created on first use) next to the
 * Sheet. Folders are listed/browsed the same way, via a GET request.
 */

const SHEET_NAME = "Downtime Log";
const LOCK_TIMEOUT_MS = 10000;
const NOTES_ROOT_FOLDER_NAME = "Unit K Pulse Notes";
const NOTE_FOLDERS = [
  "YCM Scales Settings",
  "Labeler Basic Checks",
  "Line Manning",
  "Production Plan"
];

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

    if (data.action === "uploadFile") {
      return handleUploadFile(data);
    }

    // Default (no action field): a downtime log entry, same as always.
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
  const action = e.parameter && e.parameter.action;

  if (action === "listFiles") {
    return handleListFiles(e.parameter.folder || "");
  }
  if (action === "listFolders") {
    return jsonResponse({ status: "ok", folders: NOTE_FOLDERS });
  }

  // Simple health check — open the /exec URL in a browser to confirm it's live.
  return jsonResponse({ status: "ok", message: "Unit K Pulse endpoint is running." });
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

/** The Drive folder the bound Sheet lives in. */
function getSpreadsheetParentFolder() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const file = DriveApp.getFileById(ss.getId());
  const parents = file.getParents();
  return parents.hasNext() ? parents.next() : DriveApp.getRootFolder();
}

function getOrCreateChildFolder(parent, name) {
  const existing = parent.getFoldersByName(name);
  if (existing.hasNext()) {
    return existing.next();
  }
  return parent.createFolder(name);
}

/** The fixed subfolder (YCM Scales Settings, etc.) a note file lives in. */
function getNoteFolder(folderName) {
  if (NOTE_FOLDERS.indexOf(folderName) === -1) {
    throw new Error("Unknown notes folder: " + folderName);
  }
  const root = getOrCreateChildFolder(getSpreadsheetParentFolder(), NOTES_ROOT_FOLDER_NAME);
  return getOrCreateChildFolder(root, folderName);
}

function handleUploadFile(data) {
  const folderName = data.folder || "";
  const fileName = data.fileName || "";
  const mimeType = data.mimeType || "application/octet-stream";
  const base64Data = data.base64Data || "";

  if (!fileName || !base64Data) {
    return jsonResponse({ status: "error", message: "Missing fileName or file data." });
  }

  const folder = getNoteFolder(folderName);

  // Replace an existing file of the same name, so re-uploading an updated
  // settings sheet doesn't pile up duplicate copies.
  const existing = folder.getFilesByName(fileName);
  while (existing.hasNext()) {
    existing.next().setTrashed(true);
  }

  const bytes = Utilities.base64Decode(base64Data);
  const blob = Utilities.newBlob(bytes, mimeType, fileName);
  const file = folder.createFile(blob);

  return jsonResponse({
    status: "ok",
    fileUrl: file.getUrl(),
    fileName: file.getName()
  });
}

function handleListFiles(folderName) {
  try {
    const folder = getNoteFolder(folderName);
    const files = folder.getFiles();
    const result = [];
    while (files.hasNext()) {
      const f = files.next();
      result.push({
        name: f.getName(),
        url: f.getUrl(),
        mimeType: f.getMimeType(),
        updatedAt: f.getLastUpdated().toISOString()
      });
    }
    return jsonResponse({ status: "ok", files: result });
  } catch (err) {
    return jsonResponse({ status: "error", message: err.message });
  }
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
