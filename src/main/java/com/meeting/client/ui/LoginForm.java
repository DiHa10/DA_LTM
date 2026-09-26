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
import java.awt.*;
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

    public LoginForm() {
        initUI();
    }

    private void initUI() {
        setTitle("Đăng nhập - Đặt phòng họp nội bộ");
        setSize(480, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(25, 35, 25, 35));
        panel.setBackground(Color.WHITE);

        // Header Title
        JLabel lblHeader = new JLabel("HỆ THỐNG ĐẶT PHÒNG HỌP");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblHeader.setForeground(new Color(24, 90, 188));
        lblHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Phần mềm quản lý phòng họp qua mạng TCP");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(lblHeader);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblSub);
        panel.add(Box.createVerticalStrut(20));

        // Network connection inputs
        JPanel netPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        netPanel.setOpaque(false);
        netPanel.setMaximumSize(new Dimension(420, 55));

        JPanel hostBox = new JPanel(new BorderLayout(0, 4));
        hostBox.setOpaque(false);
        hostBox.add(new JLabel("Máy chủ (Host):"), BorderLayout.NORTH);
        txtHost = new JTextField("127.0.0.1");
        hostBox.add(txtHost, BorderLayout.CENTER);

        JPanel portBox = new JPanel(new BorderLayout(0, 4));
        portBox.setOpaque(false);
        portBox.add(new JLabel("Cổng (Port):"), BorderLayout.NORTH);
        txtPort = new JTextField("8888");
        portBox.add(txtPort, BorderLayout.CENTER);

        netPanel.add(hostBox);
        netPanel.add(portBox);
        panel.add(netPanel);
        panel.add(Box.createVerticalStrut(15));

        // Quick Account Selector
        JPanel quickBox = new JPanel(new BorderLayout(0, 4));
        quickBox.setOpaque(false);
        quickBox.setMaximumSize(new Dimension(420, 55));
        quickBox.add(new JLabel("Chọn nhanh tài khoản thử nghiệm:"), BorderLayout.NORTH);

        String[] quickList = {
                "-- Tự nhập tài khoản --",
                "admin (Quản trị viên / Giám đốc)",
                "nhanvien1 (An - Ban IT)",
                "nhanvien2 (Vũ - Ban MKT)",
                "nhanvien3 (Kha - Ban HR)"
        };
        cboQuickAccounts = new JComboBox<>(quickList);
        cboQuickAccounts.addActionListener(e -> onSelectQuickAccount());
        quickBox.add(cboQuickAccounts, BorderLayout.CENTER);
        panel.add(quickBox);
        panel.add(Box.createVerticalStrut(15));

        // Username
        JPanel userBox = new JPanel(new BorderLayout(0, 4));
        userBox.setOpaque(false);
        userBox.setMaximumSize(new Dimension(420, 55));
        userBox.add(new JLabel("Tên đăng nhập:"), BorderLayout.NORTH);
        txtUsername = new JTextField("nhanvien1");
        userBox.add(txtUsername, BorderLayout.CENTER);
        panel.add(userBox);
        panel.add(Box.createVerticalStrut(12));

        // Password
        JPanel passBox = new JPanel(new BorderLayout(0, 4));
        passBox.setOpaque(false);
        passBox.setMaximumSize(new Dimension(420, 55));
        passBox.add(new JLabel("Mật khẩu:"), BorderLayout.NORTH);
        txtPassword = new JPasswordField("123456");
        passBox.add(txtPassword, BorderLayout.CENTER);
        panel.add(passBox);
        panel.add(Box.createVerticalStrut(20));

        // Login Button
        btnLogin = new JButton("Đăng Nhập");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(24, 90, 188));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(420, 42));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.addActionListener(e -> doLogin());
        panel.add(btnLogin);

        // Enter key to login
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> doLogin());

        setContentPane(panel);
    }

    private void onSelectQuickAccount() {
        int idx = cboQuickAccounts.getSelectedIndex();
        switch (idx) {
            case 1 -> {
                txtUsername.setText("admin");
                txtPassword.setText("admin123");
            }
            case 2 -> {
                txtUsername.setText("nhanvien1");
                txtPassword.setText("123456");
            }
            case 3 -> {
                txtUsername.setText("nhanvien2");
                txtPassword.setText("123456");
            }
            case 4 -> {
                txtUsername.setText("nhanvien3");
                txtPassword.setText("123456");
            }
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

        // Chạy qua worker thread để không bị đơ UI
        new Thread(() -> {
            boolean connected = client.isConnected() || client.connect(host, port);
            if (!connected) {
                SwingUtilities.invokeLater(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng Nhập");
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
                btnLogin.setText("Đăng Nhập");

                if (res != null && res.isSuccess()) {
                    User user = JsonUtil.fromJson(res.getData(), User.class);
                    // Mở màn hình chính
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
