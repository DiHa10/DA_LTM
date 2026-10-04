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
import java.awt.geom.RoundRectangle2D;
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

    // Bảng màu Warm Minimalist (Kem Sữa, Giấy Mộc & Terracotta / Xanh Rêu)
    private static final Color BG_WARM = new Color(245, 243, 239);       // Nền be kem sữa (#F5F3EF)
    private static final Color SIDEBAR_BG = new Color(28, 63, 52);       // Xanh rêu Forest trầm (#1C3F34)
    private static final Color SIDEBAR_ACTIVE = new Color(42, 85, 70);   // Rêu sáng hơn khi active
    private static final Color CARD_BG = new Color(255, 255, 255);       // Giấy mộc trắng (#FFFFFF)
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);    // Cam đất Terracotta (#C25E34)
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);    // Xanh rêu trầm
    private static final Color DANGER = new Color(220, 38, 38);          // Đỏ cam báo hủy
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);     // Nâu đen Espresso (#2D2A26)
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);// Warm gray (#78716C)
    private static final Color INPUT_BG = new Color(250, 249, 246);      // Kem nhạt
    private static final Color BORDER_WARM = new Color(229, 224, 216);   // Viền cát ấm (#E5E0D8)
    private static final Color TABLE_ROW_ALT = new Color(250, 248, 245); // Dòng xen kẽ ngà nhạt
    private static final Color GRADIENT_START = new Color(194, 94, 52);  // Terracotta
    private static final Color GRADIENT_END = new Color(217, 119, 6);    // Amber

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

    // UI Tab 4: Internal Chat
    private JPanel pnlChatMessages;
    private JScrollPane scrollChat;
    private JTextField txtChatInput;
    private JButton btnSendChat;
    private String currentCardName = "schedule";

    // Notification Center
    private final List<String> notificationHistory = new ArrayList<>();
    private JButton btnNotifications;

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
        setTitle("Meeting Room Booking — [" + currentUser.getFullName() + " - " + currentUser.getRole() + "]");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_WARM);

        // ===== SIDEBAR XANH RÊU TRẦM =====
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BorderLayout());

        // Header thông tin người dùng
        JPanel sidebarHeader = new JPanel();
        sidebarHeader.setOpaque(false);
        sidebarHeader.setLayout(new BoxLayout(sidebarHeader, BoxLayout.Y_AXIS));
        sidebarHeader.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel lblAvatar = new JLabel("\uD83D\uDC64") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillOval(0, 0, 48, 48);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblAvatar.setPreferredSize(new Dimension(48, 48));
        lblAvatar.setMaximumSize(new Dimension(48, 48));
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 26));
        lblAvatar.setForeground(Color.WHITE);
        lblAvatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUserName = new JLabel(currentUser.getFullName());
        lblUserName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblUserName.setForeground(Color.WHITE);
        lblUserName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDept = new JLabel(currentUser.getDepartment() + " • " + currentUser.getRole());
        lblDept.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDept.setForeground(new Color(245, 243, 239, 180));
        lblDept.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebarHeader.add(lblAvatar);
        sidebarHeader.add(Box.createVerticalStrut(10));
        sidebarHeader.add(lblUserName);
        sidebarHeader.add(Box.createVerticalStrut(3));
        sidebarHeader.add(lblDept);

        // Menu chính
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

        JLabel lblMenuTitle = new JLabel("    DANH MỤC CHÍNH");
        lblMenuTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblMenuTitle.setForeground(new Color(245, 243, 239, 130));
        lblMenuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuPanel.add(lblMenuTitle);
        menuPanel.add(Box.createVerticalStrut(8));

        JPanel navSchedule = createNavButton("\uD83D\uDCC5", "Lịch Đặt Phòng", "schedule");
        JPanel navMyBookings = createNavButton("\uD83D\uDCCB", "Lịch Họp Của Tôi", "mybookings");
        JPanel navChat = createNavButton("\uD83D\uDCAC", "Kênh Trao Đổi (Chat)", "chat");
        menuPanel.add(navSchedule);
        menuPanel.add(navMyBookings);
        menuPanel.add(navChat);

        if (currentUser.isAdmin()) {
            menuPanel.add(Box.createVerticalStrut(16));
            JLabel lblAdmin = new JLabel("    QUẢN TRỊ VIÊN");
            lblAdmin.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblAdmin.setForeground(new Color(245, 243, 239, 130));
            lblAdmin.setAlignmentX(Component.LEFT_ALIGNMENT);
            menuPanel.add(lblAdmin);
            menuPanel.add(Box.createVerticalStrut(8));
            JPanel navAdmin = createNavButton("\u2699", "Quản Lý Phòng", "admin");
            menuPanel.add(navAdmin);
        }

        // Footer Sidebar - Đăng xuất
        JPanel sidebarFooter = new JPanel(new BorderLayout());
        sidebarFooter.setOpaque(false);
        sidebarFooter.setBorder(new EmptyBorder(10, 16, 16, 16));

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogout.setBackground(new Color(255, 255, 255, 20));
        btnLogout.setForeground(new Color(254, 202, 202));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(0, 38));
        btnLogout.addActionListener(e -> doLogout());
        btnLogout.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btnLogout.setBackground(new Color(220, 38, 38, 50)); }
            @Override
            public void mouseExited(MouseEvent e) { btnLogout.setBackground(new Color(255, 255, 255, 20)); }
        });
        sidebarFooter.add(btnLogout, BorderLayout.CENTER);

        sidebar.add(sidebarHeader, BorderLayout.NORTH);
        sidebar.add(menuPanel, BorderLayout.CENTER);
        sidebar.add(sidebarFooter, BorderLayout.SOUTH);

        // ===== CONTENT AREA VÙNG LÀM VIỆC KEM SỮA =====
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG_WARM);

        contentArea.add(createScheduleTab(), "schedule");
        contentArea.add(createMyBookingsTab(), "mybookings");
        contentArea.add(createChatTab(), "chat");
        if (currentUser.isAdmin()) {
            contentArea.add(createAdminRoomsTab(), "admin");
        }

        setActiveNav(navSchedule);
        cardLayout.show(contentArea, "schedule");

        // Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(CARD_BG);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_WARM),
                new EmptyBorder(6, 18, 6, 18)
        ));

        JLabel lblStatus = new JLabel("● Đã kết nối TCP Server (Port 8888) — Sẵn sàng nhận thông báo Real-time");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ACCENT_FOREST);

        JLabel lblTech = new JLabel("•  TCP Socket + Thread + Synchronized");
        lblTech.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTech.setForeground(TEXT_SECONDARY);

        bottomBar.add(lblStatus, BorderLayout.WEST);
        bottomBar.add(lblTech, BorderLayout.EAST);

        root.add(sidebar, BorderLayout.WEST);
        root.add(contentArea, BorderLayout.CENTER);
        root.add(bottomBar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createNavButton(String icon, String text, String cardName) {
        JPanel navBtn = new JPanel(new BorderLayout());
        navBtn.setMaximumSize(new Dimension(245, 42));
        navBtn.setBackground(SIDEBAR_BG);
        navBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        navBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(icon + "   " + text);
        lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        lbl.setForeground(new Color(245, 243, 239, 200));
        navBtn.add(lbl, BorderLayout.CENTER);

        navBtn.putClientProperty("cardName", cardName);
        navBtn.putClientProperty("label", lbl);

        navBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentCardName = cardName;
                setActiveNav(navBtn);
                cardLayout.show(contentArea, cardName);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (navBtn != activeNavButton) {
                    navBtn.setBackground(new Color(36, 75, 62));
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
            if (oldLabel != null) oldLabel.setForeground(new Color(245, 243, 239, 200));
        }
        activeNavButton = navBtn;
        navBtn.setBackground(SIDEBAR_ACTIVE);
        navBtn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 4, 0, 0, ACCENT_TERRA),
                new EmptyBorder(10, 16, 10, 20)
        ));
        JLabel newLabel = (JLabel) navBtn.getClientProperty("label");
        if (newLabel != null) {
            newLabel.setForeground(Color.WHITE);
            newLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        }
    }

    // ========== TAB 1: SCHEDULE ==========
    private JPanel createScheduleTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_WARM);
        panel.setBorder(new EmptyBorder(22, 22, 18, 22));

        // Header Title
        JLabel lblPageTitle = new JLabel("Lịch Đặt Phòng Họp");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        // Filter Card giấy mộc
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterCard.setBackground(CARD_BG);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblDateFilter = new JLabel("Xem lịch ngày:");
        lblDateFilter.setForeground(TEXT_SECONDARY);
        lblDateFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterCard.add(lblDateFilter);

        txtScheduleDate = new JTextField(selectedDate, 10);
        txtScheduleDate.setBackground(INPUT_BG);
        txtScheduleDate.setForeground(TEXT_PRIMARY);
        txtScheduleDate.setCaretColor(ACCENT_TERRA);
        txtScheduleDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtScheduleDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(5, 8, 5, 8)
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

        // Nút đặt phòng Terracotta
        JButton btnBook = new JButton("+ Đặt Phòng Mới") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), 0, GRADIENT_END);
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
        filterCard.add(Box.createHorizontalStrut(10));
        filterCard.add(btnBook);

        btnNotifications = new JButton("\uD83D\uDD14 Th\u00F4ng b\u00E1o (0)");
        btnNotifications.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnNotifications.setBackground(INPUT_BG);
        btnNotifications.setForeground(ACCENT_TERRA);
        btnNotifications.setFocusPainted(false);
        btnNotifications.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btnNotifications.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNotifications.addActionListener(e -> openNotificationDialog());
        filterCard.add(Box.createHorizontalStrut(8));
        filterCard.add(btnNotifications);

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(lblPageTitle, BorderLayout.NORTH);
        topArea.add(filterCard, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        // Split Tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setBackground(BG_WARM);
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
        JScrollPane scrollRooms = createStyledScrollPane(tblRooms, "Danh mục các phòng họp công ty");

        splitPane.setTopComponent(scrollBookings);
        splitPane.setBottomComponent(scrollRooms);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // ========== TAB 2: MY BOOKINGS ==========
    private JPanel createMyBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_WARM);
        panel.setBorder(new EmptyBorder(22, 22, 18, 22));

        JLabel lblPageTitle = new JLabel("Lịch Họp Của Tôi");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolBar.setBackground(CARD_BG);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        toolBar.add(createFilterButton("Làm mới danh sách", this::loadMyBookings));

        JButton btnCancel = new JButton("Hủy lịch họp đã chọn");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setBackground(new Color(254, 242, 242));
        btnCancel.setForeground(DANGER);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> doCancelSelectedBooking());
        toolBar.add(btnCancel);

        JButton btnEarlyRelease = new JButton("\u2714  Tr\u1EA3 ph\u00F2ng s\u1EDBm (Gi\u1EA3i ph\u00F3ng ph\u00F2ng)");
        btnEarlyRelease.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnEarlyRelease.setBackground(new Color(240, 253, 244));
        btnEarlyRelease.setForeground(new Color(22, 101, 52));
        btnEarlyRelease.setFocusPainted(false);
        btnEarlyRelease.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btnEarlyRelease.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEarlyRelease.addActionListener(e -> doReleaseSelectedBookingEarly());
        toolBar.add(btnEarlyRelease);

        JButton btnExtend = new JButton("\u23F1  Gia h\u1EA1n gi\u1EDD h\u1ECDp (+30p)");
        btnExtend.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnExtend.setBackground(new Color(254, 243, 235));
        btnExtend.setForeground(ACCENT_TERRA);
        btnExtend.setFocusPainted(false);
        btnExtend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 215, 195), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btnExtend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExtend.addActionListener(e -> doExtendSelectedBooking());
        toolBar.add(btnExtend);

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
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_WARM);
        panel.setBorder(new EmptyBorder(22, 22, 18, 22));

        JLabel lblPageTitle = new JLabel("Quản Lý Phòng Họp (Admin)");
        lblPageTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolBar.setBackground(CARD_BG);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JButton btnAdd = new JButton("+ Thêm Phòng Mới");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setBackground(ACCENT_FOREST);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorder(new EmptyBorder(7, 14, 7, 14));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> doAddRoom());
        toolBar.add(btnAdd);

        toolBar.add(createFilterButton("Đổi trạng thái (Bảo trì/Sẵn sàng)", this::doToggleRoomStatus));

        JButton btnDel = new JButton("Xóa Phòng");
        btnDel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDel.setBackground(new Color(254, 242, 242));
        btnDel.setForeground(DANGER);
        btnDel.setFocusPainted(false);
        btnDel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
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

    // ========== TAB 4: INTERNAL CHAT (TCP REAL-TIME) ==========
    private JPanel createChatTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG_WARM);
        panel.setBorder(new EmptyBorder(22, 22, 18, 22));

        // Top Header
        JPanel topHeader = new JPanel(new BorderLayout(0, 6));
        topHeader.setOpaque(false);

        JLabel lblPageTitle = new JLabel("K\u00EAnh Trao \u0110\u1ED5i N\u1ED9i B\u1ED9 & Th\u1EA3o Lu\u1EADn (TCP Socket)");
        lblPageTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        lblPageTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Trao đổi thông tin trực tiếp theo thời gian thực (Real-time Broadcast) về công tác chuẩn bị phòng họp, tài liệu, thiết bị.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);

        topHeader.add(lblPageTitle, BorderLayout.NORTH);
        topHeader.add(lblSub, BorderLayout.CENTER);
        panel.add(topHeader, BorderLayout.NORTH);

        // Messages Area (Card giấy mộc)
        pnlChatMessages = new JPanel();
        pnlChatMessages.setLayout(new BoxLayout(pnlChatMessages, BoxLayout.Y_AXIS));
        pnlChatMessages.setBackground(CARD_BG);
        pnlChatMessages.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Tin nhắn chào mừng mặc định
        JLabel lblWelcome = new JLabel("<html><i style='color:#78716C;'>=== Kênh trao đổi nội bộ đã kết nối máy chủ TCP. Bắt đầu nhắn tin trao đổi bên dưới... ===</i></html>");
        lblWelcome.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlChatMessages.add(lblWelcome);
        pnlChatMessages.add(Box.createVerticalStrut(14));

        scrollChat = new JScrollPane(pnlChatMessages);
        scrollChat.setBackground(CARD_BG);
        scrollChat.getViewport().setBackground(CARD_BG);
        scrollChat.setBorder(BorderFactory.createLineBorder(BORDER_WARM, 1));
        scrollChat.getVerticalScrollBar().setUnitIncrement(14);
        panel.add(scrollChat, BorderLayout.CENTER);

        // Bottom Input Bar
        JPanel inputBar = new JPanel(new BorderLayout(10, 0));
        inputBar.setBackground(CARD_BG);
        inputBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        txtChatInput = new JTextField();
        txtChatInput.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtChatInput.setBackground(INPUT_BG);
        txtChatInput.setForeground(TEXT_PRIMARY);
        txtChatInput.setCaretColor(ACCENT_TERRA);
        txtChatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        txtChatInput.addActionListener(e -> doSendChatMessage());

        btnSendChat = new JButton("G\u1EEDi \u27A4") {
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
        btnSendChat.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnSendChat.setForeground(Color.WHITE);
        btnSendChat.setContentAreaFilled(false);
        btnSendChat.setFocusPainted(false);
        btnSendChat.setBorderPainted(false);
        btnSendChat.setOpaque(false);
        btnSendChat.setPreferredSize(new Dimension(100, 38));
        btnSendChat.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSendChat.addActionListener(e -> doSendChatMessage());

        inputBar.add(txtChatInput, BorderLayout.CENTER);
        inputBar.add(btnSendChat, BorderLayout.EAST);
        panel.add(inputBar, BorderLayout.SOUTH);

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
                    c.setBackground(new Color(254, 243, 235)); // Màu cam đất nhạt khi chọn dòng
                }
                c.setForeground(TEXT_PRIMARY);
                return c;
            }
        };
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(CARD_BG);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_WARM);
        table.setSelectionBackground(new Color(254, 243, 235));
        table.setSelectionForeground(ACCENT_TERRA);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(245, 241, 234));
        header.setForeground(ACCENT_FOREST);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(new MatteBorder(0, 0, 2, 0, ACCENT_TERRA));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(245, 241, 234));
        headerRenderer.setForeground(ACCENT_FOREST);
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
        sp.setBorder(BorderFactory.createLineBorder(BORDER_WARM, 1));
        if (title != null) {
            sp.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(BORDER_WARM, 1),
                            title,
                            0, 0,
                            new Font("Segoe UI", Font.BOLD, 12),
                            ACCENT_FOREST
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
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(240, 237, 230)); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setBackground(INPUT_BG); }
        });
        return btn;
    }

    // ========== NETWORK & DATA ==========
    private void setupBroadcastListener() {
        client.setBroadcastListener(res -> SwingUtilities.invokeLater(() -> {
            if (res.getAction() == ActionType.CHAT_BROADCAST) {
                try {
                    com.meeting.common.model.ChatMessage chat = JsonUtil.fromJson(res.getData(), com.meeting.common.model.ChatMessage.class);
                    if (chat != null) {
                        appendChatMessage(chat);
                    }
                } catch (Exception ignored) {}
                return;
            }

            loadBookingsByDate();
            loadMyBookings();
            loadRooms();

            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String notifText = "[" + time + "] " + res.getMessage();
            notificationHistory.add(0, notifText);
            updateNotificationBadge();

            if (res.getAction() == ActionType.REMINDER_NOTIFICATION) {
                // Phát tiếng beep cảnh báo
                java.awt.Toolkit.getDefaultToolkit().beep();

                // Hiển thị dialog nhắc nhở nổi bật
                JOptionPane.showMessageDialog(this,
                        res.getMessage(),
                        "\u23F0 NH\u1EAEC NH\u1EDE L\u1ECACH H\u1ECCAP S\u1EAEP DI\u1EC4N RA",
                        JOptionPane.WARNING_MESSAGE);
            } else if (res.getAction() == ActionType.INVITATION_NOTIFICATION) {
                // Phát tiếng beep cảnh báo
                java.awt.Toolkit.getDefaultToolkit().beep();

                // Hiển thị thông báo nhận lời mời tham gia họp
                JOptionPane.showMessageDialog(this,
                        res.getMessage(),
                        "\uD83D\uDCE9 L\u1EDCI M\u1EDEI THAM GIA H\u1ECCAP M\u1EDAI (TCP ALERT)",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        client.setDisconnectListener(() -> SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Mất kết nối tới Máy chủ TCP! Ứng dụng sẽ đóng.",
                    "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
            dispose();
            new LoginForm().setVisible(true);
        }));
    }

    private void updateNotificationBadge() {
        if (btnNotifications != null) {
            btnNotifications.setText("\uD83D\uDD14 Th\u00F4ng b\u00E1o (" + notificationHistory.size() + ")");
        }
    }

    private void openNotificationDialog() {
        JDialog dialog = new JDialog(this, "Trung tâm thông báo & Lịch sử nhắc nhở", true);
        dialog.setSize(540, 420);
        dialog.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG_WARM);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("\uD83D\uDD14  Danh sách thông báo trong phiên làm việc");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setForeground(ACCENT_FOREST);
        p.add(title, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String item : notificationHistory) {
            model.addElement(item);
        }
        if (model.isEmpty()) {
            model.addElement("Chưa có thông báo nào từ Server trong phiên làm việc hiện tại.");
        }

        JList<String> list = new JList<>(model);
        list.setBackground(CARD_BG);
        list.setForeground(TEXT_PRIMARY);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        list.setSelectionBackground(new Color(254, 243, 235));
        list.setSelectionForeground(ACCENT_TERRA);
        list.setFixedCellHeight(32);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_WARM, 1));
        p.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnClear = new JButton("Xóa lịch sử");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClear.addActionListener(e -> {
            notificationHistory.clear();
            model.clear();
            model.addElement("Đã xóa tất cả thông báo.");
            updateNotificationBadge();
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setBackground(ACCENT_TERRA);
        btnClose.setForeground(Color.WHITE);
        btnClose.addActionListener(e -> dialog.dispose());

        bottom.add(btnClear);
        bottom.add(btnClose);
        p.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(p);
        dialog.setVisible(true);
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

    private void doReleaseSelectedBookingEarly() {
        int row = tblMyBookings.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lịch họp trong bảng để trả phòng sớm!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) myBookingTableModel.getValueAt(row, 0);
        String roomName = (String) myBookingTableModel.getValueAt(row, 1);
        String status = (String) myBookingTableModel.getValueAt(row, 5);

        if (!"CONFIRMED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Chỉ có thể trả phòng sớm cho lịch họp đang CONFIRMED (Hiện tại: " + status + ")!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận cuộc họp tại [" + roomName + "] (Mã: " + bookingId + ") đã hoàn tất?\n" +
                "Hệ thống sẽ ghi nhận kết thúc ngay bây giờ và giải phóng phòng cho nhân viên khác đặt lịch.",
                "Xác nhận trả phòng sớm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                Request req = new Request(ActionType.RELEASE_ROOM_EARLY, currentUser.getId(), String.valueOf(bookingId));
                Response res = client.sendRequest(req);
                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, res.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadMyBookings();
                        loadBookingsByDate();
                    } else {
                        String msg = res != null ? res.getMessage() : "Lỗi khi trả phòng sớm!";
                        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private void doExtendSelectedBooking() {
        int row = tblMyBookings.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 lịch họp trong bảng để gia hạn thêm giờ!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) myBookingTableModel.getValueAt(row, 0);
        String roomName = (String) myBookingTableModel.getValueAt(row, 1);
        String timeSlot = (String) myBookingTableModel.getValueAt(row, 3);
        String status = (String) myBookingTableModel.getValueAt(row, 5);

        if (!"CONFIRMED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Chỉ có thể gia hạn cho lịch họp đang CONFIRMED (Hiện tại: " + status + ")!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có muốn gia hạn thêm 30 phút cho cuộc họp tại [" + roomName + "] (Hiện tại: " + timeSlot + ") không?\n" +
                "Hệ thống sẽ kiểm tra xem khung giờ kế tiếp có ai đặt phòng chưa để đảm bảo an toàn tranh chấp.",
                "Xác nhận gia hạn giờ họp (+30 phút)", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                Request req = new Request(ActionType.EXTEND_BOOKING, currentUser.getId(), String.valueOf(bookingId));
                Response res = client.sendRequest(req);
                SwingUtilities.invokeLater(() -> {
                    if (res != null) {
                        if (res.isSuccess()) {
                            JOptionPane.showMessageDialog(this, res.getMessage(), "Gia hạn thành công", JOptionPane.INFORMATION_MESSAGE);
                            loadMyBookings();
                            loadBookingsByDate();
                        } else if (res.isConflict()) {
                            JOptionPane.showMessageDialog(this, res.getMessage(), "CẢNH BÁO: TRÙNG LỊCH PHÒNG HỌP", JOptionPane.WARNING_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, res.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Không nhận được phản hồi từ Server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    private void doSendChatMessage() {
        if (txtChatInput == null) return;
        String text = txtChatInput.getText().trim();
        if (text.isEmpty()) return;

        txtChatInput.setText("");
        new Thread(() -> {
            Request req = new Request(ActionType.SEND_CHAT_MESSAGE, currentUser.getId(), text);
            client.sendRequest(req);
        }).start();
    }

    private void appendChatMessage(com.meeting.common.model.ChatMessage chat) {
        if (pnlChatMessages == null) return;
        SwingUtilities.invokeLater(() -> {
            boolean isMe = currentUser != null && currentUser.getFullName().equalsIgnoreCase(chat.getSenderName());

            JPanel bubbleCard = new JPanel(new BorderLayout(6, 4));
            bubbleCard.setMaximumSize(new Dimension(850, 75));
            bubbleCard.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Header line
            JPanel headerLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            headerLine.setOpaque(false);

            String senderTitle = isMe ? "Bạn (" + chat.getDepartment() + ")" : chat.getSenderName() + " (" + chat.getDepartment() + ")";
            JLabel lblSender = new JLabel(senderTitle);
            lblSender.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSender.setForeground(isMe ? ACCENT_TERRA : ACCENT_FOREST);

            JLabel lblTime = new JLabel(chat.getTimestamp());
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblTime.setForeground(TEXT_SECONDARY);

            headerLine.add(lblSender);
            headerLine.add(lblTime);

            // Message Bubble
            JPanel bubble = new JPanel(new BorderLayout());
            bubble.setBackground(isMe ? new Color(254, 243, 235) : new Color(245, 243, 239));
            bubble.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isMe ? new Color(254, 215, 195) : BORDER_WARM, 1),
                    new EmptyBorder(8, 12, 8, 12)
            ));

            JLabel lblMsg = new JLabel("<html><body style='width: 650px;'>" + escapeHtml(chat.getContent()) + "</body></html>");
            lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblMsg.setForeground(TEXT_PRIMARY);
            bubble.add(lblMsg, BorderLayout.CENTER);

            bubbleCard.setOpaque(false);
            bubbleCard.add(headerLine, BorderLayout.NORTH);
            bubbleCard.add(bubble, BorderLayout.CENTER);

            pnlChatMessages.add(bubbleCard);
            pnlChatMessages.add(Box.createVerticalStrut(10));
            pnlChatMessages.revalidate();
            pnlChatMessages.repaint();

            // Auto scroll down
            SwingUtilities.invokeLater(() -> {
                if (scrollChat != null) {
                    JScrollBar vertical = scrollChat.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
            });
        });
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
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
