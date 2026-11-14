package i_itsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // runs the SLA deadline check every minute (SLAService)
public class IItsmApplication {

	public static void main(String[] args) {
		SpringApplication.run(IItsmApplication.class, args);
	}

}
