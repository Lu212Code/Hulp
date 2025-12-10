package welfen.welfen_api.WelfenAPI;

import welfen.welfen_api.WelfenAPI.model.Chat;
import welfen.welfen_api.WelfenAPI.model.User;
import welfen.welfen_api.WelfenAPI.model.UserStats;
import welfen.welfen_api.WelfenAPI.repo.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Path whitelistPath;

    private Map<String, UserStats> stats = new HashMap<>();
    private final File statsFile = new File("user_stats.json");
    private final ObjectMapper mapper = new ObjectMapper();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.whitelistPath = Path.of("allowed-users.txt");
        createWhitelistIfNotExists();
        loadStats();
    }

    private void createWhitelistIfNotExists() {
        if (!Files.exists(whitelistPath)) {
            try {
                Files.createFile(whitelistPath);
            } catch (IOException e) {
                throw new RuntimeException("WHITELIST_CREATION_FAILED", e);
            }
        }
    }

    private List<String> getAllowedUsernames() {
        try {
            return Files.lines(whitelistPath)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("WHITELIST_READ_FAILED", e);
        }
    }

    // Registrierung
    public User register(String username, String password, String role) {
        if (!getAllowedUsernames().contains(username)) {
            throw new RuntimeException("USERNAME_NOT_ALLOWED");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("USERNAME_ALREADY_EXISTS");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    // Login
    public boolean login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        return passwordEncoder.matches(password, user.get().getPassword());
    }

    // Passwort ändern
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Benutzer löschen
    public void deleteUser(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        userRepository.delete(user.get());
        
        List<Chat> userChats = new ArrayList<Chat>();
        userChats = ChatController.chatService.getActiveChatsForUser(username);
        
        for (Chat chat:userChats) {
        	try {
				ChatController.chatService.deleteChat(chat.getChatId());
			} catch (Exception e) {
				System.err.println("Chat konnte nicht gelöscht werden: ");
				e.printStackTrace();
			}
        }
    }
    
    // Rolle eines Benutzers abfragen
    public String getRole(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        return user.getRole();
    }
    
    public void setRole(String username, String role) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        user.setRole(role);
        userRepository.save(user);
    }
    
    // Alle Benutzer abrufen
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // Stats laden
    private void loadStats() {
        if (statsFile.exists()) {
            try {
                stats = mapper.readValue(statsFile, new TypeReference<Map<String, UserStats>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                stats = new HashMap<>();
            }
        }
    }

    // Stats speichern
    private void saveStats() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(statsFile, stats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public UserStats getStats(String username) {
        return stats.computeIfAbsent(username, k -> new UserStats());
    }

    public void addAsked(String username) {
        getStats(username).incrementAsked();
        saveStats();
    }

    public void addAnswered(String username) {
        getStats(username).incrementAnswered();
        saveStats();
    }
    
    public List<LeaderboardEntry> getLeaderboardTop50() {
        return stats.entrySet().stream()
            .map(e -> new LeaderboardEntry(e.getKey(), e.getValue().getAnswered() * 5))
            .sorted(Comparator.comparingInt(LeaderboardEntry::getPoints).reversed())
            .limit(50)
            .collect(Collectors.toList());
    }
    
    public LeaderboardPosition getUserPosition(String username) {
        // Liste sortiert nach Punkten absteigend
        List<Map.Entry<String, UserStats>> sorted = stats.entrySet().stream()
            .sorted((a,b) -> Integer.compare(b.getValue().getAnswered() * 5, a.getValue().getAnswered() * 5))
            .collect(Collectors.toList());

        int position = 1;
        for (Map.Entry<String, UserStats> entry : sorted) {
            if (entry.getKey().equals(username)) {
                int points = entry.getValue().getAnswered() * 5;
                return new LeaderboardPosition(position, points);
            }
            position++;
        }
        return new LeaderboardPosition(-1, 0); // falls nicht gefunden
    }
    
    public static class LeaderboardEntry {
        private String username;
        private int points;

        public LeaderboardEntry(String username, int points) {
            this.username = username;
            this.points = points;
        }

        public String getUsername() { return username; }
        public int getPoints() { return points; }
    }

    public static class LeaderboardPosition {
        private int position;
        private int points;

        public LeaderboardPosition(int position, int points) {
            this.position = position;
            this.points = points;
        }

        public int getPosition() { return position; }
        public int getPoints() { return points; }
    }
}
