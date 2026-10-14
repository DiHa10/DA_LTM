package com.meeting.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:meeting_room.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Tạo bảng Users
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    department TEXT NOT NULL,
                    email TEXT
                );
            """);

            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT;");
            } catch (SQLException ignored) {
                // Cột email đã tồn tại
            }

            // 2. Tạo bảng Rooms
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    capacity INTEGER NOT NULL,
                    location TEXT NOT NULL,
                    equipment TEXT,
                    status TEXT DEFAULT 'AVAILABLE'
                );
            """);

            // 3. Tạo bảng Bookings
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    room_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    booking_date TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    purpose TEXT,
                    invited_users TEXT,
                    status TEXT DEFAULT 'CONFIRMED',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (room_id) REFERENCES rooms(id),
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
            """);

            try {
                stmt.execute("ALTER TABLE bookings ADD COLUMN invited_users TEXT;");
            } catch (SQLException ignored) {
                // Đã tồn tại cột
            }

            // 4. Tạo bảng Notifications (Lưu trữ thông báo mời họp và nhắc nhở)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    type TEXT DEFAULT 'INVITATION',
                    booking_id INTEGER,
                    is_read INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                );
            """);

            // 5. Tạo bảng ChatMessages (Lưu trữ tin nhắn kênh chung và chat riêng tư bảo mật)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_id INTEGER NOT NULL,
                    receiver_id INTEGER DEFAULT 0,
                    sender_name TEXT NOT NULL,
                    sender_dept TEXT,
                    receiver_name TEXT,
                    content TEXT NOT NULL,
                    type TEXT DEFAULT 'PUBLIC',
                    is_read INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (sender_id) REFERENCES users(id)
                );
            """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_receiver ON chat_messages(receiver_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_sender ON chat_messages(sender_id);");

            // 6. Chèn dữ liệu mẫu nếu bảng Users trống
            var rsUsers = stmt.executeQuery("SELECT COUNT(*) FROM users;");
            if (rsUsers.next() && rsUsers.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO users (username, password, full_name, role, department, email) VALUES
                    ('admin', 'admin123', 'Quản Trị Viên (Admin)', 'ADMIN', 'Ban Giám Đốc', 'admin@company.com'),
                    ('nhanvien1', '123456', 'An', 'EMPLOYEE', 'Phòng Kỹ Thuật IT', 'an.it@company.com'),
                    ('nhanvien2', '123456', 'Vũ', 'EMPLOYEE', 'Phòng Marketing', 'vu.mkt@company.com'),
                    ('nhanvien3', '123456', 'Kha', 'EMPLOYEE', 'Phòng Nhân Sự', 'kha.hr@company.com');
                """);
            } else {
                // Bổ sung email cho các user mẫu nếu chưa có
                stmt.executeUpdate("UPDATE users SET email = 'admin@company.com' WHERE username = 'admin' AND (email IS NULL OR email = '')");
                stmt.executeUpdate("UPDATE users SET email = 'an.it@company.com' WHERE username = 'nhanvien1' AND (email IS NULL OR email = '')");
                stmt.executeUpdate("UPDATE users SET email = 'vu.mkt@company.com' WHERE username = 'nhanvien2' AND (email IS NULL OR email = '')");
                stmt.executeUpdate("UPDATE users SET email = 'kha.hr@company.com' WHERE username = 'nhanvien3' AND (email IS NULL OR email = '')");
            }

            // 7. Chèn dữ liệu mẫu nếu bảng Rooms trống
            var rsRooms = stmt.executeQuery("SELECT COUNT(*) FROM rooms;");
            if (rsRooms.next() && rsRooms.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO rooms (name, capacity, location, equipment, status) VALUES
                    ('Phòng Họp Sáng Tạo A1', 12, 'Tầng 1 - Tòa A', 'Máy chiếu, TV 65", Bảng trắng', 'AVAILABLE'),
                    ('Phòng Hội Nghị VIP B2', 35, 'Tầng 2 - Tòa B', 'Camera họp trực tuyến Polycom, Micro đa hướng, TV 85"', 'AVAILABLE'),
                    ('Phòng Brainstorming C1', 8, 'Tầng 1 - Tòa C', 'Bảng kính, Màn hình di động, Bút cảm ứng', 'AVAILABLE'),
                    ('Phòng Đào Tạo & Workshop D3', 50, 'Tầng 3 - Tòa D', '2 Máy chiếu, Hệ thống âm thanh, Bục phát biểu', 'AVAILABLE'),
                    ('Phòng Khách VIP E1', 6, 'Tầng Trệt - Tòa A', 'Sofa cao cấp, TV họp nhanh, Tủ đồ uống', 'AVAILABLE');
                """);
            }

            // 8. Chèn tin nhắn chào mừng mẫu nếu bảng chat_messages trống
            var rsChat = stmt.executeQuery("SELECT COUNT(*) FROM chat_messages;");
            if (rsChat.next() && rsChat.getInt(1) == 0) {
                stmt.execute("""
                    INSERT INTO chat_messages (sender_id, receiver_id, sender_name, sender_dept, content, type, is_read) VALUES
                    (1, 0, 'Quản Trị Viên (Admin)', 'Ban Giám Đốc', 'Chào mừng toàn thể anh chị em nhân viên đến với Kênh thảo luận nội bộ công ty!', 'PUBLIC', 1),
                    (2, 0, 'An', 'Phòng Kỹ Thuật IT', 'Chào mọi người! Phòng kỹ thuật IT sẵn sàng hỗ trợ kỹ thuật phòng họp.', 'PUBLIC', 1);
                """);
            }

            // 9. Chèn dữ liệu mẫu lịch đặt phòng nếu bảng Bookings trống
            var rsBookings = stmt.executeQuery("SELECT COUNT(*) FROM bookings;");
            if (rsBookings.next() && rsBookings.getInt(1) == 0) {
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String dayAfter = LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                // Lịch họp hôm nay
                stmt.execute(String.format("""
                    INSERT INTO bookings (room_id, user_id, booking_date, start_time, end_time, purpose, invited_users, status) VALUES
                    (1, 2, '%s', '08:00', '09:30', 'Sprint Planning - Phân công task tuần mới', '3,4', 'CONFIRMED'),
                    (2, 1, '%s', '09:00', '11:00', 'Họp Ban Giám Đốc - Đánh giá KPI Q3/2026', NULL, 'CONFIRMED'),
                    (3, 3, '%s', '10:00', '11:30', 'Brainstorm chiến dịch Marketing cuối năm', '2', 'CONFIRMED'),
                    (4, 4, '%s', '13:30', '15:00', 'Workshop đào tạo nhân sự mới - Quy trình onboarding', '2,3', 'CONFIRMED'),
                    (1, 3, '%s', '14:00', '15:30', 'Review thiết kế UI/UX website công ty', '2', 'CONFIRMED'),
                    (5, 2, '%s', '16:00', '17:00', 'Họp nhanh với đối tác - Demo sản phẩm', NULL, 'CONFIRMED');
                """, today, today, today, today, today, today));

                // Lịch họp ngày mai
                stmt.execute(String.format("""
                    INSERT INTO bookings (room_id, user_id, booking_date, start_time, end_time, purpose, invited_users, status) VALUES
                    (2, 2, '%s', '08:30', '10:00', 'Họp kỹ thuật - Triển khai hệ thống mới', '3,4', 'CONFIRMED'),
                    (4, 2, '%s', '08:30', '11:30', 'Khóa đào tạo An toàn thông tin & Bảo mật nội bộ', '1,3,4', 'CONFIRMED'),
                    (1, 4, '%s', '09:00', '10:30', 'Phỏng vấn ứng viên Senior Developer', NULL, 'CONFIRMED'),
                    (3, 3, '%s', '09:00', '10:30', 'Lên ý tưởng chiến dịch truyền thông tháng 11', NULL, 'CONFIRMED'),
                    (5, 1, '%s', '10:00', '11:30', 'Tiếp đón đối tác kiểm toán độc lập', NULL, 'CONFIRMED'),
                    (2, 1, '%s', '10:30', '12:00', 'Họp thảo luận hợp tác chiến lược đối tác Q4', '2,3', 'CONFIRMED'),
                    (2, 1, '%s', '13:30', '15:30', 'Báo cáo tài chính & Kế hoạch ngân sách quý tới', NULL, 'CONFIRMED'),
                    (1, 2, '%s', '14:00', '15:30', 'Demo tính năng ứng dụng di động v2.0', '3,4', 'CONFIRMED'),
                    (3, 3, '%s', '14:00', '15:30', 'Lên kế hoạch quảng cáo Facebook Ads', '2', 'CONFIRMED'),
                    (4, 4, '%s', '14:00', '16:30', 'Workshop nâng cao kỹ năng đàm phán & thuyết trình', '2,3', 'CONFIRMED'),
                    (5, 1, '%s', '15:00', '16:30', 'Gặp gỡ và trao đổi với đại diện Quỹ đầu tư', '2', 'CONFIRMED'),
                    (1, 2, '%s', '16:00', '17:30', 'Họp giao ban nội bộ phòng Kỹ thuật IT', NULL, 'CONFIRMED');
                """, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow, tomorrow));

                // Lịch họp ngày kia
                stmt.execute(String.format("""
                    INSERT INTO bookings (room_id, user_id, booking_date, start_time, end_time, purpose, invited_users, status) VALUES
                    (4, 1, '%s', '09:00', '11:00', 'Hội nghị toàn công ty - Tổng kết tháng 10', '2,3,4', 'CONFIRMED'),
                    (5, 2, '%s', '14:00', '15:00', 'Tiếp khách VIP - Đối tác Nhật Bản', NULL, 'CONFIRMED');
                """, dayAfter, dayAfter));

                System.out.println("[DatabaseManager] Đã chèn dữ liệu mẫu lịch đặt phòng (20 cuộc họp).");
            }

            System.out.println("[DatabaseManager] Khởi tạo CSDL SQLite thành công (meeting_room.db)");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Lỗi khởi tạo CSDL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
