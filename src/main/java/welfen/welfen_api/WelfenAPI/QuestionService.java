package welfen.welfen_api.WelfenAPI;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import welfen.welfen_api.WelfenAPI.model.Question;
import welfen.welfen_api.WelfenAPI.repo.QuestionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Question addQuestion(String username, String subject, String content) {
        Question question = new Question(username, subject, content, LocalDateTime.now());
        return questionRepository.save(question);
    }


    public List<Question> getAllQuestions() {
        return questionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Question> getQuestionsBySubject(String subject) {
        return questionRepository.findBySubjectOrderByCreatedAtDesc(subject);
    }
    
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Frage nicht gefunden"));
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    // Automatische Löschung nach 7 Tagen (geplant jede Stunde)
    @Scheduled(cron = "0 0 * * * *") // jede volle Stunde
    public void deleteOldQuestions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        questionRepository.findAll().stream()
                .filter(q -> q.getCreatedAt().isBefore(cutoff))
                .forEach(q -> questionRepository.deleteById(q.getId()));
    }
}
