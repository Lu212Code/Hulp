package welfen_rv.hulp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private static final Logger logger = LoggerFactory.getLogger(MarktplatzController.class);

    @Autowired
    private FrageRepository frageRepository;

    // 1. Übersicht mit Filtern
    @GetMapping("/marktplatz")
    public String marktplatzListe(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Integer maxPreis,
                                  Model model) {
    	
    	logger.debug("Accessing Marktplatz with filters - q: {}, maxPreis: {}", q, maxPreis);
        
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
    	logger.debug("Accessing Marktplatz creation page");
        return "marktplatz_neu";
    }

    // 3. Anzeige speichern (inkl. Bild & KI-Check)
    @PostMapping("/marktplatz/neu")
    public String marktplatzSpeichern(@RequestParam String name,
                                      @RequestParam String beschreibung,
                                      @RequestParam Double preis,
                                      @RequestParam("bild") MultipartFile bild,
                                      @AuthenticationPrincipal UserDetails user) throws IOException {
    	
    	logger.debug("Saving new Marktplatz Anzeige - User: {}, Name: {}, Preis: {}", user.getUsername(), name, preis);

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
    	logger.debug("Performing AI content check for: {}", content);
        // Hier könntest du eine API wie OpenAI oder Gemini anbinden
        // Beispiel: Blockiere bestimmte Wörter oder zu kurze Texte
        String c = content.toLowerCase();
        if (c.contains("betrug") || c.contains("fake") || c.length() < 10) {
            return false;
        }
        return true;
    }
    
    @PostMapping("/marktplatz/delete/{id}")
    public String deleteMarktplatzAnzeige(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to delete Marktplatz Anzeige with ID {}", user.getUsername(), id);
        Frage f = frageRepository.findById(id).orElseThrow();
        
        // Sicherheit: Nur der Ersteller darf seine eigene Marktplatz-Anzeige löschen
        if (f.getErsteller().equals(user.getUsername()) && "MARKTPLATZ".equals(f.getBeitragTyp())) {
            frageRepository.delete(f);
        }
        
        return "redirect:/marktplatz?deleted";
    }
}