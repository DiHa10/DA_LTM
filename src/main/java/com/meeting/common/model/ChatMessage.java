package com.meeting.common.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private int id;
    private int senderId;
    private int receiverId; // 0 = PUBLIC (toàn công ty), > 0 = PRIVATE (chat riêng)
    private String senderName;
    private String department;
    private String receiverName;
    private String content;
    private String timestamp;
    private String type = "PUBLIC"; // "PUBLIC" hoặc "PRIVATE"
    private boolean isRead = false;

    public ChatMessage() {}

    // Constructor tương thích ngược cho chat công khai
    public ChatMessage(String senderName, String department, String content, String timestamp) {
        this(0, 0, 0, senderName, department, null, content, timestamp, "PUBLIC", false);
    }

    public ChatMessage(int senderId, int receiverId, String senderName, String department, 
                       String receiverName, String content, String timestamp, String type) {
        this(0, senderId, receiverId, senderName, department, receiverName, content, timestamp, type, false);
    }

    public ChatMessage(int id, int senderId, int receiverId, String senderName, String department,
                       String receiverName, String content, String timestamp, String type, boolean isRead) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.department = department;
        this.receiverName = receiverName;
        this.content = content;
        this.timestamp = timestamp;
        this.type = (type != null) ? type : "PUBLIC";
        this.isRead = isRead;
    }

    public boolean isPrivate() {
        return "PRIVATE".equalsIgnoreCase(type);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
