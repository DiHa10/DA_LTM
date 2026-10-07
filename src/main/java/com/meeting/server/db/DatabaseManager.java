package com.meeting.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

            // 5. Chèn dữ liệu mẫu nếu bảng Users trống
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

            // 5. Chèn dữ liệu mẫu nếu bảng Rooms trống
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

            System.out.println("[DatabaseManager] Khởi tạo CSDL SQLite thành công (meeting_room.db)");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Lỗi khởi tạo CSDL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
