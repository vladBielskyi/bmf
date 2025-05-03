package ua.vbielskyi.bmf.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ua.vbielskyi.bmf"})
public class BmfApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BmfApiApplication.class, args);
    }

}
