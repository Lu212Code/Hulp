package welfen.welfen_api.WelfenAPI;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import welfen.welfen_api.WelfenAPI.model.Report;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final List<Report> reports = new ArrayList<>();

    @PostMapping("/create")
    public Report createReport(@RequestParam String reporterUsername,
                               @RequestParam String type,
                               @RequestParam String targetId,
                               @RequestParam String reason) {
        Report r = new Report(reporterUsername, type, targetId, reason);
        reports.add(r);
        return r;
    }

    @GetMapping("/all")
    public List<Report> getAllReports() {
        return reports;
    }

    // 🔹 Admin: Report löschen
    @PostMapping("/delete")
    public String deleteReport(@RequestParam String targetId) {
        reports.removeIf(r -> r.getTargetId().equals(targetId));
        return "Report gelöscht";
    }

    // 🔹 Admin: Gemeldete Frage oder Chat löschen (nur Beispiel)
    @PostMapping("/delete-content")
    public String deleteContent(@RequestParam String type, @RequestParam String targetId) {
        // Beispiel: ChatService oder QuestionService anpassen
        if (type.equals("chat")) {
            // ChatService chatService = ...; // injizieren
            // chatService.deleteChat(targetId);
        } else if (type.equals("question")) {
            // QuestionService questionService = ...; // injizieren
            // questionService.deleteQuestion(targetId);
        }
        // Report nach der Aktion löschen
        reports.removeIf(r -> r.getTargetId().equals(targetId));
        return type + " gelöscht und Report entfernt";
    }
}
