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
    // Holt alle Nachrichten für einen bestimmten Chat, sortiert nach Zeit
    List<ChatMessage> findByFrageIdOrderByZeitstempelAsc(Long frageId);
}