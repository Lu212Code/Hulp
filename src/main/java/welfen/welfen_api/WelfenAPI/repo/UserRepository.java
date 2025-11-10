package welfen.welfen_api.WelfenAPI.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import welfen.welfen_api.WelfenAPI.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
