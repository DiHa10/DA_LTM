package com.meeting.common.model;

import java.io.Serializable;

public class Booking implements Serializable {
    private int id;
    private int roomId;
    private int userId;
    private String bookingDate; // Định dạng YYYY-MM-DD
    private String startTime;   // Định dạng HH:mm (vd: 09:00)
    private String endTime;     // Định dạng HH:mm (vd: 10:30)
    private String purpose;
    private String status;      // "CONFIRMED", "CANCELLED"
    private String createdAt;

    // Các trường hiển thị bổ sung để UI thuận tiện hiển thị
    private String roomName;
    private String userFullName;
    private String department;
    private String invitedUsers; // Danh sách ID người được mời: vd "2,3"
    private String invitedUserNames; // Tên hiển thị người được mời: vd "An (IT), Vũ (MKT)"

    public Booking() {}

    public Booking(int id, int roomId, int userId, String bookingDate, String startTime, String endTime, String purpose, String status, String createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTimeSlot() {
        return startTime + " - " + endTime;
    }

    public String getInvitedUsers() {
        return invitedUsers;
    }

    public void setInvitedUsers(String invitedUsers) {
        this.invitedUsers = invitedUsers;
    }

    public String getInvitedUserNames() {
        return invitedUserNames;
    }

    public void setInvitedUserNames(String invitedUserNames) {
        this.invitedUserNames = invitedUserNames;
    }
}
