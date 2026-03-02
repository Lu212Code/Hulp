package welfen_rv.hulp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Frage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fach;
    private String klasse;
    
    private String helfer;
    
    private String beitragTyp;
    private Integer preis;
    
    @Column(columnDefinition = "TEXT") // Damit auch lange Texte reinpassen
    private String text;
    
    private String ersteller;
    private LocalDateTime erstelltAm;
    private boolean geloest = false;
    
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String bild;

    // Konstruktoren
    public Frage() {
        this.erstelltAm = LocalDateTime.now();
    }

    // Getter und Setter
    public Long getId() { return id; }
    public String getFach() { return fach; }
    public void setFach(String fach) { this.fach = fach; }
    public String getKlasse() { return klasse; }
    public void setKlasse(String klasse) { this.klasse = klasse; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getErsteller() { return ersteller; }
    public void setErsteller(String ersteller) { this.ersteller = ersteller; }
    public boolean isGeloest() { return geloest; }
    public void setGeloest(boolean geloest) { this.geloest = geloest; }
    public String getHelfer() { return helfer; }
    public void setHelfer(String helfer) { this.helfer = helfer; }
	public String getBeitragTyp() { return beitragTyp; }
	public void setBeitragTyp(String beitragTyp) { this.beitragTyp = beitragTyp; }
	public Integer getPreis() { return preis; }
	public void setPreis(Integer preis) { this.preis = preis; }
	public String getBild() { return bild; }
	public void setBild(String bild) { this.bild = bild; }
}