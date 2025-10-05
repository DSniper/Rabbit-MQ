package com.singh.rabbit.RabbitMQ;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class Testing {

    private static final String BASE_URL = "http://localhost:8080/traffic";
    private static final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        // 1. Create a queue
        String queueName = "demoQueue";
        createQueue(queueName);

        // 2. Send a message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Hello from Java testing");
        sendMessage(queueName, payload);

        // 3. List queues
        listQueues();

        // 4. Get logs
        getLogs();

        // 5. Stop listener
        stopListener(queueName);
    }

    private static void createQueue(String queueName) {
        String url = BASE_URL + "/queues?queueName=" + queueName;
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        System.out.println("Create Queue Response: " + response.getBody());
    }

    private static void sendMessage(String queueName, Map<String, Object> payload) {
        String url = BASE_URL + "/send?queueName=" + queueName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        System.out.println("Send Message Response: " + response.getBody());
    }

    private static void listQueues() {
        String url = BASE_URL + "/queues";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        System.out.println("List Queues Response: " + response.getBody());
    }

    private static void getLogs() {
        String url = BASE_URL + "/logs";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        System.out.println("Logs Response: " + response.getBody());
    }

    private static void stopListener(String queueName) {
        String url = BASE_URL + "/stop?queueName=" + queueName;
        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
        System.out.println("Stop Listener Response: " + response.getBody());
    }
}
