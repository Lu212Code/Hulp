let chatInterval = null;

async function openChat(questionId) {
  if (!token) {
    await showHulpPopup("Du bist nicht eingeloggt!");
    return;
  }

  console.log("openChat aufgerufen mit questionId:", questionId);

  try {
    // Stelle sicher, dass questionId als String gesendet wird
    const chat = await apiFetch("/chat/create", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ questionId: questionId }) // Long direkt senden
    });

    console.log("Chat erstellt:", chat);
    currentChatId = chat.chatId;
    showSection("chat");
    loadChatMessages();
    if (chatInterval) clearInterval(chatInterval);
    chatInterval = setInterval(loadChatMessages, 3000);

  } catch (err) {
    console.error("Fehler beim Chat erstellen:", err);
    await showHulpPopup("Chat konnte nicht erstellt werden!");
  }
}


async function loadChatMessages() {
  if (!currentChatId) return;
  try {
    const msgs = await apiFetch(`/chat/messages?chatId=${currentChatId}`);
    const chatList = document.getElementById("chatList");
    chatList.innerHTML = "";
    msgs.forEach(m => {
      const div = document.createElement("div");
      div.className = m.sender === localStorage.getItem("username") ? "my-msg" : "other-msg";
      div.textContent = `${m.sender}: ${m.content}`;

      //const reportBtn = document.createElement("button");
      //reportBtn.textContent = "Melden";
      //reportBtn.className = "small-btn";
      //reportBtn.onclick = async () => {
      //  const reason = prompt("Grund für die Meldung?");
      //  if (!reason) return;
      //  const username = localStorage.getItem("username");
      //  await apiFetch(`/reports/create?reporterUsername=${encodeURIComponent(username)}&type=chat&targetId=${currentChatId}&reason=${encodeURIComponent(reason)}`, {
      //    method: "POST"
      //  });
      //  alert("Nachricht gemeldet!");
      //};
      //div.appendChild(reportBtn);

      chatList.appendChild(div);
    });
    chatList.scrollTop = chatList.scrollHeight;
  } catch (e) {
    console.warn("Chat laden fehlgeschlagen:", e);
  }
}


async function sendChatMessage() {
  const msg = document.getElementById("chatInput").value;
  if (!msg || !currentChatId) return;
  await apiFetch("/chat/send", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ chatId: currentChatId, content: msg })
  });
  document.getElementById("chatInput").value = "";
  loadChatMessages();
}

async function getOrCreateChat(questionId) {
  const chat = await apiFetch("/chat/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      "Authorization": token // <<< unbedingt mitschicken
    },
    body: new URLSearchParams({ questionId })
  });
  return chat;
}

async function createChatAndAssignHelper(questionId, askerUsername) {
  try {
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
      body: new URLSearchParams({ chatId: chat.chatId, askerUsername })
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

async function openExistingChat(chatId, subject, question) {
  showSection("chat"); // Chat-Section öffnen
  currentChatId = chatId;
  currentSubject = subject;
  currentQuestion = question;

  const chatList = document.getElementById("chatList");
  const chatInput = document.getElementById("chatInput");
  const sendBtn = chatInput.nextElementSibling; // Annahme: Button direkt danach

  // Vorheriges Interval stoppen
  if (chatInterval) clearInterval(chatInterval);

  async function loadChatMessages() {
    try {
      const messages = await apiFetch(`/chat/messages?chatId=${chatId}`);
      chatList.innerHTML = "";

      messages.forEach(msg => {
        const div = document.createElement("div");
        div.className = msg.sender === localStorage.getItem("username") ? "my-msg" : "other-msg";
        div.textContent = `${msg.sender}: ${msg.content}`;
        chatList.appendChild(div);
      });

      // Scroll nach unten
      chatList.scrollTop = chatList.scrollHeight;

    } catch (err) {
      console.error("Chat laden fehlgeschlagen:", err);
      chatList.textContent = "Fehler beim Laden des Chats.";
    }
  }

  // Sofort laden
  await loadChatMessages();

  // Alle 5 Sekunden aktualisieren
  chatInterval = setInterval(loadChatMessages, 5000);

  // Input aktivieren
  chatInput.disabled = false;
  sendBtn.disabled = false;
  sendBtn.textContent = "Senden";
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
