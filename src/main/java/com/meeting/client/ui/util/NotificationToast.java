package com.meeting.client.ui.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Cửa sổ popup thông báo nổi phong cách Messenger / Windows Toast.
 * Tự động hiển thị ở góc dưới bên phải màn hình khi có tin nhắn mới,
 * phát chuông âm thanh, hỗ trợ click để mở ngay cuộc trò chuyện.
 */
public class NotificationToast extends JWindow {
    private static final int TOAST_WIDTH = 370;
    private static final int TOAST_HEIGHT = 88;
    private static final int PADDING_SCREEN = 24;
    private static final int SPACING_STACK = 10;
    private static final int AUTO_CLOSE_MS = 4500;

    // Danh sách các toast đang hiển thị để xếp tầng gọn gàng
    private static final List<NotificationToast> activeToasts = new ArrayList<>();

    private final Timer dismissTimer;
    private final Runnable onClickAction;

    // Bảng màu
    private static final Color BG_CARD = new Color(255, 255, 255);
    private static final Color BORDER_COLOR = new Color(229, 224, 216);
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);

    public NotificationToast(String title, String senderName, String messageSnippet, boolean isPrivate, Runnable onClickAction) {
        this.onClickAction = onClickAction;

        setAlwaysOnTop(true);
        setFocusableWindowState(false); // Không cướp focus bàn phím của người dùng
        setSize(TOAST_WIDTH, TOAST_HEIGHT);

        // Thiết lập bo góc cửa sổ (nếu hệ thống hỗ trợ)
        try {
            setBackground(new Color(0, 0, 0, 0));
        } catch (Exception ignored) {}

        JPanel rootPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Nền thẻ trắng bo tròn
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // Viền mảnh
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // Thanh màu điểm nhấn bên trái
                g2.setColor(isPrivate ? ACCENT_TERRA : ACCENT_FOREST);
                g2.fillRoundRect(0, 0, 6, getHeight() - 1, 14, 14);
                g2.fillRect(3, 0, 3, getHeight() - 1);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        rootPanel.setOpaque(false);
        rootPanel.setLayout(new BorderLayout(12, 0));
        rootPanel.setBorder(new EmptyBorder(10, 16, 10, 14));
        rootPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 1. Avatar tròn bên trái
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Vòng tròn avatar
                Color avBg = isPrivate ? new Color(254, 237, 228) : new Color(236, 246, 241);
                g2.setColor(avBg);
                g2.fillOval(0, 0, 44, 44);

                // Viền avatar
                g2.setColor(isPrivate ? new Color(248, 180, 150) : new Color(170, 215, 195));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(0, 0, 43, 43);

                // Ký tự đầu tiên của tên
                String initial = (senderName != null && !senderName.isEmpty())
                        ? senderName.substring(0, 1).toUpperCase() : "M";
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g2.setColor(isPrivate ? ACCENT_TERRA : ACCENT_FOREST);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (44 - fm.stringWidth(initial)) / 2;
                int ty = (44 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initial, tx, ty);

                // Chấm xanh thông báo góc avatar
                g2.setColor(new Color(34, 197, 94));
                g2.fillOval(30, 30, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(30, 30, 12, 12);

                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(46, 46));
        rootPanel.add(avatarPanel, BorderLayout.WEST);

        // 2. Nội dung thông báo
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Dòng tiêu đề: Tên người gửi + Tag loại tin nhắn
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSender = new JLabel(senderName != null ? senderName : "Đồng nghiệp");
        lblSender.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSender.setForeground(TEXT_PRIMARY);

        JLabel lblTag = new JLabel(isPrivate ? "• Tin nhắn riêng" : "• Kênh nội bộ");
        lblTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTag.setForeground(isPrivate ? ACCENT_TERRA : ACCENT_FOREST);

        titleRow.add(lblSender);
        titleRow.add(lblTag);

        // Dòng nội dung tin nhắn rút gọn
        String cleanSnippet = messageSnippet != null ? messageSnippet.replaceAll("\\s+", " ").trim() : "";
        if (cleanSnippet.length() > 65) {
            cleanSnippet = cleanSnippet.substring(0, 62) + "...";
        }
        JLabel lblMsg = new JLabel("<html><body style='width: 220px; color: #57534E; font-size: 11px;'>" + escapeHtml(cleanSnippet) + "</body></html>");
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMsg.setForeground(TEXT_PRIMARY);
        lblMsg.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Dòng thời gian nhỏ
        JLabel lblTime = new JLabel("Vừa xong • Bấm để xem chi tiết");
        lblTime.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblTime.setForeground(TEXT_SECONDARY);
        lblTime.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerPanel.add(titleRow);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(lblMsg);
        centerPanel.add(Box.createVerticalStrut(3));
        centerPanel.add(lblTime);
        rootPanel.add(centerPanel, BorderLayout.CENTER);

        // 3. Nút đóng góc trên phải
        JButton btnClose = new JButton("×");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnClose.setForeground(TEXT_SECONDARY);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setPreferredSize(new Dimension(24, 24));
        btnClose.addActionListener(e -> closeToast());
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnClose.setForeground(new Color(220, 38, 38)); }
            @Override
            public void mouseExited(MouseEvent e) { btnClose.setForeground(TEXT_SECONDARY); }
        });

        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.setOpaque(false);
        eastPanel.add(btnClose, BorderLayout.NORTH);
        rootPanel.add(eastPanel, BorderLayout.EAST);

        // Sự kiện click vào thẻ để mở chat
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeToast();
                if (onClickAction != null) {
                    SwingUtilities.invokeLater(onClickAction);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Tạm dừng timer khi rê chuột vào để người dùng kịp đọc
                dismissTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Tiếp tục đếm ngược khi chuột rời đi
                dismissTimer.restart();
            }
        };

        rootPanel.addMouseListener(clickHandler);
        centerPanel.addMouseListener(clickHandler);
        avatarPanel.addMouseListener(clickHandler);

        setContentPane(rootPanel);

        // Bộ đếm thời gian tự tắt sau 4.5s
        dismissTimer = new Timer(AUTO_CLOSE_MS, e -> closeToast());
        dismissTimer.setRepeats(false);
    }

    /**
     * Hiển thị thông báo nổi ở góc dưới bên phải màn hình
     */
    public static void showNotification(String title, String senderName, String messageSnippet, boolean isPrivate, Runnable onClickAction) {
        SwingUtilities.invokeLater(() -> {
            // Phát âm thanh Messenger Chime
            SoundUtil.playMessengerChime();

            NotificationToast toast = new NotificationToast(title, senderName, messageSnippet, isPrivate, onClickAction);
            recalculatePositions(toast);
            toast.setVisible(true);
            toast.dismissTimer.start();
        });
    }

    private static synchronized void recalculatePositions(NotificationToast newToast) {
        activeToasts.add(newToast);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle screenBounds = defaultScreen.getDefaultConfiguration().getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(defaultScreen.getDefaultConfiguration());

        int usableRight = screenBounds.x + screenBounds.width - screenInsets.right - PADDING_SCREEN;
        int usableBottom = screenBounds.y + screenBounds.height - screenInsets.bottom - PADDING_SCREEN;

        int index = 0;
        for (NotificationToast t : activeToasts) {
            int x = usableRight - TOAST_WIDTH;
            int y = usableBottom - (index + 1) * TOAST_HEIGHT - index * SPACING_STACK;
            t.setLocation(x, y);
            index++;
            if (index >= 4) break; // Giới hạn tối đa 4 toast cùng lúc
        }
    }

    private synchronized void closeToast() {
        dismissTimer.stop();
        activeToasts.remove(this);
        setVisible(false);
        dispose();

        // Xếp lại các toast còn lại
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle screenBounds = defaultScreen.getDefaultConfiguration().getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(defaultScreen.getDefaultConfiguration());

        int usableRight = screenBounds.x + screenBounds.width - screenInsets.right - PADDING_SCREEN;
        int usableBottom = screenBounds.y + screenBounds.height - screenInsets.bottom - PADDING_SCREEN;

        int index = 0;
        for (NotificationToast t : activeToasts) {
            int x = usableRight - TOAST_WIDTH;
            int y = usableBottom - (index + 1) * TOAST_HEIGHT - index * SPACING_STACK;
            t.setLocation(x, y);
            index++;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
