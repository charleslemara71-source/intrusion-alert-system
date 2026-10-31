'use strict';
/* ===================== ZUVO: landing, register, email code, login, interests ===================== */
const root = () => $('#root');

function ageFrom(dob) {
  const b = new Date(dob), t = new Date();
  let a = t.getFullYear() - b.getFullYear();
  const m = t.getMonth() - b.getMonth();
  if (m < 0 || (m === 0 && t.getDate() < b.getDate())) a--;
  return a;
}
function isoDate(d) { return d.toISOString().slice(0, 10); }

/* ---------- landing ---------- */
function showLanding() {
  if (Z.ws) { try { Z.ws.close(); } catch (e) { /* ignore */ } }
  clearInterval(Z.pingT);
  closeSheet();
  root().innerHTML = `
  <div class="land">
    <header class="land-nav">
      <div class="brand">${logo(36)}<span>zuv<b>o</b></span></div>
      <div style="display:flex;gap:10px">
        <button class="btn btn-ghost btn-sm" id="l-login">Log in</button>
        <button class="btn btn-primary btn-sm" id="l-reg">Sign up</button>
      </div>
    </header>
    <section class="hero">
      <div>
        <h1>Share your world. Live.</h1>
        <p>Post photos, reels and quotes, chat privately, and go live with people you choose. A green dot shows who is online right now.</p>
        <div class="cta">
          <button class="btn btn-primary" id="h-reg">Create account</button>
          <button class="btn btn-ghost" id="h-login">Log in</button>
        </div>
        <small>Zuvo is for adults aged 18 and over.</small>
      </div>
      <div class="stage" aria-hidden="true">
        <div class="card-f f1">
          <div style="display:flex;align-items:center;gap:10px">${avatar({ id: -1, fullName: 'Amina Kamau' }, 40, true).replace('class="dot "', 'class="dot on"')}<div><b>Amina</b><div class="hint">Online now</div></div></div>
          <div class="pic" style="background:linear-gradient(135deg,#3B2FD9,#00A8C6)"></div>
          <span class="livepill">LIVE</span> <span class="hint">128 watching</span>
        </div>
        <div class="card-f f2">
          <div class="bubble them" style="max-width:100%">Joining the live tonight?</div>
          <div class="bubble me" style="margin-top:8px;max-width:100%">Yes, sending my request</div>
        </div>
        <div class="card-f f3">
          <div style="display:flex;align-items:center;gap:10px">${avatar({ id: -2, fullName: 'Juma Otieno' }, 38, false)}<div style="flex:1"><b>Juma</b><div class="hint">wants to follow you</div></div></div>
          <div style="display:flex;gap:8px;margin-top:12px"><span class="btn btn-primary btn-sm">Accept</span><span class="btn btn-ghost btn-sm">Decline</span></div>
        </div>
      </div>
    </section>
    <section class="feats">
      <div class="feat">${ic('image', 26)}<h3>Share moments</h3><p>Photos, videos, reels and quotes for the people who follow you.</p></div>
      <div class="feat">${ic('message', 26)}<h3>Chat privately</h3><p>Messages, voice and video calls with people you follow.</p></div>
      <div class="feat">${ic('live', 26)}<h3>Go live your way</h3><p>Viewers ask to join. You accept, decline or decide later.</p></div>
      <div class="feat">${ic('sparkle', 26)}<h3>Safer by design</h3><p>AI checks comments and photos before they reach anyone.</p></div>
    </section>
  </div>`;
  ['l-login', 'h-login'].forEach(id => $('#' + id).onclick = showLogin);
  ['l-reg', 'h-reg'].forEach(id => $('#' + id).onclick = showRegister);
}

function authShell(inner) {
  return `<div class="auth">
    <aside class="auth-side">
      <div>
        <div class="brand">${logo(38)}<span>zuv<b>o</b></span></div>
        <h2>Your people, your moments, live.</h2>
        <ul>
          <li>${ic('check', 18)} Post photos, reels and quotes</li>
          <li>${ic('check', 18)} Chat and call privately</li>
          <li>${ic('check', 18)} Go live and choose who joins</li>
        </ul>
      </div>
      <div class="sub hint" style="color:#C9C6FF">Adults aged 18 and over only.</div>
    </aside>
    <main class="auth-main"><div class="auth-card">${inner}</div></main>
  </div>`;
}

/* ---------- register ---------- */
function showRegister() {
  const max = new Date(); max.setFullYear(max.getFullYear() - 18);
  root().innerHTML = authShell(`
    <h1>Create your account</h1>
    <p>It takes a minute. We'll email you a 5-digit code to confirm it's you.</p>
    <form id="reg" novalidate>
      <div class="field"><label for="fullName">Full name</label><input id="fullName" name="fullName" autocomplete="name" placeholder="Amina Kamau" required></div>
      <div class="row2">
        <div class="field"><label for="dob">Date of birth</label><input id="dob" name="dob" type="date" max="${isoDate(max)}" required></div>
        <div class="field"><label for="gender">Gender</label><select id="gender" name="gender"><option value="">Select</option><option>Female</option><option>Male</option><option>Other</option><option>Prefer not to say</option></select></div>
      </div>
      <div class="hint" id="dobmsg" style="margin:-6px 0 12px">You must be 18 or older.</div>
      <div class="field"><label for="email">Email address</label><input id="email" name="email" type="email" autocomplete="email" placeholder="name@example.com" required></div>
      <div class="field"><label for="password">Password</label><div class="pw"><input id="password" name="password" type="password" autocomplete="new-password" placeholder="At least 8 characters" required><button type="button" id="pwt">Show</button></div></div>
      <div class="field"><label for="password2">Confirm password</label><input id="password2" name="password2" type="password" autocomplete="new-password" required></div>
      <div class="err" id="rerr" role="alert"></div>
      <button class="btn btn-primary btn-block" id="rbtn">Create account</button>
    </form>
    <div class="switch">Already have an account? <button id="to-login">Log in</button></div>`);
  $('#to-login').onclick = showLogin;
  $('#pwt').onclick = () => {
    const p = $('#password'), s = p.type === 'password';
    p.type = $('#password2').type = s ? 'text' : 'password';
    $('#pwt').textContent = s ? 'Hide' : 'Show';
  };
  $('#dob').addEventListener('change', () => {
    const m = $('#dobmsg'), v = $('#dob').value;
    if (!v) return;
    const ok = ageFrom(v) >= 18;
    m.textContent = ok ? 'Age confirmed.' : 'You must be 18 or older to create an account.';
    m.style.color = ok ? '#168A3E' : 'var(--danger)';
  });
  $('#reg').onsubmit = async e => {
    e.preventDefault();
    const f = e.target, err = $('#rerr'), btn = $('#rbtn');
    const body = { fullName: f.fullName.value.trim(), dateOfBirth: f.dob.value, gender: f.gender.value, email: f.email.value.trim(), password: f.password.value };
    err.textContent = '';
    if (body.fullName.length < 2) return err.textContent = 'Enter your full name.';
    if (!body.dateOfBirth) return err.textContent = 'Enter your date of birth.';
    if (ageFrom(body.dateOfBirth) < 18) return err.textContent = 'You must be 18 or older to create an account.';
    if (!body.gender) return err.textContent = 'Choose a gender.';
    if (!/^\S+@\S+\.\S+$/.test(body.email)) return err.textContent = 'Enter a valid email address.';
    if (body.password.length < 8) return err.textContent = 'Your password must be at least 8 characters.';
    if (body.password !== f.password2.value) return err.textContent = "The passwords don't match.";
    btn.disabled = true; btn.textContent = 'Creating account...';
    try {
      await api('/auth/register', { method: 'POST', json: body });
      Z.pendingEmail = body.email.toLowerCase();
      showVerify();
    } catch (ex) { err.textContent = ex.message; }
    btn.disabled = false; btn.textContent = 'Create account';
  };
}

/* ---------- 5-digit email code ---------- */
function showVerify() {
  const email = Z.pendingEmail || '';
  root().innerHTML = authShell(`
    <h1>Check your email</h1>
    <p>We sent a 5-digit code to <b>${esc(email)}</b>. Enter it below.</p>
    <div class="otp" id="otp">${[0, 1, 2, 3, 4].map(i => `<input inputmode="numeric" maxlength="1" aria-label="Digit ${i + 1}" ${i === 0 ? 'autocomplete="one-time-code"' : ''}>`).join('')}</div>
    <div class="hint" style="text-align:center" id="expiry">The code expires in 10:00</div>
    <div class="err" id="verr" role="alert" style="text-align:center;margin-top:10px"></div>
    <button class="btn btn-primary btn-block" id="vbtn">Verify</button>
    <div class="switch"><button id="resend" disabled style="opacity:.6">Resend code in 0:45</button></div>
    <div class="switch"><button id="to-reg">Use a different email</button></div>`);
  const boxes = $$('#otp input');
  boxes[0].focus();
  const code = () => boxes.map(b => b.value).join('');
  boxes.forEach((b, i) => {
    b.addEventListener('input', () => {
      b.value = b.value.replace(/\D/g, '').slice(-1);
      if (b.value && i < 4) boxes[i + 1].focus();
      if (code().length === 5) submit();
    });
    b.addEventListener('keydown', e => { if (e.key === 'Backspace' && !b.value && i > 0) boxes[i - 1].focus(); });
    b.addEventListener('paste', e => {
      const t = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 5);
      if (!t) return;
      e.preventDefault();
      t.split('').forEach((c, k) => boxes[k].value = c);
      boxes[Math.min(t.length, 4)].focus();
      if (t.length === 5) submit();
    });
  });
  let busy = false;
  async function submit() {
    if (busy) return;
    if (code().length < 5) { $('#verr').textContent = 'Enter all 5 digits.'; return; }
    busy = true; $('#verr').textContent = ''; $('#vbtn').textContent = 'Checking...';
    try {
      Z.me = await api('/auth/verify', { method: 'POST', json: { email, code: code() } });
      clearInterval(Z.cd);
      afterLogin();
    } catch (ex) {
      $('#verr').textContent = ex.message;
      boxes.forEach(b => b.value = ''); boxes[0].focus();
    }
    busy = false; const vb = $('#vbtn'); if (vb) vb.textContent = 'Verify';
  }
  $('#vbtn').onclick = submit;
  $('#to-reg').onclick = showRegister;
  // countdowns
  clearInterval(Z.cd);
  const end = Date.now() + 10 * 60000;
  let resendAt = Date.now() + 45000;
  Z.cd = setInterval(() => {
    if (!$('#expiry')) return clearInterval(Z.cd);
    const left = Math.max(0, end - Date.now()), s = Math.ceil(left / 1000);
    $('#expiry').textContent = left ? `The code expires in ${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}` : 'This code has expired. Request a new one.';
    const r = $('#resend'), w = Math.ceil((resendAt - Date.now()) / 1000);
    if (w > 0) { r.disabled = true; r.style.opacity = .6; r.textContent = `Resend code in 0:${String(w).padStart(2, '0')}`; }
    else { r.disabled = false; r.style.opacity = 1; r.textContent = 'Resend code'; }
  }, 500);
  $('#resend').onclick = async () => {
    try {
      await api('/auth/resend', { method: 'POST', json: { email } });
      toast('A new code is on its way.', 'good');
      resendAt = Date.now() + 45000;
    } catch (ex) { $('#verr').textContent = ex.message; }
  };
}

/* ---------- login ---------- */
function showLogin() {
  root().innerHTML = authShell(`
    <h1>Welcome back</h1>
    <p>Log in to see what your people are sharing.</p>
    <form id="login" novalidate>
      <div class="field"><label for="le">Email address</label><input id="le" type="email" autocomplete="email" required></div>
      <div class="field"><label for="lp">Password</label><div class="pw"><input id="lp" type="password" autocomplete="current-password" required><button type="button" id="lpt">Show</button></div></div>
      <div class="err" id="lerr" role="alert"></div>
      <button class="btn btn-primary btn-block" id="lbtn">Log in</button>
    </form>
    <div class="switch">New to Zuvo? <button id="to-reg">Create an account</button></div>`);
  $('#to-reg').onclick = showRegister;
  $('#lpt').onclick = () => { const p = $('#lp'), s = p.type === 'password'; p.type = s ? 'text' : 'password'; $('#lpt').textContent = s ? 'Hide' : 'Show'; };
  $('#login').onsubmit = async e => {
    e.preventDefault();
    const email = $('#le').value.trim(), err = $('#lerr'), btn = $('#lbtn');
    err.textContent = '';
    if (!email || !$('#lp').value) return err.textContent = 'Enter your email and password.';
    btn.disabled = true; btn.textContent = 'Logging in...';
    try {
      Z.me = await api('/auth/login', { method: 'POST', json: { email, password: $('#lp').value } });
      afterLogin();
      return;
    } catch (ex) {
      err.textContent = ex.message;
      if (ex.status === 403) {
        err.innerHTML = `${esc(ex.message)} <button type="button" class="btn btn-soft btn-sm" id="sendcode">Send me a code</button>`;
        $('#sendcode').onclick = async () => {
          try { await api('/auth/resend', { method: 'POST', json: { email } }); } catch (x) { /* a code may already exist */ }
          Z.pendingEmail = email.toLowerCase(); showVerify();
        };
      }
    }
    btn.disabled = false; btn.textContent = 'Log in';
  };
}

/* ---------- interests (feeds the Discover suggestions for new users) ---------- */
function showInterests() {
  const picked = new Set();
  root().innerHTML = authShell(`
    <h1>Pick what you like</h1>
    <p>Choose a few topics. Until you follow people, your feed shows videos and photos on these topics.</p>
    <div class="chips" id="chips">${CATEGORIES.map(c => `<button type="button" class="chip" data-c="${c}">${c}</button>`).join('')}</div>
    <div class="err" id="ierr" role="alert"></div>
    <button class="btn btn-primary btn-block" id="ibtn">Continue</button>`);
  $('#chips').onclick = e => {
    const b = e.target.closest('[data-c]'); if (!b) return;
    const c = b.dataset.c;
    picked.has(c) ? picked.delete(c) : picked.add(c);
    b.classList.toggle('on', picked.has(c));
  };
  $('#ibtn').onclick = async () => {
    if (!picked.size) return $('#ierr').textContent = 'Pick at least one topic.';
    try {
      await api('/me/interests', { method: 'POST', json: { categories: [...picked] } });
      Z.me.needsInterests = false;
      enterApp();
    } catch (ex) { $('#ierr').textContent = ex.message; }
  };
}

function afterLogin() {
  if (Z.me.needsInterests) showInterests(); else enterApp();
}
