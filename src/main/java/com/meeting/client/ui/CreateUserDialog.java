package com.meeting.client.ui;

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

/**
 * Hộp thoại Admin tạo tài khoản mới cho nhân viên cấp dưới.
 * Hỗ trợ phân quyền vai trò (EMPLOYEE, MANAGER, ADMIN), phòng ban và tùy chọn tự động gửi mail.
 */
public class CreateUserDialog extends JDialog {
    private final SocketClient client;
    private final User adminUser;
    private final Runnable onSuccessCallback;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtFullName;
    private JTextField txtEmail;
    private JComboBox<String> cboRole;
    private JComboBox<String> cboDepartment;
    private JCheckBox chkSendEmail;

    // Bảng màu Warm Minimalist
    private static final Color BG_WARM = new Color(245, 243, 239);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color ACCENT_FOREST = new Color(28, 63, 52);
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);
    private static final Color TEXT_SECONDARY = new Color(120, 113, 108);
    private static final Color INPUT_BG = new Color(250, 249, 246);
    private static final Color BORDER_WARM = new Color(229, 224, 216);

    public CreateUserDialog(Frame parent, SocketClient client, User adminUser, Runnable onSuccessCallback) {
        super(parent, "Tạo tài khoản nhân viên cấp dưới", true);
        this.client = client;
        this.adminUser = adminUser;
        this.onSuccessCallback = onSuccessCallback;

        initUI();
    }

    private void initUI() {
        setSize(460, 560);
        setResizable(false);
        setLocationRelativeTo(getParent());

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BG_WARM);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel pnlHeader = new JPanel();
        pnlHeader.setOpaque(false);
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Tạo Tài Khoản Cấp Dưới (Admin)");
        lblTitle.setIcon(AppIcon.addUser(20, ACCENT_FOREST));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Khởi tạo tài khoản, phân vai trò & gửi thông tin đăng nhập");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_SECONDARY);

        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(4));
        pnlHeader.add(lblSub);
        root.add(pnlHeader, BorderLayout.NORTH);

        // Form Card
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_WARM, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        // 1. Tên đăng nhập
        txtUsername = new JTextField();
        styleField(txtUsername);
        card.add(createFieldWrapper("Tên đăng nhập (Username): *", txtUsername));
        card.add(Box.createVerticalStrut(8));

        // 2. Mật khẩu ban đầu
        txtPassword = new JPasswordField("123456");
        styleField(txtPassword);
        card.add(createFieldWrapper("Mật khẩu ban đầu: *", txtPassword));
        card.add(Box.createVerticalStrut(8));

        // 3. Họ và tên
        txtFullName = new JTextField();
        styleField(txtFullName);
        card.add(createFieldWrapper("Họ và tên nhân viên: *", txtFullName));
        card.add(Box.createVerticalStrut(8));

        // 4. Email
        txtEmail = new JTextField();
        styleField(txtEmail);
        card.add(createFieldWrapper("Email công ty (Nhận mật khẩu & lịch họp):", txtEmail));
        card.add(Box.createVerticalStrut(8));

        // 5. Vai trò (Role)
        String[] roles = {"EMPLOYEE", "MANAGER", "ADMIN"};
        cboRole = new JComboBox<>(roles);
        cboRole.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboRole.setBackground(INPUT_BG);
        cboRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        card.add(createFieldWrapper("Vai trò (Phân quyền):", cboRole));
        card.add(Box.createVerticalStrut(8));

        // 6. Phòng ban
        String[] departments = {"Phòng Kỹ Thuật IT", "Phòng Marketing", "Phòng Nhân Sự", "Phòng Kinh Doanh", "Phòng Kế Toán", "Ban Giám Đốc", "Chung"};
        cboDepartment = new JComboBox<>(departments);
        cboDepartment.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboDepartment.setBackground(INPUT_BG);
        cboDepartment.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        card.add(createFieldWrapper("Phòng ban trực thuộc:", cboDepartment));
        card.add(Box.createVerticalStrut(12));

        // 7. Checkbox gửi Email
        chkSendEmail = new JCheckBox("Tự động gửi thông tin đăng nhập qua Email");
        chkSendEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkSendEmail.setForeground(ACCENT_FOREST);
        chkSendEmail.setOpaque(false);
        chkSendEmail.setSelected(true);
        chkSendEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(chkSendEmail);
        card.add(Box.createVerticalStrut(14));

        root.add(card, BorderLayout.CENTER);

        // Buttons
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlButtons.setOpaque(false);

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> dispose());
        pnlButtons.add(btnCancel);

        JButton btnSubmit = new JButton("Tạo Tài Khoản");
        btnSubmit.setIcon(AppIcon.addUser(14, Color.WHITE));
        btnSubmit.setIconTextGap(6);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setBackground(ACCENT_FOREST);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> doSubmit());
        pnlButtons.add(btnSubmit);

        root.add(pnlButtons, BorderLayout.SOUTH);
        setContentPane(root);
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
        pnl.add(Box.createVerticalStrut(3));
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
                new EmptyBorder(6, 10, 6, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void doSubmit() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String role = (String) cboRole.getSelectedItem();
        String department = (String) cboDepartment.getSelectedItem();
        boolean sendEmail = chkSendEmail.isSelected();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ Tên đăng nhập, Mật khẩu và Họ tên!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (sendEmail && email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn đã chọn gửi email nhưng chưa nhập địa chỉ Email cho nhân viên!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Định dạng email không hợp lệ (VD: user@company.com)!", "Lỗi định dạng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("password", password);
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("role", role);
        data.put("department", department);
        data.put("sendEmail", sendEmail);

        new Thread(() -> {
            try {
                Request req = new Request(ActionType.CREATE_USER, adminUser.getId(), JsonUtil.toJson(data));
                Response res = client.sendRequest(req);

                SwingUtilities.invokeLater(() -> {
                    if (res != null && res.isSuccess()) {
                        String msg = "Tạo tài khoản thành công!";
                        if (sendEmail) {
                            msg += "\nĐã gửi thông tin đăng nhập và mật khẩu tới email: " + email;
                        }
                        JOptionPane.showMessageDialog(this, msg, "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        dispose();
                    } else {
                        String msg = res != null ? res.getMessage() : "Không thể kết nối đến máy chủ!";
                        JOptionPane.showMessageDialog(this, msg, "Lỗi tạo tài khoản", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }
}
