package welfen.welfen_api.WelfenAPI;

import org.springframework.web.bind.annotation.*;
import welfen.welfen_api.WelfenAPI.model.Message;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    private final ChatService chatService;

    public AdminChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 🔹 1. Alle Nachrichten eines aktiven Chats abrufen
    @GetMapping("/messages")
    public List<Message> getMessages(@RequestParam String chatId) throws Exception {
        return chatService.getMessages(chatId);
    }

    // 🔹 2. Nachrichten in aktivem Chat löschen (nach Index)
    @PostMapping("/message/delete")
    public String deleteMessage(
            @RequestParam String chatId,
            @RequestParam int index
    ) throws Exception {

        List<Message> msgs = chatService.getMessages(chatId);

        if (index < 0 || index >= msgs.size())
            return "Index ungültig";

        msgs.remove(index);
        chatService.saveMessagesReflectively(chatId, msgs);

        return "Nachricht gelöscht";
    }

    // 🔹 3. Gesamten aktiven Chat löschen
    @PostMapping("/chat/delete")
    public String deleteChat(@RequestParam String chatId) throws Exception {
        chatService.endChat(chatId);

        // Verschlüsselte Datei löschen
        var f = chatService.chatDir.resolve("chat-" + chatId + ".aes");
        Files.deleteIfExists(f);

        return "Chat gelöscht";
    }

    // 🔹 4. Alle archivierten Chats-IDs abrufen
    @GetMapping("/archived")
    public List<String> getArchivedChats() {
        List<String> chats = new ArrayList<>();

        File[] files = chatService.chatDir.toFile().listFiles();
        if (files == null) return chats;

        for (File f : files) {
            if (f.getName().startsWith("meta-chat-") && f.getName().endsWith(".json")) {
                String id = f.getName()
                        .replace("meta-chat-", "")
                        .replace(".json", "");
                chats.add(id);
            }
        }
        return chats;
    }

    // 🔹 5. Archivierten Chat komplett laden (Meta + Nachrichten)
    @GetMapping("/archived/load")
    public Map<String, Object> loadArchivedChat(@RequestParam String chatId) throws Exception {
        return chatService.loadFullChat(chatId);
    }

    // 🔹 6. Archivierten Chat löschen
    @PostMapping("/archived/delete")
    public String deleteArchived(@RequestParam String chatId) throws Exception {

        Files.deleteIfExists(chatService.chatDir.resolve("meta-chat-" + chatId + ".json"));
        Files.deleteIfExists(chatService.chatDir.resolve("chat-" + chatId + ".aes"));

        return "Archivierter Chat gelöscht";
    }
}
