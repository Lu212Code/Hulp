package welfen.welfen_api.WelfenAPI.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String content;
    private String username;
    private LocalDateTime createdAt;

    // Leerer Konstruktor für JPA
    public Question() {}

    // Konstruktor für manuelles Erstellen
    public Question(String username, String subject, String content, LocalDateTime createdAt) {
        this.username = username;
        this.subject = subject;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getter und Setter
    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
