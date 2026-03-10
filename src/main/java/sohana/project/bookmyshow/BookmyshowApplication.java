package sohana.project.bookmyshow;

import sohana.project.bookmyshow.constant.BookMyShowConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Entry point for the BookMyShow Discovery Service application which acts as Eureka Server.
 */
@SpringBootApplication
@EnableEurekaServer
@Slf4j
public class BookmyshowApplication {

    /**
     * Starts the BookMyShow Discovery Service Spring Boot application.
     *
     * @param args standard application arguments
     */
    public static void main(String[] args) {

        log.info(BookMyShowConstants.LOG_DISCOVERY_STARTUP);

        SpringApplication.run(BookmyshowApplication.class, args);
    }
}

