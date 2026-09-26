package com.meeting.server.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.meeting.common.model.User;
import com.meeting.server.core.ClientHandler;
import com.meeting.server.core.ServerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ServerMonitorFrame extends JFrame {
    private final ServerManager serverManager;
    private JTextField txtPort;
    private JButton btnStartStop;
    private JLabel lblStatus;
    private JTextArea txtLogs;
    private JTable tblClients;
    private DefaultTableModel clientTableModel;
    private JLabel lblClientCount;

    public ServerMonitorFrame() {
        this.serverManager = new ServerManager();
        initUI();
        setupListeners();
        // Tự động khởi động luôn server khi bật
        toggleServer();
    }

    private void initUI() {
        setTitle("Meeting Room Booking - TCP Server Monitor");
        setSize(900, 600);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel leftControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftControl.setOpaque(false);

        JLabel lblTitle = new JLabel("MÁY CHỦ TCP ĐẶT PHÒNG HỌP");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(33, 37, 41));

        JLabel lblPort = new JLabel("Cổng (Port):");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtPort = new JTextField("8888", 5);
        txtPort.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnStartStop = new JButton("Khởi động Server");
        btnStartStop.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStartStop.setBackground(new Color(40, 167, 69));
        btnStartStop.setForeground(Color.WHITE);
        btnStartStop.setFocusPainted(false);
        btnStartStop.setCursor(new Cursor(Cursor.HAND_CURSOR));

        leftControl.add(lblTitle);
        leftControl.add(Box.createHorizontalStrut(15));
        leftControl.add(lblPort);
        leftControl.add(txtPort);
        leftControl.add(btnStartStop);

        JPanel rightControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControl.setOpaque(false);
        lblStatus = new JLabel("● ĐANG DỪNG");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(new Color(220, 53, 69));
        rightControl.add(lblStatus);

        topPanel.add(leftControl, BorderLayout.WEST);
        topPanel.add(rightControl, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center Tabs: Logs & Clients
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Tab 1: Live Log
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLogs.setBackground(new Color(30, 30, 30));
        txtLogs.setForeground(new Color(220, 220, 220));
        txtLogs.setLineWrap(true);
        txtLogs.setWrapStyleWord(true);
        JScrollPane scrollLogs = new JScrollPane(txtLogs);

        JPanel logToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClearLog = new JButton("Xóa nhật ký");
        btnClearLog.addActionListener(e -> txtLogs.setText(""));
        logToolBar.add(btnClearLog);

        logPanel.add(scrollLogs, BorderLayout.CENTER);
        logPanel.add(logToolBar, BorderLayout.SOUTH);
        tabbedPane.addTab("  Nhật ký hoạt động (Server Logs)  ", logPanel);

        // Tab 2: Connected Clients
        JPanel clientPanel = new JPanel(new BorderLayout(5, 5));
        clientPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columns = {"STT", "Địa chỉ Socket Client", "Tài khoản", "Họ tên", "Phòng ban", "Quyền hạn"};
        clientTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblClients = new JTable(clientTableModel);
        tblClients.setRowHeight(28);
        tblClients.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblClients.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        JScrollPane scrollClients = new JScrollPane(tblClients);

        clientPanel.add(scrollClients, BorderLayout.CENTER);
        tabbedPane.addTab("  Danh sách Client kết nối  ", clientPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(new EmptyBorder(5, 5, 0, 5));
        lblClientCount = new JLabel("Số Client đang trực tuyến: 0");
        lblClientCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bottomBar.add(lblClientCount, BorderLayout.WEST);

        JLabel lblCredit = new JLabel("Đồ án Lập trình mạng - TCP Socket + Thread + Synchronized");
        lblCredit.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblCredit.setForeground(Color.GRAY);
        bottomBar.add(lblCredit, BorderLayout.EAST);

        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void setupListeners() {
        btnStartStop.addActionListener(e -> toggleServer());

        // Gắn callback cập nhật log
        serverManager.setLogListener(msg -> SwingUtilities.invokeLater(() -> {
            txtLogs.append(msg + "\n");
            txtLogs.setCaretPosition(txtLogs.getDocument().getLength());
        }));

        // Gắn callback cập nhật bảng Client
        serverManager.setClientListListener(() -> SwingUtilities.invokeLater(this::refreshClientTable));
    }

    private void toggleServer() {
        if (!serverManager.isRunning()) {
            try {
                int port = Integer.parseInt(txtPort.getText().trim());
                serverManager.startServer(port);
                lblStatus.setText("● ĐANG CHẠY");
                lblStatus.setForeground(new Color(40, 167, 69));
                btnStartStop.setText("Dừng Server");
                btnStartStop.setBackground(new Color(220, 53, 69));
                txtPort.setEnabled(false);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cổng phải là số nguyên hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở cổng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            serverManager.stopServer();
            lblStatus.setText("● ĐANG DỪNG");
            lblStatus.setForeground(new Color(220, 53, 69));
            btnStartStop.setText("Khởi động Server");
            btnStartStop.setBackground(new Color(40, 167, 69));
            txtPort.setEnabled(true);
        }
    }

    private void refreshClientTable() {
        clientTableModel.setRowCount(0);
        var clients = serverManager.getActiveClients();
        int stt = 1;
        for (ClientHandler client : clients) {
            User u = client.getCurrentUser();
            clientTableModel.addRow(new Object[]{
                    stt++,
                    client.getSocket().getRemoteSocketAddress().toString(),
                    u != null ? u.getUsername() : "(Chưa đăng nhập)",
                    u != null ? u.getFullName() : "-",
                    u != null ? u.getDepartment() : "-",
                    u != null ? u.getRole() : "-"
            });
        }
        lblClientCount.setText("Số Client đang trực tuyến: " + clients.size());
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ServerMonitorFrame frame = new ServerMonitorFrame();
            frame.setVisible(true);
        });
    }
}
