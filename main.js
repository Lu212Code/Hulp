const API_BASE = "https://xksj6l873p441h9e.myfritz.net:25562/api";
let token = localStorage.getItem("jwt") || null;

// aktuelle Chat-Daten
let currentChatId = null;
let currentSubject = null;
let currentQuestion = null;

let allQuestions = [];
let allArchive = [];

let chatInterval = null; // Intervall für Live-Updates

/* ---------------------------------------------------------
   🔧 API Helper
--------------------------------------------------------- */
async function apiFetch(path, options = {}) {
	const headers = options.headers || {};
	if (token) headers["Authorization"] = token;

	let res;
	try {
		res = await fetch(`${API_BASE}${path}`, { ...options, headers });
	} catch (e) {
		throw new Error("Server nicht erreichbar");
	}

	if (res.status === 401 || res.status === 403) {
		console.warn("Session abgelaufen → Logout");

		await showHulpPopup(
			"Deine Sitzung ist abgelaufen. Bitte melde dich erneut an."
		);

		logout();
		throw new Error("Session abgelaufen");
	}

	if (!res.ok) {
		const text = await res.text();
		throw new Error(text || "Unbekannter Fehler");
	}

	const text = await res.text();
	if (!text) return null;

	try {
		return JSON.parse(text);
	} catch {
		return text;
	}
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

		token = text;
		localStorage.setItem("jwt", token);

		const userInfo = await apiFetch("/users/me");
		localStorage.setItem("username", userInfo.user);
		localStorage.setItem("role", userInfo.userRole);

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
	localStorage.removeItem("username");
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

/* ---------------------------------------------------------
   🔹 ACTIVE CHATS / WhatsApp-Style Chat
--------------------------------------------------------- */
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

/* ---------------------------------------------------------
   🔹 OPEN EXISTING CHAT (WhatsApp-Style)
--------------------------------------------------------- */
async function openExistingChat(chatId, subject, question) {
	showSection("chat");
	currentChatId = chatId;
	currentSubject = subject;
	currentQuestion = question;

	const chatList = document.getElementById("chatList");
	const chatInput = document.getElementById("chatInput");
	const sendBtn = chatInput.nextElementSibling;

	chatList.innerHTML = "Lade Chat...";
	chatInput.disabled = false;
	sendBtn.disabled = false;
	sendBtn.textContent = "Senden";

	async function loadMessages() {
		try {
			const messages = await apiFetch(`/chat/messages?chatId=${chatId}`);
			chatList.innerHTML = "";

			messages.forEach(msg => {
				const isMe = msg.username === localStorage.getItem("username");

				const bubble = document.createElement("div");
				bubble.className = `chat-bubble ${isMe ? "chat-right" : "chat-left"}`;

				const nameDiv = document.createElement("div");
				nameDiv.className = "chat-username";
				nameDiv.textContent = msg.username;

				const textDiv = document.createElement("div");
				textDiv.textContent = msg.content;

				bubble.appendChild(nameDiv);
				bubble.appendChild(textDiv);

				bubble.onclick = () => reportMessage(msg);

				chatList.appendChild(bubble);
			});

			chatList.scrollTop = chatList.scrollHeight;

		} catch (err) {
			console.error("Fehler beim Laden der Nachrichten:", err);
			chatList.textContent = "Fehler beim Laden des Chats.";
		}
	}

	await loadMessages();

	if (chatInterval) clearInterval(chatInterval);
	chatInterval = setInterval(loadMessages, 2000);

	sendBtn.onclick = async () => {
		const content = chatInput.value.trim();
		if (!content) return;

		try {
			await apiFetch("/chat/send", {
				method: "POST",
				headers: { "Content-Type": "application/x-www-form-urlencoded" },
				body: new URLSearchParams({ chatId, content })
			});
			chatInput.value = "";
			await loadMessages();
		} catch (err) {
			console.error("Fehler beim Senden:", err);
			await showHulpPopup("Nachricht konnte nicht gesendet werden!");
		}
	};
}

/* ---------------------------------------------------------
   🔹 MELDEN NACHRICHT
--------------------------------------------------------- */
async function reportMessage(msg) {
	const ok = await showHulpPopup(
		`Möchten Sie diese Nachricht melden?\n\n"${msg.content}"`,
		{ ok: true, cancel: true }
	);

	if (!ok) return;

	const reason = prompt("Grund für die Meldung?");
	if (!reason) return;

	try {
		const reporter = localStorage.getItem("username");

		await apiFetch(
			`/reports/create?reporterUsername=${encodeURIComponent(reporter)}&type=message&targetId=${msg.id}&reason=${encodeURIComponent(reason)}`,
			{ method: "POST" }
		);

		await showHulpPopup("Nachricht wurde gemeldet.");
	} catch (err) {
		console.error(err);
		await showHulpPopup("Fehler beim Melden der Nachricht.");
	}
}

/* ---------------------------------------------------------
   🔹 OPEN ARCHIVED CHAT (WhatsApp-Style)
--------------------------------------------------------- */
async function openArchivedChat(chatId, subject, question) {
	showSection("chat");
	currentChatId = chatId;
	currentSubject = subject;
	currentQuestion = question;

	const chatList = document.getElementById("chatList");
	const chatInput = document.getElementById("chatInput");
	const sendBtn = chatInput.nextElementSibling;

	chatList.innerHTML = "Lade Chat...";

	try {
		const messages = await apiFetch(`/chat/messages?chatId=${chatId}`);
		chatList.innerHTML = "";

		messages.forEach(msg => {
			const isMe = msg.username === localStorage.getItem("username");

			const bubble = document.createElement("div");
			bubble.className = `chat-bubble ${isMe ? "chat-right" : "chat-left"}`;

			const nameDiv = document.createElement("div");
			nameDiv.className = "chat-username";
			nameDiv.textContent = msg.username;

			const textDiv = document.createElement("div");
			textDiv.textContent = msg.content;

			bubble.appendChild(nameDiv);
			bubble.appendChild(textDiv);

			bubble.onclick = () => reportMessage(msg);

			chatList.appendChild(bubble);
		});

		chatInput.disabled = true;
		sendBtn.disabled = true;
		sendBtn.textContent = "Archivierter Chat";

	} catch (err) {
		console.error(err);
		chatList.textContent = "Fehler beim Laden des Chats.";
	}
}
