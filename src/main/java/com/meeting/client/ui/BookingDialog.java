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
import java.awt.geom.RoundRectangle2D;
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

    // Bảng màu Dark Neon
    private static final Color BG_DARK = new Color(18, 18, 24);
    private static final Color CARD_BG = new Color(28, 28, 38);
    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color ACCENT_GLOW = new Color(129, 140, 248);
    private static final Color ACCENT2 = new Color(16, 185, 129);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color INPUT_BG = new Color(38, 38, 52);
    private static final Color INPUT_BORDER = new Color(55, 55, 75);
    private static final Color GRADIENT_START = new Color(79, 70, 229);

    public BookingDialog(Frame parent, SocketClient client, User currentUser, List<Room> rooms, String initialDate, Runnable onSuccessCallback) {
        super(parent, "Đặt phòng họp mới", true);
        this.client = client;
        this.currentUser = currentUser;
        this.roomList = rooms;
        this.onSuccessCallback = onSuccessCallback;

        initUI(initialDate);
    }

    private void initUI(String initialDate) {
        setSize(500, 480);
        setResizable(false);
        setLocationRelativeTo(getParent());

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(24, 28, 20, 28));

        // Header
        JLabel lblTitle = new JLabel("📝  Đặt Phòng Họp Mới");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Điền thông tin bên dưới để tạo lịch họp");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSub);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Form Card
        JPanel formCard = new JPanel();
        formCard.setBackground(CARD_BG);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // 1. Phòng họp
        cboRooms = new JComboBox<>();
        cboRooms.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboRooms.setBackground(INPUT_BG);
        cboRooms.setForeground(TEXT_PRIMARY);
        for (Room r : roomList) {
            cboRooms.addItem(new RoomWrapper(r));
        }
        formCard.add(createFormRow("Chọn phòng họp", cboRooms));
        formCard.add(Box.createVerticalStrut(10));

        // 2. Ngày họp
        String defaultDate = (initialDate != null && !initialDate.isEmpty()) ? initialDate : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        txtDate = createStyledField(defaultDate);
        formCard.add(createFormRow("Ngày họp (YYYY-MM-DD)", txtDate));
        formCard.add(Box.createVerticalStrut(10));

        String[] timeSlots = generateTimeSlots();

        // 3. Giờ bắt đầu & Kết thúc
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 12, 0));
        timeRow.setOpaque(false);
        timeRow.setMaximumSize(new Dimension(500, 60));
        timeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboStartTime = new JComboBox<>(timeSlots);
        cboStartTime.setSelectedItem("09:00");
        cboStartTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboStartTime.setBackground(INPUT_BG);
        cboStartTime.setForeground(TEXT_PRIMARY);

        cboEndTime = new JComboBox<>(timeSlots);
        cboEndTime.setSelectedItem("10:30");
        cboEndTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboEndTime.setBackground(INPUT_BG);
        cboEndTime.setForeground(TEXT_PRIMARY);

        timeRow.add(createFormRow("Giờ bắt đầu", cboStartTime));
        timeRow.add(createFormRow("Giờ kết thúc", cboEndTime));
        formCard.add(timeRow);
        formCard.add(Box.createVerticalStrut(10));

        // 4. Mục đích
        txtPurpose = createStyledField("Họp dự án");
        formCard.add(createFormRow("Mục đích cuộc họp", txtPurpose));

        panel.add(formCard, BorderLayout.CENTER);

        // Bottom Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setBackground(INPUT_BG);
        btnCancel.setForeground(TEXT_SECONDARY);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());

        btnSubmit = new JButton("Xác nhận Đặt phòng") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, ACCENT);
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
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setPreferredSize(new Dimension(180, 36));
        btnSubmit.addActionListener(e -> doBookRoom());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        panel.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    private JTextField createStyledField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_GLOW);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return field;
    }

    private JPanel createFormRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(500, 55));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SECONDARY);
        row.add(lbl, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
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
        btnSubmit.setText("Đang gửi...");

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
