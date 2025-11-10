package welfen.welfen_api.WelfenAPI.model;

import java.time.LocalDateTime;

public class Chat {

    private String chatId;
    private Long questionId;
    private String askerUsername;
    private String helperUsername; // null bis Helfer zugewiesen wird
    private boolean active;
    private LocalDateTime createdAt;

    public Chat() {}

    public Chat(String chatId, Long questionId, String askerUsername, String helperUsername, boolean active, LocalDateTime createdAt) {
        this.chatId = chatId;
        this.questionId = questionId;
        this.askerUsername = askerUsername;
        this.helperUsername = helperUsername;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getAskerUsername() { return askerUsername; }
    public void setAskerUsername(String askerUsername) { this.askerUsername = askerUsername; }

    public String getHelperUsername() { return helperUsername; }
    public void setHelperUsername(String helperUsername) { this.helperUsername = helperUsername; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
