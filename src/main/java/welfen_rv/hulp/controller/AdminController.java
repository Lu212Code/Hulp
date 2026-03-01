package welfen_rv.hulp.controller;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;
import welfen_rv.hulp.model.Frage;
import welfen_rv.hulp.repository.ChatMessageRepository;
import welfen_rv.hulp.repository.FrageRepository;
import welfen_rv.hulp.repository.UserRepository;
import welfen_rv.hulp.service.UserService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FrageRepository frageRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/users")
    public String userManagement(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/users/import")
    public void handleImport(@RequestParam("namenListe") String namenListe, HttpServletResponse response) throws IOException {
        
        // 1. Robuster Split: Wir trimmen erst den gesamten Block und splitten dann.
        // Das \\s*\\n\\s* sorgt dafür, dass Leerzeichen um den Umbruch herum ignoriert werden.
        String[] namenArray = namenListe.trim().split("\\r?\\n");
        
        List<String> namen = Arrays.stream(namenArray)
                .map(n -> n.replace("\uFEFF", "").trim()) // Versteckte Zeichen & Leerzeichen weg
                .filter(n -> !n.isEmpty())               // Leere Zeilen raus
                .toList();

        // Debug-Check (optional, kannst du in der Konsole prüfen)
        System.out.println("Anzahl erkannter Namen: " + namen.size());

        // 2. User anlegen & Passwörter generieren
        Map<String, String> credentials = userService.createUsersAndReturnPasswords(namen);

        // 3. PDF Setup
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=hulp_logins.pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        document.add(new Paragraph("Hulp Zugangsdaten - " + LocalDate.now(), titleFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(15);
            cell.setMinimumHeight(100);
            cell.setBorderWidth(1);
            cell.setBorderColor(Color.LIGHT_GRAY);

            cell.addElement(new Paragraph("Hulp Login", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.RED)));
            cell.addElement(new Paragraph("Nutzer: " + entry.getKey()));
            cell.addElement(new Paragraph("Passwort: " + entry.getValue()));
            cell.addElement(new Paragraph("\nBitte Passwort im Profil ändern!"));

            table.addCell(cell);
        }
        
        table.completeRow();

        document.add(table);
        document.close();
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }
    
    @GetMapping("/chats")
    public String chatModeration(Model model) {
        // Alle Fragen laden (neueste zuerst)
        List<Frage> alleFragen = frageRepository.findAll();
        model.addAttribute("fragen", alleFragen);
        return "admin/chats";
    }

    @PostMapping("/chats/delete/{id}")
    public String deleteChat(@PathVariable Long id) {
        // 1. Zuerst alle ChatNachrichten löschen, die zu dieser Frage gehören
        // (Falls du keine Cascade-Löschung in der DB hast)
        chatMessageRepository.deleteByFrageId(id); 
        
        // 2. Dann die Frage selbst löschen
        frageRepository.deleteById(id);
        
        return "redirect:/admin/chats?deleted";
    }
}