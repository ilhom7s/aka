"use strict";

/* ------------------------------------------------------------------
 * Базовый адрес API.
 * Если фронт раздаётся самим Spring (http://localhost:8081) — оставляем '',
 * запросы идут на тот же origin. Если открыли index.html как файл или
 * через Live Server — стучимся напрямую в бэкенд на 8081.
 * ------------------------------------------------------------------ */
const API = (location.protocol === "file:" || location.port === "5500")
    ? "http://localhost:8081"
    : "";

/* ------------------------- авторизация ------------------------- */
function getToken() { return localStorage.getItem("aka_token"); }
function clearAuth() { localStorage.removeItem("aka_token"); localStorage.removeItem("aka_role"); }
function logout() { clearAuth(); location.href = "login.html"; }

// нет токена — сразу на страницу входа
if (!getToken()) {
    location.replace("login.html");
}

const MIN_IMAGES = 3;
const MAX_IMAGES = 5;

/* Текущий набор выбранных файлов (для формы) */
let selectedFiles = [];

/* ------------------------- утилиты сети ------------------------- */

async function api(path, options = {}) {
    const opts = { ...options };
    opts.headers = { ...(options.headers || {}) };
    const token = getToken();
    if (token) opts.headers["Authorization"] = "Bearer " + token;

    const res = await fetch(API + path, opts);

    if (res.status === 401) {
        clearAuth();
        location.href = "login.html";
        throw new Error("Требуется вход");
    }
    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(text || `Ошибка ${res.status}`);
    }
    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json() : res.text();
}

function imgSrc(url) {
    if (!url) return "";
    return url.startsWith("http") ? url : API + url;
}

/* ------------------------- уведомления ------------------------- */

function toast(message, type = "ok") {
    const box = document.getElementById("toasts");
    const el = document.createElement("div");
    el.className = "toast toast--" + type;
    el.textContent = message;
    box.appendChild(el);
    setTimeout(() => el.remove(), 3600);
}

/* ------------------------- навигация ------------------------- */

document.querySelectorAll(".nav__item").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".nav__item").forEach(b => b.classList.remove("is-active"));
        document.querySelectorAll(".view").forEach(v => v.classList.remove("is-active"));
        btn.classList.add("is-active");
        const view = btn.dataset.view;
        document.getElementById("view-" + view).classList.add("is-active");
        if (view === "products") loadProducts();
        if (view === "belts") loadBelts();
        if (view === "orders") loadOrders(currentOrderFilter);
        if (view === "users") loadUsers();
    });
});

/* ========================= ТОВАРЫ ========================= */

async function loadProducts() {
    const grid = document.getElementById("products-grid");
    grid.innerHTML = `<div class="placeholder">Загружаем товары…</div>`;
    try {
        const products = await api("/admin/allgoods");
        if (!products.length) {
            grid.innerHTML = `<div class="placeholder">Товаров пока нет. Нажмите «Добавить товар», чтобы создать первую карточку.</div>`;
            return;
        }
        grid.innerHTML = "";
        products.forEach(p => grid.appendChild(productCard(p)));
    } catch (e) {
        grid.innerHTML = `<div class="placeholder">Не удалось загрузить товары: ${e.message}</div>`;
    }
}

function productCard(p) {
    const card = document.createElement("article");
    card.className = "card";

    const images = (p.images && p.images.length)
        ? p.images
        : (p.imageUrl ? [{ imageUrl: p.imageUrl }] : []);

    const gallery = images.map(im =>
        `<img src="${imgSrc(im.imageUrl)}" alt="${escapeHtml(p.productName || "")}" loading="lazy">`
    ).join("");

    const chips = [
        p.color && `<span class="chip">${escapeHtml(p.color)}</span>`,
        p.size && `<span class="chip">Размер ${escapeHtml(p.size)}</span>`,
        p.material && `<span class="chip">${escapeHtml(p.material)}</span>`,
        (p.plotnost != null) && `<span class="chip">${p.plotnost} г/м²</span>`,
    ].filter(Boolean).join("");

    card.innerHTML = `
        <div class="card__gallery">${gallery || '<span class="card__id" style="padding:34px 12px">нет фото</span>'}</div>
        <div class="card__body">
            <h3 class="card__name">${escapeHtml(p.productName || "Без названия")}</h3>
            <span class="card__id">Артикул: ${escapeHtml(p.productId || "—")}</span>
            <span class="card__price">${formatPrice(p.price)} ₽</span>
            <div class="chips">${chips}</div>
            ${p.description ? `<p class="card__desc">${escapeHtml(p.description)}</p>` : ""}
            ${quickHtml(p)}
            <div class="card__foot">
                <button class="icon-btn" data-act="edit">Изменить</button>
                <button class="icon-btn icon-btn--danger" data-act="del">Удалить</button>
            </div>
        </div>`;

    bindQuick(card, "PRODUCT", p.id);
    card.querySelector('[data-act="edit"]').addEventListener("click", () => openForm(p));
    card.querySelector('[data-act="del"]').addEventListener("click", () => deleteProduct(p.id, p.productName));

    return card;
}

async function updateQuantity(id, quantity) {
    try {
        await api(`/admin/update/${id}?quantity=${encodeURIComponent(quantity)}`, { method: "PATCH" });
        toast("Количество обновлено");
    } catch (e) {
        toast(e.message, "error");
    }
}

async function deleteProduct(id, name) {
    if (!confirm(`Удалить товар «${name || id}»?`)) return;
    try {
        await api(`/admin/delete/${id}`, { method: "DELETE" });
        toast("Товар удалён");
        loadProducts();
    } catch (e) {
        toast(e.message, "error");
    }
}

/* ------------------------- форма товара ------------------------- */

const modal = document.getElementById("modal");
const form = document.getElementById("product-form");

document.getElementById("btn-add").addEventListener("click", () => openForm(null));
modal.querySelectorAll("[data-close]").forEach(el => el.addEventListener("click", closeForm));
document.addEventListener("keydown", e => { if (e.key === "Escape") closeForm(); });

function openForm(product) {
    form.reset();
    selectedFiles = [];
    renderPreviews();

    const isEdit = !!product;
    document.getElementById("form-title").textContent = isEdit ? "Изменить товар" : "Новый товар";
    document.getElementById("submit-btn").textContent = isEdit ? "Сохранить изменения" : "Создать товар";
    document.getElementById("edit-note").hidden = !isEdit;
    document.getElementById("f-id").value = isEdit ? product.id : "";

    if (isEdit) {
        setVal("f-productId", product.productId);
        setVal("f-productName", product.productName);
        setVal("f-description", product.description);
        setVal("f-color", product.color);
        setVal("f-size", product.size);
        setVal("f-material", product.material);
        setVal("f-plotnost", product.plotnost);
        setVal("f-price", product.price);
        setVal("f-quantity", product.quantity);
    }
    updateCounter();
    modal.classList.add("is-open");
    modal.setAttribute("aria-hidden", "false");
}

function closeForm() {
    modal.classList.remove("is-open");
    modal.setAttribute("aria-hidden", "true");
}

/* --- выбор и предпросмотр файлов --- */

const fileInput = document.getElementById("f-images");
const dropzone = document.getElementById("dropzone");

fileInput.addEventListener("change", () => addFiles(fileInput.files));

["dragover", "dragenter"].forEach(ev =>
    dropzone.addEventListener(ev, e => { e.preventDefault(); dropzone.classList.add("is-drag"); }));
["dragleave", "drop"].forEach(ev =>
    dropzone.addEventListener(ev, e => { e.preventDefault(); dropzone.classList.remove("is-drag"); }));
dropzone.addEventListener("drop", e => addFiles(e.dataTransfer.files));

function addFiles(fileList) {
    for (const f of fileList) {
        if (!f.type.startsWith("image/")) continue;
        if (selectedFiles.length >= MAX_IMAGES) {
            toast(`Максимум ${MAX_IMAGES} фото`, "error");
            break;
        }
        selectedFiles.push(f);
    }
    fileInput.value = "";
    renderPreviews();
    updateCounter();
}

function renderPreviews() {
    const box = document.getElementById("previews");
    box.innerHTML = "";
    selectedFiles.forEach((file, i) => {
        const div = document.createElement("div");
        div.className = "preview";
        const url = URL.createObjectURL(file);
        div.innerHTML = `<img src="${url}" alt=""><button type="button" class="preview__rm" aria-label="Убрать">×</button>`;
        div.querySelector("button").addEventListener("click", () => {
            selectedFiles.splice(i, 1);
            renderPreviews();
            updateCounter();
        });
        box.appendChild(div);
    });
}

function updateCounter() {
    const el = document.getElementById("img-count");
    const n = selectedFiles.length;
    el.textContent = `${n} из ${MIN_IMAGES}–${MAX_IMAGES}`;
    el.classList.toggle("is-ok", n >= MIN_IMAGES && n <= MAX_IMAGES);
    el.classList.toggle("is-bad", n > 0 && (n < MIN_IMAGES || n > MAX_IMAGES));
}

/* --- отправка формы --- */

form.addEventListener("submit", async e => {
    e.preventDefault();
    const id = document.getElementById("f-id").value;
    const isEdit = !!id;

    // Валидация фото: на создании обязательно 3–5, на изменении — либо 0 (оставить), либо 3–5
    const n = selectedFiles.length;
    if (!isEdit && (n < MIN_IMAGES || n > MAX_IMAGES)) {
        toast(`Нужно выбрать от ${MIN_IMAGES} до ${MAX_IMAGES} фото`, "error");
        return;
    }
    if (isEdit && n !== 0 && (n < MIN_IMAGES || n > MAX_IMAGES)) {
        toast(`Либо оставьте фото пустыми, либо выберите ${MIN_IMAGES}–${MAX_IMAGES}`, "error");
        return;
    }

    const product = {
        productId: getVal("f-productId"),
        productName: getVal("f-productName"),
        description: getVal("f-description"),
        color: getVal("f-color"),
        size: getVal("f-size"),
        material: getVal("f-material"),
        plotnost: numOrNull(getVal("f-plotnost")),
        price: numOrNull(getVal("f-price")),
        quantity: numOrNull(getVal("f-quantity")),
    };

    const fd = new FormData();
    fd.append("product", new Blob([JSON.stringify(product)], { type: "application/json" }));
    selectedFiles.forEach(f => fd.append("images", f));

    const submitBtn = document.getElementById("submit-btn");
    submitBtn.disabled = true;
    try {
        if (isEdit) {
            await api(`/admin/products/${id}`, { method: "PUT", body: fd });
            toast("Товар обновлён");
        } else {
            await api("/admin/products", { method: "POST", body: fd });
            toast("Товар создан");
        }
        closeForm();
        loadProducts();
    } catch (err) {
        toast(err.message, "error");
    } finally {
        submitBtn.disabled = false;
    }
});

/* ========================= ЗАКАЗЫ ========================= */

let currentOrderFilter = "all";

document.querySelectorAll("#orders-filter .segmented__btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll("#orders-filter .segmented__btn").forEach(b => b.classList.remove("is-active"));
        btn.classList.add("is-active");
        currentOrderFilter = btn.dataset.filter;
        loadOrders(currentOrderFilter);
    });
});

async function loadOrders(filter) {
    const list = document.getElementById("orders-list");
    list.innerHTML = `<div class="placeholder">Загружаем заказы…</div>`;
    const path = filter === "delivered" ? "/admin/delivered"
        : filter === "orders" ? "/admin/orders"
            : "/admin/all";
    try {
        const orders = await api(path);
        if (!orders.length) {
            list.innerHTML = `<div class="placeholder">Здесь пока нет заказов.</div>`;
            return;
        }
        list.innerHTML = "";
        orders.forEach(o => list.appendChild(orderCard(o)));
    } catch (e) {
        list.innerHTML = `<div class="placeholder">Не удалось загрузить заказы: ${e.message}</div>`;
    }
}

const STATUS_LABELS = {
    PENDING_PAYMENT: "Ожидает оплаты",
    PAID: "Оплачен",
    DELIVERED: "Доставлен",
    CANCELLED: "Отменён",
};

function orderCard(o) {
    const el = document.createElement("div");
    el.className = "order";
    const status = (o.status || "").toLowerCase();

    // Карточки товаров, которые заказали
    const cards = (o.orderItems || []).map(it => {
        const pv = it.productVariant;
        const name = pv ? pv.productName : ("Вариант #" + it.variantId);
        const cover = pv ? (pv.imageUrl || (pv.images && pv.images[0] && pv.images[0].imageUrl)) : null;
        const chips = pv ? [
            pv.color && `<span class="chip">${escapeHtml(pv.color)}</span>`,
            pv.size && `<span class="chip">Размер ${escapeHtml(pv.size)}</span>`,
            pv.material && `<span class="chip">${escapeHtml(pv.material)}</span>`,
        ].filter(Boolean).join("") : "";
        return `<div class="oi-card">
            <div class="oi-card__img">${cover ? `<img src="${imgSrc(cover)}" alt="" loading="lazy">` : "нет фото"}</div>
            <div class="oi-card__body">
                <div class="oi-card__name">${escapeHtml(name || "")}</div>
                <div class="chips">${chips}</div>
                <div class="oi-card__price">${it.quantity ?? 0} × ${formatPrice(it.amount)} ₽ = <b>${formatPrice(it.lineTotal)} ₽</b></div>
            </div>
        </div>`;
    }).join("");

    const options = Object.keys(STATUS_LABELS).map(key =>
        `<option value="${key}" ${key === o.status ? "selected" : ""}>${STATUS_LABELS[key]}</option>`
    ).join("");

    el.innerHTML = `
        <div class="order__head">
            <button class="order__toggle" aria-label="Раскрыть">▸</button>
            <span class="order__id">Заказ #${o.id}</span>
            <select class="order__status status--${status}" title="Изменить статус">${options}</select>
            <span class="order__total">${formatPrice(o.totalAmount)} ₽</span>
        </div>
        <div class="order__meta">
            <span>Почта: ${escapeHtml(o.userEmail || "—")}</span>
            <span>Телефон: ${escapeHtml(o.phone || "—")}</span>
            <span>Адрес: ${escapeHtml(o.address || "—")}</span>
            ${o.createdAt ? `<span>Создан: ${formatDate(o.createdAt)}</span>` : ""}
        </div>
        <div class="order__details" hidden>
            ${cards || '<div class="oi-empty">В заказе нет позиций.</div>'}
        </div>`;

    // Раскрытие/сворачивание карточек товаров
    const details = el.querySelector(".order__details");
    const toggle = el.querySelector(".order__toggle");
    const head = el.querySelector(".order__head");
    head.addEventListener("click", e => {
        if (e.target.closest(".order__status")) return; // клик по селекту не сворачивает
        const open = details.hidden;
        details.hidden = !open;
        toggle.textContent = open ? "▾" : "▸";
    });

    // Смена статуса
    const select = el.querySelector(".order__status");
    select.addEventListener("click", e => e.stopPropagation());
    select.addEventListener("change", async () => {
        const value = select.value;
        try {
            await api(`/admin/orders/${o.id}/status?status=${encodeURIComponent(value)}`, { method: "PATCH" });
            select.className = "order__status status--" + value.toLowerCase();
            toast(`Статус заказа #${o.id}: ${STATUS_LABELS[value]}`);
        } catch (err) {
            toast(err.message, "error");
            select.value = o.status; // откат при ошибке
        }
    });

    return el;
}

/* ========================= ПОЛЬЗОВАТЕЛИ ========================= */

async function loadUsers() {
    const tbody = document.querySelector("#users-table tbody");
    tbody.innerHTML = `<tr><td colspan="3">Загружаем…</td></tr>`;
    try {
        const users = await api("/admin/users");
        if (!users.length) {
            tbody.innerHTML = `<tr><td colspan="3">Пользователей нет.</td></tr>`;
            return;
        }
        tbody.innerHTML = "";
        users.forEach(u => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${u.id}</td><td>${escapeHtml(u.email || "—")}</td><td>${escapeHtml(u.userPhone || "—")}</td>`;
            tbody.appendChild(tr);
        });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="3">Ошибка: ${e.message}</td></tr>`;
    }
}

/* ========================= помощники ========================= */

function setVal(id, v) { document.getElementById(id).value = (v ?? ""); }
function getVal(id) { return document.getElementById(id).value.trim(); }
function numOrNull(v) { return v === "" || v == null ? null : Number(v); }

function formatPrice(v) {
    if (v == null) return "0";
    return Number(v).toLocaleString("ru-RU", { maximumFractionDigits: 2 });
}

function formatDate(iso) {
    try { return new Date(iso).toLocaleString("ru-RU"); }
    catch { return iso; }
}

function escapeHtml(s) {
    return String(s)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

/* ------------------------- старт ------------------------- */

async function checkConnection() {
    const dot = document.getElementById("conn-dot");
    const text = document.getElementById("conn-text");
    try {
        await api("/admin/allgoods");
        dot.classList.add("is-ok");
        text.textContent = "Бэкенд на связи";
    } catch {
        dot.classList.add("is-bad");
        text.textContent = "Бэкенд недоступен";
    }
}

const logoutBtn = document.getElementById("logout-btn");
if (logoutBtn) logoutBtn.addEventListener("click", logout);

checkConnection();
loadProducts();


/* ========================= ПОЯСА ========================= */

let beltFiles = [];
const beltModal = document.getElementById("belt-modal");
const beltForm = document.getElementById("belt-form");
const beltAddBtn = document.getElementById("btn-add-belt");
if (beltAddBtn) beltAddBtn.addEventListener("click", () => openBeltForm(null));
if (beltModal) beltModal.querySelectorAll("[data-close-belt]").forEach(el => el.addEventListener("click", closeBeltForm));
document.addEventListener("keydown", e => { if (e.key === "Escape") closeBeltForm(); });

async function loadBelts() {
    const grid = document.getElementById("belts-grid");
    grid.innerHTML = `<div class="placeholder">Загружаем пояса…</div>`;
    try {
        const belts = await api("/admin/belts");
        if (!belts.length) {
            grid.innerHTML = `<div class="placeholder">Поясов пока нет. Нажмите «Добавить пояс», чтобы создать первый.</div>`;
            return;
        }
        grid.innerHTML = "";
        belts.forEach(b => grid.appendChild(beltCard(b)));
    } catch (e) {
        grid.innerHTML = `<div class="placeholder">Не удалось загрузить пояса: ${e.message}</div>`;
    }
}

function beltCard(b) {
    const card = document.createElement("article");
    card.className = "card";

    const images = (b.beltImages && b.beltImages.length)
        ? b.beltImages
        : (b.imageUrl ? [{ imageUrl: b.imageUrl }] : []);
    const gallery = images.map(im =>
        `<img src="${imgSrc(im.imageUrl)}" alt="${escapeHtml(b.name || "")}" loading="lazy">`).join("");

    const chips = [
        b.color && `<span class="chip">${escapeHtml(b.color)}</span>`,
        (b.size != null) && `<span class="chip">Размер ${b.size}</span>`,
        b.sostav && `<span class="chip">${escapeHtml(b.sostav)}</span>`,
    ].filter(Boolean).join("");

    card.innerHTML = `
        <div class="card__gallery">${gallery || '<span class="card__id" style="padding:34px 12px">нет фото</span>'}</div>
        <div class="card__body">
            <h3 class="card__name">${escapeHtml(b.name || "Без названия")}</h3>
            <span class="card__id">В наличии: ${b.quantity ?? 0}</span>
            <span class="card__price">${formatPrice(b.price)} ₽</span>
            <div class="chips">${chips}</div>
            ${b.description ? `<p class="card__desc">${escapeHtml(b.description)}</p>` : ""}
            ${quickHtml(b)}
            <div class="card__foot">
                <button class="icon-btn" data-act="edit">Изменить</button>
                <button class="icon-btn icon-btn--danger" data-act="del">Удалить</button>
            </div>
        </div>`;

    bindQuick(card, "BELT", b.id);
    card.querySelector('[data-act="edit"]').addEventListener("click", () => openBeltForm(b));
    card.querySelector('[data-act="del"]').addEventListener("click", () => deleteBelt(b.id, b.name));
    return card;
}

async function deleteBelt(id, name) {
    if (!confirm(`Удалить пояс «${name || id}»?`)) return;
    try {
        const msg = await api(`/admin/belts/${id}`, { method: "DELETE" });
        toast(typeof msg === "string" && msg ? msg : "Пояс удалён");
        loadBelts();
    } catch (e) {
        toast(e.message, "error");
    }
}

function openBeltForm(belt) {
    beltForm.reset();
    beltFiles = [];
    renderBeltPreviews();

    const isEdit = !!belt;
    document.getElementById("belt-form-title").textContent = isEdit ? "Изменить пояс" : "Новый пояс";
    document.getElementById("belt-submit").textContent = isEdit ? "Сохранить изменения" : "Создать пояс";
    document.getElementById("belt-edit-note").hidden = !isEdit;
    document.getElementById("bf-id").value = isEdit ? belt.id : "";

    if (isEdit) {
        setVal("bf-name", belt.name);
        setVal("bf-color", belt.color);
        setVal("bf-size", belt.size);
        setVal("bf-sostav", belt.sostav);
        setVal("bf-description", belt.description);
        setVal("bf-price", belt.price);
        setVal("bf-quantity", belt.quantity);
    }
    updateBeltCounter();
    beltModal.classList.add("is-open");
    beltModal.setAttribute("aria-hidden", "false");
}

function closeBeltForm() {
    if (!beltModal) return;
    beltModal.classList.remove("is-open");
    beltModal.setAttribute("aria-hidden", "true");
}

const beltFileInput = document.getElementById("bf-images");
const beltDropzone = document.getElementById("belt-dropzone");
if (beltFileInput) beltFileInput.addEventListener("change", () => addBeltFiles(beltFileInput.files));
if (beltDropzone) {
    ["dragover", "dragenter"].forEach(ev =>
        beltDropzone.addEventListener(ev, e => { e.preventDefault(); beltDropzone.classList.add("is-drag"); }));
    ["dragleave", "drop"].forEach(ev =>
        beltDropzone.addEventListener(ev, e => { e.preventDefault(); beltDropzone.classList.remove("is-drag"); }));
    beltDropzone.addEventListener("drop", e => addBeltFiles(e.dataTransfer.files));
}

function addBeltFiles(fileList) {
    for (const f of fileList) {
        if (!f.type.startsWith("image/")) continue;
        if (beltFiles.length >= MAX_IMAGES) { toast(`Максимум ${MAX_IMAGES} фото`, "error"); break; }
        beltFiles.push(f);
    }
    beltFileInput.value = "";
    renderBeltPreviews();
    updateBeltCounter();
}

function renderBeltPreviews() {
    const box = document.getElementById("belt-previews");
    if (!box) return;
    box.innerHTML = "";
    beltFiles.forEach((file, i) => {
        const div = document.createElement("div");
        div.className = "preview";
        const url = URL.createObjectURL(file);
        div.innerHTML = `<img src="${url}" alt=""><button type="button" class="preview__rm">×</button>`;
        div.querySelector("button").addEventListener("click", () => {
            beltFiles.splice(i, 1); renderBeltPreviews(); updateBeltCounter();
        });
        box.appendChild(div);
    });
}

function updateBeltCounter() {
    const el = document.getElementById("belt-img-count");
    if (!el) return;
    const n = beltFiles.length;
    el.textContent = `${n} из ${MIN_IMAGES}–${MAX_IMAGES}`;
    el.classList.toggle("is-ok", n >= MIN_IMAGES && n <= MAX_IMAGES);
    el.classList.toggle("is-bad", n > 0 && (n < MIN_IMAGES || n > MAX_IMAGES));
}

if (beltForm) beltForm.addEventListener("submit", async e => {
    e.preventDefault();
    const id = document.getElementById("bf-id").value;
    const isEdit = !!id;

    const n = beltFiles.length;
    if (!isEdit && (n < MIN_IMAGES || n > MAX_IMAGES)) {
        toast(`Нужно выбрать от ${MIN_IMAGES} до ${MAX_IMAGES} фото`, "error"); return;
    }
    if (isEdit && n !== 0 && (n < MIN_IMAGES || n > MAX_IMAGES)) {
        toast(`Либо оставьте фото пустыми, либо выберите ${MIN_IMAGES}–${MAX_IMAGES}`, "error"); return;
    }

    const belt = {
        name: getVal("bf-name"),
        color: getVal("bf-color"),
        size: numOrNull(getVal("bf-size")),
        sostav: getVal("bf-sostav"),
        description: getVal("bf-description"),
        price: numOrNull(getVal("bf-price")),
        quantity: numOrNull(getVal("bf-quantity")),
    };

    const fd = new FormData();
    fd.append("belts", new Blob([JSON.stringify(belt)], { type: "application/json" }));
    beltFiles.forEach(f => fd.append("images", f));

    const btn = document.getElementById("belt-submit");
    btn.disabled = true;
    try {
        if (isEdit) {
            await api(`/admin/belts/${id}`, { method: "PUT", body: fd });
            toast("Пояс обновлён");
        } else {
            await api("/admin/belts", { method: "POST", body: fd });
            toast("Пояс создан");
        }
        closeBeltForm();
        loadBelts();
    } catch (err) {
        toast(err.message, "error");
    } finally {
        btn.disabled = false;
    }
});


/* ========================= БЫСТРЫЕ ПРАВКИ ЦЕНА/КОЛ-ВО ========================= */

function quickHtml(item) {
    return `<div class="quick">
        <div class="quick__row"><span>Цена, ₽</span>
            <input type="number" step="0.01" class="q-price" value="${item.price ?? ''}">
            <button class="quick__save" data-k="price" title="Сохранить цену">✓</button></div>
        <div class="quick__row"><span>Кол-во</span>
            <input type="number" class="q-qty" value="${item.quantity ?? 0}">
            <button class="quick__save" data-k="qty" title="Сохранить количество">✓</button></div>
    </div>`;
}

async function patchField(type, id, key, value) {
    if (value === "" || value == null) { toast("Введите значение", "error"); return; }
    const url = key === "price" ? "/admin/price" : "/admin/quantity";
    const body = key === "price"
        ? { type, id, price: Number(value) }
        : { type, id, quantity: Number(value) };
    await api(url, { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
}

function bindQuick(card, type, id) {
    card.querySelectorAll(".quick__save").forEach(btn => {
        btn.addEventListener("click", async () => {
            const k = btn.dataset.k;
            const input = card.querySelector(k === "price" ? ".q-price" : ".q-qty");
            btn.disabled = true;
            try {
                await patchField(type, id, k, input.value);
                toast(k === "price" ? "Цена обновлена" : "Количество обновлено");
            } catch (e) {
                toast(e.message, "error");
            } finally {
                btn.disabled = false;
            }
        });
    });
}

/* ========================= ПОИСК ЗАКАЗОВ КЛИЕНТА ========================= */

const finderBtn = document.getElementById("finder-btn");
if (finderBtn) {
    finderBtn.addEventListener("click", searchUserOrders);
    document.getElementById("finder-input").addEventListener("keydown", e => {
        if (e.key === "Enter") searchUserOrders();
    });
}

async function searchUserOrders() {
    const q = document.getElementById("finder-input").value.trim();
    const userBox = document.getElementById("finder-user");
    const list = document.getElementById("finder-list");
    userBox.innerHTML = "";
    if (!q) { list.innerHTML = `<div class="placeholder">Введите почту или телефон клиента.</div>`; return; }
    list.innerHTML = `<div class="placeholder">Ищем…</div>`;
    try {
        const orders = await api(`/admin/user-orders?phoneOrEmail=${encodeURIComponent(q)}`);
        if (!orders.length) {
            list.innerHTML = `<div class="placeholder">У этого клиента пока нет заказов.</div>`;
            return;
        }
        const first = orders[0];
        userBox.innerHTML = `<div class="finder__user">Клиент: <b>${escapeHtml(first.userEmail || "—")}</b> &nbsp;·&nbsp; ${escapeHtml(first.phone || "—")} &nbsp;·&nbsp; заказов: ${orders.length}</div>`;
        list.innerHTML = "";
        orders.forEach(o => list.appendChild(orderCard(o)));
    } catch (e) {
        const notFound = (e.message || "").toLowerCase().includes("не найден");
        list.innerHTML = `<div class="placeholder">${notFound ? "Пользователь с такой почтой/телефоном не найден." : "Ошибка: " + e.message}</div>`;
    }
}
