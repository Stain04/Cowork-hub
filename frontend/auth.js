const API_BASE = "http://localhost:8080/api";
const USER_API = `${API_BASE}/users`;
const BOOKING_API = `${API_BASE}/bookings`;
const WORKSPACE_API = `${API_BASE}/workspaces`;
const REVIEW_API = `${API_BASE}/reviews`;
const INVOICE_API = `${API_BASE}/invoices`;

let availableWorkspaces = [];
let currentReviews = [];
let currentReviewOffset = 0;
const REVIEW_PAGE_SIZE = 5;

function getToken() {
    return localStorage.getItem("token");
}

function decodeToken(token = getToken()) {
    if (!token) return null;

    try {
        const payload = token.split(".")[1];
        const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
        return JSON.parse(decodeURIComponent(escape(atob(normalized))));
    } catch (error) {
        return null;
    }
}

function authHeaders() {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
}

async function requestJson(url, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    const response = await fetch(url, { ...options, headers });
    const text = await response.text();
    let body = null;

    if (text) {
        try {
            body = JSON.parse(text);
        } catch (error) {
            body = text;
        }
    }

    if (!response.ok) {
        const message = typeof body === "string"
            ? body
            : body?.message
                || body?.error
                || (body && typeof body === "object" ? Object.values(body).join(" ") : "")
                || "حدث خطأ أثناء تنفيذ الطلب";
        throw new Error(message);
    }

    return body;
}

function setMessage(id, text, type = "") {
    const element = document.getElementById(id);
    if (!element) return;
    element.textContent = text;
    element.className = `message ${type}`.trim();
}

function toggleForm() {
    document.getElementById("loginForm").classList.toggle("hidden");
    document.getElementById("registerForm").classList.toggle("hidden");
}

function logout() {
    localStorage.removeItem("token");
    window.location.href = "./index.htm";
}

function requireAuth() {
    const decoded = decodeToken();
    if (!decoded) {
        logout();
        return null;
    }

    const userElement = document.getElementById("displayUser");
    const roleElement = document.getElementById("displayRole");
    const adminLink = document.getElementById("adminLink");

    if (userElement) userElement.textContent = decoded.sub || "";
    if (roleElement) roleElement.textContent = decoded.role || "";
    if (adminLink && (decoded.role === "ADMIN" || decoded.role === "EMPLOYEE")) adminLink.classList.remove("hidden");

    return decoded;
}

function requireAdmin() {
    const decoded = decodeToken();
    if (!decoded || (decoded.role !== "ADMIN" && decoded.role !== "EMPLOYEE")) {
        window.location.href = "./dashboard.htm";
    }
}

function toLocalDateTimeValue(date) {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function toApiDateTime(value) {
    return value ? `${value}:00` : "";
}

function formatDateTime(value) {
    if (!value) return "-";
    return new Date(value).toLocaleString("ar-EG", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatCurrency(value) {
    const amount = Number(value || 0);
    return `${amount.toFixed(2)} جنيه`;
}

function calculateHours(startDate, endDate) {
    return (endDate.getTime() - startDate.getTime()) / 3600000;
}

function readBookingWindow() {
    const startValue = document.getElementById("searchStart").value;
    const endValue = document.getElementById("searchEnd").value;
    const startDate = startValue ? new Date(startValue) : null;
    const endDate = endValue ? new Date(endValue) : null;

    return {
        startValue,
        endValue,
        startDate,
        endDate,
        start: toApiDateTime(startValue),
        end: toApiDateTime(endValue)
    };
}

function validateBookingWindow() {
    const windowData = readBookingWindow();

    if (!windowData.startValue || !windowData.endValue) {
        throw new Error("اختار وقت البداية والنهاية.");
    }

    if (Number.isNaN(windowData.startDate?.getTime()) || Number.isNaN(windowData.endDate?.getTime())) {
        throw new Error("صيغة الوقت غير صحيحة.");
    }

    if (windowData.startDate.getTime() <= Date.now()) {
        throw new Error("وقت البداية لازم يكون في المستقبل.");
    }

    if (windowData.endDate.getTime() <= windowData.startDate.getTime()) {
        throw new Error("وقت النهاية لازم يكون بعد وقت البداية.");
    }

    return windowData;
}

function setDefaultSearchTimes() {
    const start = new Date();
    start.setHours(start.getHours() + 1, 0, 0, 0);
    const end = new Date(start);
    end.setHours(end.getHours() + 2);

    const startInput = document.getElementById("searchStart");
    const endInput = document.getElementById("searchEnd");
    const minValue = toLocalDateTimeValue(new Date());

    startInput.min = minValue;
    endInput.min = minValue;
    startInput.value = toLocalDateTimeValue(start);
    endInput.value = toLocalDateTimeValue(end);
}

function initDashboard() {
    setDefaultSearchTimes();
}

function initAdmin() {
    loadAllUsers();
    loadAllBookings();
    loadAllInvoices();
}

async function register() {
    setMessage("registerMessage", "جاري إنشاء الحساب...");

    try {
        await requestJson(`${USER_API}/register`, {
            method: "POST",
            body: JSON.stringify({
                username: document.getElementById("regUser").value.trim(),
                email: document.getElementById("regEmail").value.trim(),
                password: document.getElementById("regPass").value,
                role: "CUSTOMER"
            })
        });
        setMessage("registerMessage", "تم إنشاء الحساب. سجل الدخول الآن.", "success");
        toggleForm();
    } catch (error) {
        setMessage("registerMessage", error.message, "error");
    }
}

async function login() {
    setMessage("loginMessage", "جاري تسجيل الدخول...");

    try {
        const data = await requestJson(`${USER_API}/login`, {
            method: "POST",
            body: JSON.stringify({
                username: document.getElementById("loginUser").value.trim(),
                password: document.getElementById("loginPass").value
            })
        });
        localStorage.setItem("token", data.token);
        window.location.href = "./dashboard.htm";
    } catch (error) {
        setMessage("loginMessage", error.message, "error");
    }
}

async function loadAvailableWorkspaces() {
    const list = document.getElementById("workspaceList");
    setMessage("workspaceMessage", "جاري البحث...");
    list.innerHTML = "";

    try {
        const windowData = validateBookingWindow();
        const workspaces = await requestJson(
            `${WORKSPACE_API}/available?start=${encodeURIComponent(windowData.start)}&end=${encodeURIComponent(windowData.end)}`,
            { method: "GET" }
        );

        availableWorkspaces = workspaces;

        if (!workspaces.length) {
            setMessage("workspaceMessage", "لا توجد مساحات متاحة في هذا الوقت.", "error");
            return;
        }

        setMessage("workspaceMessage", `تم العثور على ${workspaces.length} مساحة متاحة.`, "success");
        list.innerHTML = workspaces.map(workspace => `
            <article class="item-card">
                <h3>${workspace.name}</h3>
                <div class="meta">
                    <span>رقم المساحة: ${workspace.id}</span>
                    <span>النوع: ${workspace.type}</span>
                    <span>السعة: ${workspace.capacity}</span>
                    <span>السعر: ${formatCurrency(workspace.pricePerHour)} / ساعة</span>
                </div>
                <p class="muted">${workspace.description || "لا يوجد وصف."}</p>
                <div class="action-row">
                    <button onclick="createBooking(${workspace.id})">احجز هذه المساحة</button>
                    <button class="secondary" onclick="loadReviewsForWorkspace(${workspace.id})">عرض التقييمات</button>
                </div>
            </article>
        `).join("");
    } catch (error) {
        setMessage("workspaceMessage", error.message, "error");
    }
}

async function createBooking(workspaceId) {
    const decoded = requireAuth();
    setMessage("bookingStatus", "جاري إنشاء الحجز...");

    try {
        const windowData = validateBookingWindow();
        const workspace = availableWorkspaces.find(item => item.id === workspaceId);
        if (!workspace) {
            throw new Error("المساحة غير متاحة الآن. اعمل بحث مرة أخرى.");
        }

        const hours = calculateHours(windowData.startDate, windowData.endDate);
        const total = hours * Number(workspace.pricePerHour || 0);
        const confirmed = window.confirm(
            `تأكيد الحجز\n\n` +
            `المساحة: ${workspace.name}\n` +
            `من: ${formatDateTime(windowData.startDate)}\n` +
            `إلى: ${formatDateTime(windowData.endDate)}\n` +
            `المدة: ${hours.toFixed(2)} ساعة\n` +
            `الإجمالي: ${formatCurrency(total)}`
        );

        if (!confirmed) {
            setMessage("bookingStatus", "تم إلغاء عملية الحجز.");
            return;
        }

        const booking = await requestJson(`${BOOKING_API}/create`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({
                userId: decoded.userId,
                workspaceId,
                startTime: windowData.start,
                endTime: windowData.end
            })
        });

        document.getElementById("bookingStatus").textContent = "تم إنشاء الحجز";
        document.getElementById("latestBooking").innerHTML = `
            <strong>${booking.workspaceName}</strong>
            <div class="meta">
                <span>رقم الحجز: ${booking.bookingId}</span>
                <span>من: ${formatDateTime(booking.startTime)}</span>
                <span>إلى: ${formatDateTime(booking.endTime)}</span>
                <span>الإجمالي: ${formatCurrency(booking.totalAmount)}</span>
                <span>الفاتورة: ${booking.invoiceNumber || "-"}</span>
                <span>الحالة: ${booking.status}</span>
            </div>
        `;
    } catch (error) {
        document.getElementById("bookingStatus").textContent = error.message;
    }
}

async function addReview() {
    const decoded = requireAuth();
    const workspaceId = Number(document.getElementById("reviewWorkspaceId").value);

    try {
        await requestJson(`${REVIEW_API}/add`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({
                userId: decoded.userId,
                workspaceId,
                rating: Number(document.getElementById("reviewRating").value),
                comment: document.getElementById("reviewComment").value.trim()
            })
        });
        setMessage("reviewMessage", "تم إرسال التقييم.", "success");
        await loadReviewsForWorkspace(workspaceId);
    } catch (error) {
        setMessage("reviewMessage", error.message, "error");
    }
}

async function loadReviewsFromInput() {
    const workspaceId = Number(document.getElementById("reviewWorkspaceId").value);
    await loadReviewsForWorkspace(workspaceId);
}

async function loadReviewsForWorkspace(workspaceId) {
    const list = document.getElementById("reviewList");
    if (!list) return;

    if (!workspaceId) {
        list.innerHTML = `<p class="message error">اكتب رقم مساحة صحيح أولًا.</p>`;
        return;
    }

    list.innerHTML = "جاري تحميل التقييمات...";

    try {
        const reviews = await requestJson(`${REVIEW_API}/workspace/${workspaceId}`, {
            method: "GET",
            headers: authHeaders()
        });

        const userIds = [...new Set(reviews.map(review => review.userId).filter(Boolean))];
        let usernameMap = {};
        if (userIds.length) {
            const query = userIds.map(id => `ids=${encodeURIComponent(id)}`).join("&");
            usernameMap = await requestJson(`${USER_API}/lookup?${query}`, {
                method: "GET",
                headers: authHeaders()
            });
        }

        currentReviews = reviews.map(review => ({
            ...review,
            username: usernameMap[review.userId] || `User #${review.userId}`
        }));
        currentReviewOffset = 0;

        if (!currentReviews.length) {
            list.innerHTML = "<p class='muted'>لا توجد تقييمات لهذه المساحة حتى الآن.</p>";
            return;
        }

        renderMoreReviews();
    } catch (error) {
        list.innerHTML = `<p class="message error">${error.message}</p>`;
    }
}

function renderMoreReviews() {
    const list = document.getElementById("reviewList");
    if (!list) return;

    const nextOffset = currentReviewOffset + REVIEW_PAGE_SIZE;
    const visibleReviews = currentReviews.slice(0, nextOffset);
    currentReviewOffset = visibleReviews.length;

    const cards = visibleReviews.map(review => `
        <article class="stack-item">
            <div class="row">
                <strong>${review.workspaceName}</strong>
                <span class="role-pill review-pill">${review.rating} / 5</span>
            </div>
            <span class="muted">المستخدم: ${review.username}</span>
            <p>${review.comment || "لا يوجد تعليق."}</p>
        </article>
    `).join("");

    const hasMore = currentReviewOffset < currentReviews.length;
    const moreButton = hasMore
        ? `<button class="secondary" onclick="renderMoreReviews()">عرض المزيد</button>`
        : "";

    list.innerHTML = cards + moreButton;
}

async function addEmployee() {
    setMessage("adminMessage", "جاري الإضافة...");

    try {
        await requestJson(`${USER_API}/admin/create-user`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({
                username: document.getElementById("empUser").value.trim(),
                email: document.getElementById("empEmail").value.trim(),
                password: document.getElementById("empPass").value,
                role: document.getElementById("empRole").value
            })
        });
        setMessage("adminMessage", "تمت إضافة المستخدم.", "success");
        loadAllUsers();
    } catch (error) {
        setMessage("adminMessage", error.message, "error");
    }
}

async function loadAllUsers() {
    const list = document.getElementById("userList");
    if (!list) return;
    list.innerHTML = "جاري التحميل...";

    const decoded = decodeToken();

    try {
        const users = await requestJson(USER_API, {
            method: "GET",
            headers: authHeaders()
        });
        list.innerHTML = users.map(user => `
            <article class="stack-item">
                <div class="row">
                    <strong>${user.username}</strong>
                    <span class="role-pill">${user.role}</span>
                </div>
                <span class="muted">${user.email}</span>
                ${decoded && decoded.role === "ADMIN" && user.role !== "ADMIN" ? `<button class="danger" onclick="deleteUser(${user.id})">حذف</button>` : ""}
            </article>
        `).join("");
    } catch (error) {
        list.innerHTML = `<p class="message error">${error.message}</p>`;
    }
}

async function deleteUser(userId) {
    if (!confirm("هل تريد حذف هذا المستخدم؟")) return;

    try {
        await fetch(`${USER_API}/admin/delete/${userId}`, {
            method: "DELETE",
            headers: authHeaders()
        });
        loadAllUsers();
    } catch (error) {
        setMessage("adminMessage", error.message, "error");
    }
}

async function addWorkspace() {
    setMessage("workspaceAdminMessage", "جاري إضافة المساحة...");

    try {
        await requestJson(`${WORKSPACE_API}/add`, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({
                name: document.getElementById("workspaceName").value.trim(),
                type: document.getElementById("workspaceType").value,
                pricePerHour: Number(document.getElementById("workspacePrice").value),
                capacity: Number(document.getElementById("workspaceCapacity").value),
                description: document.getElementById("workspaceDescription").value.trim(),
                available: true
            })
        });
        setMessage("workspaceAdminMessage", "تمت إضافة المساحة.", "success");
    } catch (error) {
        setMessage("workspaceAdminMessage", error.message, "error");
    }
}

async function loadAllBookings() {
    const list = document.getElementById("bookingList");
    if (!list) return;
    list.innerHTML = "جاري التحميل...";

    try {
        const bookings = await requestJson(`${BOOKING_API}/all`, {
            method: "GET",
            headers: authHeaders()
        });
        list.innerHTML = bookings.map(booking => `
            <article class="stack-item">
                <h3>${booking.workspaceName}</h3>
                <div class="meta">
                    <span>رقم الحجز: ${booking.bookingId}</span>
                    <span>من: ${formatDateTime(booking.startTime)}</span>
                    <span>إلى: ${formatDateTime(booking.endTime)}</span>
                    <span>الحالة: ${booking.status}</span>
                    <span>الإجمالي: ${formatCurrency(booking.totalAmount)}</span>
                </div>
                ${booking.status !== "CANCELLED" ? `<button class="danger" onclick="cancelBooking(${booking.bookingId})">إلغاء الحجز</button>` : `<span class="muted">تم إلغاء هذا الحجز</span>`}
            </article>
        `).join("") || "<p class='muted'>لا توجد حجوزات.</p>";
    } catch (error) {
        list.innerHTML = `<p class="message error">${error.message}</p>`;
    }
}

async function cancelBooking(bookingId) {
    if (!confirm("هل تريد إلغاء هذا الحجز؟")) return;

    try {
        await requestJson(`${BOOKING_API}/${bookingId}/cancel`, {
            method: "PATCH",
            headers: authHeaders()
        });
        loadAllBookings();
    } catch (error) {
        alert(error.message);
    }
}

async function loadAllInvoices() {
    const list = document.getElementById("invoiceList");
    if (!list) return;
    list.innerHTML = "جاري التحميل...";

    try {
        const invoices = await requestJson(`${INVOICE_API}/all`, {
            method: "GET",
            headers: authHeaders()
        });
        list.innerHTML = invoices.map(invoice => `
            <article class="stack-item">
                <div class="row">
                    <strong>${invoice.invoiceNumber}</strong>
                    <span class="role-pill ${invoice.paymentStatus === "PAID" ? "paid-pill" : ""}">${invoice.paymentStatus}</span>
                </div>
                <div class="meta">
                    <span>رقم الحجز: ${invoice.bookingId}</span>
                    <span>القيمة: ${formatCurrency(invoice.amount)}</span>
                    <span>تاريخ الإصدار: ${formatDateTime(invoice.issuedAt)}</span>
                </div>
                ${invoice.paymentStatus !== "PAID" ? `<button onclick="markInvoicePaid(${invoice.id})">تحويل إلى PAID</button>` : `<span class="muted">تم السداد</span>`}
            </article>
        `).join("") || "<p class='muted'>لا توجد فواتير.</p>";
    } catch (error) {
        list.innerHTML = `<p class="message error">${error.message}</p>`;
    }
}

async function markInvoicePaid(invoiceId) {
    if (!confirm("تأكيد تحويل حالة الفاتورة إلى PAID؟")) return;

    try {
        await requestJson(`${INVOICE_API}/${invoiceId}/payment-status?status=PAID`, {
            method: "PATCH",
            headers: authHeaders()
        });
        loadAllInvoices();
    } catch (error) {
        alert(error.message);
    }
}
