package com.meeting.client.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.meeting.client.net.SocketClient;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.meeting.client.ui.util.AppIcon;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

public class LoginForm extends JFrame {
    private JTextField txtHost;
    private JTextField txtPort;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private final SocketClient client = new SocketClient();

    // Bảng màu Warm Minimalist (Kem Sữa, Giấy Mộc & Terracotta / Xanh Rêu)
    private static final Color BG_WARM = new Color(245, 243, 239);       // Nền be kem sữa (#F5F3EF)
    private static final Color CARD_BG = new Color(255, 255, 255);       // Giấy mộc trắng sứ (#FFFFFF)
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);    // Cam đất Terracotta (#C25E34)
    private static final Color ACCENT_HOVER = new Color(168, 78, 40);    // Terracotta đậm
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);    // Xanh rêu trầm (#1C3F34)
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);     // Nâu đen Espresso (#2D2A26)
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);// Warm gray (#78716C)
    private static final Color INPUT_BG = new Color(250, 249, 246);      // Nền ô nhập liệu kem nhạt
    private static final Color BORDER_WARM = new Color(229, 224, 216);   // Viền cát ấm (#E5E0D8)
    private static final Color GRADIENT_START = new Color(194, 94, 52);  // Terracotta
    private static final Color GRADIENT_END = new Color(217, 119, 6);    // Amber ấm

    public LoginForm() {
        initUI();
    }

    private void initUI() {
        setTitle("Meeting Room Booking System — Đăng nhập");
        setSize(900, 560);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(BG_WARM);

        // ===== BÊN TRÁI: Branding Panel Tông Rêu Trầm & Giấy Mộc =====
        JPanel brandPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT_FOREST, getWidth(), getHeight(), new Color(45, 90, 70));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Các họa tiết hình học mờ trang nhã
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setColor(Color.WHITE);
                g2.fillOval(-30, -30, 200, 200);
                g2.fillOval(getWidth() - 130, getHeight() - 150, 260, 260);
                g2.fillRoundRect(60, getHeight() - 90, 150, 150, 40, 40);
                g2.dispose();
            }
        };
        brandPanel.setLayout(new GridBagLayout());

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));

        JLabel lblIcon = new JLabel(AppIcon.calendar(34, Color.WHITE)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblIcon.setPreferredSize(new Dimension(68, 68));
        lblIcon.setMaximumSize(new Dimension(68, 68));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblBrand = new JLabel("MEETING ROOM");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblBrand.setForeground(Color.WHITE);
        lblBrand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblBrand2 = new JLabel("BOOKING SYSTEM");
        lblBrand2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblBrand2.setForeground(new Color(245, 243, 239, 210));
        lblBrand2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDesc = new JLabel("<html><div style='text-align:center;width:250px;'>Hệ thống đặt lịch phòng họp thông minh<br>qua giao thức TCP Socket đa luồng</div></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDesc.setForeground(new Color(245, 243, 239, 190));
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblBadge = new JLabel("  ● TCP Socket • Thread • Synchronized  ");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setForeground(new Color(245, 243, 239));
        lblBadge.setOpaque(true);
        lblBadge.setBackground(new Color(255, 255, 255, 30));
        lblBadge.setBorder(new EmptyBorder(5, 12, 5, 12));
        lblBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandContent.add(lblIcon);
        brandContent.add(Box.createVerticalStrut(12));
        brandContent.add(lblBrand);
        brandContent.add(lblBrand2);
        brandContent.add(Box.createVerticalStrut(14));
        brandContent.add(lblDesc);
        brandContent.add(Box.createVerticalStrut(22));
        brandContent.add(lblBadge);
        brandPanel.add(brandContent);

        // ===== BÊN PHẢI: Card đăng nhập Kem Sữa & Terracotta =====
        JPanel formPanel = new JPanel();
        formPanel.setBackground(BG_WARM);
        formPanel.setLayout(new GridBagLayout());

        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(28, 32, 28, 32)
        ));
        card.setPreferredSize(new Dimension(380, 420));

        // Header Form
        JLabel lblLogin = new JLabel("Đăng nhập");
        lblLogin.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogin.setForeground(TEXT_PRIMARY);
        lblLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLoginSub = new JLabel("Nhập thông tin tài khoản để đăng nhập hệ thống");
        lblLoginSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLoginSub.setForeground(TEXT_SECONDARY);
        lblLoginSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblLogin);
        card.add(Box.createVerticalStrut(4));
        card.add(lblLoginSub);
        card.add(Box.createVerticalStrut(18));

        // Host & Port
        JPanel netRow = new JPanel(new GridLayout(1, 2, 10, 0));
        netRow.setOpaque(false);
        netRow.setMaximumSize(new Dimension(380, 56));
        netRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtHost = createStyledField("127.0.0.1");
        txtPort = createStyledField("8888");
        netRow.add(createFieldGroup("Máy chủ (Host)", txtHost));
        netRow.add(createFieldGroup("Cổng (Port)", txtPort));
        card.add(netRow);
        card.add(Box.createVerticalStrut(12));

        // Username
        txtUsername = createStyledField("");
        card.add(createFieldGroup("Tên đăng nhập", txtUsername));
        card.add(Box.createVerticalStrut(12));

        // Password
        txtPassword = new JPasswordField("");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPassword.setBackground(INPUT_BG);
        txtPassword.setForeground(TEXT_PRIMARY);
        txtPassword.setCaretColor(ACCENT_TERRA);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        card.add(createFieldGroup("Mật khẩu", txtPassword));
        card.add(Box.createVerticalStrut(20));

        // Nút đăng nhập màu Terracotta
        btnLogin = new JButton("ĐĂNG NHẬP") {
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
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(380, 40));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> doLogin());
        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnLogin.setBorder(BorderFactory.createLineBorder(ACCENT_HOVER, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnLogin.setBorder(null);
            }
        });
        card.add(btnLogin);

        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> doLogin());

        formPanel.add(card);
        root.add(brandPanel);
        root.add(formPanel);

        setContentPane(root);
    }

    private JTextField createStyledField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_TERRA);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    private JPanel createFieldGroup(String label, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 3));
        group.setOpaque(false);
        group.setMaximumSize(new Dimension(380, 52));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SECONDARY);
        group.add(lbl, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }


    private void doLogin() {
        String host = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng Port phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang kết nối...");

        new Thread(() -> {
            boolean connected = client.isConnected() || client.connect(host, port);
            if (!connected) {
                SwingUtilities.invokeLater(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");
                    JOptionPane.showMessageDialog(LoginForm.this,
                            "Không thể kết nối đến Máy chủ TCP tại " + host + ":" + port + "\n\nXin hãy đảm bảo ServerApp đã được bấm 'Khởi động Server' trước!",
                            "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            Map<String, String> creds = new HashMap<>();
            creds.put("username", username);
            creds.put("password", password);

            Request req = new Request(ActionType.LOGIN, JsonUtil.toJson(creds));
            Response res = client.sendRequest(req);

            SwingUtilities.invokeLater(() -> {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");

                if (res != null && res.isSuccess()) {
                    User user = JsonUtil.fromJson(res.getData(), User.class);
                    MainDashboard dashboard = new MainDashboard(client, user);
                    dashboard.setVisible(true);
                    LoginForm.this.dispose();
                } else {
                    String msg = res != null ? res.getMessage() : "Không nhận được phản hồi từ server";
                    JOptionPane.showMessageDialog(LoginForm.this, msg, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
