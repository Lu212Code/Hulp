package welfen_rv.hulp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import welfen_rv.hulp.model.Frage;
import java.util.List;

@Repository
public interface FrageRepository extends JpaRepository<Frage, Long> {
    
    List<Frage> findByFach(String fach);
    List<Frage> findByGeloest(boolean geloest);
    List<Frage> findByTextContainingIgnoreCase(String keyword);
    List<Frage> findByGeloestFalse();
    List<Frage> findByGeloestFalseAndHelferIsNull();
    List<Frage> findByErstellerOrHelfer(String ersteller, String helfer);
}