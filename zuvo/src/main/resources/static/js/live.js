'use strict';
/* ===================== ZUVO: live streaming + 1-to-1 calls (LiveKit) ===================== */
const LK = { room: null, kind: null, streamId: null, peerId: null, isHost: false, canPublish: false, switching: false, url: null, ringTimer: null };

function lkReady(needCamera) {
  if (!window.LivekitClient) throw new Error('The video library did not load. Check your internet connection and reload.');
  if (needCamera && !(navigator.mediaDevices && navigator.mediaDevices.getUserMedia))
    throw new Error('Camera and microphone need a secure link (https) or localhost.');
}

/* ---------- overlay UI ---------- */
function lkOverlay({ title, live, bar }) {
  $('#lk-ov')?.remove();
  const ov = document.createElement('div');
  ov.className = 'live-ov'; ov.id = 'lk-ov';
  ov.innerHTML = `
    <div class="live-top">
      ${live ? '<span class="livepill">LIVE</span>' : `<span style="color:#7DD3FC">${ic('phone', 18)}</span>`}
      <span class="t">${esc(title)}</span>
      <span class="hint" style="color:#B8BDE0;display:flex;gap:6px;align-items:center">${ic('users', 16)}<span id="lk-count">1</span></span>
    </div>
    <div class="lk-grid" id="lk-grid"></div>
    <div class="lk-bar" id="lk-bar">${bar}</div>`;
  document.body.appendChild(ov);
  return ov;
}
function lkTile(identity, name, local) {
  const grid = $('#lk-grid');
  if (!grid) return null;
  let t = document.getElementById('tile-' + identity);
  if (!t) {
    t = document.createElement('div');
    t.className = 'lk-tile'; t.id = 'tile-' + identity;
    t.innerHTML = `<span class="ph">${esc(initials(name))}</span><span class="nm">${esc(local ? 'You' : name)}</span>`;
    grid.appendChild(t);
  }
  return t;
}
function lkAttach(track, participant, local) {
  const t = lkTile(participant.identity, participant.name || 'Guest', local);
  if (!t) return;
  if (track.kind === 'video') {
    $$('video', t).forEach(v => v.remove());
    const el = track.attach();
    el.playsInline = true; el.muted = true; el.autoplay = true;
    t.prepend(el);
    const ph = $('.ph', t); if (ph) ph.classList.add('hide');
  } else if (track.kind === 'audio' && !local) {
    const el = track.attach(); el.autoplay = true; el.style.display = 'none';
    t.appendChild(el);
  }
}
function lkCount() {
  const c = $('#lk-count');
  if (c && LK.room) c.textContent = LK.room.remoteParticipants.size + 1;
}

async function lkConnect(url, token, publish, withVideo) {
  lkReady(publish);
  const { Room, RoomEvent } = LivekitClient;
  const room = new Room({ adaptiveStream: true, dynacast: true });
  room.on(RoomEvent.TrackSubscribed, (track, pub, p) => lkAttach(track, p, false));
  room.on(RoomEvent.TrackUnsubscribed, (track, pub, p) => {
    track.detach().forEach(e => e.remove());
    const t = document.getElementById('tile-' + p.identity);
    if (t && track.kind === 'video') { const ph = $('.ph', t); if (ph) ph.classList.remove('hide'); }
  });
  room.on(RoomEvent.LocalTrackPublished, (pub, p) => { if (pub.track && pub.track.kind === 'video') lkAttach(pub.track, p, true); });
  room.on(RoomEvent.ParticipantConnected, p => { lkTile(p.identity, p.name || 'Guest', false); lkCount(); });
  room.on(RoomEvent.ParticipantDisconnected, p => { document.getElementById('tile-' + p.identity)?.remove(); lkCount(); lkPeerLeft(p); });
  room.on(RoomEvent.Disconnected, () => { if (!LK.switching && LK.room === room) { toast('You left the room.'); lkLeave(true); } });
  await room.connect(url, token);
  LK.room = room;
  lkTile(room.localParticipant.identity, Z.me.fullName, true);
  if (publish) {
    try {
      await room.localParticipant.setMicrophoneEnabled(true);
      if (withVideo) await room.localParticipant.setCameraEnabled(true);
    } catch (e) {
      toast('We could not use your camera or microphone. Allow access in your browser settings.', 'error');
    }
  }
  room.remoteParticipants.forEach(p => {
    lkTile(p.identity, p.name || 'Guest', false);
    p.trackPublications.forEach(pub => { if (pub.isSubscribed && pub.track) lkAttach(pub.track, p, false); });
  });
  lkCount();
  return room;
}

function lkPeerLeft(p) {
  if (LK.kind === 'call') { toast('The call ended.'); lkLeave(true); }
}

async function lkLeave(silent) {
  clearTimeout(LK.ringTimer);
  const room = LK.room;
  LK.room = null; LK.switching = false;
  if (room) { try { await room.disconnect(); } catch (e) { /* ignore */ } }
  $('#lk-ov')?.remove();
  LK.kind = LK.streamId = LK.peerId = null; LK.isHost = LK.canPublish = false;
  if (Z.view === 'home' && typeof loadLiveBar === 'function') loadLiveBar();
}

function lkToggles() {
  const mic = $('#lk-mic'), cam = $('#lk-cam');
  if (mic) mic.onclick = async () => {
    const on = !LK.room.localParticipant.isMicrophoneEnabled;
    await LK.room.localParticipant.setMicrophoneEnabled(on);
    mic.classList.toggle('off', !on); mic.innerHTML = ic(on ? 'mic' : 'micoff', 22);
  };
  if (cam) cam.onclick = async () => {
    const on = !LK.room.localParticipant.isCameraEnabled;
    try { await LK.room.localParticipant.setCameraEnabled(on); } catch (e) { return toast('Camera is not available.', 'error'); }
    cam.classList.toggle('off', !on); cam.innerHTML = ic(on ? 'video' : 'videooff', 22);
    const t = document.getElementById('tile-' + LK.room.localParticipant.identity);
    if (t && !on) { $$('video', t).forEach(v => v.remove()); $('.ph', t)?.classList.remove('hide'); }
  };
}
const MIC_CAM = `<button id="lk-mic" aria-label="Microphone">${ic('mic', 22)}</button><button id="lk-cam" aria-label="Camera">${ic('video', 22)}</button>`;

/* ---------- host: go live ---------- */
function startLive() {
  if (!Z.connected) return toast("You're offline. Reconnect to go live.", 'error');
  openSheet('Go live', `
    <div class="field"><label for="lt">Title</label><input id="lt" maxlength="120" placeholder="What's your live about?"></div>
    <p class="hint" style="margin-bottom:16px">People can watch, then send a request to join. You decide who comes on camera.</p>
    <button class="btn btn-accent btn-block" id="lgo">${ic('live', 18)} Start live</button>`, () => {
    $('#lgo').onclick = async () => {
      const btn = $('#lgo'); btn.disabled = true;
      try {
        lkReady(true);
        const d = await api('/live/start', { method: 'POST', json: { title: $('#lt').value } });
        closeSheet();
        await openHostRoom(d);
      } catch (ex) { toast(ex.message, 'error'); btn.disabled = false; }
    };
  });
}
async function openHostRoom(d) {
  LK.kind = 'live'; LK.streamId = d.stream.id; LK.isHost = true; LK.canPublish = true; LK.url = d.url;
  lkOverlay({
    title: d.stream.title, live: true,
    bar: `${MIC_CAM}<button id="lk-req" aria-label="Join requests" style="position:relative">${ic('users', 22)}<span class="badge hide" id="lk-badge">0</span></button>
          <button class="end" id="lk-end">${ic('x', 18)} End live</button>`
  });
  lkToggles();
  $('#lk-req').onclick = toggleRequestDrawer;
  $('#lk-end').onclick = async () => {
    if (!confirm('End your live for everyone?')) return;
    try { await api('/live/' + LK.streamId + '/end', { method: 'POST' }); } catch (e) { /* ignore */ }
    lkLeave(true);
  };
  try { await lkConnect(d.url, d.token, true, true); refreshRequests(); }
  catch (ex) { toast(ex.message, 'error'); try { await api('/live/' + d.stream.id + '/end', { method: 'POST' }); } catch (e) { /* ignore */ } lkLeave(true); }
}

/* ---------- host: join requests (accept / decline / keep pending) ---------- */
async function refreshRequests() {
  if (!LK.isHost || !LK.streamId) return;
  let list = [];
  try { list = await api('/live/' + LK.streamId + '/requests'); } catch (e) { return; }
  const waiting = list.filter(r => r.status === 'PENDING').length;
  const b = $('#lk-badge');
  if (b) { b.textContent = waiting; b.classList.toggle('hide', !waiting); }
  const dr = $('#lk-drawer');
  if (!dr) return;
  dr.innerHTML = `<h3>Join requests</h3>` + (list.length ? list.map(r => `
    <div class="reqrow" data-id="${r.id}">
      ${avatar(r.user, 38, false)}
      <div class="who"><b>${esc(r.user.fullName)}</b><div class="hint" style="color:#9AA1CC">${r.status === 'ACCEPTED' ? 'On camera' : r.status === 'DECLINED' ? 'Declined' : 'Waiting'}</div></div>
      <button class="btn btn-primary btn-sm" data-d="ACCEPT" aria-label="Accept">${ic('check', 16)}</button>
      <button class="btn btn-ghost btn-sm" data-d="PENDING" aria-label="Keep pending" style="color:#fff;border-color:#3A4380">${ic('clock', 16)}</button>
      <button class="btn btn-ghost btn-sm" data-d="DECLINE" aria-label="Decline" style="color:#fff;border-color:#3A4380">${ic('x', 16)}</button>
    </div>`).join('') : '<p class="hint" style="color:#9AA1CC">No requests yet. When someone asks to join, they appear here.</p>');
  $$('[data-d]', dr).forEach(btn => btn.onclick = async () => {
    const id = btn.closest('.reqrow').dataset.id;
    try { await api('/live/requests/' + id + '/decision', { method: 'POST', json: { decision: btn.dataset.d } }); refreshRequests(); }
    catch (ex) { toast(ex.message, 'error'); }
  });
}
function toggleRequestDrawer() {
  let dr = $('#lk-drawer');
  if (dr) return dr.remove();
  dr = document.createElement('div');
  dr.className = 'drawer'; dr.id = 'lk-drawer';
  $('#lk-ov').appendChild(dr);
  refreshRequests();
}

/* ---------- viewer ---------- */
async function watchLive(id) {
  try {
    lkReady(false);
    const d = await api('/live/' + id + '/watch', { method: 'POST' });
    if (d.isHost) return openHostRoom({ stream: d.stream, url: d.url, token: d.token });
    LK.kind = 'live'; LK.streamId = id; LK.isHost = false; LK.canPublish = d.myRequest === 'ACCEPTED'; LK.url = d.url;
    lkOverlay({
      title: d.stream.title + ' with ' + d.stream.host.fullName, live: true,
      bar: `${LK.canPublish ? MIC_CAM : ''}<button class="wide ${LK.canPublish ? 'hide' : ''}" id="lk-ask">${ic('plus', 18)} Request to join</button><button class="end" id="lk-leave">${ic('x', 18)} Leave</button>`
    });
    if (LK.canPublish) lkToggles();
    $('#lk-leave').onclick = () => lkLeave(true);
    $('#lk-ask').onclick = async () => {
      try {
        const r = await api('/live/' + id + '/request', { method: 'POST' });
        const b = $('#lk-ask');
        b.innerHTML = `${ic('clock', 18)} ${r.status === 'ACCEPTED' ? 'Accepted' : 'Waiting for the host'}`; b.disabled = true;
      } catch (ex) { toast(ex.message, 'error'); }
    };
    if (d.myRequest === 'PENDING') { const b = $('#lk-ask'); b.innerHTML = `${ic('clock', 18)} Waiting for the host`; b.disabled = true; }
    await lkConnect(d.url, d.token, LK.canPublish, true);
    if (LK.canPublish) lkToggles();
  } catch (ex) { toast(ex.message, 'error'); lkLeave(true); }
}

/* the host accepted me: reconnect with permission to use camera + microphone */
async function becomeGuest() {
  try {
    lkReady(true);
    const d = await api('/live/' + LK.streamId + '/publish-token', { method: 'POST' });
    LK.switching = true;
    try { await LK.room.disconnect(); } catch (e) { /* ignore */ }
    $$('#lk-grid .lk-tile').forEach(t => t.remove());
    await lkConnect(d.url, d.token, true, true);
    LK.switching = false; LK.canPublish = true;
    $('#lk-ask')?.classList.add('hide');
    $('#lk-bar').insertAdjacentHTML('afterbegin', MIC_CAM);
    lkToggles();
    toast('You are on camera.', 'good');
  } catch (ex) { LK.switching = false; toast(ex.message, 'error'); }
}

/* ---------- 1-to-1 calls ---------- */
async function startCall(userId, video) {
  if (!Z.connected) return toast("You're offline.", 'error');
  try {
    lkReady(true);
    const d = await api('/calls/' + userId + '/start', { method: 'POST', json: { video } });
    const peer = Z.contacts.find(c => c.user.id === userId)?.user || { fullName: 'your contact' };
    LK.kind = 'call'; LK.peerId = userId;
    lkOverlay({ title: 'Calling ' + peer.fullName + '...', live: false, bar: `${MIC_CAM}<button class="end" id="lk-hang">${ic('phoneoff', 20)} End call</button>` });
    lkToggles();
    $('#lk-hang').onclick = async () => { try { await api('/calls/' + userId + '/end', { method: 'POST' }); } catch (e) { /* ignore */ } lkLeave(true); };
    await lkConnect(d.url, d.token, true, video);
    LK.ringTimer = setTimeout(() => { if (LK.kind === 'call' && LK.room && LK.room.remoteParticipants.size === 0) { toast('No answer.'); api('/calls/' + userId + '/end', { method: 'POST' }).catch(() => {}); lkLeave(true); } }, 45000);
  } catch (ex) { toast(ex.message, 'error'); lkLeave(true); }
}
function incomingCall(m) {
  if (LK.room) { api('/calls/' + m.from.id + '/decline', { method: 'POST' }).catch(() => {}); return; }
  openSheet('Incoming ' + (m.video ? 'video' : 'voice') + ' call', `
    <div class="call-card">${avatar(m.from, 84, false)}<h3>${esc(m.from.fullName)}</h3><p class="hint" style="margin:4px 0 20px">is calling you</p>
    <div style="display:flex;gap:12px;justify-content:center"><button class="btn btn-danger" id="c-no">${ic('phoneoff', 18)} Decline</button><button class="btn btn-primary" id="c-yes">${ic('phone', 18)} Answer</button></div></div>`, () => {
    const timer = setTimeout(closeSheet, 40000);
    $('#c-no').onclick = () => { clearTimeout(timer); closeSheet(); api('/calls/' + m.from.id + '/decline', { method: 'POST' }).catch(() => {}); };
    $('#c-yes').onclick = async () => {
      clearTimeout(timer); closeSheet();
      try {
        lkReady(true);
        const d = await api('/calls/' + m.from.id + '/join', { method: 'POST' });
        LK.kind = 'call'; LK.peerId = m.from.id;
        lkOverlay({ title: m.from.fullName, live: false, bar: `${MIC_CAM}<button class="end" id="lk-hang">${ic('phoneoff', 20)} End call</button>` });
        lkToggles();
        $('#lk-hang').onclick = async () => { try { await api('/calls/' + m.from.id + '/end', { method: 'POST' }); } catch (e) { /* ignore */ } lkLeave(true); };
        await lkConnect(d.url, d.token, true, m.video);
      } catch (ex) { toast(ex.message, 'error'); lkLeave(true); }
    };
  });
}

/* ---------- real-time events for live + calls ---------- */
onWS(m => {
  if (m.type === 'live_request' && LK.isHost && LK.streamId === m.streamId) {
    toast(m.request.user.fullName + ' wants to join your live.');
    refreshRequests();
  } else if (m.type === 'live_decision' && LK.streamId === m.streamId && !LK.isHost) {
    if (m.decision === 'ACCEPTED') becomeGuest();
    else if (m.decision === 'DECLINED') { toast('The host declined your request.'); const b = $('#lk-ask'); if (b) { b.disabled = false; b.innerHTML = `${ic('plus', 18)} Request to join`; } }
    else toast('The host will answer your request later.');
  } else if (m.type === 'live_ended' && LK.kind === 'live' && LK.streamId === m.streamId && !LK.isHost) {
    toast('The live has ended.'); lkLeave(true);
  } else if (m.type === 'incoming_call') incomingCall(m);
  else if (m.type === 'call_declined' && LK.kind === 'call' && LK.peerId === m.by.id) { toast(m.by.fullName + ' declined the call.'); lkLeave(true); }
  else if (m.type === 'call_ended' && LK.kind === 'call' && LK.peerId === m.by.id) { toast('The call ended.'); lkLeave(true); }
  else if (m.type === 'live_started' || m.type === 'live_ended') { if (Z.view === 'home' && typeof loadLiveBar === 'function') loadLiveBar(); if (typeof renderRail === 'function') renderRail(); }
});
