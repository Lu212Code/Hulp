package welfen.welfen_api.WelfenAPI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*") // erlaubt alle Domains (auch file://)
                        .allowedMethods("*")        // GET, POST, PUT, DELETE, OPTIONS, etc.
                        .allowedHeaders("*")        // alle Header erlaubt
                        .allowCredentials(true)     // Cookies / Auth erlaubt
                        .maxAge(3600);              // Cache-Zeit für Preflight-Anfragen
            }
        };
    }
}
