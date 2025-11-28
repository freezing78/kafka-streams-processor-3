package com.example.kafkastreams;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    private String messageId;
    private String sender;
    private String receiver;
    private String content;
    private long timestamp;

    public Message() {}

    public Message(String messageId, String sender, String receiver, String content, long timestamp) {
        this.messageId = messageId;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры ДОЛЖНЫ БЫТЬ:
    @JsonProperty("messageId")
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    @JsonProperty("sender")
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    @JsonProperty("receiver")
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    @JsonProperty("content")
    public String getContent() { return content; }  // ← ЭТОТ МЕТОД ДОЛЖЕН БЫТЬ!
    public void setContent(String content) { this.content = content; }

    @JsonProperty("timestamp")
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}