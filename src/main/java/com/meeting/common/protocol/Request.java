package com.meeting.common.protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private ActionType action;
    private int userId;
    private String data; // Chuỗi JSON chứa dữ liệu chi tiết của yêu cầu

    public Request() {}

    public Request(ActionType action) {
        this.action = action;
    }

    public Request(ActionType action, String data) {
        this.action = action;
        this.data = data;
    }

    public Request(ActionType action, int userId, String data) {
        this.action = action;
        this.userId = userId;
        this.data = data;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
