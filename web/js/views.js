// Screens. Each view returns { html, mount?, live? }. All form values live in S so a re-render never loses input.
import * as C from "./constants.js";
import {
  esc, fmtDate, fmtHM, todayStr, startOfToday, intOrNull, newId, settings, downtime, handovers, breaks,
  putPhoto, compressImage, saveDowntime, stopTimer, deleteDowntime, deleteHandover, deleteBreak, syncNow, unsyncedCount, isSyncing,
  summarise, reportText, handoverText, breaksText, breakText, shareToWhatsApp, morningDowntimeText, eveningDowntimeText,
  blankHandover, HO_BOOLS, HO_NUMS, getJson, uploadNote, remindersIcs,
} from "./core.js";

export const S = { log: null, ho: null, br: { team: "Team A", breakType: "Morning", start: "", end: "" }, sumDays: 7, msg: "", notes: { folder: null, files: [], loading: false, error: "" }, speed: { left: 30, count: 0, state: "ready" }, sleeve: "" };
let nav = () => {};
export const setNav = fn => { nav = fn; };
const go = h => { location.hash = h; };

// ---------- chrome ----------
const page = (title, body, { back = "#/", actions = "", footer = "" } = {}) => `
  <header class="bar">${back ? `<a class="icon" href="${back}" aria-label="Back">←</a>` : ""}<h1>${title}</h1><div class="grow"></div>${actions}</header>
  <main class="content">${body}</main>${footer}`;

const chip = (scope, field, value, selected, icon = "", big = false) =>
  `<button type="button" class="chip${selected ? " sel" : ""}${big ? " big" : ""}" data-act="set" data-s="${scope}" data-f="${field}" data-v="${esc(value)}">${icon ? `<span class="ci">${icon}</span>` : ""}${esc(value)}</button>`;
const syncIcon = e => (e.pendingDelete ? "🗑️" : e.synced ? '<span class="ok" title="Synced">✓</span>' : '<span class="wait" title="Waiting to sync">⏳</span>');
const label = t => `<div class="lbl">${t}</div>`;
const fmtWhen = ms => new Date(ms).toLocaleString("en-GB", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

// ---------- home ----------
function todayStats() {
  const start = startOfToday(), now = Date.now(), byReason = new Map();
  let total = 0;
  for (const e of downtime.all()) {
    if (e.createdAt < start || e.pendingDelete) continue;
    const m = e.isOpen ? Math.max(0, Math.floor((now - (e.startTime || e.createdAt)) / 60000)) : (e.durationMinutes || 0);
    total += m; byReason.set(e.reason, (byReason.get(e.reason) || 0) + m);
  }
  const top = [...byReason].sort((a, b) => b[1] - a[1])[0];
  return { total, top };
}
function dueBanners() {
  const s = settings.get(), now = new Date(), out = [];
  if (!s.remindersOn || now.getHours() >= 12) return "";
  const today = todayStr();
  const mins = (hm) => { const [h, m] = hm.split(":").map(Number); return h * 60 + m; };
  const cur = now.getHours() * 60 + now.getMinutes();
  if (cur >= mins(s.breaksTime) && !breaks.all().some(b => b.date === today && !b.pendingDelete))
    out.push(`<a class="banner${cur > 6 * 60 + 30 ? " late" : ""}" href="#/breaks">☕ Break times are due before 6:30am. Tap to log them.</a>`);
  if (cur >= mins(s.handoverTime) && !handovers.all().some(h => h.type === "MORNING" && h.date === today && !h.pendingDelete))
    out.push(`<a class="banner${cur > 7 * 60 ? " late" : ""}" href="#/morning">🌅 Morning handover is due before 7:00am. Tap to send it.</a>`);
  return out.join("");
}
function homeView() {
  const s = settings.get();
  const open = downtime.all().filter(e => e.isOpen && !e.pendingDelete).sort((a, b) => b.createdAt - a.createdAt);
  const st = todayStats(), unsynced = unsyncedCount();
  const setup = !s.webhookUrl || !s.operatorName
    ? `<a class="banner" href="#/settings">⚙️ First time? Set your name and the Google Sheet link in Settings.</a>` : "";
  const timers = open.length
    ? `<h2>🔴 Running now</h2>` + open.map(e => `
        <div class="card row"><div class="grow"><b>${esc(e.line)} · ${esc(e.station)}</b>${e.part ? `<div>${esc(e.part)}</div>` : ""}
        <div class="muted">${esc(e.reason)}</div><div class="timer" data-since="${e.startTime || e.createdAt}">00:00 elapsed</div></div>
        <button class="btn" data-act="stop" data-id="${e.id}">Stop</button></div>`).join("")
    : `<p class="muted center pad">No downtime running right now.</p>`;
  const body = `
    ${setup}${dueBanners()}
    <div class="row gap top"><a class="btn red xl grow" href="#/log"><span class="big-i">🚨</span>Log Issue / Downtime</a>
      <a class="btn xl grow" href="#/speed"><span class="big-i">⏱️</span>Line Speed</a></div>
    <div class="card"><div class="muted">📊 Today's Downtime</div><div class="stat">${st.total} min</div>
      ${st.top && st.top[1] > 0 ? `<hr><div class="muted small">Top Issue Today</div><b>${esc(st.top[0])} — ${st.top[1]} min</b>` : ""}</div>
    <a class="btn out" href="#/sleeve"><span class="big-i s">📦</span>Poke Bowl Sleeve Calculator</a>
    <a class="btn out" href="#/notes"><span class="big-i s">📝</span>Notes</a>
    ${unsynced ? `<button class="pill" data-act="sync">${unsynced} entr${unsynced === 1 ? "y" : "ies"} waiting to sync — tap to retry</button>` : ""}
    ${timers}`;
  const footer = `<footer class="dock"><a class="btn out" href="#/breaks"><span class="big-i s">☕</span>Breaks</a><a class="btn out" href="#/handover"><span class="big-i s">🔄</span>Handover</a></footer>`;
  return {
    live: true,
    html: page("📡 Unit K Pulse", `<div class="with-dock">${body}</div>`, { back: "", footer,
      actions: `<a class="icon" href="#/summary" aria-label="Summary">📊</a><a class="icon" href="#/history" aria-label="History">🕘</a><a class="icon" href="#/settings" aria-label="Settings">⚙️</a>` }),
  };
}

// ---------- log / edit entry ----------
function newLogState() {
  return { mode: "new", id: null, line: C.LINES[0], category: null, equipment: null, fault: null, customFault: "", note: "", minutes: "", people: "", photo: null, photoUrl: null, hadPhoto: false, error: "" };
}
function logFromEntry(e) {
  const needsEq = C.hasEquipmentStep(e.station);
  const opts = C.faultsFor(e.station, needsEq ? e.part : null);
  const known = opts.includes(e.reason) && e.reason !== "Other";
  return { mode: "edit", id: e.id, line: e.line, category: e.station, equipment: needsEq ? e.part : null, fault: known ? e.reason : "Other", customFault: known ? "" : e.reason,
    note: e.note || "", minutes: e.durationMinutes ?? "", people: e.peopleAffected ?? "", photo: null, photoUrl: e.photoUrl || null, hadPhoto: !!(e.hasPhoto || e.photoUrl), error: "" };
}
const effFault = L => (L.fault === "Other" ? (L.customFault.trim() || null) : L.fault);

function logView(editId) {
  if (editId != null) {
    const e = downtime.get(editId);
    if (!e) return { html: page("Edit entry", `<p class="pad">That entry no longer exists.</p>`, { back: "#/history" }) };
    if (!S.log || S.log.mode !== "edit" || S.log.id !== editId) S.log = logFromEntry(e);
  } else if (!S.log || S.log.mode !== "new") S.log = newLogState();
  const L = S.log, needsEq = L.category && C.hasEquipmentStep(L.category);
  const showFaults = L.category && (!needsEq || L.equipment);
  const faults = !showFaults ? [] : C.faultsFor(L.category, needsEq ? L.equipment : null);
  const photoBlock = `
    <div class="row gap center-v"><button class="btn out sm" data-act="pickPhoto">📷 ${L.photo || L.hadPhoto ? "Retake photo" : "Add photo"}</button>
    ${L.photo ? `<img class="thumb" src="${URL.createObjectURL(L.photo)}" alt="photo"><button class="link" data-act="removePhoto">Remove</button>` : L.photoUrl ? `<a class="link" href="${esc(L.photoUrl)}" target="_blank" rel="noopener">View current photo</a>` : L.hadPhoto ? `<span class="muted small">Photo waiting to upload</span>` : ""}</div>
    <input type="file" id="photoInput" accept="image/*" hidden>`;
  const body = `
    ${label("Collation")}<div class="chips scroll">${C.LINES.map(l => chip("log", "line", l, L.line === l)).join("")}</div>
    ${label("Tray pack")}<div class="chips scroll">${C.LOMA_MDS.map(l => chip("log", "line", l, L.line === l)).join("")}</div>
    ${label("What caused the stop?")}<div class="chips">${C.CATEGORIES.map(c => chip("log", "category", c, L.category === c, C.CATEGORY_ICONS[c], true)).join("")}</div>
    ${needsEq ? label("Equipment") + `<div class="chips">${C.equipmentFor(L.category).map(q => chip("log", "equipment", q, L.equipment === q, C.EQUIPMENT_ICONS[q], true)).join("")}</div>` : ""}
    ${showFaults ? label("Specifically") + `<div class="chips">${faults.map(f => chip("log", "fault", f, L.fault === f, "", true)).join("")}</div>` : ""}
    ${L.fault === "Other" ? `<input class="field" data-s="log" data-f="customFault" placeholder="Describe it" value="${esc(L.customFault)}">` : ""}
    <textarea class="field" rows="2" data-s="log" data-f="note" placeholder="Note (optional)">${esc(L.note)}</textarea>
    <input class="field" inputmode="numeric" data-num data-s="log" data-f="people" placeholder="People affected by downtime (optional)" value="${esc(L.people)}">
    ${photoBlock}
    ${L.error ? `<p class="err">${esc(L.error)}</p>` : ""}
    ${L.mode === "new" ? `
      <p class="muted pad-t">If the line is stopped right now, start a timer.</p>
      <button class="btn xl" data-act="startTimer" ${effFault(L) ? "" : "disabled"}>Start Downtime Timer</button>
      <hr><p class="muted">Or log it directly (already resolved / no timer needed):</p>
      <input class="field" inputmode="numeric" data-num data-s="log" data-f="minutes" placeholder="Minutes lost (optional)" value="${esc(L.minutes)}">
      <button class="btn out lg" data-act="saveNoTimer" ${effFault(L) ? "" : "disabled"}>Save Entry (no timer)</button>`
    : `<input class="field" inputmode="numeric" data-num data-s="log" data-f="minutes" placeholder="Minutes lost (optional)" value="${esc(L.minutes)}">
       <button class="btn xl" data-act="saveEdit" ${effFault(L) ? "" : "disabled"}>Save changes</button>`}`;
  return { html: page(L.mode === "edit" ? "✏️ Edit entry" : "📝 Log entry", body, { back: L.mode === "edit" ? "#/history" : "#/" }) };
}

async function commitPhoto(id, L) {
  if (!L.photo) return false;
  await putPhoto(id, L.photo);
  return true;
}

// ---------- history ----------
function historyView() {
  const list = downtime.all().filter(e => !e.pendingDelete).sort((a, b) => b.createdAt - a.createdAt);
  const unsynced = unsyncedCount();
  const body = list.length ? list.map(e => `
    <div class="card"><div class="row"><b class="grow">${esc(e.line)} · ${esc(e.station)}</b>${syncIcon(e)}</div>
      ${e.part ? `<div>${esc(e.part)}</div>` : ""}<div class="muted">${esc(e.reason)}</div>${e.note ? `<div>“${esc(e.note)}”</div>` : ""}
      <div class="small">${fmtWhen(e.createdAt)}${e.durationMinutes != null ? ` · <b>${e.durationMinutes} min</b>` : ""}${e.peopleAffected != null ? ` · ${e.peopleAffected} ppl affected` : ""}${e.isOpen ? ' · <span class="ok">running</span>' : ""}</div>
      ${e.photoUrl ? `<a class="link" href="${esc(e.photoUrl)}" target="_blank" rel="noopener">📷 View photo</a>` : e.hasPhoto ? `<div class="small muted">📷 Photo waiting to upload</div>` : ""}
      ${e.lastSyncError ? `<div class="err small">Sync issue: ${esc(e.lastSyncError)}</div>` : ""}
      ${e.isOpen ? "" : `<div class="row gap pad-t"><a class="link" href="#/edit/${e.id}">✏️ Edit</a><button class="link danger" data-act="delEntry" data-id="${e.id}">🗑️ Delete</button></div>`}
    </div>`).join("") : `<p class="muted center pad">No entries yet.</p>`;
  return { live: true, html: page("History", body, { actions: unsynced ? `<button class="icon" data-act="sync" aria-label="Sync now">🔄</button>` : "" }) };
}

// ---------- breaks ----------
function breaksView() {
  const B = S.br, today = todayStr();
  const list = breaks.all().filter(b => !b.pendingDelete).sort((a, b) => b.createdAt - a.createdAt);
  const body = `
    ${label("Team")}<div class="chips">${C.TEAMS.map(t => chip("br", "team", t, B.team === t)).join("")}</div>
    ${label("Break")}<div class="chips">${C.BREAK_TYPES.map(t => `<button type="button" class="chip${B.breakType === t ? " sel" : ""}" data-act="set" data-s="br" data-f="breakType" data-v="${t}">${t} break</button>`).join("")}</div>
    <div class="row gap center-v pad-t"><div class="grow"><div class="small muted">Start</div><input class="field" type="time" data-s="br" data-f="start" value="${esc(B.start)}"></div><button class="link" data-act="now" data-f="start">Now</button></div>
    <div class="row gap center-v"><div class="grow"><div class="small muted">End</div><input class="field" type="time" data-s="br" data-f="end" value="${esc(B.end)}"></div><button class="link" data-act="now" data-f="end">Now</button></div>
    <button class="btn xl" data-act="saveBreak" ${B.start || B.end ? "" : "disabled"}>Save break</button>
    <button class="btn out lg" data-act="shareBreaks" ${list.some(b => b.date === today) ? "" : "disabled"}>💬 Send today's breaks on WhatsApp</button>
    <hr><h2>Logged breaks</h2>
    ${list.length ? list.map(b => `<div class="card row"><div class="grow"><b>${esc(b.team)} · ${esc(b.breakType)} break</b>
      <div class="small">${esc(b.startTime || "-")} to ${esc(b.endTime || "-")} · ${esc(b.date)}</div></div>
      ${syncIcon(b)}<button class="icon" data-act="shareBreak" data-id="${b.id}" aria-label="Share">💬</button>
      <button class="icon danger" data-act="delBreak" data-id="${b.id}" aria-label="Delete">🗑️</button></div>`).join("") : `<p class="muted">None yet.</p>`}`;
  return { html: page("☕ Breaks", body) };
}

// ---------- handover ----------
function handoverMenuView() {
  const body = `
    <p class="muted">Sent to the shared Sheet, same as everything else.</p>
    <a class="btn xl2" href="#/morning"><span class="big-i">🌅</span><span><b>Start of Shift</b><br><small>Morning Update</small></span></a>
    <a class="btn xl2" href="#/evening"><span class="big-i">🌇</span><span><b>End of Shift</b><br><small>End of Shift Update</small></span></a>`;
  return { html: page("🔄 Handover", body, { actions: `<a class="icon" href="#/handovers" aria-label="History">🕘</a>` }) };
}

const hoStr = v => (v === null || v === undefined ? "" : String(v));
function hoFromEntry(e) {
  const o = { ...e };
  for (const f of HO_NUMS) o[f] = hoStr(e[f]);
  return o;
}
const yesNo = (f, text, df, dl = "Add detail") => {
  const H = S.ho, on = !!H[f];
  return `<div class="yn"><div class="row center-v"><b class="grow">${text}</b>
    <button type="button" class="chip${!on ? " sel" : ""}" data-act="yn" data-f="${f}" data-v="0">No</button>
    <button type="button" class="chip${on ? " sel" : ""}" data-act="yn" data-f="${f}" data-v="1">Yes</button></div>
    ${on ? `<textarea class="field" rows="2" data-s="ho" data-f="${df}" placeholder="${dl}">${esc(H[df])}</textarea>` : ""}</div>`;
};
const num = (f, text) => `<input class="field" inputmode="numeric" data-num data-s="ho" data-f="${f}" placeholder="${text}" aria-label="${text}" value="${esc(S.ho[f])}">`;
const tm = (f, text) => `<div><div class="small muted">${text}</div><input class="field" type="time" data-s="ho" data-f="${f}" value="${esc(S.ho[f])}"></div>`;
const teamBlock = l => `<div class="card"><b>Team ${l}</b><div class="grid2 pad-t">${num(`team${l}Planned`, "Planned ppl")}${num(`team${l}Actual`, "Actual ppl")}${tm(`team${l}FinishPlanned`, "Finish (planned)")}${tm(`team${l}FinishActual`, "Finish (actual)")}</div></div>`;

function handoverFormView(type, editId) {
  const existing = editId != null ? handovers.get(editId) : null;
  if (editId != null && !existing) return { html: page("Edit", `<p class="pad">That handover no longer exists.</p>`, { back: "#/handovers" }) };
  const key = type + ":" + (editId ?? "new");
  if (!S.ho || S.hoKey !== key) {
    S.hoKey = key;
    if (existing) S.ho = hoFromEntry(existing);
    else {
      S.ho = blankHandover(type);
      for (const f of HO_NUMS) S.ho[f] = "";
      const t = type === "MORNING" ? morningDowntimeText() : eveningDowntimeText();
      if (t) { S.ho.downtimeIssue = true; S.ho.downtimeDetail = t; }
    }
  }
  const H = S.ho, morning = type === "MORNING";
  const send = existing ? "Save Changes" : morning ? "Send Morning Update" : "Send End of Shift Update";
  const refresh = `<button class="link" data-act="refreshDowntime">↻ Refresh downtime from log</button>`;
  const body = morning ? `
    <p class="muted">${esc(existing?.date || todayStr())}</p>
    <h2>In the last 24hrs</h2>
    ${yesNo("sheIncident", "SHE incidents and accidents", "sheIncidentDetail")}${yesNo("technicalIssue", "Technical", "technicalDetail")}
    ${yesNo("qualityIssue", "Quality", "qualityDetail")}${yesNo("downtimeIssue", "Downtime", "downtimeDetail")}${refresh}
    <h2>Volumes</h2><div class="grid2">${num("todaysVolume", "Today's volume")}${num("tomorrowsVolume", "Tomorrow's volume")}</div>
    <h2>Teams</h2>${teamBlock("A")}${teamBlock("B")}${teamBlock("C")}
    <div class="grid2 pad-t">${num("packingPpl", "Packing ppl")}${num("packingTrainees", "Of which on training")}</div>
    <h2>People</h2>
    ${yesNo("absence", "Absence", "absenceNames", "Names")}${yesNo("rtw", "RTW (return to work)", "rtwNames", "Names")}${yesNo("notOnRota", "Not on rota", "notOnRotaNames", "Names")}
    <h2>Risk to Service</h2>${yesNo("riskToService", "Risk to service", "riskToServiceDetail", "What")}
    <h2>Agency</h2><div class="grid2">${num("agencyCollation", "Collation")}${num("agencyPrep", "Prep")}${num("agencyHygiene", "Hygiene")}${num("agencyFridge", "Fridge")}</div>
    <div class="pad-t">${num("agencyPackagingStore", "Packaging store")}</div>`
  : `
    <p class="muted">${esc(existing?.date || todayStr())}</p>
    <h2>Summary</h2>
    ${yesNo("accidentIncident", "Accident / incident", "accidentIncidentDetail")}${yesNo("qualityIssue", "Quality", "qualityDetail")}${yesNo("downtimeIssue", "Downtime", "downtimeDetail")}${refresh}
    <div class="grid2 pad-t">${num("serviceScore", "Service score %")}${num("efficiency", "Efficiency %")}</div>
    <h2>Finish times</h2><div class="stack">${tm("teamAFinishedTime", "Team A finished")}${tm("teamBFinishedTime", "Team B finished")}${tm("teamCFinishedTime", "Team C finished")}${tm("collationFinished", "Collation finished")}${tm("trayUpSentBy", "Tray up sent by")}</div>
    <h2>Wrap-up</h2>${yesNo("shorts", "Shorts", "shortsDetail")}${yesNo("reportUpdated", "Report updated", "reportUpdatedDetail")}${yesNo("aob", "AOB", "aobDetail")}`;
  const title = (morning ? "🌅 " : "🌇 ") + (existing ? (morning ? "Edit Morning Update" : "Edit End of Shift Update") : (morning ? "Morning Update" : "End of Shift Update"));
  return { html: page(title, body + `
    <button class="btn xl" data-act="saveHandover" data-share="0">${send}</button>
    <button class="btn out lg" data-act="saveHandover" data-share="1">💬 Save &amp; share to WhatsApp</button>`, { back: existing ? "#/handovers" : "#/handover" }) };
}

function handoverHistoryView() {
  const list = handovers.all().filter(h => !h.pendingDelete).sort((a, b) => b.createdAt - a.createdAt);
  const body = list.length ? list.map(h => `
    <div class="card"><div class="row"><b class="grow">${h.type === "MORNING" ? "🌅 Start of Shift" : "🌇 End of Shift"}</b>${syncIcon(h)}</div>
      <div class="muted">${esc(h.date)}</div>${h.operatorName ? `<div>${esc(h.operatorName)}</div>` : ""}
      ${h.lastSyncError ? `<div class="err small">Sync issue: ${esc(h.lastSyncError)}</div>` : ""}
      <div class="row gap pad-t"><a class="link" href="#/${h.type === "MORNING" ? "morning" : "evening"}/${h.id}">✏️ Edit</a>
        <button class="link" data-act="shareHandover" data-id="${h.id}">💬 Share</button>
        <button class="link danger" data-act="delHandover" data-id="${h.id}">🗑️ Delete</button></div></div>`).join("")
    : `<p class="muted center pad">No handovers sent yet.</p>`;
  return { live: true, html: page("Handover History", body, { back: "#/handover" }) };
}

// ---------- weekly / monthly summary ----------
function summaryView() {
  const days = S.sumDays, since = Date.now() - days * 864e5;
  const r = summarise(downtime.all(), since), title = days === 7 ? "Last 7 days" : "Last 30 days";
  const body = `
    <div class="chips"><button class="chip${days === 7 ? " sel" : ""}" data-act="days" data-v="7">7 days</button><button class="chip${days === 30 ? " sel" : ""}" data-act="days" data-v="30">30 days</button></div>
    <div class="card"><div class="muted">Total</div><div class="stat">${r.totalMinutes} min</div><div class="small">${r.stops} stops · ${r.peopleAffected} people affected</div></div>
    <h2>By category</h2>${r.byCategory.length ? r.byCategory.map(g => `<div class="card row"><b class="grow">${esc(g.label)}</b><span>${g.minutes} min · ${g.people} ppl · ${g.stops}×</span></div>`).join("") : `<p class="muted">Nothing logged.</p>`}
    <h2>Top causes</h2>${r.topCauses.slice(0, 8).map((g, i) => `<div class="card row"><span class="grow">${i + 1}. ${esc(g.label)}</span><b>${g.minutes} min</b><span class="muted">(${g.stops}×)</span></div>`).join("")}
    <button class="btn xl" data-act="shareSummary" ${r.stops ? "" : "disabled"}>💬 Send summary on WhatsApp</button>`;
  return { html: page("📊 Summary", body) };
}

// ---------- settings ----------
function settingsView() {
  const s = settings.get();
  const body = `
    <div class="lbl">Your name</div><input class="field" id="set-name" placeholder="e.g. Maria" value="${esc(s.operatorName)}">
    <div class="lbl">Google Sheet link (Apps Script Web App URL)</div>
    <input class="field" id="set-url" placeholder="https://script.google.com/macros/s/.../exec" value="${esc(s.webhookUrl)}" autocapitalize="off" autocorrect="off">
    <p class="muted small">Set up once by a supervisor. Every phone on the team uses the same link.</p>
    <div class="row center-v pad-t"><b class="grow">Reminders</b><button class="chip${s.remindersOn ? " sel" : ""}" data-act="remToggle">${s.remindersOn ? "On" : "Off"}</button></div>
    <p class="muted small">The app shows a banner on the home screen when break times (before 6:30am) or the morning handover (before 7am) are still due.</p>
    <div class="grid2"><div><div class="small muted">Break times reminder</div><input class="field" type="time" id="set-breaks" value="${esc(s.breaksTime)}"></div>
    <div><div class="small muted">Morning handover reminder</div><input class="field" type="time" id="set-handover" value="${esc(s.handoverTime)}"></div></div>
    <button class="btn out lg" data-act="downloadIcs">📅 Add daily alarms to my Calendar</button>
    <p class="muted small">iPhones can't schedule notifications from a web app. This downloads two repeating calendar alerts at the times above. Open the file and choose Add All.</p>
    <button class="btn xl" data-act="saveSettings">Save</button>
    <button class="btn out lg" data-act="testSheet">Test Sheet connection</button>
    ${S.msg ? `<p class="${S.msgErr ? "err" : "ok"}">${esc(S.msg)}</p>` : ""}
    <hr><h2>Install on iPhone</h2>
    <p class="muted small">Open this page in <b>Safari</b>, tap the Share button, then <b>Add to Home Screen</b>. It then opens full screen like an app.</p>`;
  return { html: page("Settings", body) };
}

// ---------- line speed & sleeve calculator ----------
let speedTimer = null;
function speedView() {
  const P = S.speed;
  const body = `
    <p class="muted">Tap the button once for every pack that comes off the line. We'll count for 30 seconds and work out packs per minute.</p>
    <div class="card center"><div class="ok b">${P.state === "running" ? P.left + "s left" : P.state === "done" ? "Done" : "Ready"}</div><div class="stat huge">${P.count}</div><div class="muted">packs counted</div></div>
    ${P.state === "running" ? `<button class="btn tap" data-act="tap">TAP — Pack</button>`
      : P.state === "done" ? `<div class="card"><div class="muted">📊 Result</div><b>${P.count} packs / 30s</b><br><b class="ok">≈ ${P.count * 2} packs / min</b></div><button class="btn xl" data-act="speedStart">Run again</button>`
      : `<button class="btn xl" data-act="speedStart">Start 30s Count</button>`}
    ${P.state !== "ready" ? `<button class="btn out lg" data-act="speedReset">Reset</button>` : ""}`;
  return { html: page("⏱️ Line Speed Check", body, { back: "#/" }), mount: () => {} };
}
export function speedStart() {
  clearInterval(speedTimer);
  Object.assign(S.speed, { left: 30, count: 0, state: "running" });
  speedTimer = setInterval(() => {
    S.speed.left -= 1;
    if (S.speed.left <= 0) { clearInterval(speedTimer); S.speed.state = "done"; }
    nav();
  }, 1000);
  nav();
}
export function speedReset() { clearInterval(speedTimer); Object.assign(S.speed, { left: 30, count: 0, state: "ready" }); nav(); }

function sleeveView() {
  const PS = 50, SB = 6, amt = parseInt(S.sleeve, 10);
  let res = "";
  if (amt > 0) {
    const full = Math.floor(amt / PS), rem = amt % PS, need = Math.ceil(amt / PS), boxes = Math.floor(need / SB), left = need % SB, pl = n => (n === 1 ? "" : "s");
    res = `<div class="card center"><div class="muted">Sleeves to pull</div><div class="stat huge">${need}</div>
      <b class="${rem ? "warn" : "ok"}">${rem ? `${full} full sleeve${pl(full)} + ${rem} pack${pl(rem)} from the next sleeve` : `${full} full sleeve${pl(full)} — exact`}</b><hr>
      <div class="muted">That's in boxes</div><div class="stat">${left ? `${boxes} box${boxes === 1 ? "" : "es"} + ${left} sleeve${pl(left)}` : `${boxes} box${boxes === 1 ? "" : "es"}`}</div>
      <div class="small muted">${SB} sleeves per box</div></div>`;
  }
  return { html: page("📦 Poke Bowl Sleeves", `<p class="muted">Enter the order amount — we'll work out how many sleeves (${PS} bases/sleeve) and boxes (${PS * SB} bases/box = ${SB} sleeves) to pull.</p>
    <input class="field big-in" inputmode="numeric" data-num data-s="sleeve" data-f="" data-live placeholder="Order amount (poke bowl bases)" value="${esc(S.sleeve)}">${res}`) };
}

// ---------- notes ----------
function notesView(folder) {
  const N = S.notes;
  if (!folder) {
    return { html: page("📝 Notes", `<div class="stack">${C.NOTE_FOLDERS.map(f => `<a class="btn out lg" href="#/notes/${encodeURIComponent(f)}"><span class="big-i s">${C.NOTE_FOLDER_ICONS[f] || "📁"}</span>${esc(f)}</a>`).join("")}</div>`) };
  }
  const mount = async () => {
    if (N.folder === folder && (N.loading || (N.loaded && Date.now() - N.loadedAt < 30000))) return;
    Object.assign(N, { folder, files: [], loading: true, loaded: false, error: "" });
    nav();
    try { const j = await getJson({ action: "listFiles", folder }); N.files = j.files || []; }
    catch (e) { N.error = e.message; }
    N.loading = false; N.loaded = true; N.loadedAt = Date.now(); nav();
  };
  const body = `
    <button class="btn xl" data-act="pickNote">⬆️ Upload a file</button>
    <input type="file" id="noteInput" accept=".doc,.docx,.odt,.pdf,application/pdf" hidden>
    ${N.error ? `<p class="err">${esc(N.error)}</p>` : ""}
    ${N.loading ? `<p class="muted center pad">Loading…</p>` : N.files.length ? N.files.map(f => `<a class="card row" href="${esc(f.url)}" target="_blank" rel="noopener"><span class="grow"><b>${esc(f.name)}</b><div class="small muted">${esc((f.updatedAt || "").slice(0, 10))}</div></span>↗</a>`).join("") : (N.loaded && !N.error ? `<p class="muted center pad">No files yet.</p>` : "")}`;
  return { html: page(`${C.NOTE_FOLDER_ICONS[folder] || "📁"} ${esc(folder)}`, body, { back: "#/notes" }), mount };
}

// ---------- actions (button clicks) ----------
const val = id => document.getElementById(id)?.value ?? "";
async function operator() { return settings.get().operatorName; }

export const actions = {
  set(el) {
    const { s, f, v } = el.dataset, o = s === "log" ? S.log : S.br;
    o[f] = v;
    if (s === "log") {
      if (f === "category") { S.log.equipment = null; S.log.fault = null; S.log.customFault = ""; }
      if (f === "equipment") { S.log.fault = null; S.log.customFault = ""; }
    }
    nav();
  },
  stop(el) { stopTimer(Number(el.dataset.id)); nav(); },
  sync() { syncNow(); },
  pickPhoto() { document.getElementById("photoInput")?.click(); },
  removePhoto() { S.log.photo = null; nav(); },
  async startTimer() { await saveLog(true); },
  async saveNoTimer() { await saveLog(false); },
  async saveEdit() {
    const L = S.log, e = downtime.get(L.id);
    if (!e || !effFault(L)) return;
    const hasNew = await commitPhoto(e.id, L);
    saveDowntime({ ...e, line: L.line, station: L.category, part: C.hasEquipmentStep(L.category) ? L.equipment : null, reason: effFault(L),
      note: L.note, durationMinutes: intOrNull(L.minutes), peopleAffected: intOrNull(L.people), synced: false,
      ...(hasNew ? { hasPhoto: true, photoUrl: null } : {}) });
    S.log = null; go("#/history");
  },
  delEntry(el) { if (confirm("Delete this entry? This also removes it from the Google Sheet.")) { deleteDowntime(Number(el.dataset.id)); nav(); } },
  now(el) { S.br[el.dataset.f] = fmtHM(new Date()); nav(); },
  async saveBreak() {
    const B = S.br;
    breaks.put({ id: newId(), createdAt: Date.now(), date: todayStr(), operatorName: await operator(), team: B.team, breakType: B.breakType,
      startTime: B.start, endTime: B.end, synced: false, sheetRowCreated: false, pendingDelete: false });
    B.start = ""; B.end = ""; syncNow(); nav();
  },
  shareBreaks() { shareToWhatsApp(breaksText(todayStr(), breaks.all())); },
  shareBreak(el) { const b = breaks.get(Number(el.dataset.id)); if (b) shareToWhatsApp(breakText(b)); },
  delBreak(el) { deleteBreak(Number(el.dataset.id)); nav(); },
  yn(el) { S.ho[el.dataset.f] = el.dataset.v === "1"; nav(); },
  refreshDowntime() {
    const t = S.ho.type === "MORNING" ? morningDowntimeText() : eveningDowntimeText();
    if (t) { S.ho.downtimeIssue = true; S.ho.downtimeDetail = t; } nav();
  },
  async saveHandover(el) {
    const H = S.ho, existing = handovers.get(H.id);
    const e = { ...H, id: existing?.id ?? newId(), date: existing?.date ?? todayStr(), createdAt: existing?.createdAt ?? Date.now(),
      operatorName: existing?.operatorName ?? await operator(), synced: false, sheetRowCreated: existing?.sheetRowCreated ?? false, pendingDelete: false };
    for (const f of HO_NUMS) e[f] = intOrNull(H[f]);
    handovers.put(e); syncNow();
    if (el.dataset.share === "1") shareToWhatsApp(handoverText(e));
    S.ho = null; go("#/handovers");
  },
  shareHandover(el) { const h = handovers.get(Number(el.dataset.id)); if (h) shareToWhatsApp(handoverText(h)); },
  delHandover(el) { if (confirm("Delete this handover? This also removes it from the Google Sheet.")) { deleteHandover(Number(el.dataset.id)); nav(); } },
  days(el) { S.sumDays = Number(el.dataset.v); nav(); },
  shareSummary() { shareToWhatsApp(reportText(S.sumDays === 7 ? "Weekly downtime summary (last 7 days)" : "Downtime summary (last 30 days)", summarise(downtime.all(), Date.now() - S.sumDays * 864e5))); },
  remToggle() { settings.set({ remindersOn: !settings.get().remindersOn }); nav(); },
  saveSettings() {
    settings.set({ operatorName: val("set-name").trim(), webhookUrl: val("set-url").trim(), breaksTime: val("set-breaks") || "06:15", handoverTime: val("set-handover") || "06:45" });
    S.msg = "Saved."; S.msgErr = false; syncNow(); nav();
  },
  async testSheet() {
    settings.set({ webhookUrl: val("set-url").trim() });
    S.msg = "Testing…"; S.msgErr = false; nav();
    try { const j = await getJson({}); S.msg = "Connected: " + (j.message || "ok"); S.msgErr = false; }
    catch (e) { S.msg = e.message; S.msgErr = true; }
    nav();
  },
  downloadIcs() {
    settings.set({ breaksTime: val("set-breaks") || "06:15", handoverTime: val("set-handover") || "06:45" });
    const url = URL.createObjectURL(new Blob([remindersIcs()], { type: "text/calendar" }));
    const a = Object.assign(document.createElement("a"), { href: url, download: "unit-k-reminders.ics" });
    document.body.appendChild(a); a.click(); a.remove();
  },
  tap() { S.speed.count += 1; nav(); },
  speedStart, speedReset,
  pickNote() { document.getElementById("noteInput")?.click(); },
};

async function saveLog(timer) {
  const L = S.log, reason = effFault(L);
  if (!L.category || !reason) return;
  const now = Date.now(), id = newId();
  const hasPhoto = await commitPhoto(id, L);
  saveDowntime({ id, createdAt: now, operatorName: await operator(), line: L.line, station: L.category,
    part: C.hasEquipmentStep(L.category) ? L.equipment : null, subIssue: null, reason, note: L.note,
    startTime: timer ? now : null, endTime: null, durationMinutes: timer ? null : intOrNull(L.minutes),
    peopleAffected: intOrNull(L.people), hasPhoto, photoUrl: null, isOpen: timer,
    synced: false, sheetRowCreated: false, pendingDelete: false });
  S.log = null; go("#/");
}

// file inputs (handled via a delegated "change" listener in main.js)
export async function onFileChosen(input) {
  const f = input.files?.[0]; if (!f) return;
  if (input.id === "photoInput") {
    try { S.log.photo = await compressImage(f); S.log.error = ""; } catch (e) { S.log.error = e.message; }
    nav();
  } else if (input.id === "noteInput") {
    const N = S.notes; N.error = ""; N.loading = true; nav();
    try { await uploadNote(N.folder, f); const j = await getJson({ action: "listFiles", folder: N.folder }); N.files = j.files || []; }
    catch (e) { N.error = e.message; }
    N.loading = false; nav();
  }
  input.value = "";
}

// ---------- routing table ----------
export const routes = [
  [/^\/?$/, () => homeView()],
  [/^\/log$/, () => logView(null)],
  [/^\/edit\/(\d+)$/, m => logView(Number(m[1]))],
  [/^\/history$/, () => historyView()],
  [/^\/breaks$/, () => breaksView()],
  [/^\/handover$/, () => handoverMenuView()],
  [/^\/handovers$/, () => handoverHistoryView()],
  [/^\/morning(?:\/(\d+))?$/, m => handoverFormView("MORNING", m[1] ? Number(m[1]) : null)],
  [/^\/evening(?:\/(\d+))?$/, m => handoverFormView("EVENING", m[1] ? Number(m[1]) : null)],
  [/^\/summary$/, () => summaryView()],
  [/^\/settings$/, () => settingsView()],
  [/^\/speed$/, () => speedView()],
  [/^\/sleeve$/, () => sleeveView()],
  [/^\/notes$/, () => notesView(null)],
  [/^\/notes\/(.+)$/, m => notesView(decodeURIComponent(m[1]))],
];
