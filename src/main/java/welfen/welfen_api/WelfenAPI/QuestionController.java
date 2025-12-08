package welfen.welfen_api.WelfenAPI;

import org.springframework.web.bind.annotation.*;
import welfen.welfen_api.WelfenAPI.model.Question;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final JwtService jwtService;

    public QuestionController(QuestionService questionService, JwtService jwtService) {
        this.questionService = questionService;
        this.jwtService = jwtService;
    }

    @PostMapping("/add")
    public Question addQuestion(
            @RequestHeader("Authorization") String token,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam String klasse   // <--- neu
    ) {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");
        return questionService.addQuestion(username, subject, content, klasse);
    }

    @GetMapping("/all")
    public List<Question> getAllQuestions(@RequestHeader("Authorization") String token) {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");
        return questionService.getAllQuestions();
    }

    @GetMapping("/subject")
    public List<Question> getQuestionsBySubject(@RequestHeader("Authorization") String token,
                                                @RequestParam String subject) {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");
        return questionService.getQuestionsBySubject(subject);
    }
    
    @GetMapping("/{id}")
    public Question getQuestionById(@RequestHeader("Authorization") String token,
                                    @PathVariable Long id) {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");
        return questionService.getQuestionById(id);
    }

    @DeleteMapping("/delete")
    public void deleteQuestion(@RequestHeader("Authorization") String token,
                               @RequestParam Long id) {
        String username = jwtService.validateToken(token);
        if (username == null) throw new RuntimeException("Nicht eingeloggt");
        questionService.deleteQuestion(id);
    }
}
