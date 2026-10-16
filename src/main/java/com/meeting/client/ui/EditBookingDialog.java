package com.meeting.client.ui;

import com.google.gson.reflect.TypeToken;
import com.meeting.client.net.SocketClient;
import com.meeting.client.ui.util.AppIcon;
import com.meeting.client.ui.util.CalendarPickerPopup;
import com.meeting.common.model.Booking;
import com.meeting.common.model.Room;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;
import com.meeting.common.util.DateUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditBookingDialog extends JDialog {
    private final SocketClient client;
    private final User currentUser;
    private final List<Room> roomList;
    private final Booking bookingToEdit;
    private final Runnable onSuccessCallback;

    private JComboBox<RoomWrapper> cboRooms;
    private JTextField txtDate;
    private JComboBox<String> cboStartTime;
    private JComboBox<String> cboEndTime;
    private JTextField txtPurpose;
    private JButton btnSubmit;
    private JButton btnCancel;

    // Bảng màu Warm Minimalist (Kem Sữa, Giấy Mộc & Terracotta / Xanh Rêu)
    private static final Color BG_WARM = new Color(245, 243, 239);       // Nền be kem sữa (#F5F3EF)
    private static final Color CARD_BG = new Color(255, 255, 255);       // Giấy mộc trắng (#FFFFFF)
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);    // Cam đất Terracotta (#C25E34)
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);    // Xanh rêu trầm (#1C3F34)
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);     // Nâu đen Espresso (#2D2A26)
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);// Warm gray (#78716C)
    private static final Color INPUT_BG = new Color(250, 249, 246);      // Kem nhạt
    private static final Color BORDER_WARM = new Color(229, 224, 216);   // Viền cát ấm (#E5E0D8)
    private static final Color GRADIENT_START = new Color(194, 94, 52);  // Terracotta
    private static final Color GRADIENT_END = new Color(217, 119, 6);    // Amber

    public EditBookingDialog(Frame parent, SocketClient client, User currentUser, List<Room> rooms, Booking bookingToEdit, Runnable onSuccessCallback) {
        super(parent, "Chỉnh sửa cuộc họp #" + (bookingToEdit != null ? bookingToEdit.getId() : ""), true);
        this.client = client;
        this.currentUser = currentUser;
        this.roomList = rooms != null ? rooms : new ArrayList<>();
        this.bookingToEdit = bookingToEdit;
        this.onSuccessCallback = onSuccessCallback;

        initUI();
    }

    private void initUI() {
        setSize(520, 560);
        setMinimumSize(new Dimension(480, 500));
        setResizable(true);
        setLocationRelativeTo(getParent());

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_WARM);
        panel.setBorder(new EmptyBorder(22, 26, 20, 26));

        // Header
        JLabel lblTitle = new JLabel("Chỉnh Sửa Cuộc Họp");
        lblTitle.setIcon(AppIcon.calendar(20, ACCENT_TERRA));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Thay đổi thời gian, phòng họp hoặc mục đích (Hệ thống tự động kiểm tra trùng lịch)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSub);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Form Card giấy mộc
        JPanel formCard = new JPanel();
        formCard.setBackground(CARD_BG);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        // 1. Phòng họp
        cboRooms = new JComboBox<>();
        cboRooms.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboRooms.setBackground(INPUT_BG);
        cboRooms.setForeground(TEXT_PRIMARY);
        RoomWrapper selectedWrapper = null;
        for (Room r : roomList) {
            RoomWrapper rw = new RoomWrapper(r);
            cboRooms.addItem(rw);
            if (bookingToEdit != null && r.getId() == bookingToEdit.getRoomId()) {
                selectedWrapper = rw;
            }
        }
        if (selectedWrapper != null) {
            cboRooms.setSelectedItem(selectedWrapper);
        }

        JPanel roomRowPanel = new JPanel(new BorderLayout(8, 0));
        roomRowPanel.setOpaque(false);
        roomRowPanel.setMaximumSize(new Dimension(500, 36));
        roomRowPanel.add(cboRooms, BorderLayout.CENTER);

        JButton btnFilterFree = new JButton("Tra cứu phòng trống");
        btnFilterFree.setToolTipText("Lọc các phòng còn trống theo ngày & khung giờ mới");
        btnFilterFree.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFilterFree.setBackground(new Color(240, 253, 244));
        btnFilterFree.setForeground(new Color(22, 101, 52));
        btnFilterFree.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        btnFilterFree.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilterFree.setFocusPainted(false);
        btnFilterFree.addActionListener(e -> doFilterAvailableRooms());
        roomRowPanel.add(btnFilterFree, BorderLayout.EAST);

        formCard.add(createFormRow("Chọn phòng họp", roomRowPanel));
        formCard.add(Box.createVerticalStrut(12));

        // 2. Ngày họp
        String defaultDate = (bookingToEdit != null && bookingToEdit.getBookingDate() != null)
                ? DateUtil.toUiDate(bookingToEdit.getBookingDate())
                : DateUtil.todayUi();

        JPanel dateInputPanel = new JPanel(new BorderLayout(6, 0));
        dateInputPanel.setOpaque(false);
        dateInputPanel.setMaximumSize(new Dimension(500, 38));

        txtDate = createStyledField(defaultDate);
        txtDate.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnPickDate = new JButton();
        btnPickDate.setIcon(AppIcon.calendar(15, ACCENT_TERRA));
        btnPickDate.setToolTipText("Mở lịch chọn ngày trực quan");
        btnPickDate.setBackground(INPUT_BG);
        btnPickDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        btnPickDate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPickDate.setFocusPainted(false);

        dateInputPanel.add(txtDate, BorderLayout.CENTER);
        dateInputPanel.add(btnPickDate, BorderLayout.EAST);

        CalendarPickerPopup.attach(txtDate, btnPickDate, null);

        formCard.add(createFormRow("Ngày họp (dd-MM-yyyy)", dateInputPanel));
        formCard.add(Box.createVerticalStrut(12));

        // 3. Giờ bắt đầu & Giờ kết thúc
        String[] timeSlots = generateTimeSlots();

        JPanel timeRow = new JPanel(new GridLayout(1, 2, 12, 0));
        timeRow.setOpaque(false);
        timeRow.setMaximumSize(new Dimension(500, 60));
        timeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboStartTime = new JComboBox<>(timeSlots);
        cboStartTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboStartTime.setBackground(INPUT_BG);
        cboStartTime.setForeground(TEXT_PRIMARY);
        if (bookingToEdit != null && bookingToEdit.getStartTime() != null) {
            cboStartTime.setSelectedItem(bookingToEdit.getStartTime());
        }

        cboEndTime = new JComboBox<>(timeSlots);
        cboEndTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboEndTime.setBackground(INPUT_BG);
        cboEndTime.setForeground(TEXT_PRIMARY);
        if (bookingToEdit != null && bookingToEdit.getEndTime() != null) {
            cboEndTime.setSelectedItem(bookingToEdit.getEndTime());
        }

        timeRow.add(createFormRow("Giờ bắt đầu", cboStartTime));
        timeRow.add(createFormRow("Giờ kết thúc", cboEndTime));
        formCard.add(timeRow);
        formCard.add(Box.createVerticalStrut(12));

        // 4. Mục đích
        txtPurpose = createStyledField(bookingToEdit != null ? bookingToEdit.getPurpose() : "");
        formCard.add(createFormRow("Mục đích cuộc họp", txtPurpose));

        panel.add(formCard, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setBackground(INPUT_BG);
        btnCancel.setForeground(TEXT_PRIMARY);
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> dispose());
        buttonPanel.add(btnCancel);

        btnSubmit = new JButton("Lưu thay đổi") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, GRADIENT_END);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setContentAreaFilled(false);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setOpaque(false);
        btnSubmit.setPreferredSize(new Dimension(140, 38));
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> submitUpdateBooking());
        buttonPanel.add(btnSubmit);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    private void submitUpdateBooking() {
        if (bookingToEdit == null) return;

        RoomWrapper selectedRoom = (RoomWrapper) cboRooms.getSelectedItem();
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng họp!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String rawDate = txtDate.getText().trim();
        if (rawDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập ngày họp!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String startTime = (String) cboStartTime.getSelectedItem();
        String endTime = (String) cboEndTime.getSelectedItem();

        if (startTime == null || endTime == null || startTime.compareTo(endTime) >= 0) {
            JOptionPane.showMessageDialog(this,
                    "Giờ kết thúc (" + endTime + ") phải sau giờ bắt đầu (" + startTime + ")!",
                    "Thời gian không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String purpose = txtPurpose.getText().trim();
        if (purpose.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mục đích cuộc họp!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Booking updated = new Booking();
        updated.setId(bookingToEdit.getId());
        updated.setRoomId(selectedRoom.room.getId());
        updated.setBookingDate(DateUtil.toDbDate(rawDate));
        updated.setStartTime(startTime);
        updated.setEndTime(endTime);
        updated.setPurpose(purpose);
        updated.setInvitedUsers(bookingToEdit.getInvitedUsers());

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang lưu...");

        new Thread(() -> {
            Request req = new Request(ActionType.UPDATE_BOOKING, currentUser.getId(), JsonUtil.toJson(updated));
            Response res = client.sendRequest(req);

            SwingUtilities.invokeLater(() -> {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Lưu thay đổi");

                if (res != null) {
                    if (res.isSuccess()) {
                        JOptionPane.showMessageDialog(EditBookingDialog.this,
                                "Cập nhật cuộc họp thành công!\n" +
                                "• Phòng: " + selectedRoom.room.getName() + "\n" +
                                "• Ngày: " + DateUtil.toUiDate(rawDate) + "\n" +
                                "• Khung giờ: " + startTime + " - " + endTime + "\n" +
                                "• Mục đích: " + purpose,
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        dispose();
                    } else if (res.isConflict()) {
                        JOptionPane.showMessageDialog(EditBookingDialog.this,
                                "⚠ CẢNH BÁO TRÙNG LỊCH PHÒNG HỌP:\n\n" + res.getMessage() + "\n\nVui lòng đổi khung giờ hoặc chọn phòng họp khác!",
                                "CẢNH BÁO TRÙNG LỊCH", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(EditBookingDialog.this,
                                res.getMessage(),
                                "Lỗi cập nhật", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(EditBookingDialog.this,
                            "Không nhận được phản hồi từ Server!",
                            "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private void doFilterAvailableRooms() {
        String rawDate = txtDate.getText().trim();
        String startTime = (String) cboStartTime.getSelectedItem();
        String endTime = (String) cboEndTime.getSelectedItem();

        if (startTime.compareTo(endTime) >= 0) {
            JOptionPane.showMessageDialog(this,
                    "Giờ kết thúc (" + endTime + ") phải sau giờ bắt đầu (" + startTime + ") để tìm phòng trống!",
                    "Lỗi thời gian", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String dbDate = DateUtil.toDbDate(rawDate);
        Map<String, String> data = new HashMap<>();
        data.put("date", dbDate);
        data.put("startTime", startTime);
        data.put("endTime", endTime);

        new Thread(() -> {
            Request req = new Request(ActionType.FIND_AVAILABLE_ROOMS, currentUser.getId(), JsonUtil.toJson(data));
            Response res = client.sendRequest(req);
            SwingUtilities.invokeLater(() -> {
                if (res != null && res.isSuccess()) {
                    List<Room> freeRooms = JsonUtil.fromJson(res.getData(), new TypeToken<List<Room>>() {}.getType());
                    cboRooms.removeAllItems();
                    if (freeRooms != null && !freeRooms.isEmpty()) {
                        RoomWrapper currentRoomWrapper = null;
                        for (Room r : freeRooms) {
                            RoomWrapper rw = new RoomWrapper(r);
                            cboRooms.addItem(rw);
                            if (bookingToEdit != null && r.getId() == bookingToEdit.getRoomId()) {
                                currentRoomWrapper = rw;
                            }
                        }
                        if (currentRoomWrapper != null) {
                            cboRooms.setSelectedItem(currentRoomWrapper);
                        }
                        JOptionPane.showMessageDialog(this,
                                "Tìm thấy " + freeRooms.size() + " phòng họp còn trống trong khung giờ " + startTime + " - " + endTime + " (ngày " + DateUtil.toUiDate(rawDate) + ")!",
                                "Phòng khả dụng", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Tất cả phòng họp đều đã bận trong khung giờ " + startTime + " - " + endTime + " (ngày " + DateUtil.toUiDate(rawDate) + ")!\nVui lòng chọn khung giờ khác.",
                                "Không còn phòng trống", JOptionPane.WARNING_MESSAGE);
                        for (Room r : roomList) {
                            cboRooms.addItem(new RoomWrapper(r));
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Lỗi khi tra cứu danh sách phòng trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    private JPanel createFormRow(String labelText, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(500, 60));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_PRIMARY);
        row.add(label, BorderLayout.NORTH);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private JTextField createStyledField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBackground(INPUT_BG);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_TERRA);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        tf.setMaximumSize(new Dimension(500, 36));
        return tf;
    }

    private String[] generateTimeSlots() {
        List<String> slots = new ArrayList<>();
        for (int h = 7; h <= 21; h++) {
            slots.add(String.format("%02d:00", h));
            slots.add(String.format("%02d:30", h));
        }
        slots.add("22:00");
        return slots.toArray(new String[0]);
    }

    private static class RoomWrapper {
        final Room room;
        RoomWrapper(Room room) { this.room = room; }
        @Override
        public String toString() {
            return room.getName() + " (Sức chứa: " + room.getCapacity() + " người - " + room.getLocation() + ")";
        }
    }
}
