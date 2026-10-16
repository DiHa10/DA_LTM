package com.meeting.server.core;

import com.google.gson.reflect.TypeToken;
import com.meeting.common.model.Booking;
import com.meeting.common.model.ChatMessage;
import com.meeting.common.model.Room;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;
import com.meeting.server.service.BookingService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ServerManager serverManager;
    private final BookingService bookingService;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;
    private boolean isRunning = true;

    public ClientHandler(Socket socket, ServerManager serverManager, BookingService bookingService) {
        this.socket = socket;
        this.serverManager = serverManager;
        this.bookingService = bookingService;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            serverManager.log("[KẾT NỐI] Client mới kết nối từ " + socket.getRemoteSocketAddress());

            String line;
            while (isRunning && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Request request = JsonUtil.fromJson(line, Request.class);
                if (request != null && request.getAction() != null) {
                    Response response = handleRequest(request);
                    if (response != null) {
                        sendResponse(response);
                    }
                }
            }
        } catch (IOException e) {
            // Client ngắt kết nối
        } finally {
            close();
        }
    }

    private Response handleRequest(Request request) {
        ActionType action = request.getAction();
        serverManager.log(String.format("[REQUEST] Action: %s từ Client %s (User: %s)",
                action, socket.getRemoteSocketAddress(),
                currentUser != null ? currentUser.getUsername() : "Chưa đăng nhập"));

        try {
            switch (action) {
                case LOGIN -> {
                    Map<String, String> credentials = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    String username = credentials.get("username");
                    String password = credentials.get("password");
                    User user = bookingService.login(username, password);
                    if (user == null) {
                        return Response.error("Sai tên đăng nhập hoặc mật khẩu!");
                    }

                    // Chống đăng nhập 2 client chung 1 tài khoản
                    boolean registered = serverManager.registerUserLogin(this, user);
                    if (!registered) {
                        serverManager.log("[TỪ CHỐI ĐĂNG NHẬP] Tài khoản '" + username + "' đã đăng nhập ở thiết bị khác!");
                        return Response.error("Tài khoản '" + username + "' hiện đang đăng nhập ở một thiết bị/Client khác!");
                    }

                    return Response.success("Đăng nhập thành công!", ActionType.LOGIN, JsonUtil.toJson(user));
                }

                case LOGOUT -> {
                    serverManager.unregisterUser(this);
                    return Response.success("Đăng xuất thành công!", ActionType.LOGOUT, null);
                }

                case GET_ROOMS -> {
                    List<Room> rooms = bookingService.getAllRooms();
                    return Response.success("Lấy danh sách phòng thành công", ActionType.GET_ROOMS, JsonUtil.toJson(rooms));
                }

                case ADD_ROOM -> {
                    if (currentUser == null || !currentUser.isAdmin()) {
                        return Response.error("Bạn không có quyền quản trị để thêm phòng!");
                    }
                    Room room = JsonUtil.fromJson(request.getData(), Room.class);
                    boolean ok = bookingService.addRoom(room);
                    if (ok) {
                        serverManager.broadcast(new Response(Response.SUCCESS, "Danh sách phòng đã được cập nhật!", ActionType.BROADCAST_UPDATE, "ROOMS_UPDATED"));
                        return Response.success("Thêm phòng họp thành công!");
                    }
                    return Response.error("Thêm phòng họp thất bại!");
                }

                case UPDATE_ROOM -> {
                    if (currentUser == null || !currentUser.isAdmin()) {
                        return Response.error("Bạn không có quyền quản trị để sửa phòng!");
                    }
                    Room room = JsonUtil.fromJson(request.getData(), Room.class);
                    boolean ok = bookingService.updateRoom(room);
                    if (ok) {
                        serverManager.broadcast(new Response(Response.SUCCESS, "Danh sách phòng đã được cập nhật!", ActionType.BROADCAST_UPDATE, "ROOMS_UPDATED"));
                        return Response.success("Cập nhật phòng họp thành công!");
                    }
                    return Response.error("Cập nhật phòng họp thất bại!");
                }

                case DELETE_ROOM -> {
                    if (currentUser == null || !currentUser.isAdmin()) {
                        return Response.error("Bạn không có quyền quản trị để xóa phòng!");
                    }
                    int roomId = Integer.parseInt(request.getData());
                    boolean ok = bookingService.deleteRoom(roomId);
                    if (ok) {
                        serverManager.broadcast(new Response(Response.SUCCESS, "Danh sách phòng đã được cập nhật!", ActionType.BROADCAST_UPDATE, "ROOMS_UPDATED"));
                        return Response.success("Xóa phòng họp thành công!");
                    }
                    return Response.error("Xóa phòng họp thất bại!");
                }

                case GET_BOOKINGS_BY_DATE -> {
                    String date = request.getData(); // YYYY-MM-DD
                    List<Booking> bookings = bookingService.getBookingsByDate(date);
                    return Response.success("Lấy lịch họp thành công", ActionType.GET_BOOKINGS_BY_DATE, JsonUtil.toJson(bookings));
                }

                case GET_BOOKINGS_BY_DATE_RANGE -> {
                    Map<String, String> range = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    String fromDate = range != null ? range.get("fromDate") : "";
                    String toDate = range != null ? range.get("toDate") : "";
                    List<Booking> bookings = bookingService.getBookingsByDateRange(fromDate, toDate);
                    return Response.success("Lấy lịch họp theo khoảng ngày thành công", ActionType.GET_BOOKINGS_BY_DATE_RANGE, JsonUtil.toJson(bookings));
                }

                case GET_BOOKINGS_BY_USER -> {
                    int userId = request.getUserId() > 0 ? request.getUserId() : (currentUser != null ? currentUser.getId() : 0);
                    List<Booking> bookings = bookingService.getBookingsByUser(userId);
                    return Response.success("Lấy lịch họp của bạn thành công", ActionType.GET_BOOKINGS_BY_USER, JsonUtil.toJson(bookings));
                }

                case GET_ALL_USERS -> {
                    List<User> users = bookingService.getAllUsers();
                    return Response.success("Lấy danh sách người dùng thành công", ActionType.GET_ALL_USERS, JsonUtil.toJson(users));
                }

                case CREATE_USER -> {
                    if (currentUser == null || !currentUser.isAdmin()) {
                        return Response.error("Chỉ Quản trị viên mới có quyền tạo tài khoản cho nhân viên!");
                    }
                    Map<String, Object> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, Object>>() {}.getType());
                    User newUser = new User();
                    newUser.setUsername((String) map.get("username"));
                    newUser.setPassword((String) map.get("password"));
                    newUser.setFullName((String) map.get("fullName"));
                    newUser.setRole((String) map.get("role"));
                    newUser.setDepartment((String) map.get("department"));
                    newUser.setEmail((String) map.get("email"));
                    Response res = bookingService.createUser(newUser);
                    if (res.isSuccess()) {
                        serverManager.log("[TẠO TÀI KHOẢN] Quản trị viên tạo user: " + newUser.getUsername() + " (" + newUser.getRole() + ")");
                        serverManager.broadcast(new Response(Response.SUCCESS, "Danh sách nhân viên vừa được cập nhật!", ActionType.BROADCAST_UPDATE, "USERS_UPDATED"));
                    }
                    return res;
                }

                case UPDATE_USER_ROLE -> {
                    if (currentUser == null || !currentUser.isAdmin()) {
                        return Response.error("Chỉ Quản trị viên mới có quyền phân quyền vai trò!");
                    }
                    Map<String, String> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    int targetUserId = Integer.parseInt(map.get("userId"));
                    String newRole = map.get("role");
                    Response res = bookingService.updateUserRole(targetUserId, newRole, currentUser.getId());
                    if (res.isSuccess()) {
                        serverManager.log("[PHÂN QUYỀN] Admin cập nhật quyền cho User ID=" + targetUserId + " -> " + newRole);
                        serverManager.broadcast(new Response(Response.SUCCESS, "Danh sách nhân viên vừa được cập nhật!", ActionType.BROADCAST_UPDATE, "USERS_UPDATED"));
                    }
                    return res;
                }

                case UPDATE_PROFILE -> {
                    if (currentUser == null) {
                        return Response.error("Bạn chưa đăng nhập!");
                    }
                    Map<String, String> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    String fullName = map.get("fullName");
                    String email = map.get("email");
                    String department = map.get("department");
                    Response res = bookingService.updateProfile(currentUser.getId(), fullName, email, department);
                    if (res.isSuccess()) {
                        User updated = JsonUtil.fromJson(res.getData(), User.class);
                        this.currentUser = updated;
                        serverManager.log("[CẬP NHẬT HỒ SƠ] User ID=" + currentUser.getId() + " (" + currentUser.getUsername() + ")");
                        serverManager.broadcast(new Response(Response.SUCCESS, "Thông tin nhân sự vừa được cập nhật!", ActionType.BROADCAST_UPDATE, "USERS_UPDATED"));
                    }
                    return res;
                }

                case CHANGE_PASSWORD -> {
                    if (currentUser == null) {
                        return Response.error("Bạn chưa đăng nhập!");
                    }
                    Map<String, String> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    String oldPass = map.get("oldPassword");
                    String newPass = map.get("newPassword");
                    return bookingService.changePassword(currentUser.getId(), oldPass, newPass);
                }

                case BOOK_ROOM -> {
                    Booking booking = JsonUtil.fromJson(request.getData(), Booking.class);
                    if (currentUser != null) {
                        booking.setUserId(currentUser.getId());
                    }
                    // Gọi qua BookingService - nơi có synchronized bảo vệ
                    Response res = bookingService.bookRoom(booking);
                    if (res.isSuccess()) {
                        serverManager.log("[ĐẶT PHÒNG THÀNH CÔNG] Phòng: " + booking.getRoomId() + ", Ngày: " + booking.getBookingDate() + ", Giờ: " + booking.getTimeSlot());
                        // Broadcast tới toàn bộ các Client khác để họ tự động reload bảng lịch
                        serverManager.broadcast(new Response(Response.SUCCESS, "Có lịch đặt phòng mới!", ActionType.BROADCAST_UPDATE, "SCHEDULE_UPDATED"));

                        // Gửi thông báo Lời mời họp trực tiếp qua mạng TCP và LƯU VÀO CSDL
                        if (booking.getInvitedUsers() != null && !booking.getInvitedUsers().trim().isEmpty()) {
                            String[] userIds = booking.getInvitedUsers().split(",");
                            for (String uIdStr : userIds) {
                                try {
                                    int targetId = Integer.parseInt(uIdStr.trim());
                                    String invMsg = String.format("Bạn được %s (%s) mời tham gia cuộc họp: '%s' tại %s (%s ngày %s)",
                                            currentUser != null ? currentUser.getFullName() : "Đồng nghiệp",
                                            currentUser != null ? currentUser.getDepartment() : "Công ty",
                                            booking.getPurpose(),
                                            booking.getRoomName() != null ? booking.getRoomName() : ("Phòng ID=" + booking.getRoomId()),
                                            booking.getTimeSlot(),
                                            com.meeting.common.util.DateUtil.toUiDate(booking.getBookingDate()));

                                    // 1. Lưu bản ghi vào CSDL SQLite để người dùng mở app lúc nào cũng thấy
                                    bookingService.getNotificationDao().insertNotification(
                                            targetId,
                                            "Lời mời tham gia họp",
                                            invMsg,
                                            "INVITATION",
                                            booking.getId()
                                    );

                                    // 2. Gửi thời gian thực qua TCP nếu đang online
                                    Response invResp = new Response(Response.SUCCESS, invMsg, ActionType.INVITATION_NOTIFICATION, JsonUtil.toJson(booking));
                                    boolean sent = serverManager.sendToUser(targetId, invResp);
                                    if (sent) {
                                        serverManager.log("[LỜI MỜI HỌP TCP] Đã gửi thông báo mời trực tiếp tới User ID=" + targetId);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    } else if (res.isConflict()) {
                        serverManager.log("[CHẶN TRÙNG LỊCH] " + res.getMessage());
                    }
                    return res;
                }

                case CANCEL_BOOKING -> {
                    int bookingId = Integer.parseInt(request.getData());
                    boolean isAdmin = currentUser != null && currentUser.isAdmin();
                    int userId = currentUser != null ? currentUser.getId() : 0;
                    Response res = bookingService.cancelBooking(bookingId, userId, isAdmin);
                    if (res.isSuccess()) {
                        serverManager.log("[HỦY LỊCH THÀNH CÔNG] Lịch ID: " + bookingId);
                        serverManager.broadcast(new Response(Response.SUCCESS, "Có lịch phòng vừa bị hủy!", ActionType.BROADCAST_UPDATE, "SCHEDULE_UPDATED"));
                    }
                    return res;
                }

                case RELEASE_ROOM_EARLY -> {
                    int bookingId = Integer.parseInt(request.getData());
                    boolean isAdmin = currentUser != null && currentUser.isAdmin();
                    int userId = currentUser != null ? currentUser.getId() : 0;
                    Response res = bookingService.releaseRoomEarly(bookingId, userId, isAdmin);
                    if (res.isSuccess()) {
                        serverManager.log("[TRẢ PHÒNG SỚM] Lịch ID: " + bookingId + " đã hoàn thành và trả phòng sớm!");
                        serverManager.broadcast(new Response(Response.SUCCESS, "Một phòng họp vừa được trả phòng sớm! Khung giờ đã sẵn sàng cho nhân viên khác đặt.", ActionType.BROADCAST_UPDATE, "SCHEDULE_UPDATED"));
                    }
                    return res;
                }

                case EXTEND_BOOKING -> {
                    int bookingId;
                    int minutes = 30;
                    try {
                        bookingId = Integer.parseInt(request.getData());
                    } catch (NumberFormatException e) {
                        Map<String, Integer> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, Integer>>() {}.getType());
                        bookingId = map.get("bookingId");
                        if (map.containsKey("minutes")) {
                            minutes = map.get("minutes");
                        }
                    }
                    boolean isAdmin = currentUser != null && currentUser.isAdmin();
                    int userId = currentUser != null ? currentUser.getId() : 0;
                    Response res = bookingService.extendBooking(bookingId, userId, minutes, isAdmin);
                    if (res.isSuccess()) {
                        serverManager.log("[GIA HẠN THÀNH CÔNG] Lịch ID: " + bookingId + " đã gia hạn thêm " + minutes + " phút!");
                        serverManager.broadcast(new Response(Response.SUCCESS, "Một cuộc họp vừa được gia hạn thêm giờ!", ActionType.BROADCAST_UPDATE, "SCHEDULE_UPDATED"));
                    }
                    return res;
                }

                case UPDATE_BOOKING -> {
                    Booking booking = JsonUtil.fromJson(request.getData(), Booking.class);
                    boolean isAdmin = currentUser != null && currentUser.isAdmin();
                    int userId = currentUser != null ? currentUser.getId() : (request.getUserId() > 0 ? request.getUserId() : 0);
                    Response res = bookingService.updateBooking(booking, userId, isAdmin);
                    if (res.isSuccess()) {
                        serverManager.log("[CẬP NHẬT LỊCH HỌP] Lịch ID: " + booking.getId() + " bởi User ID: " + userId);
                        serverManager.broadcast(new Response(Response.SUCCESS, "Có cuộc họp vừa được cập nhật lại giờ/phòng!", ActionType.BROADCAST_UPDATE, "SCHEDULE_UPDATED"));
                    } else if (res.isConflict()) {
                        serverManager.log("[CHẶN SỬA TRÙNG LỊCH] " + res.getMessage());
                    }
                    return res;
                }

                case DELETE_BOOKING -> {
                    int bookingId = Integer.parseInt(request.getData());
                    boolean isAdmin = currentUser != null && currentUser.isAdmin();
                    int userId = currentUser != null ? currentUser.getId() : (request.getUserId() > 0 ? request.getUserId() : 0);
                    Response res = bookingService.deleteBooking(bookingId, userId, isAdmin);
                    if (res.isSuccess()) {
                        serverManager.log("[XÓA LỊCH HỌP] Lịch ID: " + bookingId + " đã bị xóa khỏi CSDL!");
                        serverManager.broadcast(new Response(Response.SUCCESS, "Một cuộc họp đã bị xóa!", ActionType.BROADCAST_UPDATE, "SCHEDULE_UPDATED"));
                    }
                    return res;
                }

                case GET_DEPARTMENT_STATS -> {
                    String yearMonth = request.getData();
                    if (yearMonth == null || yearMonth.trim().isEmpty()) {
                        yearMonth = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                    }
                    Map<String, Integer> stats = bookingService.getDepartmentStats(yearMonth);
                    return Response.success("Lấy thống kê phòng ban thành công", ActionType.GET_DEPARTMENT_STATS, JsonUtil.toJson(stats));
                }

                case GET_NOTIFICATIONS -> {
                    int userId = (currentUser != null) ? currentUser.getId() : request.getUserId();
                    List<com.meeting.common.model.Notification> notifs = bookingService.getNotificationDao().getNotificationsByUser(userId);
                    return Response.success("Tải danh sách thông báo thành công", ActionType.GET_NOTIFICATIONS, JsonUtil.toJson(notifs));
                }

                case MARK_NOTIFICATION_READ -> {
                    int userId = (currentUser != null) ? currentUser.getId() : request.getUserId();
                    bookingService.getNotificationDao().markAllAsRead(userId);
                    return Response.success("Đã đánh dấu đã đọc");
                }

                case FIND_AVAILABLE_ROOMS -> {
                    Map<String, String> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, String>>() {}.getType());
                    String date = map.get("date");
                    String startTime = map.get("startTime");
                    String endTime = map.get("endTime");
                    List<Room> freeRooms = bookingService.findAvailableRooms(date, startTime, endTime);
                    return Response.success("Lọc danh sách phòng trống thành công", ActionType.FIND_AVAILABLE_ROOMS, JsonUtil.toJson(freeRooms));
                }

                case SEND_CHAT_MESSAGE -> {
                    ChatMessage msg;
                    try {
                        msg = JsonUtil.fromJson(request.getData(), ChatMessage.class);
                    } catch (Exception e) {
                        msg = null;
                    }
                    if (msg == null || msg.getContent() == null) {
                        // Tương thích ngược nếu client gửi chuỗi text thô
                        msg = new ChatMessage();
                        msg.setContent(request.getData());
                        msg.setType("PUBLIC");
                    }
                    if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
                        return Response.error("Nội dung tin nhắn không được để trống!");
                    }

                    int senderId = currentUser != null ? currentUser.getId() : 0;
                    String senderName = currentUser != null ? currentUser.getFullName() : "Khách";
                    String dept = currentUser != null ? currentUser.getDepartment() : "Chung";
                    String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                            + " " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));

                    msg.setSenderId(senderId);
                    msg.setSenderName(senderName);
                    msg.setDepartment(dept);
                    msg.setTimestamp(time);

                    if (msg.isPrivate()) {
                        // 1. CHAT RIÊNG TƯ 1-1 BẢO MẬT (Chỉ người nhận và người gửi thấy)
                        if (msg.getReceiverId() <= 0) {
                            return Response.error("Vui lòng chọn người nhận cho tin nhắn riêng!");
                        }
                        if (msg.getReceiverId() == senderId) {
                            return Response.error("Bạn không thể gửi tin nhắn riêng cho chính mình!");
                        }

                        User receiver = bookingService.getUserById(msg.getReceiverId());
                        if (receiver != null) {
                            msg.setReceiverName(receiver.getFullName());
                        }

                        // Lưu vào CSDL SQLite
                        bookingService.saveChatMessage(msg);
                        serverManager.log(String.format("[CHAT RIÊNG TCP BẢO MẬT] Từ %s (ID=%d) -> %s (ID=%d): %s",
                                senderName, senderId, msg.getReceiverName() != null ? msg.getReceiverName() : ("ID=" + msg.getReceiverId()),
                                msg.getReceiverId(), msg.getContent()));

                        // Gửi đích danh tới người nhận (BẢO MẬT, hoàn toàn không broadcast tới client khác)
                        Response privatePush = new Response(Response.SUCCESS, "Tin nhắn riêng mới", ActionType.CHAT_PRIVATE_RECEIVE, JsonUtil.toJson(msg));
                        boolean delivered = serverManager.sendToUser(msg.getReceiverId(), privatePush);
                        if (delivered) {
                            serverManager.log("[CHAT RIÊNG TCP] Đã đẩy tin nhắn tới thiết bị của User ID=" + msg.getReceiverId());
                        }

                        // Trả về cho người gửi để thêm vào giao diện chat của người gửi
                        return Response.success("Đã gửi tin nhắn riêng", ActionType.SEND_CHAT_MESSAGE, JsonUtil.toJson(msg));
                    } else {
                        // 2. CHAT NỘI BỘ TOÀN CÔNG TY (Ai cũng thấy và nhắn được)
                        msg.setType("PUBLIC");
                        msg.setReceiverId(0);

                        // Lưu vào CSDL SQLite
                        bookingService.saveChatMessage(msg);
                        serverManager.log(String.format("[CHAT NỘI BỘ TCP] %s (%s): %s", senderName, dept, msg.getContent()));

                        // Broadcast tin nhắn tới tất cả client đang kết nối
                        serverManager.broadcast(new Response(Response.SUCCESS, "Tin nhắn nội bộ mới", ActionType.CHAT_BROADCAST, JsonUtil.toJson(msg)));
                        return Response.success("Đã gửi tin nhắn nội bộ", ActionType.SEND_CHAT_MESSAGE, JsonUtil.toJson(msg));
                    }
                }

                case GET_CHAT_HISTORY -> {
                    Map<String, Object> map = JsonUtil.fromJson(request.getData(), new TypeToken<Map<String, Object>>() {}.getType());
                    String type = map != null ? (String) map.get("type") : "PUBLIC";
                    int beforeId = (map != null && map.get("beforeId") != null) ? ((Number) map.get("beforeId")).intValue() : 0;
                    int limit = (map != null && map.get("limit") != null) ? ((Number) map.get("limit")).intValue() : 0;

                    if ("PRIVATE".equalsIgnoreCase(type)) {
                        int targetUserId = map != null && map.get("targetUserId") != null ? ((Number) map.get("targetUserId")).intValue() : 0;
                        int myId = currentUser != null ? currentUser.getId() : 0;
                        List<ChatMessage> history = bookingService.getPrivateChatMessages(myId, targetUserId, limit > 0 ? limit : 50, beforeId);
                        return Response.success("Lấy lịch sử chat riêng thành công", ActionType.GET_CHAT_HISTORY, JsonUtil.toJson(history));
                    } else {
                        List<ChatMessage> history = bookingService.getPublicChatMessages(limit > 0 ? limit : 50, beforeId);
                        return Response.success("Lấy lịch sử chat nội bộ thành công", ActionType.GET_CHAT_HISTORY, JsonUtil.toJson(history));
                    }
                }

                case GET_ONLINE_USERS -> {
                    List<Integer> onlineIds = serverManager.getOnlineUserIds();
                    return Response.success("Danh sách người dùng Online", ActionType.GET_ONLINE_USERS, JsonUtil.toJson(onlineIds));
                }

                case MARK_CHAT_READ -> {
                    int senderId = Integer.parseInt(request.getData());
                    int myId = currentUser != null ? currentUser.getId() : 0;
                    bookingService.markChatAsRead(senderId, myId);
                    return Response.success("Đã đánh dấu đã đọc");
                }

                default -> {
                    return Response.error("Yêu cầu không được hỗ trợ!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi xử lý máy chủ: " + e.getMessage());
        }
    }

    public void sendResponse(Response response) {
        if (out != null) {
            String json = JsonUtil.toJson(response);
            out.println(json);
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
        serverManager.removeClient(this);
        serverManager.log("[NGẮT KẾT NỐI] Client " + socket.getRemoteSocketAddress() + " đã rời mạng.");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Socket getSocket() {
        return socket;
    }
}
