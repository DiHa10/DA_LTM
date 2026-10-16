package com.meeting.server.dao;

import com.meeting.common.model.Booking;
import com.meeting.server.db.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class BookingDao {

    public List<Booking> getBookingsByDate(String date) {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.booking_date = ? AND b.status = 'CONFIRMED'
            ORDER BY b.start_time ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, com.meeting.common.util.DateUtil.toDbDate(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Booking> getBookingsByDateRange(String fromDate, String toDate) {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.booking_date >= ? AND b.booking_date <= ? AND b.status = 'CONFIRMED'
            ORDER BY b.booking_date ASC, b.start_time ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, com.meeting.common.util.DateUtil.toDbDate(fromDate));
            ps.setString(2, com.meeting.common.util.DateUtil.toDbDate(toDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Booking> getBookingsByUser(int userId) {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.user_id = ?
               OR (b.invited_users IS NOT NULL AND (',' || b.invited_users || ',') LIKE ('%,' || ? || ',%'))
            ORDER BY b.booking_date DESC, b.start_time DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Tìm xem có cuộc họp nào đang chiếm khung giờ này không.
     * Quy tắc giao thoa: start_time < newEndTime AND end_time > newStartTime
     */
    public Booking findOverlappingBooking(int roomId, String date, String newStartTime, String newEndTime) {
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.room_id = ? 
              AND b.booking_date = ? 
              AND b.status = 'CONFIRMED'
              AND b.start_time < ? 
              AND b.end_time > ?
            LIMIT 1
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, com.meeting.common.util.DateUtil.toDbDate(date));
            ps.setString(3, newEndTime);
            ps.setString(4, newStartTime);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractBooking(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertBooking(Booking booking) {
        String sql = """
            INSERT INTO bookings (room_id, user_id, booking_date, start_time, end_time, purpose, invited_users, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'CONFIRMED')
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getRoomId());
            ps.setInt(2, booking.getUserId());
            ps.setString(3, com.meeting.common.util.DateUtil.toDbDate(booking.getBookingDate()));
            ps.setString(4, booking.getStartTime());
            ps.setString(5, booking.getEndTime());
            ps.setString(6, booking.getPurpose());
            ps.setString(7, booking.getInvitedUsers());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        booking.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean cancelBooking(int bookingId, int userId, boolean isAdmin) {
        String sql = isAdmin ? 
                "UPDATE bookings SET status = 'CANCELLED' WHERE id = ?" :
                "UPDATE bookings SET status = 'CANCELLED' WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            if (!isAdmin) {
                ps.setInt(2, userId);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Trả phòng sớm: cập nhật end_time về thời điểm hiện tại và đổi status sang COMPLETED.
     */
    public boolean releaseRoomEarly(int bookingId, int userId, String actualEndTime, boolean isAdmin) {
        String sql = isAdmin ?
                "UPDATE bookings SET end_time = ?, status = 'COMPLETED' WHERE id = ? AND status = 'CONFIRMED'" :
                "UPDATE bookings SET end_time = ?, status = 'COMPLETED' WHERE id = ? AND user_id = ? AND status = 'CONFIRMED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actualEndTime);
            ps.setInt(2, bookingId);
            if (!isAdmin) {
                ps.setInt(3, userId);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy tất cả các lịch họp CONFIRMED trong ngày để phục vụ Worker nhắc nhở trước giờ họp.
     */
    public List<Booking> getUpcomingConfirmedBookings(String date) {
        List<Booking> list = new ArrayList<>();
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.booking_date = ? AND b.status = 'CONFIRMED'
            ORDER BY b.start_time ASC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBooking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Booking getBookingById(int id) {
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractBooking(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean extendBooking(int bookingId, int userId, String newEndTime, boolean isAdmin) {
        String sql = isAdmin ?
                "UPDATE bookings SET end_time = ? WHERE id = ? AND status = 'CONFIRMED'" :
                "UPDATE bookings SET end_time = ? WHERE id = ? AND user_id = ? AND status = 'CONFIRMED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newEndTime);
            ps.setInt(2, bookingId);
            if (!isAdmin) {
                ps.setInt(3, userId);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Booking findOverlappingBookingExcept(int roomId, String date, String newStartTime, String newEndTime, int excludeBookingId) {
        String sql = """
            SELECT b.*, r.name as room_name, u.full_name as user_name, u.department
            FROM bookings b
            JOIN rooms r ON b.room_id = r.id
            JOIN users u ON b.user_id = u.id
            WHERE b.room_id = ? 
              AND b.booking_date = ? 
              AND b.status = 'CONFIRMED'
              AND b.id != ?
              AND b.start_time < ? 
              AND b.end_time > ?
            LIMIT 1
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, date);
            ps.setInt(3, excludeBookingId);
            ps.setString(4, newEndTime);
            ps.setString(5, newStartTime);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractBooking(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateBooking(Booking booking) {
        String sql = """
            UPDATE bookings 
            SET room_id = ?, booking_date = ?, start_time = ?, end_time = ?, purpose = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, booking.getRoomId());
            ps.setString(2, com.meeting.common.util.DateUtil.toDbDate(booking.getBookingDate()));
            ps.setString(3, booking.getStartTime());
            ps.setString(4, booking.getEndTime());
            ps.setString(5, booking.getPurpose());
            ps.setInt(6, booking.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteBooking(int bookingId, int userId, boolean isAdmin) {
        String sql = isAdmin ?
                "DELETE FROM bookings WHERE id = ?" :
                "DELETE FROM bookings WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            if (!isAdmin) {
                ps.setInt(2, userId);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, Integer> getDepartmentBookingCount(String yearMonth) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String normalizedMonth = yearMonth;
        if (yearMonth != null && yearMonth.contains("-")) {
            String[] p = yearMonth.split("-");
            if (p.length == 2 && p[0].length() <= 2 && p[1].length() == 4) {
                try {
                    normalizedMonth = p[1] + "-" + String.format("%02d", Integer.parseInt(p[0]));
                } catch (Exception ignored) {}
            }
        }
        String sql = """
            SELECT u.department, COUNT(*) as cnt
            FROM bookings b
            JOIN users u ON b.user_id = u.id
            WHERE b.booking_date LIKE ? AND b.status = 'CONFIRMED'
            GROUP BY u.department
            ORDER BY cnt DESC
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (normalizedMonth != null ? normalizedMonth : "") + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dept = rs.getString("department");
                    if (dept == null || dept.trim().isEmpty()) {
                        dept = "Chưa phân phòng";
                    }
                    map.put(dept, rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    private Booking extractBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getInt("id"));
        b.setRoomId(rs.getInt("room_id"));
        b.setUserId(rs.getInt("user_id"));
        b.setBookingDate(rs.getString("booking_date"));
        b.setStartTime(rs.getString("start_time"));
        b.setEndTime(rs.getString("end_time"));
        b.setPurpose(rs.getString("purpose"));
        try {
            b.setInvitedUsers(rs.getString("invited_users"));
        } catch (SQLException ignored) {}
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(rs.getString("created_at"));

        b.setRoomName(rs.getString("room_name"));
        b.setUserFullName(rs.getString("user_name"));
        b.setDepartment(rs.getString("department"));
        return b;
    }
}
