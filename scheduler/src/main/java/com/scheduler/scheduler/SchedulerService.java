package com.scheduler.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SchedulerService {

    private int requestsPerSecond = 100;
    private int totalSeconds = 25;

    @Value("${microservice.spring.url}")
    private String springMicroserviceUrl;

    @Value("${microservice.vertx.url}")
    private String vertxMicroserviceUrl;

    private List<Double> springExecutionTimes = new ArrayList<>();
    private List<Double> vertxExecutionTimes = new ArrayList<>();

    @Scheduled(fixedRate = 1000) // Run every 1000 milliseconds (1 second)
    public void performScheduledTask() {
        if (totalSeconds > 0) {
            System.out.println("Scheduler is running... (Remaining seconds: " + totalSeconds + ")");

            // Perform Spring Boot and Vert.x requests concurrently
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);

            for (int i = 0; i < requestsPerSecond; i++) {
                CompletableFuture.runAsync(() -> {
                    springExecutionTimes.add(makeHttpRequest(springMicroserviceUrl, "Spring Boot"));
                    latch.countDown();
                }, executorService);

                CompletableFuture.runAsync(() -> {
                    vertxExecutionTimes.add(makeHttpRequest(vertxMicroserviceUrl, "Vert.x"));
                    latch.countDown();
                }, executorService);
            }

            try {
                // Wait for all tasks to complete
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            executorService.shutdown();

            totalSeconds--;
        } else {
            // Print final output after all requests are completed
            printFinalOutput("Spring Boot", springExecutionTimes);
            printFinalOutput("Vert.x", vertxExecutionTimes);

            System.out.println("Scheduler has completed 10 seconds. Stopping...");
            System.exit(0);
        }
    }

    private double makeHttpRequest(String url, String microserviceName) {
        long startTime = System.currentTimeMillis();
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        long endTime = System.currentTimeMillis();
        double executionTime = endTime - startTime; // Execution time in milliseconds

        // Print individual request metrics including execution time
//        System.out.println(microserviceName + " Microservice URL: " + url);
//        System.out.println(microserviceName + " Microservice Response: " + response);
//        System.out.println(microserviceName + " Execution Time: " + executionTime + " milliseconds");
//        System.out.println("-----------------------");

        return executionTime;
    }

    private void printFinalOutput(String microserviceName, List<Double> executionTimes) {
        double avgExecutionTime = calculateAverage(executionTimes);
        DecimalFormat df = new DecimalFormat("#.#########"); // Format with up to 9 decimal places
        System.out.println("Final Average " + microserviceName + " Execution Time: " + df.format(avgExecutionTime) + " milliseconds");
        System.out.println("-----------------------");
    }

    private double calculateAverage(List<Double> values) {
        return values.isEmpty() ? 0.0 :
                values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
