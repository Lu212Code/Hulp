package welfen.welfen_api.WelfenAPI;

import org.springframework.stereotype.Service;
import welfen.welfen_api.WelfenAPI.model.Chat;
import welfen.welfen_api.WelfenAPI.model.Message;
import welfen.welfen_api.WelfenAPI.util.ChatEncryption;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private final Path chatDir = Path.of("chats"); // Verzeichnis für Chat-Dateien
    private final Map<String, Chat> activeChats = new HashMap<>(); // Chat-ID -> Chat

    public ChatService() throws Exception {
        if (!chatDir.toFile().exists()) chatDir.toFile().mkdir();
    }

    // Chat erstellen mit ursprünglicher Frage
    public Chat createChat(Long questionId, String askerUsername, String questionContent) throws Exception {
        String chatId = UUID.randomUUID().toString();
        Chat chat = new Chat(chatId, questionId, askerUsername, null, false, LocalDateTime.now());

        // Ursprüngliche Frage als erste Nachricht
        List<Message> messages = new ArrayList<>();
        messages.add(new Message(askerUsername, questionContent, LocalDateTime.now()));
        saveMessages(chatId, messages);

        activeChats.put(chatId, chat);
        return chat;
    }

    // Helfer zuweisen und Chat aktivieren
    public void assignHelper(String chatId, String helperUsername) {
        Chat chat = activeChats.get(chatId);
        if (chat != null) {
            chat.setHelperUsername(helperUsername);
            chat.setActive(true);
        }
    }

    // Nachricht hinzufügen
    public void addMessage(String chatId, String sender, String content) throws Exception {
        List<Message> messages = loadMessages(chatId);
        messages.add(new Message(sender, content, LocalDateTime.now()));
        saveMessages(chatId, messages);
    }

    // Nachrichten abrufen
    public List<Message> getMessages(String chatId) throws Exception {
        return loadMessages(chatId);
    }

    // Chat nach ID abrufen
    public Chat getChatById(String chatId) {
        return activeChats.get(chatId);
    }

    // Chat beenden
    public void endChat(String chatId) {
        activeChats.remove(chatId);
    }

    // Nachrichten in verschlüsselter Datei speichern
    private void saveMessages(String chatId, List<Message> messages) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            sb.append(m.getTimestamp()).append("|").append(m.getSender()).append("|").append(m.getContent()).append("\n");
        }
        ChatEncryption.saveEncrypted(chatDir.resolve("chat-" + chatId + ".aes"), sb.toString());
    }

    // Nachrichten aus verschlüsselter Datei laden
    private List<Message> loadMessages(String chatId) throws Exception {
        Path file = chatDir.resolve("chat-" + chatId + ".aes");
        if (!file.toFile().exists()) return new ArrayList<>();
        String decrypted = ChatEncryption.loadDecrypted(file);
        List<Message> messages = new ArrayList<>();
        for (String line : decrypted.split("\n")) {
            String[] parts = line.split("\\|", 3);
            messages.add(new Message(parts[1], parts[2], LocalDateTime.parse(parts[0])));
        }
        return messages;
    }
    
    public List<Chat> getActiveChatsForUser(String username) {
        List<Chat> userChats = new ArrayList<>();
        for (Chat chat : activeChats.values()) {
            if ((chat.getAskerUsername() != null && chat.getAskerUsername().equals(username)) ||
                (chat.getHelperUsername() != null && chat.getHelperUsername().equals(username))) {
                userChats.add(chat);
            }
        }
        return userChats;
    }
}
