package com.meeting.server.service;

import com.meeting.common.model.Booking;
import com.meeting.common.model.Room;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Response;
import com.meeting.server.dao.BookingDao;
import com.meeting.server.dao.RoomDao;
import com.meeting.server.dao.UserDao;

import java.util.List;

public class BookingService {
    private final UserDao userDao = new UserDao();
    private final RoomDao roomDao = new RoomDao();
    private final BookingDao bookingDao = new BookingDao();

    // Khóa đồng bộ dùng cho việc tranh chấp tài nguyên đặt phòng
    private final Object bookingLock = new Object();

    public User login(String username, String password) {
        return userDao.authenticate(username, password);
    }

    public List<Room> getAllRooms() {
        return roomDao.getAllRooms();
    }

    public boolean addRoom(Room room) {
        return roomDao.addRoom(room);
    }

    public boolean updateRoom(Room room) {
        return roomDao.updateRoom(room);
    }

    public boolean deleteRoom(int id) {
        return roomDao.deleteRoom(id);
    }

    public List<Booking> getBookingsByDate(String date) {
        return bookingDao.getBookingsByDate(date);
    }

    public List<Booking> getBookingsByUser(int userId) {
        return bookingDao.getBookingsByUser(userId);
    }

    /**
     * PHƯƠNG THỨC TRỌNG TÂM CỦA ĐỒ ÁN: ĐẶT PHÒNG HỌP AN TOÀN ĐA LUỒNG (THREAD-SAFE)
     * Sử dụng synchronized block để khóa đoạn găng (critical section).
     * Khi 2 hoặc nhiều Thread từ các Client cùng bấm đặt 1 phòng trong cùng khung giờ:
     * - Chỉ 1 Thread được vào kiểm tra và ghi nhận thành công.
     * - Các Thread kế tiếp ngay sau đó sẽ thấy khung giờ đã bị chiếm và bị chặn ngay lập tức (Conflict).
     */
    public Response bookRoom(Booking booking) {
        // Đồng bộ hóa xử lý đặt phòng để chống Race Condition (Tranh chấp đặt trùng lịch)
        synchronized (bookingLock) {
            System.out.printf("[SYNCHRONIZED] Luồng [%s] đang kiểm tra đặt phòng ID=%d, Ngày=%s, Giờ=%s-%s%n",
                    Thread.currentThread().getName(),
                    booking.getRoomId(),
                    booking.getBookingDate(),
                    booking.getStartTime(),
                    booking.getEndTime());

            // 1. Kiểm tra phòng có đang khả dụng không
            Room room = roomDao.getRoomById(booking.getRoomId());
            if (room == null) {
                return Response.error("Phòng họp không tồn tại trên hệ thống!");
            }
            if (!room.isAvailable()) {
                return Response.error("Phòng '" + room.getName() + "' hiện đang bảo trì hoặc tạm ngưng sử dụng!");
            }

            // 2. Kiểm tra xem có bất kỳ lịch nào bị giao thoa (overlap) thời gian hay không
            Booking existingBooking = bookingDao.findOverlappingBooking(
                    booking.getRoomId(),
                    booking.getBookingDate(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );

            if (existingBooking != null) {
                String conflictMsg = String.format(
                        "TRÙNG LỊCH: Khung giờ này đã được đặt bởi %s (%s) từ %s đến %s với mục đích: '%s'!",
                        existingBooking.getUserFullName(),
                        existingBooking.getDepartment(),
                        existingBooking.getStartTime(),
                        existingBooking.getEndTime(),
                        existingBooking.getPurpose()
                );
                System.out.println("[SYNCHRONIZED - CHẶN THÀNH CÔNG] " + conflictMsg);
                return Response.conflict(conflictMsg);
            }

            // 3. Nếu chưa có ai đặt, tiến hành lưu vào CSDL
            boolean success = bookingDao.insertBooking(booking);
            if (success) {
                System.out.printf("[SYNCHRONIZED - ĐẶT THÀNH CÔNG] Đã ghi nhận lịch đặt phòng [%s] cho User ID=%d%n",
                        room.getName(), booking.getUserId());
                return Response.success("Đặt phòng thành công!", ActionType.BOOK_ROOM, JsonUtil.toJson(booking));
            } else {
                return Response.error("Lỗi hệ thống khi lưu lịch đặt phòng!");
            }
        }
    }

    public Response cancelBooking(int bookingId, int userId, boolean isAdmin) {
        synchronized (bookingLock) {
            boolean success = bookingDao.cancelBooking(bookingId, userId, isAdmin);
            if (success) {
                return Response.success("Hủy lịch họp thành công!");
            } else {
                return Response.error("Không thể hủy lịch (lịch không tồn tại hoặc bạn không có quyền hủy)!");
            }
        }
    }

    /**
     * Trả phòng sớm / Giải phóng phòng để nhân viên khác có thể đặt ngay lập tức.
     */
    public Response releaseRoomEarly(int bookingId, int userId, boolean isAdmin) {
        synchronized (bookingLock) {
            String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            boolean success = bookingDao.releaseRoomEarly(bookingId, userId, currentTime, isAdmin);
            if (success) {
                return Response.success("Đã trả phòng sớm thành công! Khung giờ đã được giải phóng.");
            } else {
                return Response.error("Không thể trả phòng (chỉ áp dụng cho lịch đang CONFIRMED do bạn đặt hoặc bạn là Admin)!");
            }
        }
    }

    public List<Booking> getUpcomingConfirmedBookings(String date) {
        return bookingDao.getUpcomingConfirmedBookings(date);
    }
}
