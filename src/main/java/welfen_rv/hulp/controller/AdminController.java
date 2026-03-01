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
import org.springframework.web.bind.annotation.*;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    public String handleImport(@RequestParam("namenListe") String namenListe, Model model, HttpSession session) {
        String[] namenArray = namenListe.trim().split("\\r?\\n");
        List<String> namen = Arrays.stream(namenArray)
                .map(n -> n.replace("\uFEFF", "").trim())
                .filter(n -> !n.isEmpty())
                .toList();

        Map<String, String> credentials = userService.createUsersAndReturnPasswords(namen);
        session.setAttribute("pdfCredentials", credentials);

        model.addAttribute("newUsers", credentials);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("downloadPdf", true); 

        return "admin/users";
    }
    
    @GetMapping("/users/download-pdf")
    public void downloadPdf(HttpSession session, HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> credentials = (Map<String, String>) session.getAttribute("pdfCredentials");

        if (credentials == null || credentials.isEmpty()) {
            response.sendRedirect("/admin/users");
            return;
        }

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
        session.removeAttribute("pdfCredentials");
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }
    
    @GetMapping("/chats")
    public String chatModeration(Model model) {
        model.addAttribute("fragen", frageRepository.findAll());
        return "admin/chats";
    }

    @PostMapping("/chats/delete/{id}")
    public String deleteChat(@PathVariable Long id) {
        chatMessageRepository.deleteByFrageId(id); 
        frageRepository.deleteById(id);
        return "redirect:/admin/chats?deleted";
    }
    
    @PostMapping("/users/update-role/{id}")
    public String updateRole(@PathVariable Long id, @RequestParam String neueRolle) {
        userRepository.findById(id).ifPresent(u -> {
            u.setRole(neueRolle);
            userRepository.save(u);
        });
        return "redirect:/admin/users?roleUpdated";
    }
}