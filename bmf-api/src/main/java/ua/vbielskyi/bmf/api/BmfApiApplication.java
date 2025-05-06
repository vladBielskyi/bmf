package ua.vbielskyi.bmf.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"ua.vbielskyi"})
public class BmfApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BmfApiApplication.class, args);
    }

}
