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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainDashboard extends JFrame {
    private final SocketClient client;
    private final User currentUser;

    private List<Room> cachedRooms = new ArrayList<>();
    private String selectedDate;

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

    public MainDashboard(SocketClient client, User currentUser) {
        this.client = client;
        this.currentUser = currentUser;
        this.selectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        initUI();
        setupBroadcastListener();
        loadAllData();
    }

    private void initUI() {
        setTitle("Hệ thống đặt phòng họp công ty - [" + currentUser.getFullName() + " - " + currentUser.getRole() + "]");
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 90, 188));
        headerPanel.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel lblLogo = new JLabel("HỆ THỐNG ĐẶT PHÒNG HỌP CÔNG TY");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblLogo.setForeground(Color.WHITE);

        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userInfoPanel.setOpaque(false);

        JLabel lblUser = new JLabel("Xin chào: " + currentUser.getFullName() + " (" + currentUser.getDepartment() + ") [" + currentUser.getRole() + "]");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setForeground(Color.WHITE);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setBackground(new Color(220, 53, 69));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> doLogout());

        userInfoPanel.add(lblUser);
        userInfoPanel.add(btnLogout);

        headerPanel.add(lblLogo, BorderLayout.WEST);
        headerPanel.add(userInfoPanel, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        tabbedPane.addTab("  📅 Lịch Đặt Phòng Họp  ", createScheduleTab());
        tabbedPane.addTab("  📋 Lịch Họp Của Tôi  ", createMyBookingsTab());

        if (currentUser.isAdmin()) {
            tabbedPane.addTab("  ⚙ Quản Lý Phòng Họp (Admin)  ", createAdminRoomsTab());
        }

        root.add(tabbedPane, BorderLayout.CENTER);

        // Footer Status
        JPanel footer = new JPanel(new BorderLayout());
        JLabel lblStatus = new JLabel("● Đã kết nối TCP Server (Port 8888) - Sẵn sàng nhận thông báo Real-time");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(40, 167, 69));
        footer.add(lblStatus, BorderLayout.WEST);

        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // TAB 1: SCHEDULE
    private JPanel createScheduleTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.add(new JLabel("Xem lịch ngày (YYYY-MM-DD):"));

        txtScheduleDate = new JTextField(selectedDate, 10);
        filterBar.add(txtScheduleDate);

        JButton btnToday = new JButton("Hôm nay");
        btnToday.addActionListener(e -> {
            txtScheduleDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            loadBookingsByDate();
        });
        filterBar.add(btnToday);

        JButton btnTomorrow = new JButton("Ngày mai");
        btnTomorrow.addActionListener(e -> {
            txtScheduleDate.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            loadBookingsByDate();
        });
        filterBar.add(btnTomorrow);

        JButton btnRefresh = new JButton("Xem Lịch");
        btnRefresh.addActionListener(e -> loadBookingsByDate());
        filterBar.add(btnRefresh);

        JButton btnBook = new JButton("➕ ĐẶT PHÒNG HỌP");
        btnBook.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBook.setBackground(new Color(40, 167, 69));
        btnBook.setForeground(Color.WHITE);
        btnBook.setFocusPainted(false);
        btnBook.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBook.addActionListener(e -> openBookingDialog());
        filterBar.add(btnBook);

        panel.add(filterBar, BorderLayout.NORTH);

        // Split Pane: Upper is Schedule Table, Lower is Room List
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Schedule Table
        String[] bookCols = {"STT", "Phòng họp", "Khung giờ", "Người đặt", "Phòng ban", "Mục đích cuộc họp", "Trạng thái"};
        bookingTableModel = new DefaultTableModel(bookCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBookings = new JTable(bookingTableModel);
        tblBookings.setRowHeight(28);
        tblBookings.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        JScrollPane scrollBookings = new JScrollPane(tblBookings);
        scrollBookings.setBorder(BorderFactory.createTitledBorder("Danh sách lịch họp đã chốt trong ngày"));

        // Room List Table
        String[] roomCols = {"ID", "Tên phòng", "Sức chứa", "Vị trí", "Trang thiết bị", "Trạng thái"};
        roomTableModel = new DefaultTableModel(roomCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRooms = new JTable(roomTableModel);
        tblRooms.setRowHeight(26);
        tblRooms.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        JScrollPane scrollRooms = new JScrollPane(tblRooms);
        scrollRooms.setBorder(BorderFactory.createTitledBorder("Danh mục các phòng họp của công ty"));

        splitPane.setTopComponent(scrollBookings);
        splitPane.setBottomComponent(scrollRooms);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // TAB 2: MY BOOKINGS
    private JPanel createMyBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnReloadMy = new JButton("Làm mới danh sách");
        btnReloadMy.addActionListener(e -> loadMyBookings());

        JButton btnCancelMy = new JButton("Hủy Lịch Họp Đã Chọn");
        btnCancelMy.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelMy.setBackground(new Color(220, 53, 69));
        btnCancelMy.setForeground(Color.WHITE);
        btnCancelMy.addActionListener(e -> doCancelSelectedBooking());

        topBar.add(btnReloadMy);
        topBar.add(btnCancelMy);
        panel.add(topBar, BorderLayout.NORTH);

        String[] myCols = {"Mã Lịch", "Phòng họp", "Ngày họp", "Khung giờ", "Mục đích", "Trạng thái", "Ngày tạo"};
        myBookingTableModel = new DefaultTableModel(myCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblMyBookings = new JTable(myBookingTableModel);
        tblMyBookings.setRowHeight(28);
        tblMyBookings.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(new JScrollPane(tblMyBookings), BorderLayout.CENTER);

        return panel;
    }

    // TAB 3: ADMIN ROOMS
    private JPanel createAdminRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnAddRoom = new JButton("➕ Thêm Phòng Mới");
        btnAddRoom.addActionListener(e -> doAddRoom());

        JButton btnToggleStatus = new JButton("Đổi Trạng Thái (Bảo trì / Sẵn sàng)");
        btnToggleStatus.addActionListener(e -> doToggleRoomStatus());

        JButton btnDeleteRoom = new JButton("Xóa Phòng");
        btnDeleteRoom.setForeground(new Color(220, 53, 69));
        btnDeleteRoom.addActionListener(e -> doDeleteRoom());

        topBar.add(btnAddRoom);
        topBar.add(btnToggleStatus);
        topBar.add(btnDeleteRoom);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Tên phòng", "Sức chứa", "Vị trí", "Trang thiết bị", "Trạng thái"};
        adminRoomTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblAdminRooms = new JTable(adminRoomTableModel);
        tblAdminRooms.setRowHeight(28);
        tblAdminRooms.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(new JScrollPane(tblAdminRooms), BorderLayout.CENTER);

        return panel;
    }



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
