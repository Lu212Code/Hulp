package welfen.welfen_api.WelfenAPI.repo;

import welfen.welfen_api.WelfenAPI.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByOrderByCreatedAtDesc();             // Neueste zuerst
    List<Question> findBySubjectOrderByCreatedAtDesc(String subject); // Nach Fach sortiert
}
