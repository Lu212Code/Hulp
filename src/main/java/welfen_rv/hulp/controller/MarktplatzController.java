package welfen_rv.hulp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import welfen_rv.hulp.model.Frage;
import welfen_rv.hulp.repository.FrageRepository;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Controller
public class MarktplatzController {

    @Autowired
    private FrageRepository frageRepository;

    // 1. Übersicht mit Filtern
    @GetMapping("/marktplatz")
    public String marktplatzListe(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Integer maxPreis,
                                  Model model) {
        
        List<Frage> anzeigen = frageRepository.findAll().stream()
                .filter(f -> "MARKTPLATZ".equals(f.getBeitragTyp()))
                .filter(f -> q == null || q.isEmpty() || 
                        f.getFach().toLowerCase().contains(q.toLowerCase()) || 
                        f.getText().toLowerCase().contains(q.toLowerCase()))
                .filter(f -> maxPreis == null || f.getPreis() <= maxPreis)
                .toList();

        model.addAttribute("anzeigen", anzeigen);
        return "marktplatz";
    }

    // 2. Seite zum Erstellen aufrufen
    @GetMapping("/marktplatz/neu")
    public String marktplatzNeu() {
        return "marktplatz_neu";
    }

    // 3. Anzeige speichern (inkl. Bild & KI-Check)
    @PostMapping("/marktplatz/neu")
    public String marktplatzSpeichern(@RequestParam String name,
                                      @RequestParam String beschreibung,
                                      @RequestParam Integer preis,
                                      @RequestParam("bild") MultipartFile bild,
                                      @AuthenticationPrincipal UserDetails user) throws IOException {

        // KI-CHECK: Simulierter Check auf unangemessene Inhalte
        if (!isContentSafe(name + " " + beschreibung)) {
            return "redirect:/marktplatz/neu?error=ai_rejected";
        }

        Frage anzeige = new Frage();
        anzeige.setFach(name); // Wir nutzen 'Fach' als Artikelnamen
        anzeige.setText(beschreibung);
        anzeige.setPreis(preis);
        anzeige.setBeitragTyp("MARKTPLATZ");
        anzeige.setErsteller(user.getUsername());
        anzeige.setGeloest(false);

        // Bild-Verarbeitung
        if (!bild.isEmpty()) {
            String base64Bild = Base64.getEncoder().encodeToString(bild.getBytes());
            anzeige.setBild(base64Bild); // Sicherstellen, dass 'bild' in Frage.java existiert!
        }

        frageRepository.save(anzeige);
        return "redirect:/marktplatz";
    }

    // KI-Prüfungs-Logik (Platzhalter für echte API)
    private boolean isContentSafe(String content) {
        // Hier könntest du eine API wie OpenAI oder Gemini anbinden
        // Beispiel: Blockiere bestimmte Wörter oder zu kurze Texte
        String c = content.toLowerCase();
        if (c.contains("betrug") || c.contains("fake") || c.length() < 10) {
            return false;
        }
        return true;
    }
}