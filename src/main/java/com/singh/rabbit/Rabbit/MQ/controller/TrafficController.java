package com.singh.rabbit.RabbitMQ.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/traffic")
public class TrafficController {

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final ConnectionFactory connectionFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, SimpleMessageListenerContainer> listenerMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> logsMap = new ConcurrentHashMap<>();

    @Value("${traffic.webhook.url}")
    private String webhookUrl;

    public TrafficController(RabbitAdmin rabbitAdmin, RabbitTemplate rabbitTemplate, ConnectionFactory connectionFactory) {
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.connectionFactory = connectionFactory;
    }

    @GetMapping("/queues")
    public List<String> listQueues() {
        return new ArrayList<>(listenerMap.keySet());
    }

    @PostMapping("/queues")
    public String createQueue(@RequestParam String queueName) {
        rabbitAdmin.declareQueue(new Queue(queueName, true));
        startListener(queueName);
        return "Queue created and listener started: " + queueName;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam String queueName, @RequestBody Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(queueName, json);
            logsMap.computeIfAbsent(queueName, k -> new ArrayList<>()).add("Sent: " + json);
            return "Message sent to " + queueName;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/stop")
    public String stopListener(@RequestParam String queueName) {
        SimpleMessageListenerContainer container = listenerMap.get(queueName);
        if (container != null) {
            container.stop();
            listenerMap.remove(queueName);
            return "Listener stopped for " + queueName;
        }
        return "No listener found for " + queueName;
    }

    @GetMapping("/logs")
    public Map<String, List<String>> getLogs() {
        return logsMap;
    }

    private void startListener(String queueName) {
        if (listenerMap.containsKey(queueName)) return;

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(message -> {
            try {
                String body = new String(message.getBody());
                logsMap.computeIfAbsent(queueName, k -> new ArrayList<>()).add("Received: " + body);
                restTemplate.postForEntity(webhookUrl, objectMapper.readValue(body, Map.class), String.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        container.start();
        listenerMap.put(queueName, container);
    }

    @PostConstruct
    public void init() {
        listenerMap.keySet().forEach(this::startListener);
    }
}
