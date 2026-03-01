package welfen_rv.hulp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // "user" ist in SQL oft ein reserviertes Wort
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    private String password;
    private int punkte = 0;
    
    @Column(nullable = false)
    private String role = "Standard";

    // Getter und Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getPunkte() { return punkte; }
    public void setPunkte(int punkte) { this.punkte = punkte; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}