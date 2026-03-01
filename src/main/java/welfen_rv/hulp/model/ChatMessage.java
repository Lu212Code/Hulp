package welfen_rv.hulp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long frageId;
    private String sender;
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    private LocalDateTime zeitstempel;

    public ChatMessage() {
        this.zeitstempel = LocalDateTime.now();
    }
    
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getFrageId() { return frageId; }
	public void setFrageId(Long frageId) { this.frageId = frageId; }
	public String getSender() { return sender; }
	public void setSender(String sender) { this.sender = sender; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
	public LocalDateTime getZeitstempel() { return zeitstempel; }
	public void setZeitstempel(LocalDateTime zeitstempel) { this.zeitstempel = zeitstempel; }
    
    
}