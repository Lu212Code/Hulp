package welfen_rv.hulp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 1. Alles, was JEDER (auch ohne Login) sehen muss
                .requestMatchers(
                    "/css/**", 
                    "/js/**", 
                    "/login", 
                    "/h2-console/**",
                    "/ws-chat/**",
                    "/datenschutz",         // Explizite Freigabe der Route
                    "/agb",                  // Explizite Freigabe der Route
                    "/nutzungsbedingungen",  // Explizite Freigabe der Route
                    "/impressum"             // Explizite Freigabe der Route
                ).permitAll()
                
                // 2. Admin-Bereich
                .requestMatchers("/admin/**").hasAuthority("admin")
                
                // 3. Alles andere erfordert Login
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")             // Deine eigene Seite
                .loginProcessingUrl("/login")    // Die URL, an die das Formular postet
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout") // Kleiner Zusatz: Zeigt dem User, dass er ausgeloggt wurde
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()) 
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}