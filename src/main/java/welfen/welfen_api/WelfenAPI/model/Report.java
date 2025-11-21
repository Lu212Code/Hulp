package welfen.welfen_api.WelfenAPI.model;

import java.time.LocalDateTime;

public class Report {
    private Long id;                  // eindeutige ID
    private String reporterUsername;  // wer meldet
    private String type;              // "chat" oder "question"
    private String targetId;          // Chat-ID oder Frage-ID
    private String reason;            // Grund der Meldung
    private LocalDateTime createdAt;

    public Report(String reporterUsername, String type, String targetId, String reason) {
        this.reporterUsername = reporterUsername;
        this.type = type;
        this.targetId = targetId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public String getTargetId() {
    	return targetId;
    }
}
