package com.meeting.common.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String senderName;
    private String department;
    private String content;
    private String timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderName, String department, String content, String timestamp) {
        this.senderName = senderName;
        this.department = department;
        this.content = content;
        this.timestamp = timestamp;
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
}
