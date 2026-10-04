package com.meeting.client.ui;

import com.google.gson.reflect.TypeToken;
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
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainDashboard extends JFrame {
    private final SocketClient client;
    private final User currentUser;

    private List<Room> cachedRooms = new ArrayList<>();
    private String selectedDate;

    // Bảng màu Dark Neon
    private static final Color BG_DARK = new Color(18, 18, 24);
    private static final Color SIDEBAR_BG = new Color(22, 22, 32);
    private static final Color CARD_BG = new Color(28, 28, 38);
    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color ACCENT_GLOW = new Color(129, 140, 248);
    private static final Color ACCENT2 = new Color(16, 185, 129);
    private static final Color DANGER = new Color(239, 68, 68);
    private static final Color WARNING_COLOR = new Color(245, 158, 11);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color INPUT_BG = new Color(38, 38, 52);
    private static final Color INPUT_BORDER = new Color(55, 55, 75);
    private static final Color TABLE_ROW_ALT = new Color(32, 32, 44);
    private static final Color SIDEBAR_ACTIVE = new Color(99, 102, 241, 30);
    private static final Color GRADIENT_START = new Color(79, 70, 229);
    private static final Color GRADIENT_END = new Color(16, 185, 129);

    // UI Tab 1: Schedule
    private JTextField txtScheduleDate;
    private JTable tblBookings;
    private DefaultTableModel bookingTableModel;
    private JTable tblRooms;
    private DefaultTableModel roomTableModel;

    // UI Tab 2: My Bookings
    private JTable tblMyBookings;
    private DefaultTableModel myBookingTableModel;

    // UI Tab 3: Admin Rooms
    private JTable tblAdminRooms;
    private DefaultTableModel adminRoomTableModel;

    // Sidebar buttons
    private JPanel activeNavButton = null;
    private CardLayout cardLayout;
    private JPanel contentArea;

    public MainDashboard(SocketClient client, User currentUser) {
        this.client = client;
        this.currentUser = currentUser;
        this.selectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        initUI();
        setupBroadcastListener();
        loadAllData();
    }

    private void initUI() {
        setTitle("Meeting Room Booking — " + currentUser.getFullName());
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        // ===== SIDEBAR =====
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, INPUT_BORDER));

        // Sidebar Header — User Avatar & Info
        JPanel sidebarHeader = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), getHeight(), GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebarHeader.setLayout(new BoxLayout(sidebarHeader, BoxLayout.Y_AXIS));
        sidebarHeader.setBorder(new EmptyBorder(24, 20, 20, 20));
        sidebarHeader.setPreferredSize(new Dimension(240, 140));

        JLabel lblAvatar = new JLabel(getAvatarEmoji()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(0, 0, 48, 48);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        lblAvatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUserName = new JLabel(currentUser.getFullName());
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUserName.setForeground(Color.WHITE);
        lblUserName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDept = new JLabel(currentUser.getDepartment() + " • " + currentUser.getRole());
        lblDept.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDept.setForeground(new Color(255, 255, 255, 180));
        lblDept.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebarHeader.add(lblAvatar);
        sidebarHeader.add(Box.createVerticalStrut(10));
        sidebarHeader.add(lblUserName);
        sidebarHeader.add(Box.createVerticalStrut(3));
        sidebarHeader.add(lblDept);

        // Sidebar Menu
        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(SIDEBAR_BG);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(16, 0, 16, 0));

        JLabel lblMenuTitle = new JLabel("    MENU CHÍNH");
        lblMenuTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblMenuTitle.setForeground(TEXT_SECONDARY);
        lblMenuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuPanel.add(lblMenuTitle);
        menuPanel.add(Box.createVerticalStrut(8));

        JPanel navSchedule = createNavButton("📅", "Lịch Đặt Phòng", "schedule");
        JPanel navMyBookings = createNavButton("📋", "Lịch Họp Của Tôi", "mybookings");
        menuPanel.add(navSchedule);
        menuPanel.add(navMyBookings);

        if (currentUser.isAdmin()) {
            menuPanel.add(Box.createVerticalStrut(16));
            JLabel lblAdmin = new JLabel("    QUẢN TRỊ");
            lblAdmin.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblAdmin.setForeground(TEXT_SECONDARY);
            lblAdmin.setAlignmentX(Component.LEFT_ALIGNMENT);
            menuPanel.add(lblAdmin);
            menuPanel.add(Box.createVerticalStrut(8));
            JPanel navAdmin = createNavButton("⚙", "Quản Lý Phòng", "admin");
            menuPanel.add(navAdmin);
        }

        // Sidebar Footer — Logout
        JPanel sidebarFooter = new JPanel(new BorderLayout());
        sidebarFooter.setBackground(SIDEBAR_BG);
        sidebarFooter.setBorder(new EmptyBorder(10, 14, 14, 14));

        JButton btnLogout = new JButton("⏻  Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogout.setBackground(new Color(239, 68, 68, 40));
        btnLogout.setForeground(DANGER);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(0, 38));
        btnLogout.addActionListener(e -> doLogout());
        btnLogout.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnLogout.setBackground(new Color(239, 68, 68, 60)); }
            @Override
            public void mouseExited(MouseEvent e) { btnLogout.setBackground(new Color(239, 68, 68, 40)); }
        });
        sidebarFooter.add(btnLogout, BorderLayout.CENTER);

        sidebar.add(sidebarHeader, BorderLayout.NORTH);
        sidebar.add(menuPanel, BorderLayout.CENTER);
        sidebar.add(sidebarFooter, BorderLayout.SOUTH);

        // ===== CONTENT AREA (CardLayout) =====
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG_DARK);

        contentArea.add(createScheduleTab(), "schedule");
        contentArea.add(createMyBookingsTab(), "mybookings");
        if (currentUser.isAdmin()) {
            contentArea.add(createAdminRoomsTab(), "admin");
        }

        // Mặc định kích hoạt tab đầu
        setActiveNav(navSchedule);
        cardLayout.show(contentArea, "schedule");

        // ===== BOTTOM STATUS BAR =====
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(CARD_BG);
        bottomBar.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel lblStatus = new JLabel("● Đã kết nối TCP Server — Nhận thông báo Real-time");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(ACCENT2);

        JLabel lblTech = new JLabel("TCP Socket + Thread + Synchronized");
        lblTech.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblTech.setForeground(TEXT_SECONDARY);

        bottomBar.add(lblStatus, BorderLayout.WEST);
        bottomBar.add(lblTech, BorderLayout.EAST);

        root.add(sidebar, BorderLayout.WEST);
        root.add(contentArea, BorderLayout.CENTER);
        root.add(bottomBar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private String getAvatarEmoji() {
        if (currentUser.isAdmin()) return "👨‍💼";
        return "👤";
    }

    private JPanel createNavButton(String icon, String text, String cardName) {
        JPanel navBtn = new JPanel(new BorderLayout());
        navBtn.setMaximumSize(new Dimension(240, 44));
        navBtn.setBackground(SIDEBAR_BG);
        navBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        navBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(icon + "  " + text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(TEXT_SECONDARY);
        navBtn.add(lbl, BorderLayout.CENTER);

        navBtn.putClientProperty("cardName", cardName);
        navBtn.putClientProperty("label", lbl);

        navBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setActiveNav(navBtn);
                cardLayout.show(contentArea, cardName);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (navBtn != activeNavButton) {
                    navBtn.setBackground(new Color(40, 40, 55));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (navBtn != activeNavButton) {
                    navBtn.setBackground(SIDEBAR_BG);
                }
            }
        });
        return navBtn;
    }

    private void setActiveNav(JPanel navBtn) {
        if (activeNavButton != null) {
            activeNavButton.setBackground(SIDEBAR_BG);
            activeNavButton.setBorder(new EmptyBorder(10, 20, 10, 20));
            JLabel oldLabel = (JLabel) activeNavButton.getClientProperty("label");
            if (oldLabel != null) oldLabel.setForeground(TEXT_SECONDARY);
        }
        activeNavButton = navBtn;
        navBtn.setBackground(SIDEBAR_ACTIVE);
        navBtn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 3, 0, 0, ACCENT),
                new EmptyBorder(10, 17, 10, 20)
        ));
        JLabel newLabel = (JLabel) navBtn.getClientProperty("label");
        if (newLabel != null) newLabel.setForeground(TEXT_PRIMARY);
    }

    // ========== TAB 1: SCHEDULE ==========
    private JPanel createScheduleTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 16, 20));

        // Page Title
        JLabel lblPageTitle = new JLabel("Lịch Đặt Phòng Họp");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        // Filter Bar Card
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterCard.setBackground(CARD_BG);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblDateFilter = new JLabel("Ngày:");
        lblDateFilter.setForeground(TEXT_SECONDARY);
        lblDateFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterCard.add(lblDateFilter);

        txtScheduleDate = new JTextField(selectedDate, 10);
        txtScheduleDate.setBackground(INPUT_BG);
        txtScheduleDate.setForeground(TEXT_PRIMARY);
        txtScheduleDate.setCaretColor(ACCENT_GLOW);
        txtScheduleDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        filterCard.add(txtScheduleDate);

        filterCard.add(createFilterButton("Hôm nay", () -> {
            txtScheduleDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            loadBookingsByDate();
        }));
        filterCard.add(createFilterButton("Ngày mai", () -> {
            txtScheduleDate.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            loadBookingsByDate();
        }));
        filterCard.add(createFilterButton("Xem Lịch", this::loadBookingsByDate));

        // Nút đặt phòng nổi bật
        JButton btnBook = new JButton("＋ Đặt Phòng") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT2, getWidth(), 0, new Color(6, 150, 100));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnBook.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBook.setForeground(Color.WHITE);
        btnBook.setContentAreaFilled(false);
        btnBook.setFocusPainted(false);
        btnBook.setBorderPainted(false);
        btnBook.setOpaque(false);
        btnBook.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBook.addActionListener(e -> openBookingDialog());
        filterCard.add(Box.createHorizontalStrut(8));
        filterCard.add(btnBook);

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(lblPageTitle, BorderLayout.NORTH);
        topArea.add(filterCard, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        // Tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setBackground(BG_DARK);
        splitPane.setBorder(null);
        splitPane.setDividerSize(6);

        String[] bookCols = {"STT", "Phòng họp", "Khung giờ", "Người đặt", "Phòng ban", "Mục đích", "Trạng thái"};
        bookingTableModel = new DefaultTableModel(bookCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBookings = createStyledTable(bookingTableModel);
        JScrollPane scrollBookings = createStyledScrollPane(tblBookings, "Lịch họp đã chốt trong ngày");

        String[] roomCols = {"ID", "Tên phòng", "Sức chứa", "Vị trí", "Trang thiết bị", "Trạng thái"};
        roomTableModel = new DefaultTableModel(roomCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRooms = createStyledTable(roomTableModel);
        JScrollPane scrollRooms = createStyledScrollPane(tblRooms, "Danh mục phòng họp");

        splitPane.setTopComponent(scrollBookings);
        splitPane.setBottomComponent(scrollRooms);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // ========== TAB 2: MY BOOKINGS ==========
    private JPanel createMyBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 16, 20));

        JLabel lblPageTitle = new JLabel("Lịch Họp Của Tôi");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolBar.setBackground(CARD_BG);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        toolBar.add(createFilterButton("🔄 Làm mới", this::loadMyBookings));

        JButton btnCancel = new JButton("✕  Hủy lịch đã chọn");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setBackground(new Color(239, 68, 68, 50));
        btnCancel.setForeground(DANGER);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> doCancelSelectedBooking());
        toolBar.add(btnCancel);

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(lblPageTitle, BorderLayout.NORTH);
        topArea.add(toolBar, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        String[] myCols = {"Mã Lịch", "Phòng họp", "Ngày họp", "Khung giờ", "Mục đích", "Trạng thái", "Ngày tạo"};
        myBookingTableModel = new DefaultTableModel(myCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblMyBookings = createStyledTable(myBookingTableModel);
        panel.add(createStyledScrollPane(tblMyBookings, null), BorderLayout.CENTER);

        return panel;
    }

    // ========== TAB 3: ADMIN ROOMS ==========
    private JPanel createAdminRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 16, 20));

        JLabel lblPageTitle = new JLabel("Quản Lý Phòng Họp (Admin)");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolBar.setBackground(CARD_BG);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JButton btnAdd = new JButton("＋ Thêm Phòng");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setBackground(ACCENT);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> doAddRoom());
        toolBar.add(btnAdd);

        toolBar.add(createFilterButton("🔄 Đổi trạng thái", this::doToggleRoomStatus));

        JButton btnDel = new JButton("🗑 Xóa Phòng");
        btnDel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDel.setBackground(new Color(239, 68, 68, 50));
        btnDel.setForeground(DANGER);
        btnDel.setFocusPainted(false);
        btnDel.setBorderPainted(false);
        btnDel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDel.addActionListener(e -> doDeleteRoom());
        toolBar.add(btnDel);

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(lblPageTitle, BorderLayout.NORTH);
        topArea.add(toolBar, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        String[] cols = {"ID", "Tên phòng", "Sức chứa", "Vị trí", "Trang thiết bị", "Trạng thái"};
        adminRoomTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblAdminRooms = createStyledTable(adminRoomTableModel);
        panel.add(createStyledScrollPane(tblAdminRooms, null), BorderLayout.CENTER);

        return panel;
    }

    // ========== UI HELPERS ==========
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? CARD_BG : TABLE_ROW_ALT);
                } else {
                    c.setBackground(new Color(99, 102, 241, 40));
                }
                c.setForeground(TEXT_PRIMARY);
                return c;
            }
        };
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(INPUT_BORDER);
        table.setSelectionBackground(new Color(99, 102, 241, 50));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(35, 35, 50));
        header.setForeground(ACCENT_GLOW);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(35, 35, 50));
        headerRenderer.setForeground(ACCENT_GLOW);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerRenderer.setBorder(new EmptyBorder(6, 8, 6, 8));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        return table;
    }

    private JScrollPane createStyledScrollPane(JTable table, String title) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(CARD_BG);
        sp.getViewport().setBackground(CARD_BG);
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1),
                new EmptyBorder(0, 0, 0, 0)
        ));
        if (title != null) {
            sp.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(INPUT_BORDER, 1),
                            title,
                            0, 0,
                            new Font("Segoe UI", Font.BOLD, 12),
                            ACCENT_GLOW
                    ),
                    new EmptyBorder(4, 4, 4, 4)
            ));
        }
        return sp;
    }

    private JButton createFilterButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(INPUT_BG);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(50, 50, 68)); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setBackground(INPUT_BG); }
        });
        return btn;
    }

    // ========== NETWORK & DATA ==========
    private void setupBroadcastListener() {
        client.setBroadcastListener(res -> SwingUtilities.invokeLater(() -> {
            loadBookingsByDate();
            loadMyBookings();
            loadRooms();
            JOptionPane.showMessageDialog(this,
                    "Thông báo từ Server: " + res.getMessage(),
                    "Cập nhật thời gian thực (Real-time)", JOptionPane.INFORMATION_MESSAGE);
        }));

        client.setDisconnectListener(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Mất kết nối tới Máy chủ TCP! Ứng dụng sẽ đóng.",
                    "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
            dispose();
            new LoginForm().setVisible(true);
        }));
    }

    private void loadAllData() {
        loadRooms();
        loadBookingsByDate();
        loadMyBookings();
    }

    private void loadRooms() {
        new Thread(() -> {
            Request req = new Request(ActionType.GET_ROOMS);
            Response res = client.sendRequest(req);
            if (res != null && res.isSuccess()) {
                List<Room> rooms = JsonUtil.fromJson(res.getData(), new TypeToken<List<Room>>() {}.getType());
                cachedRooms = rooms;
                SwingUtilities.invokeLater(() -> {
                    roomTableModel.setRowCount(0);
                    if (adminRoomTableModel != null) adminRoomTableModel.setRowCount(0);

                    for (Room r : rooms) {
                        Object[] row = {r.getId(), r.getName(), r.getCapacity() + " người", r.getLocation(), r.getEquipment(), r.getStatus()};
                        roomTableModel.addRow(row);
                        if (adminRoomTableModel != null) adminRoomTableModel.addRow(row);
                    }
                });
            }
        }).start();
    }

    private void loadBookingsByDate() {
        String date = txtScheduleDate.getText().trim();
        if (date.isEmpty()) return;

        new Thread(() -> {
            Request req = new Request(ActionType.GET_BOOKINGS_BY_DATE, date);
            Response res = client.sendRequest(req);
            if (res != null && res.isSuccess()) {
                List<Booking> list = JsonUtil.fromJson(res.getData(), new TypeToken<List<Booking>>() {}.getType());
                SwingUtilities.invokeLater(() -> {
                    bookingTableModel.setRowCount(0);
                    int stt = 1;
                    for (Booking b : list) {
                        bookingTableModel.addRow(new Object[]{
                                stt++,
                                b.getRoomName(),
                                b.getTimeSlot(),
                                b.getUserFullName(),
                                b.getDepartment(),
                                b.getPurpose(),
                                b.getStatus()
                        });
                    }
                });
            }
        }).start();
    }

    private void loadMyBookings() {
        new Thread(() -> {
            Request req = new Request(ActionType.GET_BOOKINGS_BY_USER, currentUser.getId(), null);
            Response res = client.sendRequest(req);
            if (res != null && res.isSuccess()) {
                List<Booking> list = JsonUtil.fromJson(res.getData(), new TypeToken<List<Booking>>() {}.getType());
                SwingUtilities.invokeLater(() -> {
                    myBookingTableModel.setRowCount(0);
                    for (Booking b : list) {
                        myBookingTableModel.addRow(new Object[]{
                                b.getId(),
                                b.getRoomName(),
                                b.getBookingDate(),
                                b.getTimeSlot(),
                                b.getPurpose(),
                                b.getStatus(),
                                b.getCreatedAt()
                        });
                    }
                });
            }
        }).start();
    }

    private void openBookingDialog() {
        BookingDialog dialog = new BookingDialog(this, client, currentUser, cachedRooms, txtScheduleDate.getText().trim(), () -> {
            loadBookingsByDate();
            loadMyBookings();
        });
        dialog.setVisible(true);
    }

    private void doCancelSelectedBooking() {
        int row = tblMyBookings.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lịch họp trong bảng để hủy!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) myBookingTableModel.getValueAt(row, 0);
        String status = (String) myBookingTableModel.getValueAt(row, 5);

        if ("CANCELLED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Lịch họp này đã bị hủy từ trước!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn hủy lịch họp (Mã: " + bookingId + ") không?",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                Request req = new Request(ActionType.CANCEL_BOOKING, currentUser.getId(), String.valueOf(bookingId));
                Response res = client.sendRequest(req);
                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "Đã hủy lịch họp thành công!");
                        loadMyBookings();
                        loadBookingsByDate();
                    } else {
                        String msg = res != null ? res.getMessage() : "Lỗi khi hủy lịch";
                        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private void doAddRoom() {
        JTextField txtName = new JTextField();
        JTextField txtCap = new JTextField("15");
        JTextField txtLoc = new JTextField("Tầng 2");
        JTextField txtEq = new JTextField("Máy chiếu, Bảng trắng");

        Object[] fields = {
                "Tên phòng:", txtName,
                "Sức chứa (người):", txtCap,
                "Vị trí:", txtLoc,
                "Trang thiết bị:", txtEq
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Thêm phòng họp mới", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Room r = new Room();
                r.setName(txtName.getText().trim());
                r.setCapacity(Integer.parseInt(txtCap.getText().trim()));
                r.setLocation(txtLoc.getText().trim());
                r.setEquipment(txtEq.getText().trim());
                r.setStatus("AVAILABLE");

                new Thread(() -> {
                    Request req = new Request(ActionType.ADD_ROOM, currentUser.getId(), JsonUtil.toJson(r));
                    Response res = client.sendRequest(req);
                    SwingUtilities.invokeLater(() -> {
                        if (res != null && res.isSuccess()) {
                            JOptionPane.showMessageDialog(this, "Đã thêm phòng họp thành công!");
                            loadRooms();
                        } else {
                            JOptionPane.showMessageDialog(this, res != null ? res.getMessage() : "Lỗi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }).start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Dữ liệu nhập không hợp lệ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doToggleRoomStatus() {
        int row = tblAdminRooms.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 phòng trong danh sách!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomId = (int) adminRoomTableModel.getValueAt(row, 0);
        Room room = cachedRooms.stream().filter(r -> r.getId() == roomId).findFirst().orElse(null);
        if (room != null) {
            String newStatus = room.isAvailable() ? "MAINTENANCE" : "AVAILABLE";
            room.setStatus(newStatus);

            new Thread(() -> {
                Request req = new Request(ActionType.UPDATE_ROOM, currentUser.getId(), JsonUtil.toJson(room));
                Response res = client.sendRequest(req);
                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái phòng thành: " + newStatus);
                        loadRooms();
                    } else {
                        JOptionPane.showMessageDialog(this, res != null ? res.getMessage() : "Lỗi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private void doDeleteRoom() {
        int row = tblAdminRooms.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 phòng để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int roomId = (int) adminRoomTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa phòng ID=" + roomId + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                Request req = new Request(ActionType.DELETE_ROOM, currentUser.getId(), String.valueOf(roomId));
                Response res = client.sendRequest(req);
                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "Đã xóa phòng họp!");
                        loadRooms();
                    } else {
                        JOptionPane.showMessageDialog(this, res != null ? res.getMessage() : "Lỗi", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private void doLogout() {
        client.sendRequest(new Request(ActionType.LOGOUT));
        client.disconnect();
        dispose();
        new LoginForm().setVisible(true);
    }
}
