async function loadFragen() {
  const fragenListe = document.getElementById("fragenListe");
  fragenListe.innerHTML = "Lade...";
  try {
    const fragen = await apiFetch("/questions/all");
    fragenListe.innerHTML = "";
    fragen.forEach(q => {
      const div = document.createElement("div");
      div.className = "pin-btn";
      div.textContent = `${q.subject}: ${q.content} - Von ${q.username}`;
      div.onclick = () => openChat(q.id);
      fragenListe.appendChild(div);
    });
  } catch (err) {
    fragenListe.textContent = "Fehler beim Laden.";
  }
}

async function addFrage() {
  const subject = document.getElementById("fach").value;
  const content = document.getElementById("frageText").value;
  if (!subject || !content) return await showHulpPopup("Bitte fach und Text eingeben!");
  await apiFetch("/questions/add", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ subject, content })
  });
  await showHulpPopup("Frage hinzugefügt!");

  document.getElementById("frageText").value = "";
  loadFragen();
}

function showHulpPopup(message, options = { ok:true, cancel:false }) {
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
