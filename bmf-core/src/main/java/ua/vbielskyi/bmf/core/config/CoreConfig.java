package ua.vbielskyi.bmf.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@Configuration
@EntityScan(basePackages = {
        "ua.vbielskyi.bmf.core.entity"
})
@EnableJpaRepositories(basePackages = {
        "ua.vbielskyi.bmf.core.repository"
})
@EnableCaching
@EnableScheduling
public class CoreConfig {
}
