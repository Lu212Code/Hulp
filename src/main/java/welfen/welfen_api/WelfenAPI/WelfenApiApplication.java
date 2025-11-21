package welfen.welfen_api.WelfenAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WelfenApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WelfenApiApplication.class, args);
	}

}
