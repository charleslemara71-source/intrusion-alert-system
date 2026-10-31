const form = document.getElementById("loginForm");
const msg = document.getElementById("message");
const pw = document.getElementById("password");
const submitBtn = document.getElementById("submitBtn");

document.getElementById("togglePw").addEventListener("click", () => {
  pw.type = pw.type === "password" ? "text" : "password";
});

function showMessage(text, ok) {
  msg.textContent = text;
  msg.className = ok ? "message success" : "message error";
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  submitBtn.disabled = true;
  submitBtn.textContent = "Logging in...";

  try {
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: document.getElementById("email").value.trim(),
        password: pw.value
      })
    });
    const body = await res.json();

    if (res.ok) {
      showMessage("Login successful! Redirecting...", true);
      setTimeout(() => {
        window.location.href = body.role === "ADMIN" ? "admin.html" : "dashboard.html";
      }, 600);
    } else {
      showMessage(body.message || "Login failed.", false);
    }
  } catch (err) {
    showMessage("Could not reach the server.", false);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Log in";
  }
});