package com.meeting.client.ui;

import com.google.gson.reflect.TypeToken;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Hộp thoại Hồ sơ cá nhân: Xem/sửa thông tin (Họ tên, Email, Phòng ban) & Đổi mật khẩu.
 * Thiết kế theo phong cách Warm Minimalist hài hòa với hệ thống.
 */
public class UserProfileDialog extends JDialog {
    private final SocketClient client;
    private final User currentUser;
    private final Consumer<User> onProfileUpdated;

    private JTextField txtUsername;
    private JTextField txtFullName;
    private JTextField txtEmail;
    private JComboBox<String> cboDepartment;

    private JPasswordField txtOldPassword;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;

    // Bảng màu Warm Minimalist
    private static final Color BG_WARM = new Color(245, 243, 239);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);
    private static final Color ACCENT_TERRA = new Color(194, 94, 52);
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);
    private static final Color INPUT_BG = new Color(250, 249, 246);
    private static final Color BORDER_WARM = new Color(229, 224, 216);

    public UserProfileDialog(Frame parent, SocketClient client, User currentUser, Consumer<User> onProfileUpdated) {
        super(parent, "Hồ sơ cá nhân & Tài khoản", true);
        this.client = client;
        this.currentUser = currentUser;
        this.onProfileUpdated = onProfileUpdated;

        initUI();
    }

    private void initUI() {
        setSize(480, 620);
        setResizable(true);
        setMinimumSize(new Dimension(440, 520));
        setLocationRelativeTo(getParent());

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_WARM);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Hồ Sơ Cá Nhân & Bảo Mật");
        lblTitle.setIcon(AppIcon.edit(20, ACCENT_TERRA));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Cập nhật thông tin nhận thông báo và thay đổi mật khẩu đăng nhập");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);

        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(4));
        pnlHeader.add(lblSub);
        root.add(pnlHeader, BorderLayout.NORTH);

        // TabbedPane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(CARD_BG);

        JScrollPane scrollInfo = new JScrollPane(createInfoTab());
        scrollInfo.setBorder(null);
        scrollInfo.getVerticalScrollBar().setUnitIncrement(14);
        tabbedPane.addTab("Thông Tin Chung", AppIcon.avatar(14, TEXT_SECONDARY), scrollInfo);

        JScrollPane scrollPw = new JScrollPane(createPasswordTab());
        scrollPw.setBorder(null);
        scrollPw.getVerticalScrollBar().setUnitIncrement(14);
        tabbedPane.addTab("Đổi Mật Khẩu", AppIcon.lightning(14, ACCENT_TERRA), scrollPw);

        root.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createInfoTab() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Username (Read-only)
        txtUsername = new JTextField(currentUser.getUsername());
        txtUsername.setEditable(false);
        txtUsername.setBackground(new Color(241, 245, 249));
        txtUsername.setForeground(TEXT_SECONDARY);
        styleField(txtUsername);
        card.add(createFieldWrapper("Tên đăng nhập (Cố định):", txtUsername));
        card.add(Box.createVerticalStrut(10));

        // Vai trò hiển thị
        JLabel lblRoleDesc = new JLabel("Vai trò hiện tại: " + currentUser.getRole());
        lblRoleDesc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRoleDesc.setForeground(currentUser.isAdmin() ? ACCENT_TERRA : ACCENT_FOREST);
        card.add(lblRoleDesc);
        card.add(Box.createVerticalStrut(12));

        // Full Name
        txtFullName = new JTextField(currentUser.getFullName());
        styleField(txtFullName);
        card.add(createFieldWrapper("Họ và tên của bạn: *", txtFullName));
        card.add(Box.createVerticalStrut(10));

        // Email
        txtEmail = new JTextField(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        styleField(txtEmail);
        if (!currentUser.isAdmin()) {
            txtEmail.setEditable(false);
            txtEmail.setBackground(new Color(241, 245, 249));
            txtEmail.setForeground(TEXT_SECONDARY);
        }
        card.add(createFieldWrapper(currentUser.isAdmin() ? "Email công ty (Nhận thông báo lịch họp):" : "Email công ty (Cố định):", txtEmail));
        if (!currentUser.isAdmin()) {
            JLabel lblEmailNote = new JLabel("(*) Email do Quản trị viên chỉ định, nhân viên không tự ý thay đổi.");
            lblEmailNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblEmailNote.setForeground(TEXT_SECONDARY);
            card.add(Box.createVerticalStrut(2));
            card.add(lblEmailNote);
        }
        card.add(Box.createVerticalStrut(10));

        // Phòng ban
        String[] departments = {"Phòng Kỹ Thuật IT", "Phòng Marketing", "Phòng Nhân Sự", "Phòng Kinh Doanh", "Phòng Kế Toán", "Ban Giám Đốc", "Chung"};
        cboDepartment = new JComboBox<>(departments);
        cboDepartment.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboDepartment.setBackground(INPUT_BG);
        cboDepartment.setSelectedItem(currentUser.getDepartment());
        cboDepartment.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        if (!currentUser.isAdmin()) {
            cboDepartment.setEnabled(false);
            cboDepartment.setBackground(new Color(241, 245, 249));
        }
        card.add(createFieldWrapper(currentUser.isAdmin() ? "Phòng ban làm việc:" : "Phòng ban làm việc (Cố định):", cboDepartment));
        if (!currentUser.isAdmin()) {
            JLabel lblDeptNote = new JLabel("(*) Phòng ban do Quản trị viên phân bổ, liên hệ Admin nếu chuyển ban.");
            lblDeptNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblDeptNote.setForeground(TEXT_SECONDARY);
            card.add(Box.createVerticalStrut(2));
            card.add(lblDeptNote);
        }
        card.add(Box.createVerticalStrut(20));

        // Nút Lưu
        JButton btnSave = new JButton("Lưu Thay Đổi Thông Tin");
        btnSave.setIcon(AppIcon.check(14, Color.WHITE));
        btnSave.setIconTextGap(6);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(ACCENT_FOREST);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnSave.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSave.addActionListener(e -> doSaveProfile());
        card.add(btnSave);

        return card;
    }

    private JPanel createPasswordTab() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        txtOldPassword = new JPasswordField();
        styleField(txtOldPassword);
        card.add(createFieldWrapper("Mật khẩu hiện tại: *", txtOldPassword));
        card.add(Box.createVerticalStrut(12));

        txtNewPassword = new JPasswordField();
        styleField(txtNewPassword);
        card.add(createFieldWrapper("Mật khẩu mới (Tối thiểu 4 ký tự): *", txtNewPassword));
        card.add(Box.createVerticalStrut(12));

        txtConfirmPassword = new JPasswordField();
        styleField(txtConfirmPassword);
        card.add(createFieldWrapper("Xác nhận lại mật khẩu mới: *", txtConfirmPassword));
        card.add(Box.createVerticalStrut(24));

        JButton btnChangePass = new JButton("Cập Nhật Mật Khẩu");
        btnChangePass.setIcon(AppIcon.check(14, Color.WHITE));
        btnChangePass.setIconTextGap(6);
        btnChangePass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnChangePass.setBackground(ACCENT_TERRA);
        btnChangePass.setForeground(Color.WHITE);
        btnChangePass.setFocusPainted(false);
        btnChangePass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChangePass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnChangePass.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnChangePass.addActionListener(e -> doChangePassword());
        card.add(btnChangePass);

        return card;
    }

    private JPanel createFieldWrapper(String labelText, JComponent field) {
        JPanel pnl = new JPanel();
        pnl.setOpaque(false);
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnl.add(lbl);
        pnl.add(Box.createVerticalStrut(4));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(field);
        return pnl;
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(7, 10, 7, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private void doSaveProfile() {
        String fullName = txtFullName.getText().trim();
        String email = currentUser.isAdmin() ? txtEmail.getText().trim() : (currentUser.getEmail() != null ? currentUser.getEmail() : "");
        String department = currentUser.isAdmin() ? (String) cboDepartment.getSelectedItem() : currentUser.getDepartment();

        if (fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Họ và tên không được để trống!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentUser.isAdmin() && !email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Định dạng email không hợp lệ (VD: user@company.com)!", "Lỗi định dạng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("department", department != null ? department : "Chung");

        new Thread(() -> {
            try {
                Request req = new Request(ActionType.UPDATE_PROFILE, currentUser.getId(), JsonUtil.toJson(data));
                Response res = client.sendRequest(req);

                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "Cập nhật hồ sơ cá nhân thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        User updated = JsonUtil.fromJson(res.getData(), User.class);
                        if (updated != null && onProfileUpdated != null) {
                            onProfileUpdated.accept(updated);
                        }
                        dispose();
                    } else {
                        String msg = res != null ? res.getMessage() : "Không thể kết nối đến máy chủ!";
                        JOptionPane.showMessageDialog(this, msg, "Lỗi cập nhật", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void doChangePassword() {
        String oldPass = new String(txtOldPassword.getPassword()).trim();
        String newPass = new String(txtNewPassword.getPassword()).trim();
        String confirmPass = new String(txtConfirmPassword.getPassword()).trim();

        if (oldPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mật khẩu hiện tại!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPass.length() < 4) {
            JOptionPane.showMessageDialog(this, "Mật khẩu mới phải có ít nhất 4 ký tự!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp với mật khẩu mới!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("oldPassword", oldPass);
        data.put("newPassword", newPass);

        new Thread(() -> {
            try {
                Request req = new Request(ActionType.CHANGE_PASSWORD, currentUser.getId(), JsonUtil.toJson(data));
                Response res = client.sendRequest(req);

                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        JOptionPane.showMessageDialog(this, res.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        txtOldPassword.setText("");
                        txtNewPassword.setText("");
                        txtConfirmPassword.setText("");
                    } else {
                        String msg = res != null ? res.getMessage() : "Không thể kết nối đến máy chủ!";
                        JOptionPane.showMessageDialog(this, msg, "Lỗi đổi mật khẩu", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
}
