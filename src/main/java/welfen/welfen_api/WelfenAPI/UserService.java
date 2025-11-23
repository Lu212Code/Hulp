package welfen.welfen_api.WelfenAPI;

import welfen.welfen_api.WelfenAPI.model.User;
import welfen.welfen_api.WelfenAPI.repo.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Path whitelistPath;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.whitelistPath = Path.of("allowed-users.txt");
        createWhitelistIfNotExists();
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
}
