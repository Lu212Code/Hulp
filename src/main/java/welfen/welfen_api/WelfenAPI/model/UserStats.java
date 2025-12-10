package welfen.welfen_api.WelfenAPI.model;

public class UserStats {
    private int asked;     // Anzahl gestellter Fragen
    private int answered;  // Anzahl beantworteter Fragen

    public UserStats() {
        this.asked = 0;
        this.answered = 0;
    }

    public int getAsked() { return asked; }
    public int getAnswered() { return answered; }

    public void incrementAsked() { this.asked++; }
    public void incrementAnswered() { this.answered++; }

    public void addAsked(int amount) { this.asked += amount; }
    public void addAnswered(int amount) { this.answered += amount; }
}
