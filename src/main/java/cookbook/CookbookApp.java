package cookbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Starts the app on http://localhost:8082
@SpringBootApplication
@EnableScheduling // the nightly Backup
public class CookbookApp {
    public static void main(String[] args) {
        SpringApplication.run(CookbookApp.class, args);
    }
}
