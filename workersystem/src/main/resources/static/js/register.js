const form = document.getElementById("registerForm");
const msg = document.getElementById("message");
const pw = document.getElementById("password");
const bar = document.getElementById("strengthBar");
const strengthText = document.getElementById("strengthText");
const submitBtn = document.getElementById("submitBtn");

// Show / hide password
document.getElementById("togglePw").addEventListener("click", () => {
  pw.type = pw.type === "password" ? "text" : "password";
});

// Password strength meter
pw.addEventListener("input", () => {
  const v = pw.value;
  let score = 0;
  if (v.length >= 6) score++;
  if (v.length >= 10) score++;
  if (/[A-Z]/.test(v) && /[a-z]/.test(v)) score++;
  if (/\d/.test(v)) score++;
  if (/[^A-Za-z0-9]/.test(v)) score++;

  const levels = [
    { w: "0%",   c: "#e5e7eb", t: "" },
    { w: "25%",  c: "#c62828", t: "Weak" },
    { w: "50%",  c: "#f9a825", t: "Fair" },
    { w: "75%",  c: "#7cb342", t: "Good" },
    { w: "100%", c: "#2e7d32", t: "Strong" }
  ];
  const level = v.length === 0 ? 0 : Math.min(4, Math.max(1, score - 1));
  bar.style.width = levels[level].w;
  bar.style.background = levels[level].c;
  strengthText.textContent = levels[level].t;
});

function showMessage(text, ok) {
  msg.textContent = text;
  msg.className = ok ? "message success" : "message error";
}

// Submit the form to the Java backend
form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const data = {
    name: document.getElementById("name").value.trim(),
    idNumber: document.getElementById("idNumber").value.trim(),
    email: document.getElementById("email").value.trim(),
    password: pw.value,
    role: document.getElementById("role").value,
    section: document.getElementById("section").value.trim()
  };

  submitBtn.disabled = true;
  submitBtn.textContent = "Registering...";

  try {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    const body = await res.json();
    showMessage(body.message, res.ok);
    if (res.ok) {
      form.reset();
      bar.style.width = "0%";
      strengthText.textContent = "";
    }
  } catch (err) {
    showMessage("Could not reach the server. Is the application running?", false);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Register";
  }
});