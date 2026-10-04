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
    RELEASE_ROOM_EARLY, // Trả phòng sớm / Giải phóng phòng

    // Thông báo từ Server đẩy xuống Client (TCP Push)
    BROADCAST_UPDATE,       // Cập nhật dữ liệu thời gian thực
    REMINDER_NOTIFICATION  // Nhắc nhở sắp đến giờ họp trước 1 tiếng / 15 phút
}
