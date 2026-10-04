package com.meeting.server.service;

import com.meeting.common.model.Booking;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Response;
import com.meeting.server.core.ClientHandler;
import com.meeting.server.core.ServerManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Luồng chạy nền (Background Daemon Thread) tự động quét và gửi Push Notification
 * qua TCP Socket khi sắp tới giờ họp (trước 60 phút và trước 15 phút).
 */
public class MeetingReminderService {
    private final ServerManager serverManager;
    private final BookingService bookingService;
    private ScheduledExecutorService scheduler;
    private final Set<String> sentReminderKeys = ConcurrentHashMap.newKeySet();

    public MeetingReminderService(ServerManager serverManager, BookingService bookingService) {
        this.serverManager = serverManager;
        this.bookingService = bookingService;
    }

    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Meeting-Reminder-Worker");
            t.setDaemon(true);
            return t;
        });

        // Quét định kỳ mỗi 15 giây
        scheduler.scheduleAtFixedRate(this::checkAndSendReminders, 5, 15, TimeUnit.SECONDS);
        serverManager.log("[REMINDER SERVICE] Luồng chạy nền tự động nhắc lịch họp trước 60p/15p đã khởi động.");
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void checkAndSendReminders() {
        try {
            if (!serverManager.isRunning()) return;

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime now = LocalTime.now();

            List<Booking> upcomingList = bookingService.getUpcomingConfirmedBookings(today);
            for (Booking b : upcomingList) {
                try {
                    LocalTime startTime = LocalTime.parse(b.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));

                    if (now.isBefore(startTime)) {
                        long minutesLeft = Duration.between(now, startTime).toMinutes();

                        // 1. Nhắc nhở trước 60 phút (trong khoảng 16 - 60 phút)
                        if (minutesLeft <= 60 && minutesLeft > 15) {
                            String key = b.getId() + "_60m";
                            if (sentReminderKeys.add(key)) {
                                sendPushNotification(b, minutesLeft, false);
                            }
                        }
                        // 2. Nhắc nhở khẩn cấp trước 15 phút (trong khoảng 0 - 15 phút)
                        else if (minutesLeft <= 15 && minutesLeft >= 0) {
                            String key = b.getId() + "_15m";
                            if (sentReminderKeys.add(key)) {
                                sendPushNotification(b, minutesLeft, true);
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Bỏ qua lỗi định dạng giờ nếu có
                }
            }
        } catch (Exception e) {
            // Đảm bảo worker không bị dừng do ngoại lệ
        }
    }

    private void sendPushNotification(Booking booking, long minutesLeft, boolean isUrgent) {
        String titlePrefix = isUrgent ? "[KHẨN CẤP - CÒN " + minutesLeft + " PHÚT]" : "[NHẮC HỌP TRƯỚC 1 TIẾNG]";
        String message = String.format("%s Bạn có cuộc họp tại [%s] lúc %s (còn %d phút nữa). Mục đích: '%s'!",
                titlePrefix,
                booking.getRoomName(),
                booking.getStartTime(),
                minutesLeft,
                booking.getPurpose());

        Response pushRes = new Response(Response.SUCCESS, message, ActionType.REMINDER_NOTIFICATION, JsonUtil.toJson(booking));

        // Tìm kiếm socket client của người dùng đang online trên server
        boolean userOnline = false;
        for (ClientHandler client : serverManager.getActiveClients()) {
            if (client.getCurrentUser() != null && client.getCurrentUser().getId() == booking.getUserId()) {
                client.sendResponse(pushRes);
                userOnline = true;
            }
        }

        serverManager.log(String.format("[TCP PUSH REMINDER] %s -> Gửi tới User ID=%d (%s) - Online: %s",
                titlePrefix, booking.getUserId(), booking.getUserFullName(), userOnline ? "CÓ" : "KHÔNG"));
    }
}
