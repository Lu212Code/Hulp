package welfen.welfen_api.WelfenAPI;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import welfen.welfen_api.WelfenAPI.model.Chat;
import welfen.welfen_api.WelfenAPI.model.Message;
import welfen.welfen_api.WelfenAPI.util.ChatEncryption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    final Path chatDir = Path.of("chats"); // Verzeichnis für Chat-Dateien
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
    public void assignHelper(String chatId, String helperUsername, String askerUsername) {
        Chat chat = activeChats.get(chatId);
        if (chat != null) {
            chat.setHelperUsername(helperUsername);
            chat.setAskerUsername(askerUsername);
            chat.setActive(true);
            UserController.userService.addAnswered(helperUsername);
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
    
    public void finishChatAndPersist(String chatId) throws Exception {
        Chat chat = activeChats.get(chatId);
        if (chat == null) throw new RuntimeException("Chat nicht gefunden");

        // Nur der Fragesteller muss zustimmen
        if (!chat.isAskerConsent()) {
            activeChats.remove(chatId);
            return;
        }

        // Nachrichten laden
        List<Message> messages = loadMessages(chatId);

        // Sanitizing (siehe vorherige Nachricht)
        List<Map<String, Object>> safeMessages = new ArrayList<>();
        for (Message m : messages) {
            String safeContent = sanitizeContent(m.getContent(), chat);

            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("sender", "<anon>");
            msg.put("content", safeContent);
            msg.put("timestamp", m.getTimestamp().toString());
            safeMessages.add(msg);
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("chatId", chatId);
        meta.put("questionId", chat.getQuestionId());
        meta.put("asker", "NutzerA");
        meta.put("helper", "NutzerB");
        meta.put("createdAt", chat.getCreatedAt().toString());
        meta.put("messages", safeMessages);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(
                chatDir.resolve("meta-chat-" + chatId + ".json").toFile(),
                meta
        );

        activeChats.remove(chatId);
    }
    
    public List<String> getAllStoredQuestions() throws Exception {
        List<String> questions = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (File file : Objects.requireNonNull(chatDir.toFile().listFiles())) {
            if (file.getName().startsWith("meta-chat-") && file.getName().endsWith(".json")) {
                Map<String, Object> meta = mapper.readValue(file, Map.class);

                List<Map<String, Object>> msgs = (List<Map<String, Object>>) meta.get("messages");
                if (!msgs.isEmpty()) {
                    String firstMsg = (String) msgs.get(0).get("content");
                    questions.add(firstMsg);
                }
            }
        }

        return questions;
    }
    
    public Map<String, Object> loadFullChat(String chatId) throws Exception {
        Path file = chatDir.resolve("meta-chat-" + chatId + ".json");
        if (!file.toFile().exists()) throw new RuntimeException("Chat nicht gespeichert");

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file.toFile(), Map.class);
    }
    
    public void setAskerConsent(String chatId, String username, boolean consent) {
        Chat chat = activeChats.get(chatId);
        if (chat == null) throw new RuntimeException("Chat nicht gefunden");

        if (!username.equals(chat.getAskerUsername())) {
            throw new RuntimeException("Nur der Fragesteller darf zustimmen");
        }

        chat.setAskerConsent(consent);
    }
    
    public String sanitizeContent(String content, Chat chat) {

        // Usernamen (nur ganze Wörter)
        content = content.replaceAll(
            "\\b" + chat.getAskerUsername() + "\\b",
            "NutzerA"
        );

        if (chat.getHelperUsername() != null) {
            content = content.replaceAll(
                "\\b" + chat.getHelperUsername() + "\\b",
                "NutzerB"
            );
        }

        // E-Mail
        content = content.replaceAll(
            "(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b",
            "<email>"
        );

        // Telefonnummer (realistisch)
        content = content.replaceAll(
            "\\b(\\+\\d{1,3}[ \\-]?)?(\\(?\\d{2,4}\\)?[ \\-]?)?\\d{3,}[ \\-]?\\d{3,}\\b",
            "<telefon>"
        );

        // IP-Adresse
        content = content.replaceAll(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b",
            "<ip>"
        );

        return content;
    }
    
    public void deleteOldChats() throws IOException {
        File[] files = chatDir.toFile().listFiles();
        if (files == null) return;

        LocalDateTime now = LocalDateTime.now();
        ObjectMapper mapper = new ObjectMapper();

        for (File file : files) {
            if (file.getName().startsWith("meta-chat-") && file.getName().endsWith(".json")) {
                Map<String, Object> meta = mapper.readValue(file, Map.class);
                String createdAtStr = (String) meta.get("createdAt");
                LocalDateTime createdAt = LocalDateTime.parse(createdAtStr);

                if (createdAt.isBefore(now.minusDays(30))) {
                    // Meta-Datei löschen
                    file.delete();

                    // Entsprechende verschlüsselte Chat-Datei löschen
                    String chatId = (String) meta.get("chatId");
                    Path chatFile = chatDir.resolve("chat-" + chatId + ".aes");
                    chatFile.toFile().delete();
                }
            }
        }
    }
    
    public void saveMessagesReflectively(String chatId, List<Message> messages) throws Exception {
        saveMessages(chatId, messages);
    }
    
    public void deleteChat(String chatId) throws Exception {
        boolean existsSomewhere = false;

        if (activeChats.containsKey(chatId)) {
            activeChats.remove(chatId);
            existsSomewhere = true;
        }

        Path encryptedFile = chatDir.resolve("chat-" + chatId + ".aes");
        if (encryptedFile.toFile().exists()) {
            if (!encryptedFile.toFile().delete()) {
                throw new RuntimeException("Konnte verschlüsselte Chat-Datei nicht löschen: " + encryptedFile);
            }
            existsSomewhere = true;
        }

        Path metaFile = chatDir.resolve("meta-chat-" + chatId + ".json");
        if (metaFile.toFile().exists()) {
            if (!metaFile.toFile().delete()) {
                throw new RuntimeException("Konnte Meta-Archiv nicht löschen: " + metaFile);
            }
            existsSomewhere = true;
        }

        if (!existsSomewhere) {
            throw new RuntimeException("Chat mit ID " + chatId + " existiert nicht (weder aktiv noch gespeichert).");
        }
    }
    
    public Chat getChatByQuestionId(Long questionId) {
        List<Chat> activeChats = getAllActiveChats();
        
        for (Chat c : activeChats) {
            if (c.getQuestionId() != null && c.getQuestionId().equals(questionId)) {
                return c;
            }
        }
        
        // Kein Chat gefunden
        return null;
    }
    
    public List<Chat> getAllActiveChats() {
        return new ArrayList<>(activeChats.values());
    }
}
