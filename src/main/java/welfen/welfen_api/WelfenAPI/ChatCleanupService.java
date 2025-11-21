package welfen.welfen_api.WelfenAPI;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ChatCleanupService {

    private final ChatService chatService;

    public ChatCleanupService(ChatService chatService) {
        this.chatService = chatService;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Jeden Tag um Mitternacht
    public void cleanOldChats() {
        try {
            chatService.deleteOldChats();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
