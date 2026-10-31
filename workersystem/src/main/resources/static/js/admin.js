let workers = [];
const tbody = document.getElementById("tbody");
const modal = document.getElementById("modal");
const editForm = document.getElementById("editForm");
const toast = document.getElementById("toast");

// Escape text so names like <script> can't run in the page
function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function showToast(text, isError) {
  toast.textContent = text;
  toast.className = "toast show" + (isError ? " error" : "");
  setTimeout(() => (toast.className = "toast"), 3000);
}

async function api(url, options = {}) {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json" },
    ...options
  });
  if (res.status === 401) { window.location.href = "login.html"; throw new Error("Please log in."); }
  if (res.status === 403) { window.location.href = "dashboard.html"; throw new Error("Admins only."); }
  let body = {};
  try { body = await res.json(); } catch (e) {}
  if (!res.ok) throw new Error(body.message || "Request failed.");
  return body;
}

function render(list) {
  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" class="empty">No workers found.</td></tr>`;
    return;
  }
  tbody.innerHTML = list.map(w => `
    <tr>
      <td>${w.id}</td>
      <td>${esc(w.fullName)}</td>
      <td>${esc(w.idNumber)}</td>
      <td>${esc(w.email)}</td>
      <td><span class="badge">${esc(w.role.replaceAll("_", " "))}</span></td>
      <td>${esc(w.section) || "—"}</td>
      <td>${w.salary != null ? Number(w.salary).toLocaleString() : "—"}</td>
      <td>${new Date(w.registeredAt).toLocaleDateString()}</td>
      <td class="actions">
        <button class="btn btn-primary btn-sm" data-edit="${w.id}">Edit</button>
        <button class="btn btn-danger btn-sm" data-delete="${w.id}">Delete</button>
      </td>
    </tr>`).join("");
}

function updateStats() {
  document.getElementById("statTotal").textContent = workers.length;
  const sections = new Set(workers.map(w => (w.section || "").trim().toLowerCase()).filter(Boolean));
  document.getElementById("statSections").textContent = sections.size;
  const leaders = workers.filter(w => ["SECTION_LEADER", "MANAGER", "ASSISTANT_MANAGER"].includes(w.role));
  document.getElementById("statLeaders").textContent = leaders.length;
}

async function loadWorkers() {
  workers = await api("/api/admin/workers");
  applyFilter();
  updateStats();
}

function applyFilter() {
  const q = document.getElementById("search").value.trim().toLowerCase();
  const filtered = workers.filter(w =>
    [w.fullName, w.idNumber, w.email, w.role, w.section]
      .some(v => String(v ?? "").toLowerCase().includes(q)));
  render(filtered);
}

// Open the edit window
function openEdit(id) {
  const w = workers.find(x => x.id === id);
  if (!w) return;
  document.getElementById("editId").value = w.id;
  document.getElementById("editName").value = w.fullName;
  document.getElementById("editIdNumber").value = w.idNumber;
  document.getElementById("editEmail").value = w.email;
  document.getElementById("editRole").value = w.role;
  document.getElementById("editSection").value = w.section || "";
  document.getElementById("editSalary").value = w.salary ?? "";
  document.getElementById("editPassword").value = "";
  modal.classList.add("open");
}

function closeEdit() { modal.classList.remove("open"); }

// Table button clicks (edit / delete)
tbody.addEventListener("click", async (e) => {
  const editId = e.target.dataset.edit;
  const delId = e.target.dataset.delete;

  if (editId) openEdit(Number(editId));

  if (delId) {
    const w = workers.find(x => x.id === Number(delId));
    if (!confirm(`Delete ${w.fullName}? This cannot be undone.`)) return;
    try {
      await api(`/api/admin/workers/${delId}`, { method: "DELETE" });
      showToast("Worker deleted.");
      await loadWorkers();
    } catch (err) { showToast(err.message, true); }
  }
});

// Save changes
editForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const id = document.getElementById("editId").value;
  const salaryText = document.getElementById("editSalary").value;

  const data = {
    fullName: document.getElementById("editName").value.trim(),
    idNumber: document.getElementById("editIdNumber").value.trim(),
    email: document.getElementById("editEmail").value.trim(),
    role: document.getElementById("editRole").value,
    section: document.getElementById("editSection").value.trim(),
    salary: salaryText === "" ? null : Number(salaryText),
    newPassword: document.getElementById("editPassword").value
  };

  try {
    await api(`/api/admin/workers/${id}`, { method: "PUT", body: JSON.stringify(data) });
    closeEdit();
    showToast("Worker updated.");
    await loadWorkers();
  } catch (err) { showToast(err.message, true); }
});

document.getElementById("cancelBtn").addEventListener("click", closeEdit);
modal.addEventListener("click", (e) => { if (e.target === modal) closeEdit(); });
document.getElementById("search").addEventListener("input", applyFilter);

document.getElementById("logoutBtn").addEventListener("click", async () => {
  await fetch("/api/auth/logout", { method: "POST" });
  window.location.href = "login.html";
});

// Start
(async () => {
  try {
    const me = await api("/api/auth/me");
    document.getElementById("adminName").textContent = me.fullName;
    await loadWorkers();
  } catch (err) { /* api() already redirects when not logged in */ }
})();