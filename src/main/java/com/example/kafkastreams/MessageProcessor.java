package com.example.kafkastreams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> BANNED_WORDS = ConcurrentHashMap.newKeySet();

    static {
        BANNED_WORDS.addAll(Arrays.asList("spam", "scam", "fraud", "hate"));
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "message-processor-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "10.127.1.2:9094,10.127.1.2:9095,10.127.1.2:9096");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // Дополнительные настройки для продакшена
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);

        Topology topology = buildTopology();
        KafkaStreams streams = new KafkaStreams(topology, props);

        // Обработчик для graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Kafka Streams application...");
            streams.close();
        }));

        // Запуск приложения
        try {
            streams.start();
            System.out.println("Kafka Streams application started successfully");
            System.out.println("Bootstrap servers: " + props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
            System.out.println("Application ID: " + props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
        } catch (Exception e) {
            System.err.println("Error starting Kafka Streams application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // Store для заблокированных пользователей
        StoreBuilder<KeyValueStore<String, Set<String>>> blockedUsersStoreBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore("blocked-users-store"),
                        Serdes.String(),
                        new JsonSetSerde()
                ).withLoggingDisabled();

        builder.addStateStore(blockedUsersStoreBuilder);

        // Поток блокировок пользователей
        KStream<String, String> blockedUsersStream = builder.stream("blocked_users");
        blockedUsersStream.process(() -> new BlockedUsersProcessor(), "blocked-users-store");

        // Поток для управления запрещенными словами - ДОБАВЛЕНО
        KStream<String, String> bannedWordsControlStream = builder.stream("banned_words_control");
        bannedWordsControlStream.foreach((key, value) -> {
            System.out.println("Processing banned words control: " + value);
            updateBannedWords(value);
        });

        // Основной поток сообщений
        KStream<String, String> messagesStream = builder.stream("messages");

        // Обработка сообщений
        KStream<String, String> filteredMessages = messagesStream
                .filter((key, value) -> {
                    if (value == null || value.trim().isEmpty()) {
                        System.out.println("Filtered out empty message with key: " + key);
                        return false;
                    }
                    return true;
                })
                .mapValues(value -> {
                    try {
                        Message message = OBJECT_MAPPER.readValue(value, Message.class);
                        System.out.println("Processing message from " + message.getSender() + " to " + message.getReceiver());
                        return message;
                    } catch (Exception e) {
                        System.err.println("Error parsing message JSON: " + e.getMessage());
                        System.err.println("Problematic JSON: " + value);
                        return null;
                    }
                })
                .filter((key, message) -> message != null)
                .transform(() -> new UserFilterTransformer(), "blocked-users-store")
                .filter((key, message) -> {
                    if (message == null) {
                        System.out.println("Message filtered by user block");
                        return false;
                    }
                    return true;
                })
                .mapValues(message -> {
                    Message censoredMessage = applyCensorship(message);
                    System.out.println("Applied censorship to message: " + message.getContent() + " -> " + censoredMessage.getContent());
                    return censoredMessage;
                })
                .mapValues(message -> {
                    try {
                        return OBJECT_MAPPER.writeValueAsString(message);
                    } catch (Exception e) {
                        System.err.println("Error serializing message to JSON: " + e.getMessage());
                        return null;
                    }
                })
                .filter((key, value) -> value != null);

        // Отправляем обработанные сообщения в выходной топик
        filteredMessages.to("filtered_messages");

        return builder.build();
    }

    private static Message applyCensorship(Message message) {
        String originalContent = message.getContent();
        String censoredContent = originalContent;

        System.out.println("DEBUG: Applying censorship to: " + originalContent);
        System.out.println("DEBUG: Available banned words: " + BANNED_WORDS);

        for (String bannedWord : BANNED_WORDS) {
            // Простая замена без сложных регулярных выражений
            String lowerContent = censoredContent.toLowerCase();
            String lowerBanned = bannedWord.toLowerCase();

            int index = lowerContent.indexOf(lowerBanned);
            while (index >= 0) {
                // Заменяем найденное слово на звездочки
                String before = censoredContent.substring(0, index);
                String after = censoredContent.substring(index + bannedWord.length());
                String stars = "*".repeat(bannedWord.length());
                censoredContent = before + stars + after;

                // Обновляем для поиска следующего вхождения
                lowerContent = censoredContent.toLowerCase();
                index = lowerContent.indexOf(lowerBanned, index + stars.length());
            }
        }

        System.out.println("DEBUG: Censored result: " + censoredContent);

        return new Message(
                message.getMessageId(),
                message.getSender(),
                message.getReceiver(),
                censoredContent,
                message.getTimestamp()
        );
    }

    private static void updateBannedWords(String controlMessage) {
        try {
            if (controlMessage == null || controlMessage.trim().isEmpty()) {
                return;
            }

            System.out.println("DEBUG: Received control message: " + controlMessage);

            // Простой парсинг JSON
            if (controlMessage.contains("\"action\"")) {
                if (controlMessage.contains("\"add\"") && controlMessage.contains("\"word\"")) {
                    // Извлекаем слово между "word":" и "
                    int start = controlMessage.indexOf("\"word\":\"") + 8;
                    int end = controlMessage.indexOf("\"", start);
                    if (start > 7 && end > start) {
                        String word = controlMessage.substring(start, end);
                        BANNED_WORDS.add(word.toLowerCase());
                        System.out.println("✓ Added banned word: " + word);
                        System.out.println("✓ Current banned words: " + BANNED_WORDS);
                    }
                } else if (controlMessage.contains("\"remove\"") && controlMessage.contains("\"word\"")) {
                    int start = controlMessage.indexOf("\"word\":\"") + 8;
                    int end = controlMessage.indexOf("\"", start);
                    if (start > 7 && end > start) {
                        String word = controlMessage.substring(start, end);
                        BANNED_WORDS.remove(word.toLowerCase());
                        System.out.println("✓ Removed banned word: " + word);
                        System.out.println("✓ Current banned words: " + BANNED_WORDS);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing banned words control: " + e.getMessage());
        }
    }

    // Метод для получения текущего списка запрещенных слов (для тестирования)
    public static Set<String> getBannedWords() {
        return new HashSet<>(BANNED_WORDS);
    }
}