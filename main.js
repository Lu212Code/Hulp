// ==============================
// GLOBALS
// ==============================
const API_BASE = "https://backend.welfenmc.de:25562/api";
let token = localStorage.getItem("jwt") || null;
let currentChatId = null;
let chatInterval = null;

// ==============================
// HELPER: API FETCH
// ==============================
async function apiFetch(path, options = {}) {
    const headers = options.headers || {};
    if (token) headers["Authorization"] = token;
    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (!res.ok) throw new Error(await res.text());
    return res.json().catch(() => null);
}

// ==============================
// SHOW SECTIONS
// ==============================
function showSection(id) {
    document.querySelectorAll(".section").forEach(s => s.style.display = "none");
    const section = document.getElementById(id);
    if (section) section.style.display = "flex";

    if (id === "fragen") loadFragen();
    else if (id === "activeChats") loadActiveChats();
}

// ==============================
// SIDEBAR / HAMBURGER
// ==============================
const hamburgerBtn = document.getElementById('hamburgerBtn');
const sidebar = document.querySelector('.sidebar');
const main = document.querySelector('.main');
const overlay = document.querySelector('.overlay');

hamburgerBtn.addEventListener('click', () => {
    sidebar.classList.toggle('open');
    overlay.classList.toggle('active');

    if (window.innerWidth <= 768) {
        main.classList.toggle('shifted');
    }
});

overlay.addEventListener('click', () => {
    sidebar.classList.remove('open');
    overlay.classList.remove('active');

    if (window.innerWidth <= 768) {
        main.classList.remove('shifted');
    }
});

// ==============================
// LOGIN / LOGOUT
// ==============================
async function login() {
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    if (!username || !password) return showHulpPopup("Bitte Benutzername und Passwort angeben!");

    const res = await fetch(`${API_BASE}/users/login`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ username, password })
    });

    if (!res.ok) return showHulpPopup("Login fehlgeschlagen!");
    
    token = await res.text();
    localStorage.setItem("jwt", token);
    localStorage.setItem("username", username);

    document.getElementById("userName").textContent = username;
    document.getElementById("loginArea").style.display = "none";
    document.getElementById("userArea").style.display = "block";

    loadFragen();
}

function logout() {
    token = null;
    localStorage.removeItem("jwt");
    localStorage.removeItem("username");

    document.getElementById("loginArea").style.display = "block";
    document.getElementById("userArea").style.display = "none";
    document.getElementById("fragenListe").innerHTML = "";
}

// ==============================
// FRAGEN
// ==============================
async function loadFragen() {
    const fragenListe = document.getElementById("fragenListe");
    fragenListe.innerHTML = "Lade...";

    try {
        const fragen = await apiFetch("/questions/all");
        fragenListe.innerHTML = "";
        fragen.forEach(q => {
            const div = document.createElement("div");
            div.className = "pin-btn";
            div.textContent = `${q.subject}: ${q.content} (${q.username})`;

            if (!q.active) {
                const helpBtn = document.createElement("button");
                helpBtn.textContent = "Ich helfe!";
                helpBtn.className = "btn";
                helpBtn.onclick = (e) => {
                    e.stopPropagation();
                    assignHelperToChat(q.id);
                };
                div.appendChild(helpBtn);
            }

            fragenListe.appendChild(div);
        });
    } catch (err) {
        console.error(err);
        fragenListe.textContent = "Fehler beim Laden der Fragen.";
    }
}

async function addFrage() {
    const subject = document.getElementById("fach").value.trim();
    const content = document.getElementById("frageText").value.trim();
    if (!subject || !content) return showHulpPopup("Bitte Fach und Text angeben!");

    try {
        await apiFetch("/questions/add", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ subject, content })
        });
        showHulpPopup("Frage hinzugefügt!");
        document.getElementById("frageText").value = "";
        loadFragen();
    } catch (err) {
        console.error(err);
        showHulpPopup("Fehler beim Hinzufügen der Frage");
    }
}

// ==============================
// ACTIVE CHATS
// ==============================
async function loadActiveChats() {
    const list = document.getElementById("activeChatsList");
    list.innerHTML = "Lade...";

    try {
        const chats = await apiFetch("/chat/active");
        list.innerHTML = "";

        if (!chats || chats.length === 0) {
            list.textContent = "Keine aktiven Chats.";
            return;
        }

        chats.forEach(c => {
            const div = document.createElement("div");
            div.className = "pin-btn";
            const helperText = c.helperUsername ? `Helfer: ${c.helperUsername}` : "Helfer noch nicht zugewiesen";
            div.textContent = `Chat mit ${c.askerUsername} (${helperText})`;
            div.onclick = () => openExistingChat(c.chatId);
            list.appendChild(div);
        });
    } catch (err) {
        console.error(err);
        list.textContent = "Fehler beim Laden der aktiven Chats.";
    }
}

async function endCurrentChat() {
    if (!currentChatId) return showHulpPopup("Kein Chat geöffnet!");
    const ok = await showHulpPopup("Willst du den Chat wirklich beenden?", { ok:true, cancel:true });
    if (!ok) return;

    try {
        await apiFetch("/chat/end", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ chatId: currentChatId })
        });
        showHulpPopup("Chat beendet!");
        currentChatId = null;
        clearInterval(chatInterval);
        chatInterval = null;

        showSection("fragen");
        loadActiveChats();
        document.getElementById("chatList").innerHTML = "";
    } catch (err) {
        console.error(err);
        showHulpPopup("Chat konnte nicht beendet werden!");
    }
}

// ==============================
// POPUP
// ==============================
function showHulpPopup(message, options = { ok:true, cancel:false }) {
    const popup = document.getElementById("hulpPopup");
    const text = document.getElementById("hulpPopupText");
    const okBtn = document.getElementById("hulpPopupOk");
    const cancelBtn = document.getElementById("hulpPopupCancel");

    text.textContent = message;
    okBtn.style.display = options.ok ? "inline-block" : "none";
    cancelBtn.style.display = options.cancel ? "inline-block" : "none";

    return new Promise(resolve => {
        popup.style.display = "flex";
        const cleanUp = () => { okBtn.onclick = null; cancelBtn.onclick = null; popup.style.display = "none"; };

        okBtn.onclick = () => { cleanUp(); resolve(true); };
        cancelBtn.onclick = () => { cleanUp(); resolve(false); };
    });
}

// ==============================
// RIPPLE EFFECT
// ==============================
document.querySelectorAll('.btn, .sidebar button').forEach(button => {
    button.addEventListener('click', function(e) {
        const rect = button.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const ripple = document.createElement('span');
        ripple.classList.add('ripple');
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';
        button.appendChild(ripple);
        setTimeout(() => ripple.remove(), 1000);
    });
});

// ==============================
// AUTO-LOGIN
// ==============================
document.addEventListener("DOMContentLoaded", () => {
    showSection('fragen');
    if (token) {
        const username = localStorage.getItem("username") || "Unbekannt";
        document.getElementById("userName").textContent = username;
        document.getElementById("loginArea").style.display = "none";
        document.getElementById("userArea").style.display = "block";
        loadFragen();
    }
});
