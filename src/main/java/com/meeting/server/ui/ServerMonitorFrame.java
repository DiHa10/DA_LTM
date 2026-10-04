package com.meeting.server.ui;

import com.formdev.flatlaf.FlatDarkLaf;
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

    // Bảng màu Dark Cyber
    private static final Color BG_DARK = new Color(12, 12, 18);
    private static final Color CARD_BG = new Color(22, 22, 32);
    private static final Color PANEL_BG = new Color(28, 28, 40);
    private static final Color ACCENT = new Color(56, 189, 248);     // Cyan neon
    private static final Color ACCENT_GLOW = new Color(103, 232, 249);
    private static final Color ACCENT_GREEN = new Color(16, 185, 129);
    private static final Color DANGER = new Color(239, 68, 68);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color INPUT_BG = new Color(38, 38, 52);
    private static final Color INPUT_BORDER = new Color(55, 55, 75);
    private static final Color TERMINAL_BG = new Color(10, 10, 16);
    private static final Color TERMINAL_TEXT = new Color(74, 222, 128);
    private static final Color TABLE_ROW_ALT = new Color(28, 28, 42);
    private static final Color GRADIENT_START = new Color(6, 182, 212);
    private static final Color GRADIENT_END = new Color(59, 130, 246);

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
        root.setBackground(BG_DARK);

        // ===== TOP HEADER BAR (Gradient) =====
        JPanel headerBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative circles
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
                g2.setColor(Color.WHITE);
                g2.fillOval(getWidth() - 100, -30, 120, 120);
                g2.fillOval(-30, -20, 80, 80);
                g2.dispose();
            }
        };
        headerBar.setLayout(new BorderLayout());
        headerBar.setPreferredSize(new Dimension(0, 60));
        headerBar.setBorder(new EmptyBorder(0, 20, 0, 20));

        JLabel lblTitle = new JLabel("🖥  TCP SERVER MONITOR");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Meeting Room Booking System");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(255, 255, 255, 180));

        JPanel titleGroup = new JPanel();
        titleGroup.setOpaque(false);
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setBorder(new EmptyBorder(10, 0, 10, 0));
        titleGroup.add(lblTitle);
        titleGroup.add(lblSubtitle);

        headerBar.add(titleGroup, BorderLayout.WEST);
        root.add(headerBar, BorderLayout.NORTH);

        // ===== CONTROL BAR =====
        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controlBar.setBackground(CARD_BG);
        controlBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, INPUT_BORDER),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPort.setForeground(TEXT_SECONDARY);
        controlBar.add(lblPort);

        txtPort = new JTextField("8888", 5);
        txtPort.setFont(new Font("Consolas", Font.BOLD, 14));
        txtPort.setBackground(INPUT_BG);
        txtPort.setForeground(ACCENT);
        txtPort.setCaretColor(ACCENT_GLOW);
        txtPort.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        controlBar.add(txtPort);

        btnStartStop = new JButton("▶  Khởi động Server") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (serverManager.isRunning()) {
                    g2.setColor(new Color(239, 68, 68, 60));
                } else {
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT_GREEN, getWidth(), 0, new Color(6, 150, 100));
                    g2.setPaint(gp);
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
        btnStartStop.setPreferredSize(new Dimension(180, 34));
        controlBar.add(btnStartStop);

        controlBar.add(Box.createHorizontalStrut(20));

        lblStatus = new JLabel("● ĐANG DỪNG");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setForeground(DANGER);
        controlBar.add(lblStatus);

        // ===== CENTER CONTENT =====
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBackground(BG_DARK);

        // Thêm control bar vào trên nội dung chính
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(BG_DARK);
        contentWrapper.add(controlBar, BorderLayout.NORTH);

        // Split: Left = Log terminal, Right = Clients table
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(5);
        splitPane.setBackground(BG_DARK);
        splitPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        // LEFT: Terminal-style log
        JPanel logPanel = new JPanel(new BorderLayout(0, 8));
        logPanel.setBackground(BG_DARK);

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setOpaque(false);
        JLabel lblLogTitle = new JLabel("📡  Server Logs");
        lblLogTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLogTitle.setForeground(ACCENT_GLOW);
        logHeader.add(lblLogTitle, BorderLayout.WEST);

        JButton btnClearLog = new JButton("Xóa log");
        btnClearLog.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClearLog.setBackground(INPUT_BG);
        btnClearLog.setForeground(TEXT_SECONDARY);
        btnClearLog.setFocusPainted(false);
        btnClearLog.setBorderPainted(false);
        btnClearLog.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearLog.addActionListener(e -> txtLogs.setText(""));
        btnClearLog.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnClearLog.setBackground(new Color(50, 50, 68)); }
            @Override
            public void mouseExited(MouseEvent e) { btnClearLog.setBackground(INPUT_BG); }
        });
        logHeader.add(btnClearLog, BorderLayout.EAST);

        logPanel.add(logHeader, BorderLayout.NORTH);

        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLogs.setBackground(TERMINAL_BG);
        txtLogs.setForeground(TERMINAL_TEXT);
        txtLogs.setCaretColor(TERMINAL_TEXT);
        txtLogs.setLineWrap(true);
        txtLogs.setWrapStyleWord(true);
        txtLogs.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scrollLogs = new JScrollPane(txtLogs);
        scrollLogs.setBorder(BorderFactory.createLineBorder(INPUT_BORDER, 1));
        scrollLogs.getViewport().setBackground(TERMINAL_BG);
        logPanel.add(scrollLogs, BorderLayout.CENTER);

        // RIGHT: Client table
        JPanel clientPanel = new JPanel(new BorderLayout(0, 8));
        clientPanel.setBackground(BG_DARK);

        JPanel clientHeader = new JPanel(new BorderLayout());
        clientHeader.setOpaque(false);
        JLabel lblClientTitle = new JLabel("👥  Clients Đang Kết Nối");
        lblClientTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblClientTitle.setForeground(ACCENT_GLOW);
        clientHeader.add(lblClientTitle, BorderLayout.WEST);

        lblClientCount = new JLabel("0 online");
        lblClientCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblClientCount.setForeground(ACCENT_GREEN);
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
                    c.setBackground(new Color(56, 189, 248, 40));
                }
                c.setForeground(TEXT_PRIMARY);
                return c;
            }
        };
        tblClients.setRowHeight(30);
        tblClients.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblClients.setBackground(CARD_BG);
        tblClients.setForeground(TEXT_PRIMARY);
        tblClients.setGridColor(INPUT_BORDER);
        tblClients.setSelectionBackground(new Color(56, 189, 248, 50));
        tblClients.setSelectionForeground(TEXT_PRIMARY);
        tblClients.setShowHorizontalLines(true);
        tblClients.setShowVerticalLines(false);
        tblClients.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = tblClients.getTableHeader();
        header.setBackground(new Color(30, 30, 46));
        header.setForeground(ACCENT_GLOW);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(30, 30, 46));
        headerRenderer.setForeground(ACCENT_GLOW);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        headerRenderer.setBorder(new EmptyBorder(6, 8, 6, 8));
        for (int i = 0; i < tblClients.getColumnCount(); i++) {
            tblClients.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        JScrollPane scrollClients = new JScrollPane(tblClients);
        scrollClients.setBorder(BorderFactory.createLineBorder(INPUT_BORDER, 1));
        scrollClients.getViewport().setBackground(CARD_BG);
        clientPanel.add(scrollClients, BorderLayout.CENTER);

        splitPane.setLeftComponent(logPanel);
        splitPane.setRightComponent(clientPanel);

        contentWrapper.add(splitPane, BorderLayout.CENTER);
        centerPanel.add(contentWrapper, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);

        // ===== BOTTOM STATUS =====
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(CARD_BG);
        bottomBar.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel lblTech = new JLabel("⚡ TCP Socket + Multi-Thread + Synchronized");
        lblTech.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblTech.setForeground(TEXT_SECONDARY);

        JLabel lblCredit = new JLabel("Đồ án Lập trình mạng");
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
                lblStatus.setForeground(ACCENT_GREEN);
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
        lblClientCount.setText(clients.size() + " online");
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ServerMonitorFrame frame = new ServerMonitorFrame();
            frame.setVisible(true);
        });
    }
}
