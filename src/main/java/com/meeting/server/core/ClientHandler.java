package com.meeting.server.core;

import com.google.gson.reflect.TypeToken;
import com.meeting.common.model.Booking;
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

                case GET_BOOKINGS_BY_USER -> {
                    int userId = request.getUserId() > 0 ? request.getUserId() : (currentUser != null ? currentUser.getId() : 0);
                    List<Booking> bookings = bookingService.getBookingsByUser(userId);
                    return Response.success("Lấy lịch họp của bạn thành công", ActionType.GET_BOOKINGS_BY_USER, JsonUtil.toJson(bookings));
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
