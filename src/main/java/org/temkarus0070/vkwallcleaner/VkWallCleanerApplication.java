package org.temkarus0070.vkwallcleaner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VkWallCleanerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VkWallCleanerApplication.class, args);
    }

}
