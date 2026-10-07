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

    // Quản lý người dùng, phân quyền & hồ sơ cá nhân
    GET_ALL_USERS,
    CREATE_USER,             // Admin tạo tài khoản cho cấp dưới
    UPDATE_USER_ROLE,        // Admin phân quyền vai trò cho cấp dưới
    UPDATE_PROFILE,          // Người dùng chỉnh sửa thông tin cá nhân
    CHANGE_PASSWORD,         // Người dùng đổi mật khẩu
    INVITATION_NOTIFICATION, // Server gửi lời mời thời gian thực tới đồng nghiệp

    // Đặt phòng & Lịch họp
    GET_BOOKINGS_BY_DATE,
    GET_BOOKINGS_BY_USER,
    BOOK_ROOM,
    CANCEL_BOOKING,
    RELEASE_ROOM_EARLY, // Trả phòng sớm / Giải phóng phòng
    EXTEND_BOOKING,     // Gia hạn thêm giờ họp với kiểm tra tranh chấp phòng

    // Thông báo & Email nhắc nhở
    SEND_MANUAL_EMAIL_REMINDER, // Chủ phòng kích hoạt gửi email nhắc nhở thủ công

    // Kênh chat nhanh nội bộ qua TCP
    SEND_CHAT_MESSAGE,
    CHAT_BROADCAST,

    // Thông báo từ Server đẩy xuống Client (TCP Push)
    BROADCAST_UPDATE,       // Cập nhật dữ liệu thời gian thực
    REMINDER_NOTIFICATION  // Nhắc nhở sắp đến giờ họp trước 1 tiếng / 15 phút
}
