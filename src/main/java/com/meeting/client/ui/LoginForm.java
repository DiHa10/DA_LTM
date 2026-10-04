package com.meeting.client.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.meeting.client.net.SocketClient;
import com.meeting.common.model.User;
import com.meeting.common.protocol.ActionType;
import com.meeting.common.protocol.JsonUtil;
import com.meeting.common.protocol.Request;
import com.meeting.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JComboBox<String> cboQuickAccounts;
    private JButton btnLogin;
    private final SocketClient client = new SocketClient();

    // Bảng màu Dark Neon
    private static final Color BG_DARK = new Color(18, 18, 24);
    private static final Color CARD_BG = new Color(28, 28, 38);
    private static final Color ACCENT = new Color(99, 102, 241);    // Indigo neon
    private static final Color ACCENT_GLOW = new Color(129, 140, 248);
    private static final Color ACCENT2 = new Color(16, 185, 129);   // Emerald
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color INPUT_BG = new Color(38, 38, 52);
    private static final Color INPUT_BORDER = new Color(55, 55, 75);
    private static final Color GRADIENT_START = new Color(79, 70, 229);
    private static final Color GRADIENT_END = new Color(16, 185, 129);

    public LoginForm() {
        initUI();
    }

    private void initUI() {
        setTitle("Meeting Room Booking System");
        setSize(900, 560);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(BG_DARK);

        // ===== BÊN TRÁI: Branding Panel Gradient =====
        JPanel brandPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), getHeight(), GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Vẽ các vòng tròn trang trí mờ
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setColor(Color.WHITE);
                g2.fillOval(-40, -40, 200, 200);
                g2.fillOval(getWidth() - 120, getHeight() - 160, 250, 250);
                g2.fillOval(80, getHeight() - 80, 140, 140);
                g2.dispose();
            }
        };
        brandPanel.setLayout(new GridBagLayout());

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));

        JLabel lblIcon = new JLabel("🏢");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblBrand = new JLabel("MEETING ROOM");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblBrand.setForeground(Color.WHITE);
        lblBrand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblBrand2 = new JLabel("BOOKING SYSTEM");
        lblBrand2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblBrand2.setForeground(new Color(255, 255, 255, 200));
        lblBrand2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDesc = new JLabel("<html><div style='text-align:center;width:260px;'>Hệ thống đặt phòng họp thông minh<br>sử dụng giao thức TCP Socket</div></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDesc.setForeground(new Color(255, 255, 255, 180));
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTech = new JLabel("⚡ TCP + Thread + Synchronized");
        lblTech.setFont(new Font("Consolas", Font.BOLD, 12));
        lblTech.setForeground(new Color(255, 255, 255, 150));
        lblTech.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandContent.add(lblIcon);
        brandContent.add(Box.createVerticalStrut(12));
        brandContent.add(lblBrand);
        brandContent.add(lblBrand2);
        brandContent.add(Box.createVerticalStrut(16));
        brandContent.add(lblDesc);
        brandContent.add(Box.createVerticalStrut(24));
        brandContent.add(lblTech);
        brandPanel.add(brandContent);

        // ===== BÊN PHẢI: Form đăng nhập Dark =====
        JPanel formPanel = new JPanel();
        formPanel.setBackground(BG_DARK);
        formPanel.setLayout(new GridBagLayout());

        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(30, 32, 30, 32)
        ));
        card.setPreferredSize(new Dimension(370, 460));

        // Tiêu đề form
        JLabel lblLogin = new JLabel("Đăng nhập");
        lblLogin.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogin.setForeground(TEXT_PRIMARY);
        lblLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLoginSub = new JLabel("Nhập thông tin kết nối và tài khoản của bạn");
        lblLoginSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLoginSub.setForeground(TEXT_SECONDARY);
        lblLoginSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblLogin);
        card.add(Box.createVerticalStrut(4));
        card.add(lblLoginSub);
        card.add(Box.createVerticalStrut(18));

        // Host & Port trong 1 dòng
        JPanel netRow = new JPanel(new GridLayout(1, 2, 10, 0));
        netRow.setOpaque(false);
        netRow.setMaximumSize(new Dimension(370, 60));
        netRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtHost = createStyledField("127.0.0.1");
        txtPort = createStyledField("8888");
        netRow.add(createFieldGroup("Máy chủ", txtHost));
        netRow.add(createFieldGroup("Cổng", txtPort));
        card.add(netRow);
        card.add(Box.createVerticalStrut(10));

        // Chọn nhanh tài khoản
        String[] quickList = {
                "— Tự nhập tài khoản —",
                "admin (Quản trị viên)",
                "nhanvien1 (An - IT)",
                "nhanvien2 (Vũ - MKT)",
                "nhanvien3 (Kha - HR)"
        };
        cboQuickAccounts = new JComboBox<>(quickList);
        cboQuickAccounts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboQuickAccounts.setBackground(INPUT_BG);
        cboQuickAccounts.setForeground(TEXT_PRIMARY);
        cboQuickAccounts.setMaximumSize(new Dimension(370, 32));
        cboQuickAccounts.setAlignmentX(Component.LEFT_ALIGNMENT);
        cboQuickAccounts.addActionListener(e -> onSelectQuickAccount());

        JPanel quickGroup = createFieldGroup("Chọn nhanh", cboQuickAccounts);
        card.add(quickGroup);
        card.add(Box.createVerticalStrut(10));

        // Tài khoản
        txtUsername = createStyledField("nhanvien1");
        card.add(createFieldGroup("Tên đăng nhập", txtUsername));
        card.add(Box.createVerticalStrut(10));

        // Mật khẩu
        txtPassword = new JPasswordField("123456");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBackground(INPUT_BG);
        txtPassword.setForeground(TEXT_PRIMARY);
        txtPassword.setCaretColor(ACCENT_GLOW);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        card.add(createFieldGroup("Mật khẩu", txtPassword));
        card.add(Box.createVerticalStrut(18));

        // Nút đăng nhập gradient
        btnLogin = new JButton("ĐĂNG NHẬP") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, ACCENT);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(370, 42));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> doLogin());
        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnLogin.setBorder(BorderFactory.createLineBorder(ACCENT_GLOW, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnLogin.setBorder(null);
            }
        });

        card.add(btnLogin);

        // Enter key to login
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> doLogin());

        formPanel.add(card);

        root.add(brandPanel);
        root.add(formPanel);
        setContentPane(root);
    }

    private JTextField createStyledField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_GLOW);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    private JPanel createFieldGroup(String label, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        group.setMaximumSize(new Dimension(370, 55));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SECONDARY);
        group.add(lbl, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void onSelectQuickAccount() {
        int idx = cboQuickAccounts.getSelectedIndex();
        switch (idx) {
            case 1 -> { txtUsername.setText("admin"); txtPassword.setText("admin123"); }
            case 2 -> { txtUsername.setText("nhanvien1"); txtPassword.setText("123456"); }
            case 3 -> { txtUsername.setText("nhanvien2"); txtPassword.setText("123456"); }
            case 4 -> { txtUsername.setText("nhanvien3"); txtPassword.setText("123456"); }
        }
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
            FlatDarkLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
