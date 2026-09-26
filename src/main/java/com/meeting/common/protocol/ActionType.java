package com.meeting.common.protocol;

public enum ActionType {
    // Xác thực
    LOGIN,
    LOGOUT,

    // Quản lý phòng
    GET_ROOMS,
    ADD_ROOM,
    UPDATE_ROOM,
    DELETE_ROOM,

    // Đặt phòng & Lịch họp
    GET_BOOKINGS_BY_DATE,
    GET_BOOKINGS_BY_USER,
    BOOK_ROOM,
    CANCEL_BOOKING,

    // Server chủ động broadcast dữ liệu mới tới các Client
    BROADCAST_UPDATE
}
