package com.meeting.server.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.meeting.common.model.User;
import com.meeting.server.core.ClientHandler;
import com.meeting.server.core.ServerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ServerMonitorFrame extends JFrame {
    private final ServerManager serverManager;
    private JTextField txtPort;
    private JButton btnStartStop;
    private JLabel lblStatus;
    private JTextArea txtLogs;
    private JTable tblClients;
    private DefaultTableModel clientTableModel;
    private JLabel lblClientCount;

    // Bảng màu Warm Minimalist (Kem Sữa, Giấy Mộc & Terracotta / Xanh Rêu)
    private static final Color BG_WARM = new Color(245, 243, 239);       // Nền be kem sữa (#F5F3EF)
    private static final Color CARD_BG = new Color(255, 255, 255);       // Giấy mộc trắng (#FFFFFF)
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);    // Xanh rêu trầm (#1C3F34)
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);    // Cam đất Terracotta (#C25E34)
    private static final Color DANGER = new Color(220, 38, 38);          // Đỏ dừng
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);     // Nâu đen Espresso (#2D2A26)
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);// Warm gray (#78716C)
    private static final Color INPUT_BG = new Color(250, 249, 246);      // Kem nhạt
    private static final Color BORDER_WARM = new Color(229, 224, 216);   // Viền cát ấm (#E5E0D8)
    private static final Color TABLE_ROW_ALT = new Color(250, 248, 245); // Dòng xen kẽ
    private static final Color LOG_BG = new Color(30, 28, 26);           // Nền log Espresso sẫm
    private static final Color LOG_TEXT = new Color(220, 245, 230);      // Chữ log xanh nhạt dễ đọc
    private static final Color GRADIENT_START = new Color(28, 63, 52);   // Xanh rêu Forest
    private static final Color GRADIENT_END = new Color(48, 95, 78);     // Rêu sáng

    public ServerMonitorFrame() {
        this.serverManager = new ServerManager();
        initUI();
        setupListeners();
        // Tự động khởi động luôn server khi bật
        toggleServer();
    }

    private void initUI() {
        setTitle("Meeting Room — TCP Server Monitor");
        setSize(960, 640);
        setMinimumSize(new Dimension(860, 540));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_WARM);

        // ===== TOP HEADER BAR (Xanh Rêu Forest Gradient) =====
        JPanel headerBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Họa tiết trang trí mờ
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setColor(Color.WHITE);
                g2.fillOval(getWidth() - 90, -25, 110, 110);
                g2.fillOval(-20, -20, 80, 80);
                g2.dispose();
            }
        };
        headerBar.setLayout(new BorderLayout());
        headerBar.setPreferredSize(new Dimension(0, 62));
        headerBar.setBorder(new EmptyBorder(0, 22, 0, 22));

        JLabel lblTitle = new JLabel("🌿  TCP SERVER MONITOR");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Hệ thống quản lý phòng họp qua mạng TCP Socket đa luồng");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(245, 243, 239, 190));

        JPanel titleGroup = new JPanel();
        titleGroup.setOpaque(false);
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setBorder(new EmptyBorder(10, 0, 10, 0));
        titleGroup.add(lblTitle);
        titleGroup.add(lblSubtitle);

        headerBar.add(titleGroup, BorderLayout.WEST);
        root.add(headerBar, BorderLayout.NORTH);

        // ===== CONTROL BAR GIẤY MỘC =====
        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controlBar.setBackground(CARD_BG);
        controlBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_WARM),
                new EmptyBorder(10, 18, 10, 18)
        ));

        JLabel lblPort = new JLabel("Cổng (Port):");
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPort.setForeground(TEXT_PRIMARY);
        controlBar.add(lblPort);

        txtPort = new JTextField("8888", 5);
        txtPort.setFont(new Font("Consolas", Font.BOLD, 14));
        txtPort.setBackground(INPUT_BG);
        txtPort.setForeground(ACCENT_FOREST);
        txtPort.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));
        controlBar.add(txtPort);

        btnStartStop = new JButton("▶  Khởi động Server") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (serverManager.isRunning()) {
                    g2.setColor(new Color(220, 38, 38));
                } else {
                    g2.setColor(ACCENT_FOREST);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnStartStop.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStartStop.setForeground(Color.WHITE);
        btnStartStop.setContentAreaFilled(false);
        btnStartStop.setFocusPainted(false);
        btnStartStop.setBorderPainted(false);
        btnStartStop.setOpaque(false);
        btnStartStop.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStartStop.setPreferredSize(new Dimension(175, 34));
        controlBar.add(btnStartStop);

        controlBar.add(Box.createHorizontalStrut(18));

        lblStatus = new JLabel("● ĐANG DỪNG");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(DANGER);
        controlBar.add(lblStatus);

        // ===== CONTENT AREA =====
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(BG_WARM);
        contentWrapper.add(controlBar, BorderLayout.NORTH);

        // Split: Left = Terminal log, Right = Clients table
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);
        splitPane.setBackground(BG_WARM);
        splitPane.setBorder(new EmptyBorder(14, 16, 14, 16));

        // LEFT: Server logs
        JPanel logPanel = new JPanel(new BorderLayout(0, 8));
        logPanel.setBackground(BG_WARM);

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        JLabel lblLogTitle = new JLabel("📡  Nhật ký hoạt động (Server Logs)");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLogTitle.setForeground(ACCENT_FOREST);
        logHeader.add(lblLogTitle, BorderLayout.WEST);

        JButton btnClearLog = new JButton("Xóa log");
        btnClearLog.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearLog.setBackground(CARD_BG);
        btnClearLog.setForeground(TEXT_SECONDARY);
        btnClearLog.setFocusPainted(false);
        btnClearLog.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnClearLog.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearLog.addActionListener(e -> txtLogs.setText(""));
        btnClearLog.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnClearLog.setBackground(new Color(240, 237, 230)); }
            @Override
            public void mouseExited(MouseEvent e) { btnClearLog.setBackground(CARD_BG); }
        });
        logHeader.add(btnClearLog, BorderLayout.EAST);
        logPanel.add(logHeader, BorderLayout.NORTH);

        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLogs.setBackground(LOG_BG);
        txtLogs.setForeground(LOG_TEXT);
        txtLogs.setCaretColor(LOG_TEXT);
        txtLogs.setLineWrap(true);
        txtLogs.setWrapStyleWord(true);
        txtLogs.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scrollLogs = new JScrollPane(txtLogs);
        scrollLogs.setBorder(BorderFactory.createLineBorder(BORDER_WARM, 1));
        logPanel.add(scrollLogs, BorderLayout.CENTER);

        // RIGHT: Connected clients
        JPanel clientPanel = new JPanel(new BorderLayout(0, 8));
        clientPanel.setBackground(BG_WARM);

        JPanel clientHeader = new JPanel(new BorderLayout());
        clientHeader.setOpaque(false);
        JLabel lblClientTitle = new JLabel("👥  Clients Đang Kết Nối");
        lblClientTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblClientTitle.setForeground(ACCENT_FOREST);
        clientHeader.add(lblClientTitle, BorderLayout.WEST);

        lblClientCount = new JLabel("0 trực tuyến");
        lblClientCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblClientCount.setForeground(ACCENT_TERRA);
        clientHeader.add(lblClientCount, BorderLayout.EAST);

        clientPanel.add(clientHeader, BorderLayout.NORTH);

        String[] columns = {"#", "Socket Address", "Tài khoản", "Họ tên", "Phòng ban", "Quyền"};
        clientTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblClients = new JTable(clientTableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? CARD_BG : TABLE_ROW_ALT);
                } else {
                    c.setBackground(new Color(254, 243, 235));
                }
                c.setForeground(TEXT_PRIMARY);
                return c;
            }
        };
        tblClients.setRowHeight(30);
        tblClients.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblClients.setBackground(CARD_BG);
        tblClients.setForeground(TEXT_PRIMARY);
        tblClients.setGridColor(BORDER_WARM);
        tblClients.setSelectionBackground(new Color(254, 243, 235));
        tblClients.setSelectionForeground(ACCENT_TERRA);
        tblClients.setShowHorizontalLines(true);
        tblClients.setShowVerticalLines(false);
        tblClients.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = tblClients.getTableHeader();
        header.setBackground(new Color(245, 241, 234));
        header.setForeground(ACCENT_FOREST);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_TERRA));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(245, 241, 234));
        headerRenderer.setForeground(ACCENT_FOREST);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        headerRenderer.setBorder(new EmptyBorder(6, 8, 6, 8));
        for (int i = 0; i < tblClients.getColumnCount(); i++) {
            tblClients.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        JScrollPane scrollClients = new JScrollPane(tblClients);
        scrollClients.setBorder(BorderFactory.createLineBorder(BORDER_WARM, 1));
        scrollClients.getViewport().setBackground(CARD_BG);
        clientPanel.add(scrollClients, BorderLayout.CENTER);

        splitPane.setLeftComponent(logPanel);
        splitPane.setRightComponent(clientPanel);

        contentWrapper.add(splitPane, BorderLayout.CENTER);
        root.add(contentWrapper, BorderLayout.CENTER);

        // ===== BOTTOM STATUS =====
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(CARD_BG);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_WARM),
                new EmptyBorder(6, 18, 6, 18)
        ));

        JLabel lblTech = new JLabel("⚡ TCP Socket + Multi-Thread + Synchronized");
        lblTech.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTech.setForeground(TEXT_SECONDARY);

        JLabel lblCredit = new JLabel("Đồ án Lập trình mạng — Khoa CNTT");
        lblCredit.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblCredit.setForeground(TEXT_SECONDARY);

        bottomBar.add(lblTech, BorderLayout.WEST);
        bottomBar.add(lblCredit, BorderLayout.EAST);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);
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
                lblStatus.setForeground(new Color(22, 101, 52));
                btnStartStop.setText("■  Dừng Server");
                btnStartStop.setForeground(Color.WHITE);
                txtPort.setEnabled(false);
                btnStartStop.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Cổng phải là số nguyên hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở cổng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            serverManager.stopServer();
            lblStatus.setText("● ĐANG DỪNG");
            lblStatus.setForeground(DANGER);
            btnStartStop.setText("▶  Khởi động Server");
            btnStartStop.setForeground(Color.WHITE);
            txtPort.setEnabled(true);
            btnStartStop.repaint();
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
                    u != null ? u.getFullName() : "—",
                    u != null ? u.getDepartment() : "—",
                    u != null ? u.getRole() : "—"
            });
        }
        lblClientCount.setText(clients.size() + " trực tuyến");
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
