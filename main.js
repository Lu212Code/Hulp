const API_BASE = "https://xksj6l873p441h9e.myfritz.net:25562/api";
let token = localStorage.getItem("jwt") || null;

// aktuelle Chat-Daten
let currentChatId = null;
let currentSubject = null;
let currentQuestion = null;

let allQuestions = [];
let allArchive = [];


/* ---------------------------------------------------------
   🔧 API Helper
--------------------------------------------------------- */
async function apiFetch(path, options = {}) {
	const headers = options.headers || {};
	if (token) headers["Authorization"] = token;

	const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
	if (!res.ok) throw new Error(await res.text());
	return res.json().catch(() => null);
}

/* ---------------------------------------------------------
   👤 Login / Logout
--------------------------------------------------------- */
async function login() {
	const username = document.getElementById("username").value.trim();
	const password = document.getElementById("password").value.trim();

	if (!username || !password) {
		await showHulpPopup("Bitte Benutzername und Passwort angeben!");
		return;
	}

	try {
		const res = await fetch(`${API_BASE}/users/login`, {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: new URLSearchParams({ username, password })
		});

		const text = await res.text();

		if (text === "Error") {
			await showHulpPopup("Benutzername oder Passwort falsch!");
			return;
		}

		// Token speichern
		token = text;
		localStorage.setItem("jwt", token);

		// Benutzerinfo abrufen
		const userInfo = await apiFetch("/users/me"); // { user: "username", userRole: "role" }
		localStorage.setItem("username", userInfo.user);
		localStorage.setItem("role", userInfo.userRole);

		// UI aktualisieren
		document.getElementById("userName").textContent = userInfo.user;
		document.getElementById("userCornerText").textContent = userInfo.user;
		document.getElementById("loginArea").style.display = "none";
		document.getElementById("userArea").style.display = "block";

		if (userInfo.userRole === "mod") {
			document.getElementById("adminTabBtn").style.display = "inline-block";
		} else {
			document.getElementById("adminTabBtn").style.display = "none";
		}

		loadFragen();

	} catch (err) {
		console.error(err);
		await showHulpPopup("Serverfehler beim Login!");
	}
}

function logout() {
	token = null;
	localStorage.removeItem("jwt");
	localStorage.removeItem("username"); // <--- hinzufügen
	document.getElementById("loginArea").style.display = "block";
	document.getElementById("userArea").style.display = "none";
	document.getElementById("fragenListe").innerHTML = "";
	document.getElementById("userCornerText").textContent = "Login";
}

/* ---------------------------------------------------------
   📚 Fragen laden / hinzufügen
--------------------------------------------------------- */
async function loadFragen() {
	const fragenListe = document.getElementById("fragenListe");
	fragenListe.innerHTML = "Lade...";

	try {
		allQuestions = await apiFetch("/questions/all");
		renderFilteredFragen();
	} catch (err) {
		console.error(err);
		fragenListe.textContent = "Fehler beim Laden der Fragen.";
	}
}

function renderFilteredFragen() {
	const text = document.getElementById("filterTextFragen")?.value.toLowerCase() || "";
	const fach = document.getElementById("filterFachFragen")?.value || "";
	const klasse = document.getElementById("filterKlasseFragen")?.value || "";

	const list = document.getElementById("fragenListe");
	list.innerHTML = "";

	const filtered = allQuestions.filter(q =>
		(!text || q.content.toLowerCase().includes(text)) &&
		(!fach || q.subject === fach) &&
		(!klasse || String(q.klasse) === klasse)
	);

	if (filtered.length === 0) {
		list.textContent = "Keine passenden Fragen.";
		return;
	}

	filtered.forEach(q => {
		const div = document.createElement("div");
		div.className = "pin-btn";
		div.textContent = `${q.subject} (Klasse ${q.klasse}): ${q.content} (${q.username})`;

		if (!q.active) {
			const helpBtn = document.createElement("button");
			helpBtn.textContent = "Ich helfe!";
			helpBtn.className = "btn";
			helpBtn.onclick = (e) => {
				e.stopPropagation();
				assignHelperToChat(q.id, q.username);
			};
			div.appendChild(helpBtn);
		}

		list.appendChild(div);
	});
}

async function addFrage() {
	const subject = document.getElementById("fach").value.trim();
	const content = document.getElementById("frageText").value.trim();
	const klasse = document.getElementById("klasse").value.trim();

	if (!subject || !content || !klasse) {
		await showHulpPopup("Bitte Fach, Text und Klasse angeben!");
		return;
	}

	try {
		await apiFetch("/questions/add", {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: new URLSearchParams({ subject, content, klasse })
		});

		await showHulpPopup("Frage hinzugefügt!");
		document.getElementById("frageText").value = "";
		loadFragen();

	} catch (err) {
		await showHulpPopup("Fehler beim Hinzufügen der Frage");
		console.error(err);
	}
}

/* ---------------------------------------------------------
   🔁 Abschnittsverwaltung
--------------------------------------------------------- */
function showSection(id) {
	document.querySelectorAll(".section").forEach(s => s.style.display = "none");
	document.getElementById(id).style.display = "block";

	if (id === "fragen") loadFragen();
	else if (id === "activeChats") loadActiveChats();
	else if (id === "admin") loadAdmin();
	else if (id === "leaderboard") loadLeaderboard();
}

/* ---------------------------------------------------------
   🚀 Auto-Login beim Laden
--------------------------------------------------------- */
document.addEventListener("DOMContentLoaded", () => {
	["filterTextFragen", "filterFachFragen", "filterKlasseFragen"]
		.forEach(id => {
			const el = document.getElementById(id);
			if (el) el.addEventListener("input", renderFilteredFragen);
		});
	["filterTextArchiv", "filterFachArchiv"]
		.forEach(id => {
			const el = document.getElementById(id);
			if (el) el.addEventListener("input", renderFilteredArchiv);
		});
	showSection("home");
	if (token) {
		const username = localStorage.getItem("username") || "Unbekannt";
		const role = localStorage.getItem("role") || "user";
		document.getElementById("userName").textContent = username;
		document.getElementById("loginArea").style.display = "none";
		document.getElementById("userArea").style.display = "block";

		console.log("Eingeloggt als:", username, "mit Rolle:", role);

		if (role === "mod") {
			document.getElementById("adminTabBtn").style.display = "inline-block";
		} else {
			document.getElementById("adminTabBtn").style.display = "none";
		}
	}
	loadArchiv();
});

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

		const me = localStorage.getItem("username");

		chats.forEach(c => {
			const div = document.createElement("div");
			div.className = "pin-btn";

			// Wer ist wer?
			let otherPerson = "";
			if (c.askerUsername === me) {
				otherPerson = c.helperUsername || "Helfer noch nicht zugewiesen";
			} else {
				otherPerson = c.askerUsername;
			}

			const helperText = c.helperUsername ? `Helfer: ${c.helperUsername}` : "Helfer noch nicht zugewiesen";
			div.textContent = `Chat mit ${otherPerson} (${helperText})`;

			div.onclick = () => openExistingChat(c.chatId, c.subject, c.content);
			list.appendChild(div);
		});
	} catch (err) {
		console.error("Fehler beim Laden der aktiven Chats:", err);
		list.textContent = "Fehler beim Laden der aktiven Chats.";
	}
}

async function endCurrentChat() {
	if (!currentChatId) return await showHulpPopup("Kein Chat geöffnet!");

	const ok = await showHulpPopup("Willst du den Chat wirklich beenden?", { ok: true, cancel: true });
	if (!ok) return;

	const consent = await showHulpPopup(
		"Darf diese Frage anonymisiert ins öffentliche Archiv?",
		{ ok: true, cancel: true }
	);

	try {
		if (consent) {
			await apiFetch("/chat/consent", {
				method: "POST",
				headers: { "Content-Type": "application/x-www-form-urlencoded" },
				body: new URLSearchParams({ chatId: currentChatId, consent: true })
			});
		}

		await apiFetch("/chat/end", {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded" },
			body: new URLSearchParams({ chatId: currentChatId })
		});

		await showHulpPopup("Chat beendet!");
		currentChatId = null;
		currentSubject = null;
		currentQuestion = null;
		clearInterval(chatInterval);
		chatInterval = null;

		showSection("home");
		loadArchiv();
		loadActiveChats();
		document.getElementById("chatList").innerHTML = "";

	} catch (err) {
		console.error("Fehler beim Beenden des Chats:", err);
		await showHulpPopup("Chat konnte nicht beendet werden!");
	}
}

function showRegisterPopup() {
	document.getElementById('registerPopup').style.display = 'block';
}

function closeRegisterPopup() {
	document.getElementById('registerPopup').style.display = 'none';
}

async function register() {
	const username = document.getElementById('regUsername').value.trim();
	const password1 = document.getElementById('regPassword1').value;
	const password2 = document.getElementById('regPassword2').value;

	if (!username || !password1 || !password2) {
		await showHulpPopup("Bitte alle Felder ausfüllen!");
		return;
	}

	if (password1.length < 4) {
		await showHulpPopup("Das Passwort muss mindestens vier Zeichen haben!");
		return;
	}

	if (password1 !== password2) {
		await showHulpPopup("Die Passwörter stimmen nicht überein!");
		return;
	}

	try {
		const res = await fetch(`${API_BASE}/users/register`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
			body: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password1)}`
		});

		if (res.ok) {
			await showHulpPopup("Registrierung erfolgreich!");
			closeRegisterPopup();
		} else {
			const text = await res.text();
			await showHulpPopup("Fehler bei der Registrierung, bitte versuche es später erneut.");
		}
	} catch (err) {
		await showHulpPopup("Fehler bei der Registrierung, bitte versuche es später erneut.");
	}
}

function showHulpPopup(message, options = { ok: true, cancel: false }) {
	const popup = document.getElementById("hulpPopup");
	const text = document.getElementById("hulpPopupText");
	const okBtn = document.getElementById("hulpPopupOk");
	const cancelBtn = document.getElementById("hulpPopupCancel");

	text.textContent = message;
	okBtn.style.display = options.ok ? "inline-block" : "none";
	cancelBtn.style.display = options.cancel ? "inline-block" : "none";

	return new Promise((resolve) => {
		popup.style.display = "flex";

		const cleanUp = () => {
			okBtn.onclick = null;
			cancelBtn.onclick = null;
			popup.style.display = "none";
		}

		okBtn.onclick = () => { cleanUp(); resolve(true); }
		cancelBtn.onclick = () => { cleanUp(); resolve(false); }
	});
}

async function loadArchiv() {
	const list = document.getElementById("archivListe");
	if (!list) return;
	list.innerHTML = "Lade...";

	try {
		allArchive = await apiFetch("/chat/archiv");
		renderFilteredArchiv();
	} catch (err) {
		console.error(err);
		list.textContent = "Fehler beim Laden des Archivs.";
	}
}

function renderFilteredArchiv() {
	const text = document.getElementById("filterTextArchiv")?.value.toLowerCase() || "";
	const fach = document.getElementById("filterFachArchiv")?.value || "";

	const list = document.getElementById("archivListe");
	list.innerHTML = "";

	const filtered = allArchive.filter(e =>
		(!text || e.question.toLowerCase().includes(text)) &&
		(!fach || e.subject === fach)
	);

	if (filtered.length === 0) {
		list.textContent = "Keine Treffer im Archiv.";
		return;
	}

	filtered.forEach(e => {
		const div = document.createElement("div");
		div.className = "pin-btn";
		div.textContent = `${e.subject}: ${e.question}`;
		div.onclick = () => openArchivedChat(e.chatId, e.subject, e.question);
		list.appendChild(div);
	});
}

async function saveChatToArchive(chatId, messages, subject, question) {
	return await apiFetch("/chat/save", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({
			chatId,
			messages,
			subject,
			question
		})
	});
}

async function openArchivedChat(chatId, subject, question) {
	showSection("chat"); // Chat-Section öffnen
	currentChatId = chatId;
	currentSubject = subject;
	currentQuestion = question;

	const chatList = document.getElementById("chatList");
	const chatInput = document.getElementById("chatInput");
	const sendBtn = chatInput.nextElementSibling; // Annahme: Button direkt danach
	chatList.innerHTML = "Lade Chat...";

	try {
		const messages = await apiFetch(`/chat/messages?chatId=${chatId}`);

		chatList.innerHTML = "";
		messages.forEach(msg => {
			const div = document.createElement("div");
			div.className = msg.username === localStorage.getItem("username") ? "my-msg" : "other-msg";
			div.textContent = `${msg.username}: ${msg.content}`;
			chatList.appendChild(div);
		});

		// Archivierte Chats: Eingabe deaktivieren
		chatInput.disabled = true;
		sendBtn.disabled = true;
		sendBtn.textContent = "Archivierter Chat";

	} catch (err) {
		console.error(err);
		chatList.textContent = "Fehler beim Laden des Chats.";
	}
}

async function loadAdmin() {
	const container = document.getElementById("adminContent");
	container.innerHTML = "Lade aktive Chats...";

	try {
		// 1️⃣ Aktive Chats abrufen
		const activeChats = await apiFetch("/chat/active");
		container.innerHTML = "";

		if (!activeChats || activeChats.length === 0) {
			container.textContent = "Keine aktiven Chats vorhanden.";
			return;
		}

		activeChats.forEach(chat => {
			const div = document.createElement("div");
			div.className = "pin-btn";
			const helperText = chat.helperUsername ? `Helfer: ${chat.helperUsername}` : "Helfer noch nicht zugewiesen";
			div.textContent = `Chat mit ${chat.askerUsername} (${helperText})`;

			// Chat öffnen
			div.onclick = () => openExistingChat(chat.chatId, chat.subject, chat.content);

			// Nachricht löschen (nach Index)
			const delMsgBtn = document.createElement("button");
			delMsgBtn.textContent = "Nachricht löschen";
			delMsgBtn.className = "btn";
			delMsgBtn.onclick = async () => {
				const index = prompt("Index der Nachricht zum Löschen?");
				if (index === null) return;
				await apiFetch(`/admin/chat/message/delete?chatId=${chat.chatId}&index=${index}`, { method: "POST" });
				await loadAdmin();
			};
			div.appendChild(delMsgBtn);

			// Gesamten Chat löschen
			const delChatBtn = document.createElement("button");
			delChatBtn.textContent = "Chat löschen";
			delChatBtn.className = "btn";
			delChatBtn.onclick = async () => {
				if (!confirm("Gesamten Chat löschen?")) return;
				await apiFetch(`/admin/chat/chat/delete?chatId=${chat.chatId}`, { method: "POST" });
				await loadAdmin();
			};
			div.appendChild(delChatBtn);

			container.appendChild(div);
		});

		// 2️⃣ Archivierte Chats
		const archHeader = document.createElement("h3");
		archHeader.textContent = "Archivierte Chats";
		container.appendChild(archHeader);

		const archived = await apiFetch("/admin/chat/archived");
		archived.forEach(chatId => {
			const div = document.createElement("div");
			div.className = "pin-btn";
			div.textContent = `Archivierter Chat: ${chatId}`;

			const loadBtn = document.createElement("button");
			loadBtn.textContent = "Anzeigen";
			loadBtn.className = "btn";
			loadBtn.onclick = async () => {
				const chat = await apiFetch(`/admin/chat/archived/load?chatId=${chatId}`);
				console.log(chat); // hier kannst du Meta + Nachrichten darstellen
			};
			div.appendChild(loadBtn);

			const deleteBtn = document.createElement("button");
			deleteBtn.textContent = "Archiv löschen";
			deleteBtn.className = "btn";
			deleteBtn.onclick = async () => {
				if (!confirm("Archivierten Chat löschen?")) return;
				await apiFetch(`/admin/chat/archived/delete?chatId=${chatId}`, { method: "POST" });
				await loadAdmin();
			};
			div.appendChild(deleteBtn);

			container.appendChild(div);
		});

	} catch (err) {
		console.error(err);
		container.textContent = "Fehler beim Laden des Adminbereichs.";
	}
}

async function assignHelperToChat(questionId, askerUsername) {
	try {
		if (!askerUsername) {
			console.error("askerUsername fehlt!");
			return;
		}

		// 1️⃣ Chat erstellen
		const chat = await apiFetch("/chat/create", {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded", "Authorization": token },
			body: new URLSearchParams({ questionId: String(questionId) })
		});

		if (!chat.chatId) {
			console.error("Chat wurde nicht erstellt!", chat);
			await showHulpPopup("Fehler beim Erstellen des Chats!");
			return;
		}

		// 2️⃣ Helfer zuweisen
		const assignedChat = await apiFetch("/chat/assign-helper", {
			method: "POST",
			headers: { "Content-Type": "application/x-www-form-urlencoded", "Authorization": token },
			body: new URLSearchParams({ chatId: chat.chatId, askerUsername: String(askerUsername) })
		});

		console.log("Helfer zugewiesen:", assignedChat);
		await showHulpPopup("Du bist jetzt Helfer!");
		loadFragen(); // UI aktualisieren
		openExistingChat(assignedChat.chatId, assignedChat.subject, assignedChat.content);

	} catch (err) {
		console.error("Fehler beim Helfer zuweisen:", err);
		await showHulpPopup("Fehler beim Helfer zuweisen!");
	}
}

function openUserPanel() {
	if (!token) {
		// Login anzeigen
		showSection("home");
		document.getElementById("loginArea").style.display = "block";
		document.getElementById("userArea").style.display = "none";
		return;
	}

	// Benutzerprofil öffnen
	document.getElementById("profileUsername").textContent =
		localStorage.getItem("username") || "Unbekannt";

	document.getElementById("profileRank").textContent =
		localStorage.getItem("role") || "User";

	loadUserStats();
	showSection("userPanel");
}

async function loadUserStats() {
	try {
		const data = await apiFetch("/users/stats");
		document.getElementById("askedCount").textContent = data.asked || 0;
		document.getElementById("answeredCount").textContent = data.answered || 0;
	} catch (err) {
		console.error("Fehler beim Laden der Nutzerstatistik:", err);
	}
}

async function changePassword() {
	const p1 = document.getElementById("newPw1").value;
	const p2 = document.getElementById("newPw2").value;

	if (!p1 || !p2) return showHulpPopup("Bitte beide Felder ausfüllen!");
	if (p1 !== p2) return showHulpPopup("Passwörter stimmen nicht überein!");
	if (p1.length < 4) return showHulpPopup("Mindestens 4 Zeichen!");

	const username = localStorage.getItem("username");
	const token = localStorage.getItem("token");

	try {
		await apiFetch("/users/changepassword", {
			method: "POST",
			headers: {
				"Content-Type": "application/x-www-form-urlencoded",
				"Authorization": token
			},
			body: new URLSearchParams({
				username: username,
				newPassword: p1
			})
		});

		showHulpPopup("Passwort geändert!");
		document.getElementById("newPw1").value = "";
		document.getElementById("newPw2").value = "";

	} catch (err) {
		console.error(err);
		showHulpPopup("Fehler beim Ändern!");
	}
}

async function loadLeaderboard() {
	if (!token) return showHulpPopup("Bitte einloggen!");

	try {
		const data = await apiFetch("/users/leaderboard");

		const tbody = document.getElementById("leaderboardBody");
		tbody.innerHTML = "";

		// Top 50
		data.leaderboard.forEach((entry, index) => {
			const tr = document.createElement("tr");

			const tdRank = document.createElement("td");
			tdRank.textContent = index + 1;
			const tdUser = document.createElement("td");
			tdUser.textContent = entry.username;
			const tdPoints = document.createElement("td");
			tdPoints.textContent = entry.points;

			tr.appendChild(tdRank);
			tr.appendChild(tdUser);
			tr.appendChild(tdPoints);

			// Eigenen User hervorheben
			if (entry.username === localStorage.getItem("username")) {
				tr.style.backgroundColor = "#ffd70040";
				tr.style.fontWeight = "bold";
			}

			tbody.appendChild(tr);
		});

		// Eigenen Platz anzeigen
		const myPos = data.me;
		document.getElementById("myLeaderboardPosition").textContent =
			`Dein Platz: ${myPos.position > 0 ? myPos.position : "-"} | Punkte: ${myPos.points}`;

	} catch (err) {
		console.error(err);
		showHulpPopup("Fehler beim Laden der Rangliste!");
	}
}
