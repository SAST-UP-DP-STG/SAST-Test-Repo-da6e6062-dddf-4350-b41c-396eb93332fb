const TOKEN_KEY = "fsd.token";
const NAME_KEY = "fsd.name";
const ROLE_KEY = "fsd.role";

function saveSession(data) {
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(NAME_KEY, data.fullName);
    localStorage.setItem(ROLE_KEY, data.role);
}

function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    localStorage.removeItem(ROLE_KEY);
}

function token() {
    return localStorage.getItem(TOKEN_KEY);
}

function authHeaders() {
    const t = token();
    return t ? { "Authorization": "Bearer " + t } : {};
}

function renderSession() {
    const el = document.getElementById("session");
    if (!el) {
        return;
    }
    const name = localStorage.getItem(NAME_KEY);
    const role = localStorage.getItem(ROLE_KEY);
    if (name) {
        el.textContent = name + " (" + role + ")";
    } else {
        el.textContent = "Not signed in";
    }
}

function logout() {
    clearSession();
    window.location.href = "/login";
}

function statusBadge(status) {
    const cls = (status || "").toLowerCase();
    return '<span class="badge ' + cls + '">' + status + "</span>";
}

async function login(event) {
    event.preventDefault();
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const errorEl = document.getElementById("login-error");
    errorEl.textContent = "";

    const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
    });

    if (!response.ok) {
        errorEl.textContent = "Invalid username or password.";
        return;
    }

    const data = await response.json();
    saveSession(data);
    window.location.href = "/dashboard";
}

async function loadDashboard() {
    renderSession();
    if (!token()) {
        window.location.href = "/login";
        return;
    }

    const summary = await fetch("/api/dashboard/summary", { headers: authHeaders() });
    if (summary.status === 401) {
        logout();
        return;
    }
    const metrics = await summary.json();
    document.getElementById("m-open").textContent = metrics.openWorkOrders;
    document.getElementById("m-total").textContent = metrics.totalWorkOrders;
    document.getElementById("m-tech").textContent = metrics.availableTechnicians;
    document.getElementById("m-parts").textContent = metrics.partsBelowReorder;

    const orders = await (await fetch("/api/work-orders", { headers: authHeaders() })).json();
    const orderBody = document.getElementById("work-orders");
    orderBody.innerHTML = orders.map(o =>
        "<tr><td>" + o.reference + "</td><td>" + o.customerName + "</td><td>" + o.priority +
        "</td><td>" + statusBadge(o.status) + "</td><td>" + (o.assignedTechnicianId || "—") + "</td></tr>"
    ).join("");

    const techs = await (await fetch("/api/technicians", { headers: authHeaders() })).json();
    const techBody = document.getElementById("technicians");
    techBody.innerHTML = techs.map(t =>
        "<tr><td>" + t.id + "</td><td>" + t.name + "</td><td>" + t.skill + "</td><td>" + t.region +
        "</td><td>" + (t.available ? "Available" : "Busy") + "</td></tr>"
    ).join("");

    const parts = await (await fetch("/api/parts", { headers: authHeaders() })).json();
    const partsBody = document.getElementById("parts");
    partsBody.innerHTML = parts.map(p =>
        "<tr><td>" + p.sku + "</td><td>" + p.name + "</td><td>" + p.quantityOnHand + "</td><td>" + p.location +
        "</td><td>" + p.reorderLevel + "</td></tr>"
    ).join("");
}

async function closeWorkOrder(event) {
    event.preventDefault();
    const workOrderId = document.getElementById("close-id").value;
    const resolution = document.getElementById("close-resolution").value;
    const response = await fetch("/integration/work-orders/close", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ workOrderId, resolution })
    });
    showResult("close-result", response);
}

async function releaseDispatch(event) {
    event.preventDefault();
    const workOrderId = document.getElementById("release-id").value;
    const technicianId = document.getElementById("release-tech").value;
    const response = await fetch("/integration/dispatch/release", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ workOrderId, technicianId })
    });
    showResult("release-result", response);
}

async function exportCustomers() {
    const response = await fetch("/reports/customers/export");
    const text = await response.text();
    document.getElementById("export-result").textContent = text;
}

async function recountParts(event) {
    event.preventDefault();
    const fileInput = document.getElementById("recount-file");
    if (!fileInput.files.length) {
        document.getElementById("recount-result").textContent = "Select a CSV file first.";
        return;
    }
    const form = new FormData();
    form.append("file", fileInput.files[0]);
    const response = await fetch("/tools/parts/recount", { method: "POST", body: form });
    showResult("recount-result", response);
}

async function showResult(elementId, response) {
    const el = document.getElementById(elementId);
    const text = await response.text();
    try {
        el.textContent = JSON.stringify(JSON.parse(text), null, 2);
    } catch (e) {
        el.textContent = text;
    }
}

async function loadAdmin() {
    renderSession();
    if (!token()) {
        window.location.href = "/login";
        return;
    }
    const response = await fetch("/admin/operators", { headers: authHeaders() });
    if (response.status === 401) {
        logout();
        return;
    }
    const operators = await response.json();
    const body = document.getElementById("operators");
    body.innerHTML = operators.map(o =>
        "<tr><td>" + o.id + "</td><td>" + o.username + "</td><td>" + o.fullName + "</td><td>" + o.role +
        "</td><td>" + o.createdAt + "</td></tr>"
    ).join("");
}
