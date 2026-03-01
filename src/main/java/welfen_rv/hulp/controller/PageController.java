package welfen_rv.hulp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    	List<Frage> archivierteFragen = frageRepository.findByGeloest(true).stream()
    	        .filter(f -> !f.getText().equals("[Inhalt vom User gelöscht]"))
    	        .toList();
        model.addAttribute("archivedMessages", archivierteFragen);
        return "home";
    }

    // 2. Fragen-Liste: Alle offenen Fragen anzeigen
    @GetMapping("/fragen")
    public String fragen(@RequestParam(required = false) String fach,
                         @RequestParam(required = false) String klasse,
                         @RequestParam(required = false) String search,
                         Model model) {
                         
        List<Frage> offeneFragen = frageRepository.findByGeloestFalse().stream()
                .filter(f -> "FRAGE".equals(f.getBeitragTyp()))
                .filter(f -> f.getHelfer() == null) // Nur Fragen ohne Helfer
                // Filter: Fach
                .filter(f -> fach == null || fach.isEmpty() || f.getFach().equals(fach))
                // Filter: Klasse
                .filter(f -> klasse == null || klasse.isEmpty() || f.getKlasse().equals(klasse))
                // Filter: Textsuche
                .filter(f -> search == null || search.isEmpty() || 
                        f.getText().toLowerCase().contains(search.toLowerCase()) || 
                        f.getFach().toLowerCase().contains(search.toLowerCase()))
                .toList();
        
        model.addAttribute("fragen", offeneFragen);
        model.addAttribute("allFaecher", FAECHER); // Wichtig für das Dropdown!
        return "fragen";
    }

    // 2. Nachhilfe-Liste (hier könntest du später auch Filter einbauen)
    @GetMapping("/nachhilfe")
    public String nachhilfeListe(Model model) {
        List<Frage> anzeigen = frageRepository.findByGeloestFalse().stream()
                .filter(f -> !"FRAGE".equals(f.getBeitragTyp()))
                .toList();
                
        model.addAttribute("anzeigen", anzeigen);
        model.addAttribute("allFaecher", FAECHER);
        return "nachhilfe";
    }

    // 3. Formular für neue Frage anzeigen
    @GetMapping("/fragen/neu")
    public String neueFrage() {
        return "neu";
    }

    // 4. Neue Frage SPEICHERN (Das Gegenstück zum HTML Formular)
    @PostMapping("/fragen/neu")
    public String beitragSpeichern(@RequestParam String fach, 
                                   @RequestParam String klasse, 
                                   @RequestParam String frageText,
                                   @RequestParam String beitragTyp,
                                   @RequestParam(defaultValue = "0") Integer preis,
                                   @AuthenticationPrincipal UserDetails user) {
        Frage f = new Frage();
        f.setFach(fach);
        f.setKlasse(klasse);
        f.setText(frageText);
        f.setBeitragTyp(beitragTyp);
        f.setPreis(preis);
        f.setErsteller(user.getUsername());
        
        frageRepository.save(f);
        return (beitragTyp.equals("FRAGE")) ? "redirect:/fragen" : "redirect:/nachhilfe";
    }

    // 5. Leaderboard (Beispielhaft)
    @GetMapping("/leaderboard")
    public String leaderboard(Model model, @AuthenticationPrincipal UserDetails currentUser) {
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
        Frage f = frageRepository.findById(id).orElseThrow();
        
        // Sicherheit: Nur der Ersteller darf löschen!
        if (f.getErsteller().equals(user.getUsername())) {
            // Falls es schon einen Chat gibt, müssen auch die Nachrichten weg
            chatMessageRepository.deleteByFrageId(id); 
            frageRepository.delete(f);
        }
        
        // Zurück zur vorherigen Seite (entweder Fragen oder Nachhilfe)
        return "FRAGE".equals(f.getBeitragTyp()) ? "redirect:/fragen?deleted" : "redirect:/nachhilfe?deleted";
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