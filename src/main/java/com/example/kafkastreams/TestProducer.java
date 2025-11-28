package com.example.kafkastreams;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import java.util.Scanner;

public class TestProducer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BOOTSTRAP_SERVERS = "10.127.1.2:9094,10.127.1.2:9095,10.127.1.2:9096";

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Kafka Test Producer ===");
        System.out.println("Bootstrap servers: " + BOOTSTRAP_SERVERS);
        System.out.println("Available commands: message, block, ban, exit");
        System.out.println("=================================");

        while (true) {
            System.out.print("\nEnter command: ");
            String command = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(command)) {
                break;
            }

            switch (command.toLowerCase()) {
                case "message":
                    sendMessage(producer, scanner);
                    break;
                case "block":
                    blockUser(producer, scanner);
                    break;
                case "ban":
                    manageBannedWords(producer, scanner);
                    break;
                case "help":
                    showHelp();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }

        producer.close();
        scanner.close();
        System.out.println("Test Producer stopped.");
    }

    private static void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("message  - Send a test message");
        System.out.println("block    - Block a user");
        System.out.println("ban      - Manage banned words");
        System.out.println("exit     - Stop the producer");
        System.out.println("help     - Show this help");
    }

    private static void sendMessage(KafkaProducer<String, String> producer, Scanner scanner) throws Exception {
        System.out.print("Enter message ID: ");
        String messageId = scanner.nextLine();

        System.out.print("Enter sender: ");
        String sender = scanner.nextLine();

        System.out.print("Enter receiver: ");
        String receiver = scanner.nextLine();

        System.out.print("Enter content: ");
        String content = scanner.nextLine();

        Message message = new Message(messageId, sender, receiver, content, System.currentTimeMillis());
        String jsonMessage = OBJECT_MAPPER.writeValueAsString(message);

        ProducerRecord<String, String> record = new ProducerRecord<>("messages", messageId, jsonMessage);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error sending message: " + exception.getMessage());
            } else {
                System.out.println("✓ Message sent successfully to topic: " + metadata.topic());
                System.out.println("  Content: " + content);
            }
        });

        producer.flush();
    }

    private static void blockUser(KafkaProducer<String, String> producer, Scanner scanner) throws Exception {
        System.out.print("Enter user ID (who is blocking): ");
        String userId = scanner.nextLine();

        System.out.print("Enter user ID to block: ");
        String blockedUserId = scanner.nextLine();

        BlockedUser blockedUser = new BlockedUser(userId, blockedUserId, System.currentTimeMillis());
        String jsonBlockedUser = OBJECT_MAPPER.writeValueAsString(blockedUser);

        ProducerRecord<String, String> record = new ProducerRecord<>("blocked_users", userId, jsonBlockedUser);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error sending block command: " + exception.getMessage());
            } else {
                System.out.println("✓ User block command sent successfully");
                System.out.println("  " + userId + " blocked " + blockedUserId);
            }
        });

        producer.flush();
    }

    private static void manageBannedWords(KafkaProducer<String, String> producer, Scanner scanner) throws Exception {
        System.out.print("Enter action (add/remove): ");
        String action = scanner.nextLine();

        String controlJson;

        if ("add".equalsIgnoreCase(action)) {
            System.out.print("Enter word to add to banned list: ");
            String wordToAdd = scanner.nextLine();
            controlJson = "{\"action\":\"add\",\"word\":\"" + wordToAdd + "\"}";
        } else if ("remove".equalsIgnoreCase(action)) {
            System.out.print("Enter word to remove from banned list: ");
            String wordToRemove = scanner.nextLine();
            controlJson = "{\"action\":\"remove\",\"word\":\"" + wordToRemove + "\"}";
        } else {
            System.out.println("Invalid action. Use 'add' or 'remove'.");
            return;
        }

        ProducerRecord<String, String> record = new ProducerRecord<>("banned_words_control", "control", controlJson);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                System.err.println("Error sending banned words control: " + exception.getMessage());
            } else {
                System.out.println("✓ Banned words control sent successfully");
                System.out.println("  Action: " + action);
            }
        });

        producer.flush();
    }
}