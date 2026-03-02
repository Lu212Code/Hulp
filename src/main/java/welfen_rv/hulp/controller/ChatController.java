package welfen_rv.hulp.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import welfen_rv.hulp.model.ChatMessage;
import welfen_rv.hulp.model.Frage;
import welfen_rv.hulp.model.User;
import welfen_rv.hulp.repository.ChatMessageRepository;
import welfen_rv.hulp.repository.FrageRepository;
import welfen_rv.hulp.repository.UserRepository;

@Controller
public class ChatController {

	private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
	
    @Autowired
    private FrageRepository frageRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private UserRepository userRepository; // Für die Punkte

    @GetMapping("/chat/{id}")
    public String showChat(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to access chat for Frage ID {}", user.getUsername(), id);
        Frage frage = frageRepository.findById(id).orElseThrow();
        
        // Sicherheit: Nur Frager und Helfer dürfen in den Chat
        if (!user.getUsername().equals(frage.getErsteller()) && !user.getUsername().equals(frage.getHelfer())) {
            logger.warn("Unauthorized access attempt: User {} tried to access chat for Frage ID {}", user.getUsername(), id);
            return "redirect:/fragen";
        }

        model.addAttribute("frage", frage);
        model.addAttribute("messages", chatMessageRepository.findByFrageIdOrderByZeitstempelAsc(id));
        model.addAttribute("helfer", frage.getHelfer());
        return "chat";
    }

    @MessageMapping("/chat.send/{id}")
    @SendTo("/topic/messages/{id}")
    public ChatMessage sendMessage(@DestinationVariable Long id, ChatMessage chatMsg, Principal principal) {
    	logger.debug("Received message for Frage ID {}.", id);
        chatMsg.setFrageId(id);
        chatMsg.setSender(principal.getName());
        chatMsg.setZeitstempel(LocalDateTime.now());
        
        // Speichern in DB (optional, aber für Verlauf nötig)
        return chatMessageRepository.save(chatMsg);
    }
    
    @GetMapping("/chat/start/{id}")
    public String helpFrage(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to help with Frage ID {}", user.getUsername(), id);
        Frage beitrag = frageRepository.findById(id).orElseThrow();
        String aktuellerUser = user.getUsername();

        if (beitrag.getErsteller().equals(aktuellerUser)) {
            // Dynamische Rückleitung basierend auf dem Typ
            if ("MARKTPLATZ".equals(beitrag.getBeitragTyp())) {
                return "redirect:/marktplatz?error=self";
            }
            return "FRAGE".equals(beitrag.getBeitragTyp()) ? 
                   "redirect:/fragen?error=self" : "redirect:/nachhilfe?error=self";
        }

        if (beitrag.getHelfer() == null) {
            beitrag.setHelfer(aktuellerUser);
            frageRepository.save(beitrag); 
        }

        return "redirect:/chat/" + id;
    }

    @PostMapping("/chat/beenden/{id}")
    public String beenden(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to end chat for Frage ID {}", user.getUsername(), id);
        Frage frage = frageRepository.findById(id).orElseThrow();
        
        // Nur der Frager darf beenden!
        if (frage.getErsteller().equals(user.getUsername())) {
            frage.setGeloest(true);
            frageRepository.save(frage);

            // Punkte vergeben (Logik beispielhaft)
            updatePunkte(frage.getHelfer(), 20);
            updatePunkte(frage.getErsteller(), 5);
        }
        return "redirect:/";
    }

    private void updatePunkte(String username, int punkte) {
    	logger.info("Updating points for user {}: adding {} points", username, punkte);
        User u = userRepository.findByUsername(username);
        if(u != null) {
            u.setPunkte(u.getPunkte() + punkte);
            userRepository.save(u);
        }
    }
    
    @PostMapping("/chat/archivieren/{id}")
    public ResponseEntity<Void> archivieren(@PathVariable Long id, 
                                            @RequestBody List<String> sensitiveData, 
                                            @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to archive chat for Frage ID {}", user.getUsername(), id);
        Frage frage = frageRepository.findById(id).orElseThrow();
        
        if (frage.getErsteller().equals(user.getUsername())) {
            // 1. Alle Chat-Nachrichten holen
            List<ChatMessage> messages = chatMessageRepository.findByFrageIdOrderByZeitstempelAsc(id);
            
            // 2. Anonymisierung des Frage-Textes
            String cleanFrageText = anonymize(frage.getText(), sensitiveData);
            frage.setText(cleanFrageText);
            
            // 3. Anonymisierung der Nachrichten
            for (ChatMessage msg : messages) {
                msg.setText(anonymize(msg.getText(), sensitiveData));
                chatMessageRepository.save(msg);
            }

            // 4. Status setzen
            frage.setGeloest(true);
            frageRepository.save(frage);

            // Belohnungssystem
            updatePunkte(frage.getHelfer(), 20);
            updatePunkte(frage.getErsteller(), 5);
            
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private String anonymize(String text, List<String> sensitiveData) {
    	if (text == null) return "";
        logger.debug("Anonymizing text for a message (length: {})", text.length());
        String cleanText = text;
        for (String data : sensitiveData) {
            if (data != null && !data.isEmpty()) {
                // Case-insensitive Replacement
                cleanText = cleanText.replaceAll("(?i)" + Pattern.quote(data), "[ANONYMISIERT]");
            }
        }
        return cleanText;
    }
    
    @PostMapping("/chat/beenden-ohne-archiv/{id}")
    public ResponseEntity<Void> beendenOhneArchiv(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
    	logger.debug("User {} is trying to end chat without archiving for Frage ID {}", user.getUsername(), id);
        Frage frage = frageRepository.findById(id).orElseThrow();
        
        if (frage.getErsteller().equals(user.getUsername())) {
            // 1. Nachrichten löschen (damit nichts im Archiv landen kann)
            List<ChatMessage> messages = chatMessageRepository.findByFrageIdOrderByZeitstempelAsc(id);
            chatMessageRepository.deleteAll(messages);
            
            // 2. Frage als gelöst markieren, aber wir "verstecken" sie
            // Entweder du löscht die Frage ganz, oder du setzt den Text auf einen Platzhalter
            frage.setGeloest(true);
            frage.setText("[Inhalt vom User gelöscht]");
            frageRepository.save(frage);

            // Punkte gibt es trotzdem für die Hilfe
            updatePunkte(frage.getHelfer(), 20);
            updatePunkte(frage.getErsteller(), 5);
            
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}