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
    private final EmailService emailService = new EmailService();

    // Khóa đồng bộ dùng cho việc tranh chấp tài nguyên đặt phòng
    private final Object bookingLock = new Object();

    public EmailService getEmailService() {
        return emailService;
    }

    public User getUserById(int id) {
        return userDao.getUserById(id);
    }

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
                booking.setRoomName(room.getName());
                User host = userDao.getUserById(booking.getUserId());
                if (host != null) {
                    booking.setUserFullName(host.getFullName());
                    booking.setDepartment(host.getDepartment());
                }

                // Tự động kích hoạt gửi Email thông báo mở phòng họp cho Host và Attendees
                List<User> attendees = parseAttendees(booking.getInvitedUsers());
                emailService.sendMeetingCreatedEmailAsync(booking, host, attendees);

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

    public Response extendBooking(int bookingId, int userId, int extendMinutes, boolean isAdmin) {
        synchronized (bookingLock) {
            Booking b = bookingDao.getBookingById(bookingId);
            if (b == null) {
                return Response.error("Lịch họp không tồn tại!");
            }
            if (!isAdmin && b.getUserId() != userId) {
                return Response.error("Bạn không có quyền gia hạn lịch họp này!");
            }
            if (!"CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                return Response.error("Chỉ có thể gia hạn cuộc họp đang có trạng thái ĐÃ XÁC NHẬN!");
            }

            try {
                java.time.LocalTime currentEnd = java.time.LocalTime.parse(b.getEndTime());
                java.time.LocalTime newEnd = currentEnd.plusMinutes(extendMinutes);
                String newEndTimeStr = newEnd.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                if (newEnd.isAfter(java.time.LocalTime.of(22, 0)) || newEnd.isBefore(currentEnd)) {
                    return Response.error("Không thể gia hạn sau 22:00 (vượt quá giờ vận hành tòa nhà)!");
                }

                Booking conflict = bookingDao.findOverlappingBookingExcept(
                        b.getRoomId(),
                        b.getBookingDate(),
                        b.getEndTime(),
                        newEndTimeStr,
                        bookingId
                );

                if (conflict != null) {
                    String conflictMsg = String.format(
                            "KHÔNG THỂ GIA HẠN: Khung giờ kế tiếp (%s - %s) tại '%s' đã có cuộc họp của %s (%s) đặt trước! Mục đích: '%s'.",
                            b.getEndTime(),
                            newEndTimeStr,
                            b.getRoomName(),
                            conflict.getUserFullName(),
                            conflict.getDepartment(),
                            conflict.getPurpose()
                    );
                    System.out.println("[SYNCHRONIZED - CHẶN GIA HẠN TRÙNG LỊCH] " + conflictMsg);
                    return Response.conflict(conflictMsg);
                }

                boolean success = bookingDao.extendBooking(bookingId, userId, newEndTimeStr, isAdmin);
                if (success) {
                    System.out.printf("[SYNCHRONIZED - GIA HẠN THÀNH CÔNG] Lịch ID=%d đã gia hạn tới %s%n",
                            bookingId, newEndTimeStr);
                    return Response.success("Gia hạn phòng thành công đến " + newEndTimeStr + "!", ActionType.EXTEND_BOOKING, newEndTimeStr);
                } else {
                    return Response.error("Lỗi cập nhật CSDL khi gia hạn lịch họp!");
                }
            } catch (Exception e) {
                return Response.error("Lỗi tính toán thời gian gia hạn: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // QUẢN LÝ USER, PHÂN QUYỀN & HỒ SƠ CÁ NHÂN
    // ==========================================

    public Response createUser(User newUser, boolean sendEmail) {
        if (newUser == null) return Response.error("Dữ liệu nhân viên không hợp lệ!");
        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            return Response.error("Tên đăng nhập không được để trống!");
        }
        if (newUser.getPassword() == null || newUser.getPassword().trim().length() < 4) {
            return Response.error("Mật khẩu ban đầu phải có ít nhất 4 ký tự!");
        }
        if (newUser.getFullName() == null || newUser.getFullName().trim().isEmpty()) {
            return Response.error("Họ và tên không được để trống!");
        }

        newUser.setUsername(newUser.getUsername().trim().toLowerCase());
        if (userDao.existsByUsername(newUser.getUsername())) {
            return Response.error("Tên đăng nhập '" + newUser.getUsername() + "' đã tồn tại!");
        }

        if (newUser.getEmail() != null && !newUser.getEmail().trim().isEmpty()) {
            newUser.setEmail(newUser.getEmail().trim());
            if (userDao.existsByEmail(newUser.getEmail(), 0)) {
                return Response.error("Email '" + newUser.getEmail() + "' đã được sử dụng bởi nhân viên khác!");
            }
        }

        if (newUser.getRole() == null || newUser.getRole().trim().isEmpty()) {
            newUser.setRole("EMPLOYEE");
        }
        if (newUser.getDepartment() == null || newUser.getDepartment().trim().isEmpty()) {
            newUser.setDepartment("Chung");
        }

        String rawPassword = newUser.getPassword();
        boolean ok = userDao.createUser(newUser);
        if (ok) {
            if (sendEmail) {
                emailService.sendNewAccountEmailAsync(newUser, rawPassword);
            }
            return Response.success("Tạo tài khoản '" + newUser.getUsername() + "' thành công!", ActionType.CREATE_USER, JsonUtil.toJson(newUser));
        }
        return Response.error("Không thể tạo tài khoản do lỗi cơ sở dữ liệu!");
    }

    public Response updateUserRole(int targetUserId, String newRole, int requesterId) {
        if (newRole == null || (!newRole.equalsIgnoreCase("ADMIN") && !newRole.equalsIgnoreCase("MANAGER") && !newRole.equalsIgnoreCase("EMPLOYEE"))) {
            return Response.error("Vai trò mới không hợp lệ! (Chỉ chấp nhận ADMIN, MANAGER hoặc EMPLOYEE)");
        }
        User target = userDao.getUserById(targetUserId);
        if (target == null) {
            return Response.error("Không tìm thấy người dùng có ID=" + targetUserId);
        }
        if (targetUserId == requesterId && !newRole.equalsIgnoreCase("ADMIN")) {
            return Response.error("Bạn không thể tự hạ vai trò Quản trị viên của chính mình!");
        }

        boolean ok = userDao.updateUserRole(targetUserId, newRole.toUpperCase());
        if (ok) {
            return Response.success("Đã phân quyền thành công! Tài khoản '" + target.getUsername() + "' có quyền: " + newRole.toUpperCase());
        }
        return Response.error("Lỗi khi cập nhật vai trò người dùng!");
    }

    public Response updateProfile(int userId, String fullName, String email, String department) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return Response.error("Họ và tên không được để trống!");
        }
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return Response.error("Định dạng email không hợp lệ!");
            }
            if (userDao.existsByEmail(email, userId)) {
                return Response.error("Email '" + email + "' đã được sử dụng bởi người dùng khác!");
            }
        } else {
            email = "";
        }

        boolean ok = userDao.updateProfile(userId, fullName.trim(), email, department != null ? department.trim() : "Chung");
        if (ok) {
            User updated = userDao.getUserById(userId);
            return Response.success("Cập nhật thông tin cá nhân thành công!", ActionType.UPDATE_PROFILE, JsonUtil.toJson(updated));
        }
        return Response.error("Lỗi khi lưu thông tin cá nhân!");
    }

    public Response changePassword(int userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Response.error("Vui lòng nhập mật khẩu hiện tại!");
        }
        if (newPassword == null || newPassword.trim().length() < 4) {
            return Response.error("Mật khẩu mới phải có tối thiểu 4 ký tự!");
        }
        if (!userDao.checkPassword(userId, oldPassword)) {
            return Response.error("Mật khẩu hiện tại không chính xác!");
        }

        boolean ok = userDao.changePassword(userId, newPassword.trim());
        if (ok) {
            return Response.success("Đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.");
        }
        return Response.error("Lỗi khi cập nhật mật khẩu mới!");
    }

    // ==========================================
    // GỬI EMAIL NHẮC NHỞ THỦ CÔNG TỪ CHỦ PHÒNG
    // ==========================================

    public Response sendManualReminderEmail(int bookingId, int requesterId, boolean isAdmin, String customNote) {
        Booking booking = bookingDao.getBookingById(bookingId);
        if (booking == null) {
            return Response.error("Lịch họp không tồn tại!");
        }
        if (!isAdmin && booking.getUserId() != requesterId) {
            return Response.error("Chỉ chủ trì cuộc họp hoặc Admin mới có quyền gửi thư nhắc nhở!");
        }

        User host = userDao.getUserById(booking.getUserId());
        List<User> attendees = parseAttendees(booking.getInvitedUsers());

        if (attendees.isEmpty()) {
            return Response.error("Cuộc họp này không có danh sách đồng nghiệp được mời để gửi thư nhắc nhở!");
        }

        int count = emailService.sendManualReminderEmailSync(booking, host, attendees, customNote);
        if (count > 0) {
            return Response.success("Đã kích hoạt gửi email nhắc nhở tới " + count + " thành viên tham gia!");
        } else {
            return Response.error("Không tìm thấy địa chỉ email hợp lệ nào trong danh sách người được mời!");
        }
    }

    private List<User> parseAttendees(String invitedUsersStr) {
        List<User> list = new java.util.ArrayList<>();
        if (invitedUsersStr == null || invitedUsersStr.trim().isEmpty()) return list;

        List<Integer> ids = new java.util.ArrayList<>();
        for (String part : invitedUsersStr.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (Exception ignored) {}
        }
        if (!ids.isEmpty()) {
            list.addAll(userDao.getUsersByIds(ids));
        }
        return list;
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    public List<Booking> getUpcomingConfirmedBookings(String date) {
        return bookingDao.getUpcomingConfirmedBookings(date);
    }
}
