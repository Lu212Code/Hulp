package welfen_rv.hulp.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import welfen_rv.hulp.model.User;
import welfen_rv.hulp.repository.UserRepository;

@Service
public class UserInitializer {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        try {
            var resource = getClass().getResourceAsStream("/users.txt");
            if (resource == null) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String username = line.trim();
             // Innerhalb der while-Schleife im UserInitializer
                if (!username.isEmpty() && userRepository.findByUsername(username) == null) {
                    String rawPw = generatePass(5);
                    
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setPassword(passwordEncoder.encode(rawPw));
                    newUser.setPunkte(0);
                    
                    if (username.equalsIgnoreCase("admin")) {
                        newUser.setRole("admin");
                    } else {
                        newUser.setRole("Standard");
                    }
                    
                    userRepository.save(newUser);
                    System.out.println("User angelegt: " + username + " | PW: " + rawPw + " | Rolle: " + newUser.getRole());
                    FileWriter writer = new FileWriter("created_users.txt", true);
                    BufferedWriter bw = new BufferedWriter(writer);
                    bw.write("User angelegt: " + username + " | PW: " + rawPw + " | Rolle: " + newUser.getRole() + "\n");
                    bw.write("Please delete this file after use to avoid security risks.\n");
                    bw.flush();
					bw.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generatePass(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}