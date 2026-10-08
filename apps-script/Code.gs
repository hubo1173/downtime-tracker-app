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
 *
 * EDIT / DELETE FROM THE APP: every downtime row carries a hidden
 * "Entry ID" in its last column. Editing or deleting an entry on the
 * phone sends action "updateEntry" / "deleteEntry" with that ID, and
 * this script finds the matching row by ID and updates or removes it
 * in place — so the phone and the Sheet never show two different
 * versions of the same entry.
 *
 * SHIFT HANDOVER: the Morning Update and End of Shift Update each post
 * to their own sheet tab ("Morning Handover" / "Evening Handover"),
 * created automatically on first use. Both carry a hidden "Entry ID" in
 * their last column too, so edits/deletes from the app's Handover
 * History screen find and update/remove the right row, same as the
 * Downtime Log.
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

// "Entry ID" is last so existing column positions (and any formulas that
// reference them) never shift.
const HEADERS = [
  "Date", "Time", "Operator", "Line", "Category", "Equipment",
  "Fault", "Note", "Duration (min)", "Start", "End", "Received At", "Entry ID"
];
const ENTRY_ID_COLUMN = HEADERS.length; // 1-based column number

// "Entry ID" is last on both Handover sheets too, same reasoning as the
// Downtime Log — it lets an edit or delete sent from the phone find the
// right row without disturbing any existing column positions.
const MORNING_SHEET_NAME = "Morning Handover";
const MORNING_HEADERS = [
  "Date", "Operator",
  "SHE Incident", "SHE Detail", "Technical", "Technical Detail",
  "Quality", "Quality Detail", "Downtime", "Downtime Detail",
  "Today's Volume", "Tomorrow's Volume",
  "Team A Planned", "Team A Actual", "Team A Finish Planned", "Team A Finish Actual",
  "Team B Planned", "Team B Actual", "Team B Finish Planned", "Team B Finish Actual",
  "Team C Planned", "Team C Actual", "Team C Finish Planned", "Team C Finish Actual",
  "Packing Ppl", "Packing Trainees",
  "Absence", "Absence Names", "RTW", "RTW Names", "Not On Rota", "Not On Rota Names",
  "Risk To Service", "Risk Detail",
  "Agency Collation", "Agency Prep", "Agency Hygiene", "Agency Fridge", "Agency Packaging Store",
  "Received At", "Entry ID"
];
const MORNING_ENTRY_ID_COLUMN = MORNING_HEADERS.length;

const EVENING_SHEET_NAME = "Evening Handover";
const EVENING_HEADERS = [
  "Date", "Operator",
  "Accident/Incident", "Accident Detail", "Quality", "Quality Detail", "Downtime", "Downtime Detail",
  "Service Score %", "Efficiency %",
  "Team A Finished", "Team B Finished", "Team C Finished", "Collation Finished", "Tray Up Sent By",
  "Shorts", "Shorts Detail", "Report Updated", "Report Detail", "AOB", "AOB Detail",
  "Received At", "Entry ID"
];
const EVENING_ENTRY_ID_COLUMN = EVENING_HEADERS.length;

function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
  } catch (lockErr) {
    return jsonResponse({ status: "error", message: "Server busy, please retry." });
  }

  try {
    const data = JSON.parse(e.postData.contents);

    switch (data.action) {
      case "uploadFile":
        return handleUploadFile(data);
      case "updateEntry":
        return handleUpdateEntry(data);
      case "deleteEntry":
        return handleDeleteEntry(data);
      case "morningHandover":
        return handleHandover(data, MORNING_SHEET_NAME, MORNING_HEADERS, morningRow);
      case "eveningHandover":
        return handleHandover(data, EVENING_SHEET_NAME, EVENING_HEADERS, eveningRow);
      case "updateMorningHandover":
        return handleUpdateHandover(data, MORNING_SHEET_NAME, MORNING_HEADERS, MORNING_ENTRY_ID_COLUMN, morningRow);
      case "deleteMorningHandover":
        return handleDeleteHandover(data, MORNING_SHEET_NAME, MORNING_HEADERS, MORNING_ENTRY_ID_COLUMN);
      case "updateEveningHandover":
        return handleUpdateHandover(data, EVENING_SHEET_NAME, EVENING_HEADERS, EVENING_ENTRY_ID_COLUMN, eveningRow);
      case "deleteEveningHandover":
        return handleDeleteHandover(data, EVENING_SHEET_NAME, EVENING_HEADERS, EVENING_ENTRY_ID_COLUMN);
    }

    // Default (no action field): a new downtime log entry.
    const sheet = getOrCreateSheet();
    sheet.appendRow(downtimeRow(data));
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

function downtimeRow(data) {
  return [
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
    new Date(),
    data.id === null || data.id === undefined ? "" : data.id
  ];
}

function handleUpdateEntry(data) {
  const sheet = getOrCreateSheet();
  const row = findRowByEntryId(sheet, ENTRY_ID_COLUMN, data.id);
  if (row === -1) {
    // Nothing to update yet (e.g. it never actually landed before) — append
    // instead of silently dropping the edit.
    sheet.appendRow(downtimeRow(data));
    return jsonResponse({ status: "ok", note: "Row not found, appended instead." });
  }
  // Overwrite every column except Entry ID itself.
  sheet.getRange(row, 1, 1, ENTRY_ID_COLUMN - 1).setValues([downtimeRow(data).slice(0, ENTRY_ID_COLUMN - 1)]);
  return jsonResponse({ status: "ok" });
}

function handleDeleteEntry(data) {
  const sheet = getOrCreateSheet();
  const row = findRowByEntryId(sheet, ENTRY_ID_COLUMN, data.id);
  if (row !== -1) {
    sheet.deleteRow(row);
  }
  return jsonResponse({ status: "ok" });
}

/** Scans the given 1-based ID column for a matching Entry ID; returns the row number, or -1. */
function findRowByEntryId(sheet, idColumn, id) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 2 || id === null || id === undefined) return -1;
  const ids = sheet.getRange(2, idColumn, lastRow - 1, 1).getValues();
  for (let i = 0; i < ids.length; i++) {
    if (String(ids[i][0]) === String(id) && String(id) !== "") {
      return i + 2;
    }
  }
  return -1;
}

function yn(value) {
  return value ? "Y" : "N";
}

function numOrBlank(value) {
  return value === null || value === undefined ? "" : value;
}

function morningRow(data) {
  return [
    data.date || "", data.operator || "",
    yn(data.sheIncident), data.sheIncidentDetail || "",
    yn(data.technicalIssue), data.technicalDetail || "",
    yn(data.qualityIssue), data.qualityDetail || "",
    yn(data.downtimeIssue), data.downtimeDetail || "",
    numOrBlank(data.todaysVolume), numOrBlank(data.tomorrowsVolume),
    numOrBlank(data.teamAPlanned), numOrBlank(data.teamAActual), data.teamAFinishPlanned || "", data.teamAFinishActual || "",
    numOrBlank(data.teamBPlanned), numOrBlank(data.teamBActual), data.teamBFinishPlanned || "", data.teamBFinishActual || "",
    numOrBlank(data.teamCPlanned), numOrBlank(data.teamCActual), data.teamCFinishPlanned || "", data.teamCFinishActual || "",
    numOrBlank(data.packingPpl), numOrBlank(data.packingTrainees),
    yn(data.absence), data.absenceNames || "",
    yn(data.rtw), data.rtwNames || "",
    yn(data.notOnRota), data.notOnRotaNames || "",
    yn(data.riskToService), data.riskToServiceDetail || "",
    numOrBlank(data.agencyCollation), numOrBlank(data.agencyPrep), numOrBlank(data.agencyHygiene),
    numOrBlank(data.agencyFridge), numOrBlank(data.agencyPackagingStore),
    new Date(),
    data.id === null || data.id === undefined ? "" : data.id
  ];
}

function eveningRow(data) {
  return [
    data.date || "", data.operator || "",
    yn(data.accidentIncident), data.accidentIncidentDetail || "",
    yn(data.qualityIssue), data.qualityDetail || "",
    yn(data.downtimeIssue), data.downtimeDetail || "",
    numOrBlank(data.serviceScore), numOrBlank(data.efficiency),
    data.teamAFinishedTime || "", data.teamBFinishedTime || "", data.teamCFinishedTime || "",
    data.collationFinished || "", data.trayUpSentBy || "",
    yn(data.shorts), data.shortsDetail || "",
    yn(data.reportUpdated), data.reportUpdatedDetail || "",
    yn(data.aob), data.aobDetail || "",
    new Date(),
    data.id === null || data.id === undefined ? "" : data.id
  ];
}

function handleHandover(data, sheetName, headers, rowBuilder) {
  const sheet = getOrCreateNamedSheet(sheetName, headers);
  sheet.appendRow(rowBuilder(data));
  return jsonResponse({ status: "ok" });
}

/** Edits an already-synced Handover row in place, found by its Entry ID —
 *  same pattern as handleUpdateEntry for the Downtime Log. */
function handleUpdateHandover(data, sheetName, headers, idColumn, rowBuilder) {
  const sheet = getOrCreateNamedSheet(sheetName, headers);
  const row = findRowByEntryId(sheet, idColumn, data.id);
  if (row === -1) {
    sheet.appendRow(rowBuilder(data));
    return jsonResponse({ status: "ok", note: "Row not found, appended instead." });
  }
  sheet.getRange(row, 1, 1, idColumn - 1).setValues([rowBuilder(data).slice(0, idColumn - 1)]);
  return jsonResponse({ status: "ok" });
}

function handleDeleteHandover(data, sheetName, headers, idColumn) {
  const sheet = getOrCreateNamedSheet(sheetName, headers);
  const row = findRowByEntryId(sheet, idColumn, data.id);
  if (row !== -1) {
    sheet.deleteRow(row);
  }
  return jsonResponse({ status: "ok" });
}

function getOrCreateSheet() {
  return getOrCreateNamedSheet(SHEET_NAME, HEADERS);
}

function getOrCreateNamedSheet(name, headers) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
  }
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
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
