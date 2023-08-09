package searchengine;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring application starter class.
 */
@SpringBootApplication
public class Application {
    /**
     * Starts application.
     *
     * @param args Command line parameters.
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
