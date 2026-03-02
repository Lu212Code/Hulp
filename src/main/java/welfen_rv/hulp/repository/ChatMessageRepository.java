package welfen_rv.hulp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import welfen_rv.hulp.model.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Transactional
    void deleteByFrageId(Long frageId);

    // Nutzt du vielleicht später für die Archiv-Ansicht
    List<ChatMessage> findByFrageIdOrderByZeitstempelAsc(Long frageId);

    List<ChatMessage> findTop10ByFrageIdOrderByZeitstempelDesc(Long frageId);
}