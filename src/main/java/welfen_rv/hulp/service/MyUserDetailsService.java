package welfen_rv.hulp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import welfen_rv.hulp.controller.PageController;
import welfen_rv.hulp.model.User;
import welfen_rv.hulp.repository.UserRepository;

@Service
public class MyUserDetailsService implements UserDetailsService {

	private static final Logger logger = LoggerFactory.getLogger(MyUserDetailsService.class);
	
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    	logger.debug("Loading user by username: {}", username);
        User user = userRepository.findByUsername(username);
        if (user == null) throw new UsernameNotFoundException("User nicht gefunden");

        String dbRole = user.getRole(); 
        if (dbRole == null) dbRole = "Standard"; // Sicherheitsnetz

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(dbRole)
                .build();
    }
}