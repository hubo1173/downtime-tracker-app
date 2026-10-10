// Storage, Google Sheet sync, summaries and WhatsApp text. No UI in here.

// ---------- small helpers ----------
export const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const pad = n => String(n).padStart(2, "0");
export const fmtDate = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
export const fmtTimeS = d => `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
export const fmtHM = d => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
export const todayStr = () => fmtDate(new Date());
export const startOfToday = () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d.getTime(); };
export const intOrNull = v => { const n = parseInt(v, 10); return Number.isFinite(n) ? n : null; };
let lastId = 0;
export const newId = () => { lastId = Math.max(Date.now(), lastId + 1); return lastId; };

const LS = {
  get(k, d) { try { const v = localStorage.getItem(k); return v === null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { try { localStorage.setItem(k, JSON.stringify(v)); } catch (e) { console.warn("storage full?", e); } },
};

// ---------- settings ----------
const SETTINGS_DEFAULTS = { operatorName: "", webhookUrl: "", remindersOn: true, handoverTime: "06:45", breaksTime: "06:15" };
export const settings = {
  get() { return { ...SETTINGS_DEFAULTS, ...LS.get("ukp.settings", {}) }; },
  set(patch) { LS.set("ukp.settings", { ...this.get(), ...patch }); },
};

// ---------- collections ----------
function collection(key) {
  const read = () => LS.get(key, []);
  return {
    all: read,
    get: id => read().find(x => x.id === id),
    put(obj) {
      const list = read();
      const i = list.findIndex(x => x.id === obj.id);
      obj.rev = (obj.rev || 0) + 1;
      if (i >= 0) list[i] = obj; else list.push(obj);
      LS.set(key, list);
      return obj;
    },
    // update without bumping rev (used by the sync engine for flags)
    patch(id, p) {
      const list = read();
      const i = list.findIndex(x => x.id === id);
      if (i < 0) return;
      list[i] = { ...list[i], ...p };
      LS.set(key, list);
    },
    remove(id) { LS.set(key, read().filter(x => x.id !== id)); },
  };
}
export const downtime = collection("ukp.dt");
export const handovers = collection("ukp.ho");
export const breaks = collection("ukp.br");

// ---------- photos (IndexedDB, so big images don't fill localStorage) ----------
let dbp;
function db() {
  if (!dbp) dbp = new Promise((res, rej) => {
    const r = indexedDB.open("ukp", 1);
    r.onupgradeneeded = () => r.result.createObjectStore("photos");
    r.onsuccess = () => res(r.result);
    r.onerror = () => rej(r.error);
  });
  return dbp;
}
async function idb(mode, fn) {
  const d = await db();
  return new Promise((res, rej) => {
    const t = d.transaction("photos", mode);
    const req = fn(t.objectStore("photos"));
    t.oncomplete = () => res(req && req.result);
    t.onerror = () => rej(t.error);
  });
}
export const putPhoto = (id, blob) => idb("readwrite", s => s.put(blob, id));
export const getPhoto = id => idb("readonly", s => s.get(id));
export const delPhoto = id => idb("readwrite", s => s.delete(id));

/** Downscale a camera photo (max 1280px, JPEG 80%) like the Android app does. */
export async function compressImage(file, maxSide = 1280) {
  const bmp = await createImageBitmap(file);
  const scale = Math.min(1, maxSide / Math.max(bmp.width, bmp.height));
  const c = document.createElement("canvas");
  c.width = Math.round(bmp.width * scale);
  c.height = Math.round(bmp.height * scale);
  c.getContext("2d").drawImage(bmp, 0, 0, c.width, c.height);
  return new Promise((res, rej) => c.toBlob(b => (b ? res(b) : rej(new Error("Could not read photo"))), "image/jpeg", 0.8));
}
const blobToBase64 = blob => new Promise((res, rej) => {
  const r = new FileReader();
  r.onload = () => res(String(r.result).split(",")[1] || "");
  r.onerror = () => rej(r.error);
  r.readAsDataURL(blob);
});

// ---------- payloads (identical to the Android uploaders) ----------
function dtJson(e) {
  const created = new Date(e.createdAt);
  return {
    id: e.id, date: fmtDate(created), time: fmtTimeS(created), operator: e.operatorName,
    line: e.line, station: e.station, part: e.part || "", subIssue: e.subIssue || "", reason: e.reason, note: e.note || "",
    durationMinutes: e.durationMinutes ?? null, peopleAffected: e.peopleAffected ?? null, photoUrl: e.photoUrl || "",
    startTime: e.startTime ? fmtTimeS(new Date(e.startTime)) : "", endTime: e.endTime ? fmtTimeS(new Date(e.endTime)) : "",
  };
}
const HO_FIELDS = [
  "sheIncident", "sheIncidentDetail", "technicalIssue", "technicalDetail", "qualityIssue", "qualityDetail",
  "downtimeIssue", "downtimeDetail", "todaysVolume", "tomorrowsVolume",
  "teamAPlanned", "teamAActual", "teamAFinishPlanned", "teamAFinishActual",
  "teamBPlanned", "teamBActual", "teamBFinishPlanned", "teamBFinishActual",
  "teamCPlanned", "teamCActual", "teamCFinishPlanned", "teamCFinishActual",
  "packingPpl", "packingTrainees", "absence", "absenceNames", "rtw", "rtwNames", "notOnRota", "notOnRotaNames",
  "riskToService", "riskToServiceDetail", "agencyCollation", "agencyPrep", "agencyHygiene", "agencyFridge", "agencyPackagingStore",
  "accidentIncident", "accidentIncidentDetail", "serviceScore", "efficiency",
  "teamAFinishedTime", "teamBFinishedTime", "teamCFinishedTime", "collationFinished", "trayUpSentBy",
  "shorts", "shortsDetail", "reportUpdated", "reportUpdatedDetail", "aob", "aobDetail",
];
export const HO_BOOLS = new Set(["sheIncident", "technicalIssue", "qualityIssue", "downtimeIssue", "absence", "rtw", "notOnRota",
  "riskToService", "accidentIncident", "shorts", "reportUpdated", "aob"]);
export const HO_NUMS = new Set(["todaysVolume", "tomorrowsVolume", "teamAPlanned", "teamAActual", "teamBPlanned", "teamBActual",
  "teamCPlanned", "teamCActual", "packingPpl", "packingTrainees", "agencyCollation", "agencyPrep", "agencyHygiene",
  "agencyFridge", "agencyPackagingStore", "serviceScore", "efficiency"]);
export function blankHandover(type) {
  const o = { type, date: todayStr(), operatorName: "", createdAt: Date.now(), synced: false, sheetRowCreated: false, pendingDelete: false };
  for (const f of HO_FIELDS) o[f] = HO_BOOLS.has(f) ? false : HO_NUMS.has(f) ? null : "";
  return o;
}
function hoJson(e, withAction = true) {
  const j = { id: e.id, date: e.date, operator: e.operatorName };
  if (withAction) j.action = e.type === "MORNING" ? "morningHandover" : "eveningHandover";
  for (const f of HO_FIELDS) j[f] = e[f] === undefined ? (HO_BOOLS.has(f) ? false : HO_NUMS.has(f) ? null : "") : e[f];
  return j;
}
const brJson = e => ({ action: "breakLog", id: e.id, date: e.date, operator: e.operatorName, team: e.team, breakType: e.breakType, startTime: e.startTime, endTime: e.endTime });

// ---------- network ----------
function sheetUrl() { return settings.get().webhookUrl.trim(); }

async function post(json) {
  const url = sheetUrl();
  if (!url) throw new Error("No Google Sheet link set yet (see Settings).");
  let r;
  try {
    // text/plain keeps this a "simple" request, so the browser doesn't send a CORS preflight (Apps Script can't answer one).
    r = await fetch(url, { method: "POST", headers: { "Content-Type": "text/plain;charset=utf-8" }, body: JSON.stringify(json), redirect: "follow" });
  } catch {
    throw new Error("Could not reach the Sheet (offline, or the link is wrong).");
  }
  if (!r.ok) throw new Error("Server responded " + r.status);
  let j;
  try { j = JSON.parse(await r.text()); } catch { throw new Error("Unexpected reply. Check the link, and that access is set to Anyone."); }
  if (j.status !== "ok") throw new Error(j.message || "Server error");
  return j;
}

export async function getJson(params) {
  const url = sheetUrl();
  if (!url) throw new Error("No Google Sheet link set yet (see Settings).");
  const u = new URL(url);
  for (const [k, v] of Object.entries(params || {})) u.searchParams.set(k, v);
  let r;
  try { r = await fetch(u.toString(), { redirect: "follow" }); } catch { throw new Error("Could not reach the Sheet."); }
  if (!r.ok) throw new Error("Server responded " + r.status);
  let j;
  try { j = await r.json(); } catch { throw new Error("Unexpected reply from the Sheet link."); }
  if (j.status && j.status !== "ok") throw new Error(j.message || "Server error");
  return j;
}

export async function uploadNote(folder, file) {
  const base64Data = await blobToBase64(file);
  return post({ action: "uploadFile", folder, fileName: file.name, mimeType: file.type || "application/octet-stream", base64Data });
}

// ---------- sync engine (same order as the Android SyncWorker) ----------
let syncing = false;
export const isSyncing = () => syncing;
export const emitChange = () => window.dispatchEvent(new Event("ukp-change"));

async function runList(coll, pendingDel, upload, update, del) {
  let ok = true;
  for (const e of coll.all().filter(x => x.pendingDelete)) {
    if (!e.sheetRowCreated) { coll.remove(e.id); continue; }
    try { await del(e); coll.remove(e.id); }
    catch (err) { ok = false; coll.patch(e.id, { lastSyncError: err.message }); }
  }
  for (const e of coll.all().filter(x => !x.synced && !x.pendingDelete)) {
    try {
      await (e.sheetRowCreated ? update(e) : upload(e));
      const cur = coll.get(e.id);
      // If it was edited while uploading, keep it queued for another pass.
      coll.patch(e.id, { sheetRowCreated: true, lastSyncError: null, synced: !!cur && cur.rev === e.rev });
    } catch (err) { ok = false; coll.patch(e.id, { lastSyncError: err.message }); }
  }
  return ok;
}

export async function syncNow() {
  if (syncing || !sheetUrl() || !navigator.onLine) return;
  syncing = true; emitChange();
  try {
    // photos first so the Drive link rides along with the row
    for (const e of downtime.all().filter(x => x.hasPhoto && !x.photoUrl && !x.pendingDelete)) {
      try {
        const blob = await getPhoto(e.id);
        if (!blob) { downtime.patch(e.id, { hasPhoto: false, synced: false }); continue; }
        const j = await post({ action: "uploadPhoto", fileName: `downtime_${e.id}_${e.createdAt}.jpg`, mimeType: "image/jpeg", base64Data: await blobToBase64(blob) });
        downtime.patch(e.id, { photoUrl: j.fileUrl, synced: false });
        delPhoto(e.id).catch(() => {});
      } catch (err) { downtime.patch(e.id, { lastSyncError: "Photo: " + err.message }); }
    }
    await runList(downtime, null, e => post(dtJson(e)), e => post({ ...dtJson(e), action: "updateEntry" }), e => post({ action: "deleteEntry", id: e.id }));
    await runList(handovers, null, e => post(hoJson(e)),
      e => post({ ...hoJson(e, false), action: e.type === "MORNING" ? "updateMorningHandover" : "updateEveningHandover" }),
      e => post({ action: e.type === "MORNING" ? "deleteMorningHandover" : "deleteEveningHandover", id: e.id }));
    await runList(breaks, null, e => post(brJson(e)), e => post(brJson(e)), e => post({ action: "deleteBreak", id: e.id }));
    pruneOld();
  } finally { syncing = false; emitChange(); }
}

export const unsyncedCount = () =>
  [downtime, handovers, breaks].reduce((n, c) => n + c.all().filter(x => !x.synced || x.pendingDelete).length, 0);

/** Keep local storage small: drop fully-synced records older than 60 days. */
function pruneOld() {
  const cutoff = Date.now() - 60 * 864e5;
  for (const c of [downtime, handovers, breaks])
    for (const e of c.all()) if (e.synced && !e.pendingDelete && !e.isOpen && e.createdAt < cutoff) c.remove(e.id);
}

// ---------- repository-style actions ----------
export function saveDowntime(entry) { downtime.put(entry); syncNow(); return entry; }
export function stopTimer(id) {
  const e = downtime.get(id); if (!e) return;
  const end = Date.now();
  const mins = Math.max(0, Math.floor((end - (e.startTime || e.createdAt)) / 60000));
  downtime.put({ ...e, endTime: end, durationMinutes: mins, isOpen: false, synced: false });
  syncNow();
}
export function deleteDowntime(id) {
  const e = downtime.get(id); if (!e) return;
  delPhoto(id).catch(() => {});
  if (!e.sheetRowCreated) downtime.remove(id); else downtime.put({ ...e, pendingDelete: true, synced: false });
  syncNow();
}
export function deleteHandover(id) {
  const e = handovers.get(id); if (!e) return;
  if (!e.sheetRowCreated) handovers.remove(id); else handovers.put({ ...e, pendingDelete: true, synced: false });
  syncNow();
}
export function deleteBreak(id) {
  const e = breaks.get(id); if (!e) return;
  if (!e.sheetRowCreated) breaks.remove(id); else breaks.put({ ...e, pendingDelete: true, synced: false });
  syncNow();
}

// ---------- downtime summary (port of DowntimeSummary.kt) ----------
const minutesOf = (e, now) => e.durationMinutes ?? (e.isOpen ? Math.max(0, Math.floor((now - (e.startTime || e.createdAt)) / 60000)) : 0);
const causeOf = e => (e.part ? `${e.part}: ${e.reason}` : `${e.station}: ${e.reason}`);
function groupBy(list, now, key) {
  const m = new Map();
  for (const e of list) { const k = key(e); (m.get(k) || m.set(k, []).get(k)).push(e); }
  return [...m].map(([label, l]) => ({
    label, minutes: l.reduce((a, e) => a + minutesOf(e, now), 0), people: l.reduce((a, e) => a + (e.peopleAffected || 0), 0), stops: l.length,
  })).sort((a, b) => b.minutes - a.minutes || b.stops - a.stops);
}
export function summarise(all, sinceMillis, now = Date.now()) {
  const list = all.filter(e => e.createdAt >= sinceMillis && !e.pendingDelete);
  return {
    totalMinutes: list.reduce((a, e) => a + minutesOf(e, now), 0), stops: list.length,
    peopleAffected: list.reduce((a, e) => a + (e.peopleAffected || 0), 0),
    byCategory: groupBy(list, now, e => e.station), topCauses: groupBy(list, now, causeOf),
  };
}
export function handoverDowntimeText(r) {
  if (!r.stops) return "";
  const top = r.topCauses.slice(0, 3).map(g => `${g.label} ${g.minutes} min`).join("; ");
  const ppl = r.peopleAffected > 0 ? `, ${r.peopleAffected} ppl affected` : "";
  return `${r.totalMinutes} min lost across ${r.stops} stop${r.stops === 1 ? "" : "s"}${ppl}. Top: ${top}`;
}
export function reportText(title, r) {
  const lines = [`*${title}*`, `Total: ${r.totalMinutes} min lost, ${r.stops} stops, ${r.peopleAffected} people affected`, "", "By category:"];
  r.byCategory.forEach(g => lines.push(`- ${g.label}: ${g.minutes} min, ${g.people} ppl, ${g.stops} stops`));
  lines.push("", "Top causes:");
  r.topCauses.slice(0, 5).forEach((g, i) => lines.push(`${i + 1}. ${g.label}: ${g.minutes} min (${g.stops}x)`));
  return lines.join("\n");
}

/** Start of Shift: today's downtime from midnight up to 07:15 (yesterday's went out in the End of Shift message). */
export function morningDowntimeText() {
  const now = Date.now();
  const cutoff = new Date(); cutoff.setHours(7, 15, 0, 0);
  const c = cutoff.getTime();
  const all = downtime.all().filter(e => e.createdAt <= c);
  return handoverDowntimeText(summarise(all, startOfToday(), Math.min(now, c)));
}
/** End of Shift: everything logged today. */
export function eveningDowntimeText() { return handoverDowntimeText(summarise(downtime.all(), startOfToday())); }

// ---------- WhatsApp text (port of HandoverShare.kt) ----------
const yn = (flag, detail) => (flag ? "Y" + (detail ? " - " + detail : "") : "N");
const n = v => (v === null || v === undefined || v === "" ? "-" : String(v));
const dash = v => v || "-";
const teamLine = (name, p, a, fp, fa) => `${name}:  planned ${n(p)}  actual ${n(a)}\nFinish time: planned ${dash(fp)}  actual ${dash(fa)}`;

export function handoverText(e) {
  if (e.type === "MORNING") return [
    `*Morning Update* - ${e.date}`, "", "In the last 24hrs:",
    `SHE incidents and accidents: ${yn(e.sheIncident, e.sheIncidentDetail)}`,
    `Technical: ${yn(e.technicalIssue, e.technicalDetail)}`,
    `Quality: ${yn(e.qualityIssue, e.qualityDetail)}`,
    `Downtime: ${yn(e.downtimeIssue, e.downtimeDetail)}`,
    `Today's Volume: ${n(e.todaysVolume)}`, `Tomorrow's Volume: ${n(e.tomorrowsVolume)}`, "",
    teamLine("Team A", e.teamAPlanned, e.teamAActual, e.teamAFinishPlanned, e.teamAFinishActual),
    teamLine("Team B", e.teamBPlanned, e.teamBActual, e.teamBFinishPlanned, e.teamBFinishActual),
    teamLine("Team C", e.teamCPlanned, e.teamCActual, e.teamCFinishPlanned, e.teamCFinishActual),
    `Packing: ${n(e.packingPpl)} ppl + ${n(e.packingTrainees)} training`, "",
    `Absence: ${yn(e.absence, e.absenceNames)}`, `RTW: ${yn(e.rtw, e.rtwNames)}`, `Not on rota: ${yn(e.notOnRota, e.notOnRotaNames)}`, "",
    `Risk to Service: ${yn(e.riskToService, e.riskToServiceDetail)}`, "",
    `Agency: ${n(e.agencyCollation)} Collation, ${n(e.agencyPrep)} Prep, ${n(e.agencyHygiene)} Hygiene, ${n(e.agencyFridge)} Fridge, ${n(e.agencyPackagingStore)} Packaging Store`, "",
    "Thank you", e.operatorName || "",
  ].join("\n");
  return [
    `*End of Shift Update* - ${e.date}`, "",
    `Accident/incident: ${yn(e.accidentIncident, e.accidentIncidentDetail)}`,
    `Quality: ${yn(e.qualityIssue, e.qualityDetail)}`,
    `Downtime: ${yn(e.downtimeIssue, e.downtimeDetail)}`,
    `Service score: ${n(e.serviceScore)}%`, `Efficiency: ${n(e.efficiency)}%`, "",
    `Team A finished: ${dash(e.teamAFinishedTime)}`, `Team B finished: ${dash(e.teamBFinishedTime)}`, `Team C finished: ${dash(e.teamCFinishedTime)}`,
    `Collation finished: ${dash(e.collationFinished)}`, `Tray up sent by: ${dash(e.trayUpSentBy)}`, "",
    `Shorts: ${yn(e.shorts, e.shortsDetail)}`, `Report updated: ${yn(e.reportUpdated, e.reportUpdatedDetail)}`, `AOB: ${yn(e.aob, e.aobDetail)}`, "",
    "Regards", e.operatorName || "",
  ].join("\n");
}
export function breaksText(date, list) {
  const today = list.filter(b => b.date === date && !b.pendingDelete);
  const out = [`*Break Times* - ${date}`];
  for (const t of ["Team A", "Team B", "Team C"]) {
    out.push("", `*${t}*`);
    for (const type of ["Morning", "Afternoon"]) {
      const b = today.filter(x => x.team === t && x.breakType === type).sort((a, b2) => b2.createdAt - a.createdAt)[0];
      out.push(`${type} break: ${b ? `${dash(b.startTime)} to ${dash(b.endTime)}` : "-"}`);
    }
  }
  return out.join("\n");
}
export const breakText = b => `*Break Times* - ${b.date}\n${b.team} ${b.breakType.toLowerCase()} break: ${dash(b.startTime)} to ${dash(b.endTime)}`;

/** Opens WhatsApp with the text pre-filled (you then pick the group or person). */
export function shareToWhatsApp(text) {
  const url = "https://wa.me/?text=" + encodeURIComponent(text);
  const w = window.open(url, "_blank");
  if (!w) location.href = url;
}

// ---------- reminders as a calendar file (iPhone has no scheduled web notifications) ----------
export function remindersIcs() {
  const s = settings.get();
  const ev = (uid, title, hm) => {
    const [h, m] = hm.split(":").map(Number);
    const d = new Date(); const stamp = `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`;
    return ["BEGIN:VEVENT", `UID:${uid}@unitkpulse`, `DTSTAMP:${stamp}T000000Z`, `DTSTART:${stamp}T${pad(h)}${pad(m)}00`, `DTEND:${stamp}T${pad(h)}${pad(Math.min(59, m + 5))}00`,
      "RRULE:FREQ=DAILY", `SUMMARY:${title}`, "BEGIN:VALARM", "ACTION:DISPLAY", `DESCRIPTION:${title}`, "TRIGGER:PT0S", "END:VALARM", "END:VEVENT"].join("\r\n");
  };
  return ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Unit K Pulse//EN",
    ev("breaks", "Unit K: send break times (before 6:30)", s.breaksTime),
    ev("handover", "Unit K: send morning handover (before 7:00)", s.handoverTime),
    "END:VCALENDAR"].join("\r\n");
}
