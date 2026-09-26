-- ====================================================================
-- HỆ THỐNG ĐẶT PHÒNG HỌP CÔNG TY (MEETING ROOM BOOKING SYSTEM)
-- Đồ án môn: Lập trình mạng (TCP Socket + Multi-threading + Synchronized)
-- Cơ sở dữ liệu: SQLite (tự động tạo meeting_room.db khi khởi động Server)
-- ====================================================================

-- 1. Bảng Users (Tài khoản nhân viên & Quản trị viên)
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL,         -- 'ADMIN' hoặc 'EMPLOYEE'
    department TEXT NOT NULL    -- Ban/Phòng làm việc
);

-- 2. Bảng Rooms (Danh mục các phòng họp)
CREATE TABLE IF NOT EXISTS rooms (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    capacity INTEGER NOT NULL,  -- Sức chứa (số lượng người)
    location TEXT NOT NULL,      -- Vị trí (Tầng, Tòa nhà)
    equipment TEXT,             -- Trang thiết bị hỗ trợ họp
    status TEXT DEFAULT 'AVAILABLE' -- 'AVAILABLE' (Sẵn sàng), 'MAINTENANCE' (Bảo trì)
);

-- 3. Bảng Bookings (Lịch đặt phòng họp)
CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    room_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    booking_date TEXT NOT NULL, -- Định dạng YYYY-MM-DD
    start_time TEXT NOT NULL,   -- Định dạng HH:mm (VD: 09:00)
    end_time TEXT NOT NULL,     -- Định dạng HH:mm (VD: 10:30)
    purpose TEXT,               -- Mục đích cuộc họp
    status TEXT DEFAULT 'CONFIRMED', -- 'CONFIRMED' (Đã chốt), 'CANCELLED' (Đã hủy)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- DỮ LIỆU MẪU (SEED DATA)
INSERT INTO users (username, password, full_name, role, department) VALUES
('admin', 'admin123', 'Quản Trị Viên (Admin)', 'ADMIN', 'Ban Giám Đốc'),
('nhanvien1', '123456', 'An', 'EMPLOYEE', 'Phòng Kỹ Thuật IT'),
('nhanvien2', '123456', 'Vũ', 'EMPLOYEE', 'Phòng Marketing'),
('nhanvien3', '123456', 'Kha', 'EMPLOYEE', 'Phòng Nhân Sự');

INSERT INTO rooms (name, capacity, location, equipment, status) VALUES
('Phòng Họp Sáng Tạo A1', 12, 'Tầng 1 - Tòa A', 'Máy chiếu, TV 65", Bảng trắng', 'AVAILABLE'),
('Phòng Hội Nghị VIP B2', 35, 'Tầng 2 - Tòa B', 'Camera họp Polycom, Micro đa hướng, TV 85"', 'AVAILABLE'),
('Phòng Brainstorming C1', 8, 'Tầng 1 - Tòa C', 'Bảng kính, Màn hình di động, Bút cảm ứng', 'AVAILABLE'),
('Phòng Đào Tạo & Workshop D3', 50, 'Tầng 3 - Tòa D', '2 Máy chiếu, Hệ thống âm thanh, Bục phát biểu', 'AVAILABLE'),
('Phòng Khách VIP E1', 6, 'Tầng Trệt - Tòa A', 'Sofa cao cấp, TV họp nhanh, Tủ đồ uống', 'AVAILABLE');
