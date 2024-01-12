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

    private int requestsPerSecond = 10000;
    private int totalSeconds = 60;

    @Value("${microservice.spring.url}")
    private String springMicroserviceUrl;

    @Value("${microservice.vertx.url}")
    private String vertxMicroserviceUrl;

    private List<Double> springExecutionTimes = new ArrayList<>();
    private List<Double> vertxExecutionTimes = new ArrayList<>();
    private long springTotalLatency = 0;
    private long vertxTotalLatency = 0;
    private List<Long> springLatencies = new ArrayList<>();
    private List<Long> vertxLatencies = new ArrayList<>();
    private long springMinLatency = Long.MAX_VALUE;
    private long springMaxLatency = Long.MIN_VALUE;
    private long vertxMinLatency = Long.MAX_VALUE;
    private long vertxMaxLatency = Long.MIN_VALUE;

    @Scheduled(fixedRate = 1000) // Run every 1000 milliseconds (1 second)
    public void performScheduledTask() {
        if (totalSeconds > 0) {
            System.out.println("Scheduler is running... (Remaining seconds: " + totalSeconds + ")");

            // Perform Spring Boot and Vert.x requests concurrently
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(2);

            for (int i = 0; i < requestsPerSecond; i++) {
                CompletableFuture.runAsync(() -> {
                    double springExecutionTime = makeHttpRequest(springMicroserviceUrl, "Spring Boot");
                    long springLatency = calculateLatencyInNano(springMicroserviceUrl);
                    synchronized (springExecutionTimes) {
                        springExecutionTimes.add(springExecutionTime);
                    }
                    synchronized (springLatencies) {
                        springLatencies.add(springLatency);
                    }
                    updateLatencyStats(springLatency, "Spring Boot");
                    latch.countDown();
                }, executorService);

                CompletableFuture.runAsync(() -> {
                    double vertxExecutionTime = makeHttpRequest(vertxMicroserviceUrl, "Vert.x");
                    long vertxLatency = calculateLatencyInNano(vertxMicroserviceUrl);
                    synchronized (vertxExecutionTimes) {
                        vertxExecutionTimes.add(vertxExecutionTime);
                    }
                    synchronized (vertxLatencies) {
                        vertxLatencies.add(vertxLatency);
                    }
                    updateLatencyStats(vertxLatency, "Vert.x");
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
            printFinalOutput("Spring Boot", copyList(springExecutionTimes), springTotalLatency, springMinLatency, springMaxLatency);
            printFinalOutput("Vert.x", copyList(vertxExecutionTimes), vertxTotalLatency, vertxMinLatency, vertxMaxLatency);

            System.out.println("Scheduler has completed 5 seconds. Stopping...");
            System.exit(0);
        }
    }

    private double makeHttpRequest(String url, String microserviceName) {
        long startTime = System.nanoTime();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(url, String.class);
        long endTime = System.nanoTime();
        double executionTime = (endTime - startTime) / 1e9; // Convert to seconds

        // Print individual request metrics including execution time
        // System.out.println(microserviceName + " Microservice URL: " + url);
        // System.out.println(microserviceName + " Execution Time: " + executionTime + " seconds");
        // System.out.println("-----------------------");

        return executionTime;
    }

    private long calculateLatencyInNano(String url) {
        long startTime = System.nanoTime();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(url, String.class);
        long endTime = System.nanoTime();
        return (endTime - startTime);
    }

    private void updateLatencyStats(long latency, String microserviceName) {
        if (microserviceName.equals("Spring Boot")) {
            springTotalLatency += latency;
            springMinLatency = Math.min(springMinLatency, latency);
            springMaxLatency = Math.max(springMaxLatency, latency);
        } else if (microserviceName.equals("Vert.x")) {
            vertxTotalLatency += latency;
            vertxMinLatency = Math.min(vertxMinLatency, latency);
            vertxMaxLatency = Math.max(vertxMaxLatency, latency);
        }
    }


    private void printFinalOutput(String microserviceName, List<Double> executionTimes,
                                  long totalLatency, long minLatency, long maxLatency) {
        double avgExecutionTime = calculateAverageDouble(executionTimes);
        DecimalFormat executionTimeFormat = new DecimalFormat("#.#########"); // Format with up to 9 decimal places
        DecimalFormat latencyFormat = new DecimalFormat("#.######"); // Format with up to 6 decimal places for latency

        // Convert nanoseconds to milliseconds for display
        double totalLatencyMillis = totalLatency / 1_000_000.0;
        double minLatencyMillis = minLatency / 1_000_000.0;
        double maxLatencyMillis = maxLatency / 1_000_000.0;

        System.out.println("Final Average " + microserviceName + " Execution Time: " + executionTimeFormat.format(avgExecutionTime) + " seconds");
        System.out.println("Total " + microserviceName + " Latency: " + latencyFormat.format(totalLatencyMillis) + " milliseconds");
        System.out.println("Min " + microserviceName + " Latency: " + latencyFormat.format(minLatencyMillis) + " milliseconds");
        System.out.println("Max " + microserviceName + " Latency: " + latencyFormat.format(maxLatencyMillis) + " milliseconds");
        System.out.println("-----------------------");
    }

    private double calculateAverageDouble(List<Double> values) {
        return values.isEmpty() ? 0.0 :
                values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private List<Double> copyList(List<Double> originalList) {
        synchronized (originalList) {
            return new ArrayList<>(originalList);
        }
    }
}
