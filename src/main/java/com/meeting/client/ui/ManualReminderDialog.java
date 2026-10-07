package com.meeting.client.ui;

import com.meeting.client.net.SocketClient;
import com.meeting.common.model.Booking;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.meeting.client.ui.util.AppIcon;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Hộp thoại cho phép Chủ phòng (Host) gửi email nhắc nhở thủ công tới các đồng nghiệp/cấp dưới
 * tham gia cuộc họp, có thể đính kèm lời nhắn khẩn cấp hoặc ghi chú bổ sung.
 */
public class ManualReminderDialog extends JDialog {
    private final SocketClient client;
    private final User currentUser;
    private final Booking booking;

    private JTextArea txtNote;
    private JButton btnSend;

    // Bảng màu Warm Minimalist
    private static final Color BG_WARM = new Color(245, 243, 239);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);
    private static final Color INPUT_BG = new Color(250, 249, 246);
    private static final Color BORDER_WARM = new Color(229, 224, 216);

    public ManualReminderDialog(Frame parent, SocketClient client, User currentUser, Booking booking) {
        super(parent, "Gửi email nhắc nhở cuộc họp (Chủ phòng)", true);
        this.client = client;
        this.currentUser = currentUser;
        this.booking = booking;

        initUI();
    }

    private void initUI() {
        setSize(480, 480);
        setResizable(false);
        setLocationRelativeTo(getParent());

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BG_WARM);
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Header
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Nhắc Nhở Lịch Họp Thủ Công");
        lblTitle.setIcon(AppIcon.mail(20, ACCENT_TERRA));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Gửi email trực tiếp tới tất cả đồng nghiệp/cấp dưới tham gia cuộc họp");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);

        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(4));
        pnlHeader.add(lblSub);
        root.add(pnlHeader, BorderLayout.NORTH);

        // Center Card
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Box tóm tắt cuộc họp
        JPanel infoBox = new JPanel(new GridLayout(4, 1, 4, 4));
        infoBox.setBackground(new Color(254, 243, 235));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 215, 195), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblRoom = new JLabel("• Phòng họp: " + (booking.getRoomName() != null ? booking.getRoomName() : ("ID=" + booking.getRoomId())));
        lblRoom.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblRoom.setForeground(ACCENT_TERRA);

        JLabel lblTime = new JLabel("• Thời gian: " + booking.getStartTime() + " - " + booking.getEndTime() + " (Ngày " + com.meeting.common.util.DateUtil.toUiDate(booking.getBookingDate()) + ")");
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblPurpose = new JLabel("• Mục đích: " + (booking.getPurpose() != null ? booking.getPurpose() : "Họp nội bộ"));
        lblPurpose.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        String invitees = booking.getInvitedUserNames() != null && !booking.getInvitedUserNames().isEmpty()
                ? booking.getInvitedUserNames()
                : (booking.getInvitedUsers() != null ? ("User IDs: " + booking.getInvitedUsers()) : "Chưa có");
        JLabel lblAttendees = new JLabel("• Người tham gia: " + invitees);
        lblAttendees.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblAttendees.setForeground(TEXT_SECONDARY);

        infoBox.add(lblRoom);
        infoBox.add(lblTime);
        infoBox.add(lblPurpose);
        infoBox.add(lblAttendees);
        card.add(infoBox);
        card.add(Box.createVerticalStrut(14));

        // Lời nhắn bổ sung
        JLabel lblNotePrompt = new JLabel("Lời nhắn / Ghi chú bổ sung đính kèm thư (Tùy chọn):");
        lblNotePrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNotePrompt.setForeground(TEXT_PRIMARY);
        lblNotePrompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblNotePrompt);
        card.add(Box.createVerticalStrut(6));

        txtNote = new JTextArea("Vui lòng mang theo tài liệu báo cáo và có mặt đúng giờ!");
        txtNote.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNote.setBackground(INPUT_BG);
        txtNote.setForeground(TEXT_PRIMARY);
        txtNote.setLineWrap(true);
        txtNote.setWrapStyleWord(true);

        JScrollPane scrollNote = new JScrollPane(txtNote);
        scrollNote.setBorder(BorderFactory.createLineBorder(BORDER_WARM, 1));
        scrollNote.setPreferredSize(new Dimension(0, 100));
        scrollNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(scrollNote);

        root.add(card, BorderLayout.CENTER);

        // Buttons Footer
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlFooter.setOpaque(false);

        JButton btnCancel = new JButton("Đóng");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> dispose());
        pnlFooter.add(btnCancel);

        btnSend = new JButton("Gửi Mail Nhắc Nhở Ngay");
        btnSend.setIcon(AppIcon.mail(14, Color.WHITE));
        btnSend.setIconTextGap(6);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setBackground(ACCENT_TERRA);
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.addActionListener(e -> doSendReminder());
        pnlFooter.add(btnSend);

        root.add(pnlFooter, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void doSendReminder() {
        btnSend.setEnabled(false);
        btnSend.setText("Đang gửi...");

        String customNote = txtNote.getText().trim();
        Map<String, String> data = new HashMap<>();
        data.put("bookingId", String.valueOf(booking.getId()));
        data.put("customNote", customNote);

        new Thread(() -> {
            try {
                Request req = new Request(ActionType.SEND_MANUAL_EMAIL_REMINDER, currentUser.getId(), JsonUtil.toJson(data));
                Response res = client.sendRequest(req);

                SwingUtilities.invokeLater(() -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("Gửi Mail Nhắc Nhở Ngay");

                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, res.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        String msg = res != null ? res.getMessage() : "Không thể kết nối đến máy chủ!";
                        JOptionPane.showMessageDialog(this, msg, "Thông báo lỗi", JOptionPane.WARNING_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnSend.setEnabled(true);
                    btnSend.setText("Gửi Mail Nhắc Nhở Ngay");
                    JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
