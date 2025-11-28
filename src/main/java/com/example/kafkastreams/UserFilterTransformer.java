package com.example.kafkastreams;

import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.KeyValue;
import java.util.Set;

public class UserFilterTransformer implements Transformer<String, Message, KeyValue<String, Message>> {

    private KeyValueStore<String, Set<String>> blockedUsersStore;

    @Override
    public void init(ProcessorContext context) {
        this.blockedUsersStore = context.getStateStore("blocked-users-store");
        System.out.println("UserFilterTransformer initialized");
    }

    @Override
    public KeyValue<String, Message> transform(String key, Message message) {
        if (message == null) {
            return KeyValue.pair(key, null);
        }

        String receiver = message.getReceiver();
        String sender = message.getSender();

        // Получаем список заблокированных пользователей для получателя
        Set<String> blockedUsers = blockedUsersStore.get(receiver);

        // Если отправитель заблокирован получателем, фильтруем сообщение
        if (blockedUsers != null && blockedUsers.contains(sender)) {
            System.out.println("BLOCKED: Message from " + sender + " to " + receiver + " was blocked");
            return KeyValue.pair(key, null); // Возвращаем null для фильтрации
        }

        System.out.println("ALLOWED: Message from " + sender + " to " + receiver + " passed through");
        return KeyValue.pair(key, message);
    }

    @Override
    public void close() {
        // cleanup resources
        System.out.println("UserFilterTransformer closed");
    }
}