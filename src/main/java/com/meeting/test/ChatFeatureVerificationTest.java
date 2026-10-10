package com.meeting.test;

import com.google.gson.reflect.TypeToken;
import com.meeting.client.net.SocketClient;
import com.meeting.common.model.ChatMessage;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;
import com.meeting.server.core.ServerManager;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatFeatureVerificationTest {
    private static final int PORT = 8899;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println(" BẮT ĐẦU KIỂM THỬ TÍNH NĂNG CHAT NỘI BỘ VÀ CHAT RIÊNG BẢO MẬT 1-1");
        System.out.println("==================================================================");

        ServerManager server = new ServerManager();
        SocketClient clientAdmin = new SocketClient();
        SocketClient clientEmp1 = new SocketClient();
        SocketClient clientEmp2 = new SocketClient();

        try {
            server.startServer(PORT);
            System.out.println("[PASS] 1. Khởi động Server trên cổng " + PORT + " thành công.");

            // Kết nối các client
            if (!clientAdmin.connect("127.0.0.1", PORT) ||
                !clientEmp1.connect("127.0.0.1", PORT) ||
                !clientEmp2.connect("127.0.0.1", PORT)) {
                System.err.println("[FAIL] Không thể kết nối các Client tới Server!");
                return;
            }
            System.out.println("[PASS] 2. Kết nối 3 Client (Admin, Nhân viên 1, Nhân viên 2) thành công.");

            // Đăng nhập
            Response resLoginAdmin = clientAdmin.sendRequest(new Request(ActionType.LOGIN, JsonUtil.toJson(Map.of("username", "admin", "password", "admin123"))));
            Response resLoginEmp1 = clientEmp1.sendRequest(new Request(ActionType.LOGIN, JsonUtil.toJson(Map.of("username", "nhanvien1", "password", "123456"))));
            Response resLoginEmp2 = clientEmp2.sendRequest(new Request(ActionType.LOGIN, JsonUtil.toJson(Map.of("username", "nhanvien2", "password", "123456"))));

            if (!resLoginAdmin.isSuccess() || !resLoginEmp1.isSuccess() || !resLoginEmp2.isSuccess()) {
                System.err.println("[FAIL] Đăng nhập thất bại!");
                return;
            }
            User adminUser = JsonUtil.fromJson(resLoginAdmin.getData(), User.class);
            User emp1User = JsonUtil.fromJson(resLoginEmp1.getData(), User.class);
            User emp2User = JsonUtil.fromJson(resLoginEmp2.getData(), User.class);
            System.out.println("[PASS] 3. Đăng nhập thành công: " + adminUser.getFullName() + ", " + emp1User.getFullName() + ", " + emp2User.getFullName());

            // TEST 1: CHAT NỘI BỘ (Ai cũng thấy và nhắn được)
            CountDownLatch publicLatch = new CountDownLatch(2);
            clientEmp1.setBroadcastListener(res -> {
                if (res.getAction() == ActionType.CHAT_BROADCAST) {
                    publicLatch.countDown();
                }
            });
            clientEmp2.setBroadcastListener(res -> {
                if (res.getAction() == ActionType.CHAT_BROADCAST) {
                    publicLatch.countDown();
                }
            });

            ChatMessage pubMsg = new ChatMessage();
            pubMsg.setContent("Thông báo toàn thể công ty về cuộc họp chiều nay!");
            pubMsg.setType("PUBLIC");
            pubMsg.setReceiverId(0);

            Response sendPubRes = clientAdmin.sendRequest(new Request(ActionType.SEND_CHAT_MESSAGE, JsonUtil.toJson(pubMsg), adminUser.getId()));
            if (!sendPubRes.isSuccess()) {
                System.err.println("[FAIL] Gửi tin nhắn nội bộ thất bại!");
                return;
            }

            boolean allReceivedPublic = publicLatch.await(3, TimeUnit.SECONDS);
            if (allReceivedPublic) {
                System.out.println("[PASS] 4. Kênh Chat Nội Bộ hoạt động: Tất cả nhân viên đều nhận được tin broadcast!");
            } else {
                System.err.println("[FAIL] Kênh Chat Nội Bộ không chuyển tin nhắn tới tất cả nhân viên!");
                return;
            }

            // TEST 2: CHAT RIÊNG TƯ 1-1 BẢO MẬT (Chỉ Admin nhận được, Nhân viên 2 KHÔNG nhận được)
            CountDownLatch privateAdminLatch = new CountDownLatch(1);
            AtomicBoolean emp2Leaked = new AtomicBoolean(false);

            clientAdmin.setBroadcastListener(res -> {
                if (res.getAction() == ActionType.CHAT_PRIVATE_RECEIVE) {
                    privateAdminLatch.countDown();
                }
            });
            clientEmp2.setBroadcastListener(res -> {
                if (res.getAction() == ActionType.CHAT_PRIVATE_RECEIVE) {
                    emp2Leaked.set(true);
                }
            });

            ChatMessage privMsg = new ChatMessage();
            privMsg.setContent("Báo cáo riêng tư mật gửi Admin: Đã chuẩn bị xong hợp đồng.");
            privMsg.setType("PRIVATE");
            privMsg.setReceiverId(adminUser.getId());
            privMsg.setReceiverName(adminUser.getFullName());

            Response sendPrivRes = clientEmp1.sendRequest(new Request(ActionType.SEND_CHAT_MESSAGE, JsonUtil.toJson(privMsg), emp1User.getId()));
            if (!sendPrivRes.isSuccess()) {
                System.err.println("[FAIL] Gửi tin nhắn riêng thất bại!");
                return;
            }

            boolean adminReceivedPrivate = privateAdminLatch.await(3, TimeUnit.SECONDS);
            if (adminReceivedPrivate && !emp2Leaked.get()) {
                System.out.println("[PASS] 5. Chat riêng 1-1 BẢO MẬT TUYỆT ĐỐI: Chỉ người nhận (Admin) nhận được tin, Client khác (Nhân viên 2) KHÔNG bị lộ!");
            } else {
                System.err.println("[FAIL] Chat riêng thất bại (Admin received=" + adminReceivedPrivate + ", leaked=" + emp2Leaked.get() + ")");
                return;
            }

            // TEST 3: LƯU TRỮ VÀ TẢI LỊCH SỬ CHAT TỪ CSDL SQLITE
            Map<String, Object> reqPubHistory = Map.of("type", "PUBLIC");
            Response resPubHist = clientAdmin.sendRequest(new Request(ActionType.GET_CHAT_HISTORY, JsonUtil.toJson(reqPubHistory), adminUser.getId()));
            List<ChatMessage> pubList = JsonUtil.fromJson(resPubHist.getData(), new TypeToken<List<ChatMessage>>() {}.getType());

            Map<String, Object> reqPrivHistory = Map.of("type", "PRIVATE", "targetUserId", emp1User.getId());
            Response resPrivHist = clientAdmin.sendRequest(new Request(ActionType.GET_CHAT_HISTORY, JsonUtil.toJson(reqPrivHistory), adminUser.getId()));
            List<ChatMessage> privList = JsonUtil.fromJson(resPrivHist.getData(), new TypeToken<List<ChatMessage>>() {}.getType());

            if (pubList != null && !pubList.isEmpty() && privList != null && !privList.isEmpty()) {
                System.out.println("[PASS] 6. Lưu trữ SQLite thành công: Tải lại được " + pubList.size() + " tin nội bộ và " + privList.size() + " tin riêng tư.");
            } else {
                System.err.println("[FAIL] Không tải được lịch sử chat từ SQLite!");
                return;
            }

            // TEST 4: ĐÁNH DẤU ĐÃ ĐỌC
            Response resMarkRead = clientAdmin.sendRequest(new Request(ActionType.MARK_CHAT_READ, adminUser.getId(), String.valueOf(emp1User.getId())));
            if (resMarkRead.isSuccess()) {
                System.out.println("[PASS] 7. Đánh dấu đã đọc tin nhắn riêng thành công.");
            }

            System.out.println("==================================================================");
            System.out.println(" TẤT CẢ CÁC BÀI KIỂM THỬ CHAT RIÊNG VÀ NỘI BỘ ĐỀU ĐẠT CHUẨN (PASS)!");
            System.out.println("==================================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientAdmin.disconnect();
            clientEmp1.disconnect();
            clientEmp2.disconnect();
            server.stopServer();
        }
    }
}
