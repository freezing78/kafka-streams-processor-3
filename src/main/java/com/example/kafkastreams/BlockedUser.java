package com.example.kafkastreams;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlockedUser {
    private String userId;
    private String blockedUserId;
    private long timestamp;

    // Конструктор по умолчанию (обязателен для Jackson)
    public BlockedUser() {}

    // Конструктор с параметрами
    public BlockedUser(String userId, String blockedUserId, long timestamp) {
        this.userId = userId;
        this.blockedUserId = blockedUserId;
        this.timestamp = timestamp;
    }

    // Геттер для userId
    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    // Сеттер для userId
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Геттер для blockedUserId
    @JsonProperty("blockedUserId")
    public String getBlockedUserId() {
        return blockedUserId;
    }

    // Сеттер для blockedUserId
    public void setBlockedUserId(String blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    // Геттер для timestamp
    @JsonProperty("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    // Сеттер для timestamp
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Метод для удобного вывода
    @Override
    public String toString() {
        return "BlockedUser{" +
                "userId='" + userId + '\'' +
                ", blockedUserId='" + blockedUserId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    // Дополнительные методы для сравнения объектов (опционально)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockedUser that = (BlockedUser) o;

        if (timestamp != that.timestamp) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        return blockedUserId != null ? blockedUserId.equals(that.blockedUserId) : that.blockedUserId == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (blockedUserId != null ? blockedUserId.hashCode() : 0);
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}