package welfen_rv.hulp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HulpApplication {

    private static final Logger logger = LoggerFactory.getLogger(HulpApplication.class);

    public static void main(String[] args) {
        logger.info("Starting HulpApplication...");

        SpringApplication.run(HulpApplication.class, args);

        logger.info("HulpApplication started successfully.");
        
    }
}