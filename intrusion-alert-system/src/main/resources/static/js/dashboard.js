// Makes text safe to show on the page.
// Important: log content comes from outside, so an attacker could try to sneak in HTML.
function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
}

async function refreshStats() {
    const response = await fetch('/api/stats');
    const stats = await response.json();
    ['total', 'critical', 'high', 'medium', 'low'].forEach(key => {
        document.getElementById('stat-' + key).textContent = stats[key];
    });
}

async function refreshAlerts() {
    const response = await fetch('/api/alerts');
    const alerts = await response.json();
    const body = document.getElementById('alert-rows');

    if (alerts.length === 0) {
        body.innerHTML =
            '<tr><td colspan="7" class="text-center text-secondary py-4">No alerts yet. All quiet.</td></tr>';
        return;
    }

    body.innerHTML = alerts.map(alert => `
        <tr>
            <td>${escapeHtml(new Date(alert.lastSeen).toLocaleString())}</td>
            <td><span class="badge-sev ${escapeHtml(alert.severity)}">${escapeHtml(alert.severity)}</span></td>
            <td>${escapeHtml(alert.title)}</td>
            <td>${escapeHtml(alert.sourceIp)}</td>
            <td>${escapeHtml(alert.username)}</td>
            <td>${escapeHtml(alert.eventCount)}</td>
            <td>${escapeHtml(alert.status)}</td>
        </tr>`).join('');
}

async function refreshAll() {
    try {
        await Promise.all([refreshStats(), refreshAlerts()]);
        document.getElementById('last-updated').textContent =
            'Updated ' + new Date().toLocaleTimeString();
    } catch (error) {
        console.warn('Could not refresh dashboard', error);
    }
}

refreshAll();
setInterval(refreshAll, 3000);   // refresh every 3 seconds