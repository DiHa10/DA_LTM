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
    GET_BOOKINGS_BY_DATE_RANGE, // Lọc lịch họp theo khoảng ngày (từ ngày - đến ngày / tuần / tháng)
    GET_BOOKINGS_BY_USER,
    BOOK_ROOM,
    CANCEL_BOOKING,
    UPDATE_BOOKING,             // Chỉnh sửa lịch họp (giờ, phòng, mục đích) kèm chống trùng lịch
    DELETE_BOOKING,             // Xóa hẳn cuộc họp khỏi CSDL
    RELEASE_ROOM_EARLY,         // Trả phòng sớm / Giải phóng phòng
    EXTEND_BOOKING,             // Gia hạn thêm giờ họp với kiểm tra tranh chấp phòng
    GET_DEPARTMENT_STATS,       // Thống kê số lượng cuộc họp theo phòng ban theo tháng

    // Thông báo & Lịch sử
    GET_NOTIFICATIONS,          // Tải danh sách thông báo đã lưu trong CSDL
    MARK_NOTIFICATION_READ,     // Đánh dấu đã đọc thông báo

    // Tra cứu phòng trống
    FIND_AVAILABLE_ROOMS,       // Lọc danh sách phòng trống theo khung giờ

    // Kênh chat nhanh nội bộ & Chat riêng bảo mật qua TCP Socket
    SEND_CHAT_MESSAGE,
    CHAT_BROADCAST,
    CHAT_PRIVATE_RECEIVE,    // Server đẩy tin nhắn riêng bảo mật tới người nhận
    GET_CHAT_HISTORY,        // Lấy lịch sử tin nhắn từ CSDL SQLite
    GET_ONLINE_USERS,        // Lấy danh sách ID người dùng đang trực tuyến
    USER_STATUS_UPDATE,      // Broadcast trạng thái Online/Offline theo thời gian thực
    MARK_CHAT_READ,          // Đánh dấu đã đọc tin nhắn riêng

    // Thông báo từ Server đẩy xuống Client (TCP Push)
    BROADCAST_UPDATE,       // Cập nhật dữ liệu thời gian thực
    REMINDER_NOTIFICATION  // Nhắc nhở sắp đến giờ họp trước 1 tiếng / 15 phút
}
