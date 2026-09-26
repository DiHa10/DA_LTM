package com.meeting.test;

import com.meeting.client.net.SocketClient;
import com.meeting.common.model.Booking;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Script kiểm thử tải & xung đột lịch (Race Condition Test)
 * Giả lập 10 Client cùng kết nối qua TCP Socket và gửi lệnh đặt phòng cùng 1 khung giờ tại cùng 1 mili-giây.
 */
public class ConcurrencySimulationTest {
    private static final int THREAD_COUNT = 10;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8888;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============================================================");
        System.out.println("BẮT ĐẦU KIỂM THỬ ĐỒNG BỘ SYNCHRONIZED TRÊN TCP SOCKET SERVER");
        System.out.println("Giả lập " + THREAD_COUNT + " nhân viên cùng bấm ĐẶT PHÒNG tại cùng 1 thời điểm!");
        System.out.println("===============================================================");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 1; i <= THREAD_COUNT; i++) {
            final int employeeId = i;
            executor.submit(() -> {
                try {
                    // Chờ tiếng còi xuất phát để tất cả cùng bắn tại 1 nano-giây
                    startSignal.await();

                    SocketClient client = new SocketClient();
                    if (!client.connect(HOST, PORT)) {
                        System.err.println("[Thread #" + employeeId + "] Không thể kết nối tới Server!");
                        return;
                    }

                    Booking booking = new Booking();
                    booking.setRoomId(1); // Cùng đặt Phòng 1
                    booking.setUserId(employeeId);
                    booking.setBookingDate("2026-10-10"); // Cùng 1 ngày
                    booking.setStartTime("09:00");         // Cùng giờ bắt đầu
                    booking.setEndTime("10:30");           // Cùng giờ kết thúc
                    booking.setPurpose("Họp Sprint Review bởi nhân viên #" + employeeId);

                    Request req = new Request(ActionType.BOOK_ROOM, employeeId, JsonUtil.toJson(booking));
                    Response res = client.sendRequest(req);

                    if (res != null && res.isSuccess()) {
                        successCount.incrementAndGet();
                        System.out.println(">>> [THÀNH CÔNG] Thread #" + employeeId + ": " + res.getMessage());
                    } else if (res != null && res.isConflict()) {
                        conflictCount.incrementAndGet();
                        System.out.println("--- [BỊ TỪ CHỐI] Thread #" + employeeId + ": " + res.getMessage());
                    } else {
                        System.out.println("[LỖI KHÁC] Thread #" + employeeId + ": " + (res != null ? res.getMessage() : "null"));
                    }

                    client.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Bắn tín hiệu bắt đầu
        System.out.println(">>> ĐANG PHÁT LỆNH GỬI ĐỒNG THỜI...");
        startSignal.countDown();

        // Chờ tất cả kết thúc
        doneSignal.await();
        executor.shutdown();

        System.out.println("\n================ KẾT QUẢ KIỂM ĐỊNH ================");
        System.out.println("Tổng số yêu cầu gửi: " + THREAD_COUNT);
        System.out.println("Số yêu cầu thành công : " + successCount.get() + " (Mong đợi: ĐÚNG 1)");
        System.out.println("Số yêu cầu bị chặn    : " + conflictCount.get() + " (Mong đợi: " + (THREAD_COUNT - 1) + ")");

        if (successCount.get() == 1 && conflictCount.get() == (THREAD_COUNT - 1)) {
            System.out.println(">>> KẾT LUẬN: BÀI TOÁN SYNCHRONIZED ĐẠT CHUẨN 100%! KHÔNG BỊ TRÙNG LỊCH!");
        } else {
            System.out.println(">>> KẾT LUẬN: CẦN KIỂM TRA LẠI KHÓA ĐỒNG BỘ!");
        }
        System.out.println("====================================================");
    }
}
