package welfen_rv.hulp.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import welfen_rv.hulp.model.Frage;
import welfen_rv.hulp.model.User;
import welfen_rv.hulp.repository.ChatMessageRepository;
import welfen_rv.hulp.repository.FrageRepository;
import welfen_rv.hulp.repository.UserRepository;

@Controller
public class PageController {
	
	private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private FrageRepository frageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    private final List<String> FAECHER = List.of(
    	    "Mathe", "Deutsch", "Englisch", "Französisch", "Latein", "Spanisch", 
    	    "Italienisch", "NWT", "IMP", "Informatik", "Musik", "Religion", 
    	    "Ethik", "Physik", "Chemie", "Biologie", "Geschichte", "Geografie"
    	);

    // 1. Startseite: Archivierte (gelöste) Fragen anzeigen
    @GetMapping("/")
    public String home(Model model) {
    	logger.debug("Accessing home page - loading archived questions");
    	List<Frage> archivierteFragen = frageRepository.findByGeloest(true).stream()
    	        .filter(f -> !f.getText().equals("[Inhalt vom User gelöscht]"))
    	        .toList();
        model.addAttribute("archivedMessages", archivierteFragen);
        return "home";
    }

    // 2. Fragen-Liste: Alle offenen Fragen anzeigen
    @GetMapping("/fragen")
    public String fragen(@RequestParam(required = false, defaultValue = "FRAGE") String type,
                         @RequestParam(required = false) String fach,
                         @RequestParam(required = false) String search,
                         Model model) {
    	
    	logger.debug("Accessing questions page with filters - type: {}, fach: {}, search: {}", type, fach, search);
        
        List<Frage> offeneAnzeigen = frageRepository.findByGeloestFalse().stream()
                .filter(f -> f.getBeitragTyp().equals(type)) // Filtert nach dem Tab (FRAGE, BIETE_NACHHILFE, etc.)
                .filter(f -> fach == null || fach.isEmpty() || f.getFach().equals(fach))
                .filter(f -> search == null || search.isEmpty() || f.getText().toLowerCase().contains(search.toLowerCase()))
                .toList();

        model.addAttribute("anzeigen", offeneAnzeigen);
        model.addAttribute("currentTab", type);
        model.addAttribute("allFaecher", FAECHER);
        return "fragen";
    }

    // 3. Formular für neue Frage anzeigen
    @GetMapping("/fragen/neu")
    public String neueFrage() {
    	logger.debug("Accessing new question creation page");
        return "neu";
    }

    // 4. Neue Frage SPEICHERN (Das Gegenstück zum HTML Formular)
    @PostMapping("/fragen/neu")
    public String beitragSpeichern(@RequestParam String fach, 
                                   @RequestParam String klasse, 
                                   @RequestParam String frageText,
                                   @RequestParam String beitragTyp,
                                   @RequestParam(defaultValue = "0") Double preis,
                                   @AuthenticationPrincipal UserDetails user) {
    	
    	logger.debug("Saving new contribution - type: {}, fach: {}, klasse: {}, preis: {}", beitragTyp, fach, klasse, preis);
        
        // Wir erstellen EIN neues Objekt
        Frage f = new Frage();
        f.setFach(fach);
        f.setKlasse(klasse);
        f.setText(frageText); // Hier wird dein 'frageText' Feld auf 'text' im Model gemappt
        f.setBeitragTyp(beitragTyp);
        
        // Falls es eine FRAGE ist, Preis immer 0 erzwingen
        if ("FRAGE".equals(beitragTyp)) {
            f.setPreis(0.0);
        } else {
            f.setPreis(preis);
        }
        
        f.setErsteller(user.getUsername());
        f.setGeloest(false); // Sicherstellen, dass sie offen ist
        
        // Nur dieses eine Objekt speichern!
        frageRepository.save(f);
        
        // Redirect zum passenden Tab
        return "redirect:/fragen?type=" + beitragTyp;
    }

    // 5. Leaderboard (Beispielhaft)
    @GetMapping("/leaderboard")
    public String leaderboard(Model model, @AuthenticationPrincipal UserDetails currentUser) {
    	logger.debug("Accessing leaderboard page");
        // 1. Top 10 User laden
        List<User> topUsers = userRepository.findTop10ByOrderByPunkteDesc();
        model.addAttribute("topUsers", topUsers);

        // 2. Eigene Position finden (optional, aber cool)
        if (currentUser != null) {
            List<User> allUsers = userRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "punkte"));
            int rank = -1;
            for (int i = 0; i < allUsers.size(); i++) {
                if (allUsers.get(i).getUsername().equals(currentUser.getUsername())) {
                    rank = i + 1;
                    break;
                }
            }
            model.addAttribute("myRank", rank);
            model.addAttribute("myPoints", userRepository.findByUsername(currentUser.getUsername()).getPunkte());
        }
        return "leaderboard";
    }

    @GetMapping("/profil")
    public String profil(Model model, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("Accessing profile page for user: " + (user != null ? user.getUsername() : "null"));
        if (user != null) {
            String username = user.getUsername();
            User dbUser = userRepository.findByUsername(username);

            // 1. Zähle alle Fragen, die dieser User erstellt hat
            long askedCount = frageRepository.findAll().stream()
                .filter(f -> username.equals(f.getErsteller()))
                .count();

            // 2. Zähle alle Fragen, bei denen der User der Helfer ist UND die gelöst wurden
            long helpedCount = frageRepository.findAll().stream()
                .filter(f -> username.equals(f.getHelfer()) && f.isGeloest())
                .count();

            // 3. Daten ans Model übergeben
            model.addAttribute("askedCount", askedCount);
            model.addAttribute("helpedCount", helpedCount);
            model.addAttribute("punkte", dbUser.getPunkte());
            model.addAttribute("role", dbUser.getRole()); // Falls du das Role-Feld in User.java eingebaut hast
        }
        return "profil";
    }
    
    @PostMapping("/profil/passwort-aendern")
    public String passwortAendern(@RequestParam String neuesPasswort, 
                                  @AuthenticationPrincipal UserDetails currentUser) {
    	logger.debug("User {} is trying to change password", currentUser.getUsername());
        if (neuesPasswort != null && neuesPasswort.length() >= 5) {
            User user = userRepository.findByUsername(currentUser.getUsername());
            if (user != null) {
                user.setPassword(passwordEncoder.encode(neuesPasswort));
                userRepository.save(user);
            }
        }
        return "redirect:/profil?success";
    }
    
    @GetMapping("/meine-chats")
    public String meineChats(Model model, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("Accessing 'My Chats' page for user: {}", user.getUsername());
        String username = user.getUsername();
        List<Frage> aktiveChats = frageRepository.findAll().stream()
            .filter(f -> !f.isGeloest())
            .filter(f -> username.equals(f.getErsteller()) || username.equals(f.getHelfer()))
            .toList();

        model.addAttribute("chats", aktiveChats);
        return "meine_chats";
    }
    

    @GetMapping("/archiv/chat/{id}")
    public String showArchivedChat(@PathVariable Long id, Model model) {
    	logger.debug("Accessing archived chat detail page for Frage ID: {}", id);
        Frage frage = frageRepository.findById(id).orElseThrow();
        
        // Sicherheit: Nur gelöste Fragen im Archiv anzeigen
        if (!frage.isGeloest()) {
            return "redirect:/";
        }

        model.addAttribute("frage", frage);
        model.addAttribute("messages", chatMessageRepository.findByFrageIdOrderByZeitstempelAsc(id));
        return "archiv_detail"; // Eine neue HTML Datei für die Ansicht
    }
    
    @PostMapping("/fragen/delete/{id}")
    public String deleteEigeneFrage(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to delete Frage ID {}", user.getUsername(), id);
        Frage f = frageRepository.findById(id).orElseThrow();
        
        // Wir merken uns den Typ, BEVOR wir löschen
        String aktuellerTyp = f.getBeitragTyp();
        
        // Sicherheit: Nur der Ersteller darf löschen!
        if (f.getErsteller().equals(user.getUsername())) {
            // Nachrichten löschen
            chatMessageRepository.deleteByFrageId(id); 
            // Anzeige löschen
            frageRepository.delete(f);
        }
        
        // Redirect zur neuen Zentrale mit dem passenden Tab
        return "redirect:/fragen?type=" + aktuellerTyp + "&deleted";
    }
    
    @GetMapping("/datenschutz")
    public String datenschutz() {
        return "rechtliches/datenschutz";
    }

    @GetMapping("/nutzungsbedingungen")
    public String nutzung() {
        return "rechtliches/nutzungsbedingungen";
    }

    @GetMapping("/agb")
    public String agb() {
        return "rechtliches/agb";
    }
}