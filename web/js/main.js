import { routes, actions, onFileChosen, setNav, S } from "./views.js";
import { syncNow, unsyncedCount } from "./core.js";

const app = document.getElementById("app");
let current = null;

function path() { return (location.hash.replace(/^#/, "") || "/"); }

function render() {
  const p = path();
  let view = null;
  for (const [re, fn] of routes) { const m = p.match(re); if (m) { view = fn(m); break; } }
  if (!view) view = { html: `<main class="content"><p class="pad">Page not found. <a href="#/">Home</a></p></main>` };
  current = view;
  const y = window.scrollY;
  app.innerHTML = view.html;
  window.scrollTo(0, y);
  tick();
  if (view.mount) view.mount();
}
setNav(render);

// leaving a form discards its draft
function onRoute() {
  const p = path();
  if (!/^\/(log|edit\/)/.test(p)) S.log = null;
  if (!/^\/(morning|evening)/.test(p)) { S.ho = null; S.hoKey = null; }
  S.msg = "";
  window.scrollTo(0, 0);
  render();
}
window.addEventListener("hashchange", onRoute);

// ---- delegated events ----
document.addEventListener("click", e => {
  const el = e.target.closest("[data-act]");
  if (!el || el.disabled) return;
  const fn = actions[el.dataset.act];
  if (fn) { e.preventDefault(); fn(el, e); }
});

const scopes = () => ({ log: S.log, br: S.br, ho: S.ho });
document.addEventListener("input", e => {
  const t = e.target;
  if (!(t instanceof HTMLInputElement || t instanceof HTMLTextAreaElement) || !t.dataset.s) return;
  if ("num" in t.dataset) { const d = t.value.replace(/\D/g, ""); if (d !== t.value) t.value = d; }
  if (t.dataset.s === "sleeve") {
    S.sleeve = t.value;
    const pos = t.selectionStart;
    render();
    const again = document.querySelector('[data-s="sleeve"]');
    if (again) { again.focus(); try { again.setSelectionRange(pos, pos); } catch {} }
    return;
  }
  const obj = scopes()[t.dataset.s];
  if (!obj) return;
  obj[t.dataset.f] = t.value;
  // Save buttons depend on a few fields; refresh their disabled state without re-rendering.
  if (t.dataset.s === "log" && t.dataset.f === "customFault") {
    const ok = !!obj.customFault.trim();
    document.querySelectorAll('[data-act="startTimer"],[data-act="saveNoTimer"],[data-act="saveEdit"]').forEach(b => (b.disabled = !ok));
  }
  if (t.dataset.s === "br") document.querySelector('[data-act="saveBreak"]')?.toggleAttribute("disabled", !(S.br.start || S.br.end));
});
document.addEventListener("change", e => {
  const t = e.target;
  if (t instanceof HTMLInputElement && t.type === "file") onFileChosen(t);
  else if (t instanceof HTMLInputElement && t.type === "time" && t.dataset.s === "br") document.querySelector('[data-act="saveBreak"]')?.toggleAttribute("disabled", !(S.br.start || S.br.end));
});

// ---- running-timer clock ----
function tick() {
  document.querySelectorAll("[data-since]").forEach(el => {
    const ms = Date.now() - Number(el.dataset.since);
    const m = Math.floor(ms / 60000), s = Math.floor(ms / 1000) % 60;
    el.textContent = `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")} elapsed`;
  });
}
setInterval(tick, 1000);

// ---- live refresh when sync state changes (never while typing) ----
window.addEventListener("ukp-change", () => {
  const a = document.activeElement;
  if (current?.live && !(a && /^(INPUT|TEXTAREA)$/.test(a.tagName))) render();
});
setInterval(() => { if (current?.live && path() === "/") render(); }, 60000); // keeps "due" banners and totals fresh

// ---- sync triggers ----
window.addEventListener("online", syncNow);
document.addEventListener("visibilitychange", () => { if (!document.hidden) { syncNow(); if (current?.live) render(); } });
setInterval(() => { if (unsyncedCount()) syncNow(); }, 60000);

// ---- service worker (offline shell + installable) ----
if ("serviceWorker" in navigator) navigator.serviceWorker.register("./sw.js").catch(() => {});
if (navigator.storage?.persist) navigator.storage.persist().catch(() => {});

render();
syncNow();
