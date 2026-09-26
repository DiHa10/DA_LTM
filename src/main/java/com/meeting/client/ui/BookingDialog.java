package com.meeting.client.ui;

import com.meeting.client.net.SocketClient;
import com.meeting.common.model.Booking;
import com.meeting.common.model.Room;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingDialog extends JDialog {
    private final SocketClient client;
    private final User currentUser;
    private final List<Room> roomList;
    private final Runnable onSuccessCallback;

    private JComboBox<RoomWrapper> cboRooms;
    private JTextField txtDate;
    private JComboBox<String> cboStartTime;
    private JComboBox<String> cboEndTime;
    private JTextField txtPurpose;
    private JButton btnSubmit;
    private JButton btnCancel;

    public BookingDialog(Frame parent, SocketClient client, User currentUser, List<Room> rooms, String initialDate, Runnable onSuccessCallback) {
        super(parent, "Đặt phòng họp mới", true);
        this.client = client;
        this.currentUser = currentUser;
        this.roomList = rooms;
        this.onSuccessCallback = onSuccessCallback;

        initUI(initialDate);
    }

    private void initUI(String initialDate) {
        setSize(480, 420);
        setResizable(false);
        setLocationRelativeTo(getParent());

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 15));

        // 1. Phòng họp
        formPanel.add(new JLabel("Chọn phòng họp:"));
        cboRooms = new JComboBox<>();
        for (Room r : roomList) {
            cboRooms.addItem(new RoomWrapper(r));
        }
        formPanel.add(cboRooms);

        // 2. Ngày họp (YYYY-MM-DD)
        formPanel.add(new JLabel("Ngày họp (YYYY-MM-DD):"));
        String defaultDate = (initialDate != null && !initialDate.isEmpty()) ? initialDate : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        txtDate = new JTextField(defaultDate);
        formPanel.add(txtDate);

        // Khung giờ phổ biến 07:00 -> 21:00 bước 30 phút
        String[] timeSlots = generateTimeSlots();

        // 3. Giờ bắt đầu
        formPanel.add(new JLabel("Giờ bắt đầu:"));
        cboStartTime = new JComboBox<>(timeSlots);
        cboStartTime.setSelectedItem("09:00");
        formPanel.add(cboStartTime);

        // 4. Giờ kết thúc
        formPanel.add(new JLabel("Giờ kết thúc:"));
        cboEndTime = new JComboBox<>(timeSlots);
        cboEndTime.setSelectedItem("10:30");
        formPanel.add(cboEndTime);

        // 5. Mục đích
        formPanel.add(new JLabel("Mục đích cuộc họp:"));
        txtPurpose = new JTextField("Họp dự án");
        formPanel.add(txtPurpose);

        panel.add(formPanel, BorderLayout.CENTER);

        // Bottom Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnCancel = new JButton("Hủy bỏ");
        btnCancel.addActionListener(e -> dispose());

        btnSubmit = new JButton("Xác nhận Đặt phòng");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSubmit.setBackground(new Color(24, 90, 188));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.addActionListener(e -> doBookRoom());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        panel.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    private String[] generateTimeSlots() {
        String[] slots = new String[29]; // 07:00 -> 21:00
        int idx = 0;
        for (int h = 7; h <= 21; h++) {
            slots[idx++] = String.format("%02d:00", h);
            if (h < 21) {
                slots[idx++] = String.format("%02d:30", h);
            }
        }
        return slots;
    }

    private void doBookRoom() {
        RoomWrapper selected = (RoomWrapper) cboRooms.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phòng họp!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String date = txtDate.getText().trim();
        String startTime = (String) cboStartTime.getSelectedItem();
        String endTime = (String) cboEndTime.getSelectedItem();
        String purpose = txtPurpose.getText().trim();

        if (date.isEmpty() || purpose.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ ngày họp và mục đích!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate ngày hợp lệ YYYY-MM-DD
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Định dạng ngày không hợp lệ! Vui lòng dùng: YYYY-MM-DD (VD: 2026-09-28)", "Lỗi ngày", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate giờ bắt đầu < giờ kết thúc
        if (startTime.compareTo(endTime) >= 0) {
            JOptionPane.showMessageDialog(this, "Giờ kết thúc (" + endTime + ") phải sau giờ bắt đầu (" + startTime + ")!", "Lỗi thời gian", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Booking booking = new Booking();
        booking.setRoomId(selected.room.getId());
        booking.setUserId(currentUser.getId());
        booking.setBookingDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose(purpose);

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi yêu cầu...");

        new Thread(() -> {
            Request req = new Request(ActionType.BOOK_ROOM, currentUser.getId(), JsonUtil.toJson(booking));
            Response res = client.sendRequest(req);

            SwingUtilities.invokeLater(() -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Xác nhận Đặt phòng");

                if (res != null) {
                    if (res.isSuccess()) {
                        JOptionPane.showMessageDialog(BookingDialog.this,
                                "Chúc mừng! Bạn đã đặt phòng thành công:\n" +
                                        "• Phòng: " + selected.room.getName() + "\n" +
                                        "• Ngày: " + date + "\n" +
                                        "• Thời gian: " + startTime + " - " + endTime,
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        dispose();
                    } else if (res.isConflict()) {
                        // Thông báo khi Server chặn trùng lịch nhờ synchronized
                        JOptionPane.showMessageDialog(BookingDialog.this,
                                res.getMessage(),
                                "CẢNH BÁO: TRÙNG LỊCH PHÒNG HỌP", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(BookingDialog.this,
                                res.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(BookingDialog.this,
                            "Không nhận được phản hồi từ Server!",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private static class RoomWrapper {
        final Room room;
        RoomWrapper(Room room) { this.room = room; }
        @Override
        public String toString() {
            return room.getName() + " (Sức chứa: " + room.getCapacity() + " người)";
        }
    }
}
