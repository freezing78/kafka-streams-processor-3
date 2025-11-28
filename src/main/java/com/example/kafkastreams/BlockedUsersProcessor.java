package com.example.kafkastreams;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;

public class BlockedUsersProcessor implements Processor<String, String> {

    private KeyValueStore<String, Set<String>> blockedUsersStore;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(ProcessorContext context) {
        this.blockedUsersStore = context.getStateStore("blocked-users-store");
        System.out.println("BlockedUsersProcessor initialized");
    }

    @Override
    public void process(String key, String value) {
        try {
            BlockedUser blockedUser = objectMapper.readValue(value, BlockedUser.class);
            String userId = blockedUser.getUserId();
            String blockedUserId = blockedUser.getBlockedUserId();

            // Получаем текущий список заблокированных пользователей
            Set<String> blockedUsers = blockedUsersStore.get(userId);
            if (blockedUsers == null) {
                blockedUsers = new HashSet<>();
            }

            // Добавляем нового заблокированного пользователя
            blockedUsers.add(blockedUserId);
            blockedUsersStore.put(userId, blockedUsers);

            System.out.println("USER BLOCK: " + userId + " blocked user " + blockedUserId);
            System.out.println("Current blocked users for " + userId + ": " + blockedUsers);

        } catch (Exception e) {
            System.err.println("Error processing blocked user: " + e.getMessage());
            System.err.println("Problematic JSON: " + value);
        }
    }

    @Override
    public void close() {
        System.out.println("BlockedUsersProcessor closed");
    }
}