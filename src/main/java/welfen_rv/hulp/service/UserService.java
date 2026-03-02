package welfen_rv.hulp.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import welfen_rv.hulp.controller.PageController;
import welfen_rv.hulp.model.User;
import welfen_rv.hulp.repository.UserRepository;

@Service
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);
	
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Generiert 5-stellige Passwörter (Buchstaben & Zahlen)
    private String generateRandomPassword() {
    	logger.debug("Generating random password");
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void createUsers(List<String> usernames) {
    	logger.info("Creating users from list: {}", usernames);
        for (String name : usernames) {
            if (userRepository.findByUsername(name) == null) {
                String rawPassword = generateRandomPassword();
                
                User user = new User();
                user.setUsername(name);
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setPunkte(0);
                
                if (name.equalsIgnoreCase("admin")) {
                	user.setRole("admin");
                }
                
                userRepository.save(user);
            }
        }
    }
    
    public Map<String, String> createUsersAndReturnPasswords(List<String> usernames) {
    	logger.info("Creating users and returning passwords for list: {}", usernames);
        Map<String, String> credentials = new HashMap<>();
        for (String name : usernames) {
            String trimmedName = name.trim();
            if (!trimmedName.isEmpty() && userRepository.findByUsername(trimmedName) == null) {
                String rawPassword = generateRandomPassword();
                User user = new User();
                user.setUsername(trimmedName);
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setRole(trimmedName.equalsIgnoreCase("admin") ? "admin" : "Standard");
                userRepository.save(user);
                credentials.put(trimmedName, rawPassword);
            }
        }
        return credentials;
    }
}