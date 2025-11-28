package com.example.kafkastreams;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

public class JsonSetSerde implements Serde<Set<String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Serializer<Set<String>> serializer() {
        return new Serializer<Set<String>>() {
            @Override
            public byte[] serialize(String topic, Set<String> data) {
                try {
                    if (data == null) {
                        return null;
                    }
                    return objectMapper.writeValueAsBytes(data);
                } catch (IOException e) {
                    throw new RuntimeException("Error serializing Set", e);
                }
            }
        };
    }

    @Override
    public Deserializer<Set<String>> deserializer() {
        return new Deserializer<Set<String>>() {
            @Override
            public Set<String> deserialize(String topic, byte[] data) {
                if (data == null) {
                    return new HashSet<>();
                }
                try {
                    return objectMapper.readValue(data, new TypeReference<Set<String>>() {});
                } catch (IOException e) {
                    System.err.println("Error deserializing Set, returning empty set. Error: " + e.getMessage());
                    return new HashSet<>();
                }
            }
        };
    }
}