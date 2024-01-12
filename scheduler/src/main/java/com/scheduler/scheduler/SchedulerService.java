package com.scheduler.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class SchedulerService implements ApplicationListener<ContextRefreshedEvent> {

    private static final int MAX_EXECUTIONS = 10;
    private int executionCount = 0;
    private Instant springStartupTime;
    private Instant vertxStartupTime;

    @Value("${microservice.spring.url}")
    private String springMicroserviceUrl;

    @Value("${microservice.vertx.url}")
    private String vertxMicroserviceUrl;

    private final ApplicationContext applicationContext;

    public SchedulerService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().equals(applicationContext)) {
            if (springMicroserviceUrl != null && event.getApplicationContext().getParent() == null) {
                springStartupTime = Instant.now();
                System.out.println("Spring Boot Application started at: " + springStartupTime);
            } else if (vertxMicroserviceUrl != null) {
                vertxStartupTime = Instant.now();
                System.out.println("Vert.x Application started at: " + vertxStartupTime);
            }
        }
    }

    @Scheduled(fixedRate = 1000) // Run every 1000 milliseconds (1 second)
    public void performScheduledTask() {
        if (executionCount < MAX_EXECUTIONS) {
            System.out.println("Scheduler is running... (Execution " + (executionCount + 1) + ")");
            makeHttpRequest(springMicroserviceUrl, "Spring Boot");
            makeHttpRequest(vertxMicroserviceUrl, "Vert.x");
            executionCount++;
        } else {
            System.out.println("Scheduler has completed 10 executions. Stopping...");
            System.exit(0);
        }
    }

    private void makeHttpRequest(String url, String microserviceName) {
        long startTime = System.currentTimeMillis();
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println(microserviceName + " Microservice URL: " + url);
        System.out.println(microserviceName + " Microservice Response: " + response);
        System.out.println(microserviceName + " Execution Time: " + executionTime + " ms");
        System.out.println("-----------------------");
    }
}
