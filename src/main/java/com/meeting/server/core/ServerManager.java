package com.meeting.server.core;

import com.meeting.common.model.User;
import com.meeting.common.protocol.Response;
import com.meeting.server.db.DatabaseManager;
import com.meeting.server.service.BookingService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ServerManager {
    private ServerSocket serverSocket;
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private final BookingService bookingService = new BookingService();
    private ExecutorService threadPool;
    private boolean isRunning = false;

    // Callbacks để cập nhật UI Server Monitor
    private Consumer<String> logListener;
    private Runnable clientListListener;

    public ServerManager() {
        // Tự động khởi tạo cấu trúc CSDL SQLite
        DatabaseManager.initializeDatabase();
    }

    public synchronized void startServer(int port) throws IOException {
        if (isRunning) return;

        serverSocket = new ServerSocket(port);
        isRunning = true;
        threadPool = Executors.newCachedThreadPool();

        log("=== SERVER ĐÃ KHỞI CHẠY TRÊN CỔNG " + port + " ===");
        log("Sẵn sàng tiếp nhận kết nối từ các máy trạm (Client)...");

        // Luồng nền lắng nghe kết nối
        new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this, bookingService);
                    activeClients.add(handler);
                    threadPool.submit(handler);
                    updateClientList();
                } catch (IOException e) {
                    if (!isRunning) {
                        break;
                    }
                    log("Lỗi tiếp nhận kết nối: " + e.getMessage());
                }
            }
        }, "Server-Accept-Thread").start();
    }

    public synchronized void stopServer() {
        if (!isRunning) return;
        isRunning = false;

        log("Đang dừng máy chủ...");
        for (ClientHandler client : activeClients) {
            client.close();
        }
        activeClients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (threadPool != null) {
            threadPool.shutdownNow();
        }

        updateClientList();
        log("=== SERVER ĐÃ DỪNG ===");
    }

    public void broadcast(Response response) {
        for (ClientHandler client : activeClients) {
            client.sendResponse(response);
        }
    }

    public void removeClient(ClientHandler handler) {
        activeClients.remove(handler);
        updateClientList();
    }

    /**
     * Kiểm tra và đăng ký người dùng đăng nhập đồng thời (Thread-safe).
     * Ngăn chặn 2 Client cùng đăng nhập vào 1 tài khoản cùng lúc.
     */
    public synchronized boolean registerUserLogin(ClientHandler handler, User user) {
        for (ClientHandler client : activeClients) {
            if (client != handler && client.getCurrentUser() != null && client.getCurrentUser().getId() == user.getId()) {
                return false; // Tài khoản này đã đang online trên 1 client khác
            }
        }
        handler.setCurrentUser(user);
        updateClientList();
        return true;
    }

    /**
     * Hủy đăng ký tài khoản khi người dùng bấm Đăng xuất.
     */
    public synchronized void unregisterUser(ClientHandler handler) {
        handler.setCurrentUser(null);
        updateClientList();
    }

    public void updateClientList() {
        if (clientListListener != null) {
            clientListListener.run();
        }
    }

    public void log(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String formatted = String.format("[%s] %s", timestamp, message);
        System.out.println(formatted);
        if (logListener != null) {
            logListener.accept(formatted);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public List<ClientHandler> getActiveClients() {
        return activeClients;
    }

    public void setLogListener(Consumer<String> logListener) {
        this.logListener = logListener;
    }

    public void setClientListListener(Runnable clientListListener) {
        this.clientListListener = clientListListener;
    }
}
