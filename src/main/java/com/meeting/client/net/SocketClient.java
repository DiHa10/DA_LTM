package com.meeting.client.net;

import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private boolean isConnected = false;

    // Hàng đợi nhận phản hồi đồng bộ từ Server cho các request
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    // Listener lắng nghe sự kiện Server chủ động đẩy về (Broadcast)
    private Consumer<Response> broadcastListener;
    private Runnable disconnectListener;

    public synchronized boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            isConnected = true;

            // Bắt đầu luồng đọc dữ liệu từ Socket Server
            listenerThread = new Thread(this::listenLoop, "Client-Socket-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;
        } catch (IOException e) {
            isConnected = false;
            return false;
        }
    }

    private void listenLoop() {
        try {
            String line;
            while (isConnected && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Response response = JsonUtil.fromJson(line, Response.class);
                if (response != null) {
                    // Nếu là thông báo Broadcast cập nhật từ Server
                    if (response.getAction() == ActionType.BROADCAST_UPDATE) {
                        if (broadcastListener != null) {
                            broadcastListener.accept(response);
                        }
                    } else {
                        // Nếu là kết quả của Request vừa gửi đi
                        responseQueue.offer(response);
                    }
                }
            }
        } catch (IOException e) {
            // Mất kết nối
        } finally {
            disconnect();
        }
    }

    public synchronized Response sendRequest(Request request) {
        if (!isConnected || out == null) {
            return Response.error("Chưa kết nối tới máy chủ TCP!");
        }

        // Dọn hàng đợi phản hồi cũ nếu còn sót
        responseQueue.clear();

        // Gửi chuỗi JSON qua Socket
        String json = JsonUtil.toJson(request);
        out.println(json);

        // Chờ kết quả phản hồi từ Server (timeout 6 giây)
        try {
            Response response = responseQueue.poll(6, TimeUnit.SECONDS);
            if (response == null) {
                return Response.error("Hết thời gian chờ phản hồi từ máy chủ (Timeout)!");
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.error("Yêu cầu bị ngắt quãng!");
        }
    }

    public synchronized void disconnect() {
        if (!isConnected) return;
        isConnected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}

        if (disconnectListener != null) {
            disconnectListener.run();
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    public void setBroadcastListener(Consumer<Response> broadcastListener) {
        this.broadcastListener = broadcastListener;
    }

    public void setDisconnectListener(Runnable disconnectListener) {
        this.disconnectListener = disconnectListener;
    }
}
