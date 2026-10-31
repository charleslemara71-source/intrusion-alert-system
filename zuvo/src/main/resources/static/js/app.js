'use strict';
/* ===================== ZUVO: main app (shell, feed, reels, create, messages, profile) ===================== */
const debounce = (fn, ms) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; };
const emptyHtml = (icon, title, text) => `<div class="empty">${ic(icon, 34)}<h3 style="margin:6px 0 4px;color:var(--ink)">${esc(title)}</h3><p>${esc(text)}</p></div>`;

async function boot() {
  try { Z.me = await api('/auth/me'); afterLogin(); }
  catch (e) { showLanding(); }
}

/* ---------- shell ---------- */
function enterApp() {
  Z.connected = false; Z.view = 'home';
  const nav = [['home', 'Home', 'home'], ['reels', 'Reels', 'play'], ['live', 'Live', 'live'], ['messages', 'Messages', 'message'], ['profile', 'Profile', 'user']];
  root().innerHTML = `
  <div class="app">
    <header class="topbar">
      <div class="brand" style="cursor:pointer" id="home-logo">${logo(32)}<span class="desk-only">zuv<b>o</b></span></div>
      <div class="search">${ic('search', 18)}<input id="q" type="search" placeholder="Search people" aria-label="Search people" autocomplete="off"></div>
      ${netPill()}
      <button class="iconbtn" id="btn-req" aria-label="Follow requests">${ic('users')}<span class="badge hide" data-badge="req">0</span></button>
      <button class="iconbtn" id="btn-me" aria-label="Your profile">${avatar(Z.me, 34, true)}</button>
    </header>
    <div class="layout">
      <nav class="side" aria-label="Main">
        ${nav.map(([k, l, i]) => `<button class="nav-item" data-nav="${k}">${ic(i, 22)}<span class="l">${l}</span>${k === 'messages' ? '<span class="badge hide" data-badge="msg">0</span>' : ''}</button>`).join('')}
        <button class="btn btn-primary" id="btn-create">${ic('plus', 18)}<span>Create</span></button>
      </nav>
      <main id="view"></main>
      <aside class="rail" id="rail"></aside>
    </div>
    <nav class="bottom" aria-label="Main">
      <button data-nav="home" aria-label="Home">${ic('home', 24)}</button>
      <button data-nav="reels" aria-label="Reels">${ic('play', 24)}</button>
      <button class="plus" id="bn-create" aria-label="Create">${ic('plus', 24)}</button>
      <button data-nav="messages" aria-label="Messages">${ic('message', 24)}<span class="badge hide" data-badge="msg">0</span></button>
      <button data-nav="profile" aria-label="Profile">${ic('user', 24)}</button>
    </nav>
  </div>`;
  $$('[data-nav]').forEach(b => b.onclick = () => go(b.dataset.nav));
  $('#btn-create').onclick = $('#bn-create').onclick = openComposer;
  $('#btn-req').onclick = openRequests;
  $('#home-logo').onclick = () => go('home');
  $('#btn-me').onclick = () => go('profile');
  const doSearch = debounce(() => { const q = $('#q').value.trim(); if (q.length >= 2) go('search', q); }, 350);
  $('#q').addEventListener('input', doSearch);
  connectWS();
  go('home');
  refreshBadges();
}

function go(view, arg) {
  Z.view = view; Z.openChat = null;
  $$('[data-nav]').forEach(b => b.classList.toggle('active', b.dataset.nav === view || (view === 'user' && arg === Z.me.id && b.dataset.nav === 'profile')));
  window.scrollTo({ top: 0 });
  closeSheet();
  if (view === 'home') renderHome();
  else if (view === 'reels') renderReels();
  else if (view === 'live') renderLiveDir();
  else if (view === 'messages') renderMessages(arg);
  else if (view === 'profile') renderProfile(Z.me.id);
  else if (view === 'user') renderProfile(arg);
  else if (view === 'search') renderSearch(arg);
}

function setBadge(name, n) {
  $$(`[data-badge="${name}"]`).forEach(b => { b.textContent = n; b.classList.toggle('hide', !n); });
}
function updateUnread() {
  Z.unread = Z.contacts.reduce((s, c) => s + (c.unread || 0), 0);
  setBadge('msg', Z.unread);
}
async function refreshBadges() {
  try { const r = await api('/follows/requests'); Z.reqCount = r.length; setBadge('req', Z.reqCount); } catch (e) { /* ignore */ }
  try { Z.contacts = await api('/contacts'); updateUnread(); } catch (e) { /* ignore */ }
  renderRail();
}

/* ---------- right rail (desktop) ---------- */
async function renderRail() {
  const r = $('#rail'); if (!r) return;
  let live = [];
  try { live = await api('/live'); } catch (e) { /* ignore */ }
  const online = Z.contacts.filter(c => Z.online.has(c.user.id));
  r.innerHTML = `
    <div class="panel"><h3>Online now</h3>${online.length ? online.map(c => `<button class="mini" data-chat="${c.user.id}">${avatar(c.user, 36)}<span class="t">${esc(c.user.fullName)}</span></button>`).join('') : '<p class="hint">People you follow appear here when they are online.</p>'}</div>
    <div class="panel"><h3>Live now</h3>${live.length ? live.map(s => `<button class="mini" data-live="${s.id}">${avatar(s.host, 36, false)}<span class="t">${esc(s.title)}</span><span class="livepill">LIVE</span></button>`).join('') : '<p class="hint">Nobody is live right now.</p>'}</div>`;
  $$('[data-chat]', r).forEach(b => b.onclick = () => go('messages', +b.dataset.chat));
  $$('[data-live]', r).forEach(b => b.onclick = () => watchLive(+b.dataset.live));
}
const railSoon = debounce(renderRail, 600);

/* ---------- home feed ---------- */
function postHtml(p) {
  const a = p.author;
  const media = p.type === 'QUOTE' ? `<div class="quote">${esc(p.caption)}</div>`
    : p.type === 'PHOTO' ? `<div class="post-media"><img loading="lazy" src="${esc(p.mediaUrl)}" alt="Photo by ${esc(a.fullName)}"></div>`
    : `<div class="post-media"><video src="${esc(p.mediaUrl)}" controls playsinline preload="metadata"></video></div>`;
  const cap = p.type !== 'QUOTE' && p.caption ? `<div class="post-cap">${esc(p.caption)}</div>` : '';
  return `<article class="post" data-id="${p.id}" data-author="${a.id}" data-can="${p.canInteract ? 1 : 0}">
    <div class="post-head">
      <button data-user="${a.id}" aria-label="Open ${esc(a.fullName)}'s profile">${avatar(a, 42)}</button>
      <button class="who" data-user="${a.id}"><b>${esc(a.fullName)}</b><small>${timeAgo(p.createdAt)}</small></button>
      ${p.category ? `<span class="tag">${esc(p.category)}</span>` : ''}${p.discover ? '<span class="tag disc">Suggested</span>' : ''}
      ${p.mine ? `<button class="iconbtn" data-act="del" aria-label="Delete post">${ic('trash', 18)}</button>` : ''}
    </div>
    ${cap}${media}
    <div class="post-acts">
      <button class="act ${p.likedByMe ? 'liked' : ''} ${p.canInteract ? '' : 'locked'}" data-act="like" aria-label="Like">${ic('heart')}<span>${p.likeCount}</span></button>
      <button class="act ${p.canInteract ? '' : 'locked'}" data-act="comment" aria-label="Comments">${ic('message')}<span>${p.commentCount}</span></button>
    </div>
  </article>`;
}
function bindPosts(c) {
  c.onclick = e => {
    const u = e.target.closest('[data-user]');
    if (u) return go('user', +u.dataset.user);
    const a = e.target.closest('[data-act]'); if (!a) return;
    const card = a.closest('[data-id]'), id = +card.dataset.id;
    if (a.dataset.act === 'like') doLike(id, a);
    else if (a.dataset.act === 'comment') openComments(id, card);
    else if (a.dataset.act === 'del') doDelete(id, card);
  };
}
async function doLike(id, btn) {
  try {
    const r = await api('/posts/' + id + '/like', { method: 'POST' });
    btn.classList.toggle('liked', r.liked); $('span', btn).textContent = r.likeCount;
  } catch (e) { toast(e.message, 'error'); }
}
async function doDelete(id, card) {
  if (!confirm('Delete this post?')) return;
  try { await api('/posts/' + id, { method: 'DELETE' }); card.remove(); toast('Post deleted.'); }
  catch (e) { toast(e.message, 'error'); }
}
function openComments(id, card) {
  const can = card.dataset.can === '1', author = +card.dataset.author;
  const form = can
    ? `<form class="cform" id="cform"><input class="input" id="ctext" maxlength="1000" placeholder="Add a comment" autocomplete="off"><button class="btn btn-primary btn-sm" aria-label="Send comment">${ic('send', 16)}</button></form>`
    : `<div class="banner" style="margin-top:8px">${ic('lock', 24)}<div><b>Follow to comment</b><span>You can like and comment once this person accepts your follow request.</span></div></div><button class="btn btn-primary btn-block" style="margin-top:12px" id="cfollow">Send follow request</button>`;
  openSheet('Comments', `<div id="clist"><div class="skel" style="height:90px"></div></div>${form}`, async () => {
    const draw = c => `<div class="comment">${avatar(c.author, 36)}<div class="bub"><b>${esc(c.author.fullName)}</b> <small class="hint">${timeAgo(c.createdAt)}</small><p>${esc(c.body)}</p></div></div>`;
    try {
      const list = await api('/posts/' + id + '/comments');
      $('#clist').innerHTML = list.length ? list.map(draw).join('') : '<p class="hint" style="margin-bottom:12px">No comments yet. Start the conversation.</p>';
    } catch (e) { $('#clist').innerHTML = `<p class="hint">${esc(e.message)}</p>`; }
    if ($('#cfollow')) $('#cfollow').onclick = async () => {
      try { await api('/follows/' + author, { method: 'POST' }); toast('Follow request sent.', 'good'); closeSheet(); } catch (e) { toast(e.message, 'error'); }
    };
    if ($('#cform')) $('#cform').onsubmit = async e => {
      e.preventDefault();
      const t = $('#ctext').value.trim(); if (!t) return;
      const btn = $('button', e.target); btn.disabled = true;
      try {
        const c = await api('/posts/' + id + '/comments', { method: 'POST', json: { text: t } });
        if ($('#clist .hint')) $('#clist').innerHTML = '';
        $('#clist').insertAdjacentHTML('beforeend', draw(c));
        $('#ctext').value = '';
        const cnt = $('[data-act="comment"] span', card); if (cnt) cnt.textContent = (+cnt.textContent) + 1;
      } catch (ex) { toast(ex.message, 'error'); }
      btn.disabled = false;
    };
  });
}

function renderHome() {
  $('#view').innerHTML = `<div class="stack">
    <div class="prompt" id="pp" role="button" tabindex="0">${avatar(Z.me, 42, false)}<div class="fake">Share a photo, video, reel or quote</div>${ic('plus', 22)}</div>
    <div id="livebar"></div>
    <div id="feed"><div class="skel"></div></div></div>`;
  $('#pp').onclick = openComposer;
  loadLiveBar(); loadFeed();
}
async function loadLiveBar() {
  const el = $('#livebar'); if (!el) return;
  let list = [];
  try { list = await api('/live'); } catch (e) { /* ignore */ }
  el.innerHTML = `<div class="livebar"><button class="live-chip" id="golive"><span class="golive">${ic('live', 24)}</span>Go live</button>${list.map(s => `<button class="live-chip" data-live="${s.id}"><span class="ring">${avatar(s.host, 56, false)}</span><span>${esc(s.host.fullName.split(' ')[0])}</span><span class="livepill">LIVE</span></button>`).join('')}</div>`;
  $('#golive').onclick = startLive;
  $$('[data-live]', el).forEach(b => b.onclick = () => watchLive(+b.dataset.live));
}
async function loadFeed() {
  const el = $('#feed'); if (!el) return;
  try {
    const d = await api('/feed');
    let h = '';
    if (d.discoverMode) h += `<div class="banner">${ic('sparkle', 26)}<div><b>Welcome to Zuvo</b><span>${d.hasFollowing ? 'Your feed is still quiet, so we added some suggestions.' : 'Follow people to fill your feed. Until then, here are photos and videos picked for you.'}</span></div></div>`;
    h += d.posts.length ? d.posts.map(postHtml).join('') : emptyHtml('image', 'Nothing here yet', 'Post something or search for people to follow.');
    el.innerHTML = `<div class="stack">${h}</div>`;
    bindPosts(el);
  } catch (e) { el.innerHTML = emptyHtml('x', "Couldn't load your feed", e.message); }
}

/* ---------- live directory ---------- */
async function renderLiveDir() {
  $('#view').innerHTML = '<div class="skel"></div>';
  let list = [];
  try { list = await api('/live'); } catch (e) { /* ignore */ }
  $('#view').innerHTML = `<div class="stack">
    <div class="banner" style="background:var(--accent);color:var(--accent-ink)">${ic('live', 28)}<div style="flex:1"><b>Go live</b><span style="color:var(--accent-ink)">Start a live. People ask to join and you decide.</span></div><button class="btn btn-primary btn-sm" id="gl">Start</button></div>
    ${list.length ? list.map(s => `<div class="panel" style="display:flex;align-items:center;gap:12px">${avatar(s.host, 46, false)}<div style="flex:1;min-width:0"><b>${esc(s.title)}</b><div class="hint">${esc(s.host.fullName)}</div></div><span class="livepill">LIVE</span><button class="btn btn-soft btn-sm" data-live="${s.id}">Watch</button></div>`).join('') : emptyHtml('live', 'Nobody is live', 'When someone goes live, you can watch and ask to join.')}</div>`;
  $('#gl').onclick = startLive;
  $$('[data-live]').forEach(b => b.onclick = () => watchLive(+b.dataset.live));
}

/* ---------- reels ---------- */
async function renderReels() {
  const v = $('#view');
  v.innerHTML = '<div class="reels"><div class="reel"><div class="skel" style="height:100%;width:100%;border-radius:0"></div></div></div>';
  try {
    const list = await api('/reels');
    if (!list.length) { v.innerHTML = emptyHtml('play', 'No reels yet', 'Post a reel, or follow people to see theirs.'); return; }
    const box = $('.reels');
    box.innerHTML = list.map(p => `<div class="reel" data-id="${p.id}" data-author="${p.author.id}" data-can="${p.canInteract ? 1 : 0}">
      <video src="${esc(p.mediaUrl)}" loop muted playsinline preload="metadata"></video>
      <div class="reel-ui"><div class="txt"><button data-user="${p.author.id}" style="display:flex;gap:8px;align-items:center;color:#fff;font-weight:600;margin-bottom:6px">${avatar(p.author, 34, false)}${esc(p.author.fullName)}</button><p>${esc(p.caption || '')}</p></div>
      <div class="acts"><button class="act ${p.likedByMe ? 'liked' : ''}" data-act="like" aria-label="Like">${ic('heart', 24)}<span>${p.likeCount}</span></button><button class="act" data-act="comment" aria-label="Comments">${ic('message', 24)}<span>${p.commentCount}</span></button></div></div></div>`).join('');
    bindPosts(box);
    const io = new IntersectionObserver(es => es.forEach(en => {
      const vd = $('video', en.target); if (!vd) return;
      if (en.isIntersecting) vd.play().catch(() => {}); else vd.pause();
    }), { root: box, threshold: 0.65 });
    $$('.reel', box).forEach(r => io.observe(r));
    $$('video', box).forEach(vd => vd.onclick = () => { vd.muted = !vd.muted; toast(vd.muted ? 'Sound off' : 'Sound on'); });
  } catch (e) { v.innerHTML = emptyHtml('x', "Couldn't load reels", e.message); }
}

/* ---------- create post ---------- */
function composerStatus() {
  const s = $('#cstat'); if (!s) return;
  s.innerHTML = Z.connected ? '<span class="net"><i></i><span>Online, ready to post</span></span>'
    : '<span class="net off"><i></i><span>Offline. Posting resumes when you reconnect</span></span>';
}
function openComposer() {
  let type = 'PHOTO', file = null;
  openSheet('Create', `
    <div class="seg" id="seg">${[['PHOTO', 'Photo', 'image'], ['VIDEO', 'Video', 'video'], ['REEL', 'Reel', 'play'], ['QUOTE', 'Quote', 'quote']].map(([t, l, i], n) => `<button type="button" data-t="${t}" class="${n ? '' : 'on'}">${ic(i, 16)}${l}</button>`).join('')}</div>
    <label class="drop" id="drop"><input type="file" id="cfile" accept="image/*" hidden><span id="dropmsg">${ic('image', 30)}<br>Tap to choose a photo</span></label>
    <div class="field"><textarea id="ccap" maxlength="1000" placeholder="Write a caption"></textarea></div>
    <div class="row2">
      <div class="field"><label for="ccat">Topic</label><select id="ccat"><option value="">None</option>${CATEGORIES.map(c => `<option>${c}</option>`).join('')}</select></div>
      <div class="field"><label for="cvis">Who can see it</label><select id="cvis"><option value="FOLLOWERS">Followers</option><option value="PUBLIC">Everyone</option></select></div>
    </div>
    <div class="status" id="cstat"></div>
    <button class="btn btn-primary btn-block" id="cpost">Post</button>
    <button class="btn btn-ghost btn-block" id="cgo" style="margin-top:10px">${ic('live', 18)} Go live instead</button>`, () => {
    composerStatus();
    const drop = $('#drop'), inp = $('#cfile'), msg = $('#dropmsg');
    $('#seg').onclick = e => {
      const b = e.target.closest('[data-t]'); if (!b) return;
      type = b.dataset.t; file = null; inp.value = '';
      $$('#seg button').forEach(x => x.classList.toggle('on', x === b));
      drop.classList.toggle('hide', type === 'QUOTE');
      inp.accept = type === 'PHOTO' ? 'image/*' : 'video/*';
      msg.innerHTML = `${ic(type === 'PHOTO' ? 'image' : 'video', 30)}<br>Tap to choose a ${type === 'PHOTO' ? 'photo' : 'video'}`;
      $('#ccap').placeholder = type === 'QUOTE' ? 'Write your quote' : 'Write a caption';
    };
    inp.onchange = () => {
      file = inp.files[0]; if (!file) return;
      const url = URL.createObjectURL(file);
      msg.innerHTML = file.type.startsWith('video') ? `<video src="${url}" controls playsinline muted></video>` : `<img src="${url}" alt="Preview">`;
    };
    $('#cgo').onclick = () => { closeSheet(); startLive(); };
    $('#cpost').onclick = async () => {
      const cap = $('#ccap').value.trim();
      if (!Z.connected) return toast("You're offline. Reconnect to post.", 'error');
      if (type === 'QUOTE' && !cap) return toast('Write your quote first.', 'error');
      if (type !== 'QUOTE' && !file) return toast('Choose a file to post.', 'error');
      const fd = new FormData();
      fd.append('type', type); fd.append('caption', cap); fd.append('category', $('#ccat').value); fd.append('visibility', $('#cvis').value);
      if (file) fd.append('file', file);
      const btn = $('#cpost'); btn.disabled = true; btn.textContent = 'Posting...';
      try {
        await api('/posts', { method: 'POST', form: fd });
        closeSheet(); toast('Posted.', 'good');
        go(Z.view === 'reels' ? 'reels' : 'profile');
      } catch (ex) { toast(ex.message, 'error'); btn.disabled = false; btn.textContent = 'Post'; }
    };
  });
}
onWS(m => { if (m.type === 'net') composerStatus(); });

/* ---------- follow requests ---------- */
async function openRequests() {
  openSheet('Follow requests', '<div class="skel" style="height:90px"></div>', async body => {
    async function draw() {
      let list = [];
      try { list = await api('/follows/requests'); } catch (e) { body.innerHTML = `<p class="hint">${esc(e.message)}</p>`; return; }
      Z.reqCount = list.length; setBadge('req', Z.reqCount);
      body.innerHTML = list.length ? list.map(r => `<div class="reqrow" data-id="${r.id}">
        <button data-user="${r.user.id}">${avatar(r.user, 46)}</button>
        <div class="who"><b>${esc(r.user.fullName)}</b><div class="hint">${timeAgo(r.createdAt)}</div></div>
        <button class="btn btn-primary btn-sm" data-r="1">Accept</button><button class="btn btn-ghost btn-sm" data-r="0">Decline</button></div>`).join('')
        : emptyHtml('users', 'No requests', 'When someone asks to follow you, it shows up here.');
      $$('[data-r]', body).forEach(b => b.onclick = async () => {
        try {
          await api('/follows/' + b.closest('.reqrow').dataset.id + '/respond', { method: 'POST', json: { accept: b.dataset.r === '1' } });
          toast(b.dataset.r === '1' ? 'Accepted. They can now like and comment on your posts.' : 'Request declined.', b.dataset.r === '1' ? 'good' : 'info');
          draw(); refreshBadges();
        } catch (ex) { toast(ex.message, 'error'); }
      });
      $$('[data-user]', body).forEach(b => b.onclick = () => go('user', +b.dataset.user));
    }
    draw();
  });
}

/* ---------- search ---------- */
async function renderSearch(q) {
  $('#view').innerHTML = '<div class="skel" style="height:120px"></div>';
  try {
    const list = await api('/users/search?q=' + encodeURIComponent(q));
    $('#view').innerHTML = `<div class="stack"><h3 style="font-size:18px">People matching "${esc(q)}"</h3>${list.length ? list.map(u => `<button class="panel" data-user="${u.id}" style="display:flex;gap:12px;align-items:center;width:100%;text-align:left">${avatar(u, 46)}<div style="flex:1"><b>${esc(u.fullName)}</b><div class="hint">${esc(u.bio || '')}</div></div><span class="btn btn-soft btn-sm">View</span></button>`).join('') : emptyHtml('search', 'No one found', 'Try a different name.')}</div>`;
    $$('[data-user]').forEach(b => b.onclick = () => go('user', +b.dataset.user));
  } catch (e) { $('#view').innerHTML = emptyHtml('x', 'Search failed', e.message); }
}

/* ---------- messages ---------- */
async function renderMessages(openId) {
  Z.view = 'messages';
  $('#view').innerHTML = `<div class="chat" id="chat"><div class="contacts" id="contacts"></div><div class="thread" id="thread"><div class="thread-empty"><div>${ic('message', 34)}<p style="margin-top:8px">Choose a chat to start talking.</p></div></div></div></div>`;
  await loadContacts();
  if (openId) openThread(openId);
}
async function loadContacts() {
  try { Z.contacts = await api('/contacts'); } catch (e) { Z.contacts = []; }
  updateUnread();
  drawContacts();
}
function drawContacts() {
  const el = $('#contacts'); if (!el) return;
  el.innerHTML = Z.contacts.length ? Z.contacts.map(c => `<button class="contact ${Z.openChat === c.user.id ? 'active' : ''}" data-id="${c.user.id}">${avatar(c.user, 46)}<span class="m"><b>${esc(c.user.fullName)}</b><small>${c.last ? esc(c.last.content) : 'Say hello'}</small></span>${c.unread ? `<span class="badge" style="position:static">${c.unread}</span>` : ''}</button>`).join('')
    : emptyHtml('users', 'No chats yet', 'You can message someone once your follow request is accepted.');
  $$('.contact', el).forEach(b => b.onclick = () => openThread(+b.dataset.id));
}
function addBubble(m) {
  const b = $('#bubbles'); if (!b) return;
  b.insertAdjacentHTML('beforeend', `<div class="bubble ${m.senderId === Z.me.id ? 'me' : 'them'}">${esc(m.content)}<small>${clock(m.createdAt)}</small></div>`);
  b.scrollTop = b.scrollHeight;
}
async function openThread(id) {
  Z.openChat = id;
  const c = Z.contacts.find(x => x.user.id === id);
  let user;
  try { user = c ? c.user : (await api('/users/' + id)).user; } catch (e) { return toast(e.message, 'error'); }
  $('#chat').classList.add('open');
  drawContacts();
  $('#thread').innerHTML = `
    <div class="thread-head"><button class="iconbtn" id="tback" aria-label="Back to chats">${ic('back')}</button>
      <button class="who" data-user="${id}" style="display:flex;gap:10px;align-items:center;text-align:left">${avatar(user, 40)}<span><b>${esc(user.fullName)}</b><br><small data-status-for="${id}">${Z.online.has(id) ? 'Online' : 'Offline'}</small></span></button>
      <button class="iconbtn" id="tcall" aria-label="Voice call">${ic('phone')}</button><button class="iconbtn" id="tvid" aria-label="Video call">${ic('video')}</button></div>
    <div class="bubbles" id="bubbles"></div>
    <form class="chat-input" id="mform"><input id="mtext" maxlength="2000" autocomplete="off" placeholder="Write a message"><button class="btn btn-primary btn-sm" aria-label="Send message">${ic('send', 16)}</button></form>`;
  $('#tback').onclick = () => { Z.openChat = null; $('#chat').classList.remove('open'); drawContacts(); };
  $('#tcall').onclick = () => startCall(id, false);
  $('#tvid').onclick = () => startCall(id, true);
  $('#thread .who').onclick = () => go('user', id);
  $('#mform').onsubmit = async e => {
    e.preventDefault();
    const t = $('#mtext').value.trim(); if (!t) return;
    $('#mtext').value = '';
    try { addBubble(await api('/messages/' + id, { method: 'POST', json: { text: t } })); }
    catch (ex) { toast(ex.message, 'error'); $('#mtext').value = t; }
  };
  try {
    const msgs = await api('/messages/' + id);
    if (Z.openChat !== id) return;
    msgs.forEach(addBubble);
    if (c) { c.unread = 0; updateUnread(); drawContacts(); }
  } catch (e) { toast(e.message, 'error'); }
  $('#mtext')?.focus();
}

/* ---------- profile ---------- */
async function renderProfile(id) {
  Z.viewUser = id;
  const v = $('#view');
  v.innerHTML = '<div class="skel" style="height:380px"></div>';
  let p, pp;
  try { [p, pp] = await Promise.all([api('/users/' + id), api('/users/' + id + '/posts')]); }
  catch (e) { v.innerHTML = emptyHtml('x', "Couldn't open this profile", e.message); return; }
  const u = p.user, self = p.relation === 'SELF', first = u.fullName.split(' ')[0];
  let actions = '';
  if (self) {
    actions = `<button class="btn btn-ghost btn-sm" id="editp">Edit profile</button><button class="btn btn-soft btn-sm" id="reqs">${ic('users', 16)} Requests</button><button class="btn btn-ghost btn-sm" id="logout">${ic('logout', 16)} Log out</button>`;
  } else {
    if (p.relation === 'NONE') actions += `<button class="btn btn-primary" id="follow">Follow</button>`;
    else if (p.relation === 'PENDING') actions += `<button class="btn btn-ghost" id="unfollow">${ic('clock', 16)} Requested</button>`;
    else actions += `<button class="btn btn-ghost" id="unfollow">${ic('check', 16)} Following</button>`;
    if (p.requestedMe) actions += `<button class="btn btn-primary btn-sm" id="acc">Accept request</button><button class="btn btn-ghost btn-sm" id="dec">Decline</button>`;
    if (p.relation === 'ACCEPTED' || p.followsMe) actions += `<button class="btn btn-soft" id="msg">${ic('message', 16)} Message</button><button class="iconbtn" id="pcall" aria-label="Voice call">${ic('phone')}</button><button class="iconbtn" id="pvid" aria-label="Video call">${ic('video')}</button>`;
  }
  const posts = pp.posts;
  const grid = pp.locked
    ? `<div class="panel locked-box">${ic('lock', 30)}<p style="margin-top:8px"><b>Follow ${esc(first)} to see their posts</b></p><p class="hint">Send a follow request. Their posts appear once it's accepted.</p></div>`
    : posts.length ? `<div class="grid3">${posts.map((x, i) => `<div class="tile-post" data-i="${i}" role="button" tabindex="0">${x.type === 'QUOTE' ? esc((x.caption || '').slice(0, 70)) : x.type === 'PHOTO' ? `<img loading="lazy" src="${esc(x.mediaUrl)}" alt="">` : `<video src="${esc(x.mediaUrl)}#t=0.1" muted preload="metadata"></video>${ic('play', 18)}`}</div>`).join('')}</div>`
    : emptyHtml('image', self ? 'No posts yet' : 'No posts to show', self ? 'Share your first photo, video, reel or quote.' : '');
  v.innerHTML = `<div class="stack"><div class="prof"><div class="cover"></div><div class="prof-body">
    <div class="prof-top"><span style="position:relative;display:inline-flex">${avatar(u, 92)}${self ? `<button class="camera" id="pic" aria-label="Change profile picture">${ic('camera', 16)}</button><input type="file" id="picfile" accept="image/*" hidden>` : ''}</span><div class="acts-row">${actions}</div></div>
    <h2>${esc(u.fullName)}</h2>
    <p class="bio">${esc(u.bio || (self ? 'Add a short bio from Edit profile.' : ''))}</p>
    <div class="stats"><div><b>${p.posts}</b><span>Posts</span></div><div><b>${p.followers}</b><span>Followers</span></div><div><b>${p.following}</b><span>Following</span></div></div>
    <div class="hint" data-status-for="${u.id}">${Z.online.has(u.id) ? 'Online' : 'Offline'}</div>
  </div></div>${grid}</div>`;
  const again = () => renderProfile(id);
  const on = (sel, fn) => { const el = $(sel); if (el) el.onclick = fn; };
  on('#follow', async () => { try { await api('/follows/' + id, { method: 'POST' }); toast('Follow request sent.', 'good'); again(); } catch (e) { toast(e.message, 'error'); } });
  on('#unfollow', async () => {
    if (!confirm(p.relation === 'PENDING' ? 'Cancel your follow request?' : 'Unfollow ' + first + '?')) return;
    try { await api('/follows/' + id, { method: 'DELETE' }); again(); refreshBadges(); } catch (e) { toast(e.message, 'error'); }
  });
  on('#acc', async () => { try { await api('/follows/' + p.incomingRequestId + '/respond', { method: 'POST', json: { accept: true } }); toast('Accepted.', 'good'); again(); refreshBadges(); } catch (e) { toast(e.message, 'error'); } });
  on('#dec', async () => { try { await api('/follows/' + p.incomingRequestId + '/respond', { method: 'POST', json: { accept: false } }); again(); refreshBadges(); } catch (e) { toast(e.message, 'error'); } });
  on('#msg', () => go('messages', id));
  on('#pcall', () => startCall(id, false));
  on('#pvid', () => startCall(id, true));
  on('#reqs', openRequests);
  on('#logout', async () => { try { await api('/auth/logout', { method: 'POST' }); } catch (e) { /* ignore */ } Z.me = null; showLanding(); });
  on('#editp', () => editProfile());
  if (self) {
    on('#pic', () => $('#picfile').click());
    $('#picfile').onchange = async e => {
      const f = e.target.files[0]; if (!f) return;
      const fd = new FormData(); fd.append('file', f);
      toast('Uploading your picture...');
      try { const r = await api('/me/picture', { method: 'POST', form: fd }); Z.me.profilePicUrl = r.profilePicUrl; toast('Profile picture updated.', 'good'); $('#btn-me').innerHTML = avatar(Z.me, 34, true); again(); }
      catch (ex) { toast(ex.message, 'error'); }
    };
  }
  $$('.tile-post').forEach(t => t.onclick = () => {
    const x = posts[+t.dataset.i];
    openSheet('Post', postHtml(x), body => bindPosts(body));
  });
}
function editProfile() {
  openSheet('Edit profile', `
    <div class="field"><label for="en">Full name</label><input id="en" value="${esc(Z.me.fullName)}" maxlength="120"></div>
    <div class="field"><label for="eb">Bio</label><textarea id="eb" maxlength="300" placeholder="Tell people about you">${esc(Z.me.bio || '')}</textarea></div>
    <div class="field"><label for="eg">Gender</label><select id="eg">${['Female', 'Male', 'Other', 'Prefer not to say'].map(g => `<option ${Z.me.gender === g ? 'selected' : ''}>${g}</option>`).join('')}</select></div>
    <div class="hint" style="margin-bottom:14px">Your email and date of birth can't be changed here.</div>
    <button class="btn btn-primary btn-block" id="esave">Save changes</button>`, () => {
    $('#esave').onclick = async () => {
      try {
        await api('/me', { method: 'PUT', json: { fullName: $('#en').value, bio: $('#eb').value, gender: $('#eg').value } });
        Z.me.fullName = $('#en').value.trim(); Z.me.bio = $('#eb').value.trim(); Z.me.gender = $('#eg').value;
        closeSheet(); toast('Profile saved.', 'good'); renderProfile(Z.me.id);
      } catch (ex) { toast(ex.message, 'error'); }
    };
  });
}

/* ---------- real-time events ---------- */
onWS(m => {
  if (!Z.me) return;
  if (m.type === 'presence' || m.type === 'hello') { railSoon(); }
  else if (m.type === 'follow_request') {
    Z.reqCount++; setBadge('req', Z.reqCount);
    toast(m.from.fullName + ' wants to follow you.');
  } else if (m.type === 'follow_response') {
    toast(m.by.fullName + (m.accepted ? ' accepted your follow request.' : ' declined your follow request.'), m.accepted ? 'good' : 'info');
    refreshBadges();
    if (Z.view === 'user' && Z.viewUser === m.by.id) renderProfile(m.by.id);
  } else if (m.type === 'message') {
    if (Z.view === 'messages' && Z.openChat === m.from.id) { addBubble(m.message); api('/messages/' + m.from.id).catch(() => {}); }
    else { toast('New message from ' + m.from.fullName); loadContacts(); }
  } else if (m.type === 'notify') toast(m.text);
});
