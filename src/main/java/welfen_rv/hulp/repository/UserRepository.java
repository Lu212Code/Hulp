package welfen_rv.hulp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import welfen_rv.hulp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Wichtig für den Login und die Punktevergabe
    User findByUsername(String username);
    List<User> findTop10ByOrderByPunkteDesc();
}