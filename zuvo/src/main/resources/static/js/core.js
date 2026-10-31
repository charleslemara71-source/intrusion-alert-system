'use strict';
/* ===================== ZUVO core: helpers, API, icons, WebSocket ===================== */
const Z = { me: null, online: new Set(), ws: null, connected: false, listeners: [], view: 'home', contacts: [], reqCount: 0, unread: 0, openChat: null };
const $ = (s, el = document) => el.querySelector(s);
const $$ = (s, el = document) => [...el.querySelectorAll(s)];
const CATEGORIES = ['Nature', 'Music', 'Sports', 'Travel', 'Comedy', 'Food', 'Tech', 'Art', 'Fitness'];

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/* ---------- icons (inline SVG, work offline) ---------- */
const PATHS = {
  home: '<path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
  search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
  plus: '<path d="M5 12h14"/><path d="M12 5v14"/>',
  play: '<polygon points="6 3 20 12 6 21 6 3"/>',
  message: '<path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/>',
  user: '<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  phone: '<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>',
  video: '<path d="m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5"/><rect x="2" y="6" width="14" height="12" rx="2"/>',
  videooff: '<path d="M10.66 6H14a2 2 0 0 1 2 2v2.5l5.248-3.062A.5.5 0 0 1 22 7.87v8.196"/><path d="M16 16a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h2"/><path d="m2 2 20 20"/>',
  heart: '<path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/>',
  bell: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  x: '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
  clock: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
  camera: '<path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/>',
  send: '<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
  live: '<path d="M4.9 19.1C1 15.2 1 8.8 4.9 4.9"/><path d="M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5"/><circle cx="12" cy="12" r="2"/><path d="M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5"/><path d="M19.1 4.9C23 8.8 23 15.1 19.1 19"/>',
  image: '<rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>',
  quote: '<path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3c0 1 0 1 1 1z"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/>',
  back: '<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',
  eye: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/>',
  mic: '<path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/>',
  micoff: '<line x1="2" x2="22" y1="2" y2="22"/><path d="M18.89 13.23A7.12 7.12 0 0 0 19 12v-2"/><path d="M5 10v2a7 7 0 0 0 12 5"/><path d="M15 9.34V5a3 3 0 0 0-5.68-1.33"/><path d="M9 9v3a3 3 0 0 0 5.12 2.12"/><line x1="12" x2="12" y1="19" y2="22"/>',
  trash: '<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>',
  phoneoff: '<path d="M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07"/><path d="m2 2 20 20"/>',
  sparkle: '<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>',
  lock: '<rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>'
};
function ic(name, size = 20) {
  return `<svg class="ic" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${PATHS[name] || ''}</svg>`;
}
function logo(size = 34) {
  return `<svg class="logo" width="${size}" height="${size}" viewBox="0 0 120 120" aria-hidden="true"><defs><linearGradient id="lg${size}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#3B2FD9"/><stop offset="1" stop-color="#00A8C6"/></linearGradient></defs><rect width="120" height="120" rx="28" fill="url(#lg${size})"/><path d="M36 40 H80 L36 82 H84" fill="none" stroke="#fff" stroke-width="13" stroke-linecap="round" stroke-linejoin="round"/><circle cx="92" cy="34" r="9" fill="#22C55E" stroke="#fff" stroke-width="3.5"/></svg>`;
}

/* ---------- API ---------- */
async function api(path, opts = {}) {
  const o = { method: opts.method || 'GET', credentials: 'same-origin', headers: {} };
  if (opts.json !== undefined) { o.headers['Content-Type'] = 'application/json'; o.body = JSON.stringify(opts.json); }
  if (opts.form) o.body = opts.form;
  let r;
  try { r = await fetch('/api' + path, o); }
  catch (e) { throw new Error("Can't reach Zuvo. Check your internet connection."); }
  let d = null;
  try { d = await r.json(); } catch (e) { /* no body */ }
  if (!r.ok) {
    if (r.status === 401 && Z.me && !path.startsWith('/auth/')) { Z.me = null; showLanding(); }
    const err = new Error((d && d.error) || 'Something went wrong. Try again.');
    err.status = r.status;
    throw err;
  }
  return d;
}

/* ---------- small UI helpers ---------- */
function toast(msg, kind = 'info') {
  const el = document.createElement('div');
  el.className = 'toast ' + (kind === 'error' ? 'error' : kind === 'good' ? 'good' : '');
  el.textContent = msg;
  $('#toasts').appendChild(el);
  setTimeout(() => el.remove(), 4200);
}
function timeAgo(iso) {
  if (!iso) return '';
  const s = Math.max(1, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (s < 60) return 'just now';
  if (s < 3600) return Math.floor(s / 60) + ' min ago';
  if (s < 86400) return Math.floor(s / 3600) + ' h ago';
  if (s < 604800) return Math.floor(s / 86400) + ' d ago';
  return new Date(iso).toLocaleDateString();
}
function clock(iso) { return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }
function hue(str) { let h = 0; for (const c of String(str)) h = (h * 31 + c.charCodeAt(0)) % 360; return h; }
function initials(name) { return String(name || '?').trim().split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase(); }
function avatar(u, size = 40, dot = true) {
  const face = u.profilePicUrl
    ? `<img class="face" src="${esc(u.profilePicUrl)}" alt="" loading="lazy">`
    : `<span class="face" style="background:linear-gradient(135deg,hsl(${hue(u.fullName)} 62% 48%),hsl(${(hue(u.fullName) + 40) % 360} 62% 40%))">${esc(initials(u.fullName))}</span>`;
  return `<span class="av" style="--s:${size}px" data-uid="${u.id}">${face}${dot ? `<i class="dot ${Z.online.has(u.id) ? 'on' : ''}"></i>` : ''}</span>`;
}
function applyPresence() {
  $$('.av[data-uid]').forEach(el => {
    const d = el.querySelector('.dot');
    if (d) d.classList.toggle('on', Z.online.has(+el.dataset.uid));
  });
  $$('[data-status-for]').forEach(el => { el.textContent = Z.online.has(+el.dataset.statusFor) ? 'Online' : 'Offline'; });
}

/* ---------- bottom sheet / modal ---------- */
function openSheet(title, html, mount) {
  closeSheet();
  const back = document.createElement('div');
  back.className = 'back';
  back.id = 'sheet';
  back.innerHTML = `<div class="sheet" role="dialog" aria-modal="true" aria-label="${esc(title)}"><div class="sheet-head"><h3>${esc(title)}</h3><button class="iconbtn" data-close aria-label="Close">${ic('x')}</button></div><div class="sheet-body">${html}</div></div>`;
  back.addEventListener('mousedown', e => { if (e.target === back) closeSheet(); });
  back.addEventListener('click', e => { if (e.target.closest('[data-close]')) closeSheet(); });
  $('#modal-root').appendChild(back);
  if (mount) mount($('.sheet-body', back), back);
  return back;
}
function closeSheet() { const s = $('#sheet'); if (s) s.remove(); }
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeSheet(); });

/* ---------- network indicator + WebSocket (the green "online" signal) ---------- */
function onWS(fn) { Z.listeners.push(fn); }
function netPill() { return `<span class="net ${Z.connected ? '' : 'off'}" id="net"><i></i><span>${Z.connected ? 'Online' : 'Offline'}</span></span>`; }
function setNet(on) {
  Z.connected = !!on && navigator.onLine !== false;
  const el = $('#net');
  if (el) { el.classList.toggle('off', !Z.connected); $('span', el).textContent = Z.connected ? 'Online' : 'Offline'; }
  Z.listeners.forEach(fn => fn({ type: 'net', online: Z.connected }));
}
function connectWS() {
  if (!Z.me) return;
  if (Z.ws && (Z.ws.readyState === 0 || Z.ws.readyState === 1)) return;
  const ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
  Z.ws = ws;
  ws.onopen = () => {
    setNet(true);
    clearInterval(Z.pingT);
    Z.pingT = setInterval(() => { if (ws.readyState === 1) ws.send('{"type":"ping"}'); }, 20000); // heartbeat
  };
  ws.onmessage = e => {
    let m; try { m = JSON.parse(e.data); } catch (err) { return; }
    if (m.type === 'hello') { Z.online = new Set(m.online); applyPresence(); }
    else if (m.type === 'presence') { m.online ? Z.online.add(m.userId) : Z.online.delete(m.userId); applyPresence(); }
    Z.listeners.forEach(fn => { try { fn(m); } catch (err) { console.error(err); } });
  };
  ws.onclose = () => {
    setNet(false);
    clearInterval(Z.pingT);
    if (Z.me) { clearTimeout(Z.retry); Z.retry = setTimeout(connectWS, 2500); }
  };
  ws.onerror = () => { try { ws.close(); } catch (e) { /* ignore */ } };
}
window.addEventListener('offline', () => setNet(false));
window.addEventListener('online', () => { if (Z.me) connectWS(); });
