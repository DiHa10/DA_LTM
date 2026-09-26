package com.meeting.common.protocol;

import java.io.Serializable;

public class Response implements Serializable {
    public static final String SUCCESS = "SUCCESS";
    public static final String CONFLICT = "CONFLICT"; // Đụng độ lịch (Race Condition)
    public static final String ERROR = "ERROR";

    private String status;      // SUCCESS, CONFLICT, ERROR
    private String message;     // Mô tả thông báo
    private ActionType action;  // Hành động tương ứng
    private String data;        // JSON payload kết quả trả về

    public Response() {}

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response(String status, String message, ActionType action, String data) {
        this.status = status;
        this.message = message;
        this.action = action;
        this.data = data;
    }

    public static Response success(String message, ActionType action, String data) {
        return new Response(SUCCESS, message, action, data);
    }

    public static Response success(String message) {
        return new Response(SUCCESS, message);
    }

    public static Response conflict(String message) {
        return new Response(CONFLICT, message);
    }

    public static Response error(String message) {
        return new Response(ERROR, message);
    }

    public boolean isSuccess() {
        return SUCCESS.equals(status);
    }

    public boolean isConflict() {
        return CONFLICT.equals(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
