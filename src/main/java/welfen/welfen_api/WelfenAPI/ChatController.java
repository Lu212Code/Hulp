package welfen.welfen_api.WelfenAPI;

import org.springframework.web.bind.annotation.*;
import welfen.welfen_api.WelfenAPI.model.Chat;
import welfen.welfen_api.WelfenAPI.model.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public static ChatService chatService;
    private final JwtService jwtService;
    private final QuestionService questionService;
    private final StatsService stats;

    public ChatController(ChatService chatService, JwtService jwtService, QuestionService questionService, StatsService stats) {
        this.chatService = chatService;
        this.jwtService = jwtService;
        this.questionService = questionService;
        this.stats = stats;
    }

    /**
     * Chat erstellen für eine Frage
     * Fragesteller wird automatisch hinzugefügt
     */
    @PostMapping("/create")
    public Chat createChat(@RequestHeader("Authorization") String token,
                           @RequestParam Long questionId) throws Exception {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        // Frage aus QuestionService holen
        var question = questionService.getQuestionById(questionId);
        if (question == null) throw new RuntimeException("Frage nicht gefunden");

        return chatService.createChat(questionId, username, question.getContent());
    }

    /**
     * Helfer zuweisen → Chat aktivieren
     * Die Frage wird aus offenen Fragen gelöscht
     */
    @PostMapping("/assign-helper")
    public Chat assignHelper(
            @RequestHeader("Authorization") String token,
            @RequestParam String chatId,
            @RequestParam String askerUsername) throws Exception {

        // Helfer-Username validieren
        String helperUsername = jwtService.validateToken(token);
        if (helperUsername == null) throw new RuntimeException("Nicht eingeloggt");

        // Helfer und Fragesteller im Chat setzen
        chatService.assignHelper(chatId, helperUsername, askerUsername);

        // Chat auslesen
        Chat chat = chatService.getChatById(chatId);

        // Frage aus offenen Fragen löschen
        questionService.deleteQuestion(chat.getQuestionId());

        stats.addBeantwortet();
        
        return chat;
    }

    /**
     * Nachricht senden
     */
    @PostMapping("/send")
    public void sendMessage(@RequestHeader("Authorization") String token,
                            @RequestParam String chatId,
                            @RequestParam String content) throws Exception {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        stats.addMessage();
        chatService.addMessage(chatId, username, content);
    }

    /**
     * Nachrichten abrufen
     */
    @GetMapping("/messages")
    public List<Message> getMessages(@RequestHeader("Authorization") String token,
                                     @RequestParam String chatId) throws Exception {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        return chatService.getMessages(chatId);
    }

    /**
     * Chat beenden
     */
    @PostMapping("/end")
    public void endChat(@RequestHeader("Authorization") String token,
                        @RequestParam String chatId) throws Exception {

        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        chatService.finishChatAndPersist(chatId);
    }
    
    @GetMapping("/active")
    public List<Chat> getActiveChats(@RequestHeader("Authorization") String token) throws Exception {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        return chatService.getActiveChatsForUser(username);
    }
    
    @GetMapping("/questions")
    public List<String> getAllFinishedQuestions() throws Exception {
        return chatService.getAllStoredQuestions();
    }
    
    @PostMapping("/consent")
    public void giveConsent(
            @RequestHeader("Authorization") String token,
            @RequestParam String chatId,
            @RequestParam boolean consent) throws Exception {

        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        chatService.setAskerConsent(chatId, username, consent);
    }
    
    @GetMapping("/archiv")
    public List<Map<String, String>> getArchivedChats() throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        for (File file : Objects.requireNonNull(chatService.chatDir.toFile().listFiles())) {
            if (file.getName().startsWith("meta-chat-") && file.getName().endsWith(".json")) {
                Map<String, Object> meta = chatService.loadFullChat(file.getName().replace("meta-chat-", "").replace(".json",""));
                Map<String, String> entry = new HashMap<>();
                entry.put("chatId", (String) meta.get("chatId"));
                entry.put("subject", meta.get("questionId").toString()); // Optional: Frage-Fach speichern
                List<Map<String,Object>> msgs = (List<Map<String,Object>>) meta.get("messages");
                entry.put("question", (String) msgs.get(0).get("content"));
                result.add(entry);
            }
        }
        return result;
    }
}
