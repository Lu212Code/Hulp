package welfen.welfen_api.WelfenAPI;

import org.springframework.web.bind.annotation.*;
import welfen.welfen_api.WelfenAPI.model.Chat;
import welfen.welfen_api.WelfenAPI.model.Message;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;
    private final QuestionService questionService;

    public ChatController(ChatService chatService, JwtService jwtService, QuestionService questionService) {
        this.chatService = chatService;
        this.jwtService = jwtService;
        this.questionService = questionService;
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
    public Chat assignHelper(@RequestHeader("Authorization") String token,
                             @RequestParam String chatId) throws Exception {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        chatService.assignHelper(chatId, username);

        Chat chat = chatService.getChatById(chatId);
        // Frage aus offenen Fragen löschen
        questionService.deleteQuestion(chat.getQuestionId());

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

        chatService.endChat(chatId);
    }
    
    @GetMapping("/active")
    public List<Chat> getActiveChats(@RequestHeader("Authorization") String token) throws Exception {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");

        return chatService.getActiveChatsForUser(username);
    }
}
