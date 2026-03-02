package welfen_rv.hulp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HulpApplication {

	public static void main(String[] args) {
		System.out.println("Starting HulpApplication...");
		SpringApplication.run(HulpApplication.class, args);
		System.out.println("HulpApplication started successfully.");
	}

}
