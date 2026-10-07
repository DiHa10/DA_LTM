package com.meeting.server.service;

import com.meeting.common.model.Booking;
import com.meeting.common.model.User;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dịch vụ xử lý gửi Email thông báo & nhắc nhở qua SMTP (Jakarta Mail).
 * Hoạt động trên luồng chạy nền (Worker Thread Pool) độc lập để không làm nghẽn Socket TCP.
 * Tự động chuyển sang chế độ Mock/Log Console nếu chưa cấu hình tài khoản SMTP thực tế.
 */
public class EmailService {
    private static final String CONFIG_FILE = "email.properties";
    private final ExecutorService executor;
    private final Properties props = new Properties();
    private boolean enabled = true;
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private String smtpUsername = "";
    private String smtpPassword = "";
    private String fromName = "Hệ Thống Đặt Phòng Họp";

    public EmailService() {
        this.executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "Email-Worker");
            t.setDaemon(true);
            return t;
        });
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            createDefaultConfig(file);
        }
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            enabled = Boolean.parseBoolean(props.getProperty("mail.enabled", "true"));
            smtpHost = props.getProperty("mail.smtp.host", "smtp.gmail.com");
            smtpPort = Integer.parseInt(props.getProperty("mail.smtp.port", "587"));
            smtpUsername = props.getProperty("mail.smtp.username", "");
            smtpPassword = props.getProperty("mail.smtp.password", "");
            fromName = props.getProperty("mail.from.name", "Hệ Thống Đặt Phòng Họp");
            System.out.println("[EmailService] Đã tải cấu hình email từ " + CONFIG_FILE + " (SMTP User: " + (smtpUsername.isEmpty() ? "Chưa điền - Chế độ Giả lập" : smtpUsername) + ")");
        } catch (Exception e) {
            System.err.println("[EmailService] Không thể đọc file cấu hình " + CONFIG_FILE + ": " + e.getMessage());
        }
    }

    private void createDefaultConfig(File file) {
        Properties defaultProps = new Properties();
        defaultProps.setProperty("mail.enabled", "true");
        defaultProps.setProperty("mail.smtp.host", "smtp.gmail.com");
        defaultProps.setProperty("mail.smtp.port", "587");
        defaultProps.setProperty("mail.smtp.auth", "true");
        defaultProps.setProperty("mail.smtp.starttls.enable", "true");
        defaultProps.setProperty("mail.smtp.username", "");
        defaultProps.setProperty("mail.smtp.password", "");
        defaultProps.setProperty("mail.from.name", "Hệ Thống Đặt Phòng Họp Công Ty");
        try (OutputStream out = new FileOutputStream(file)) {
            defaultProps.store(out, "Cấu hình SMTP gửi Email. Điền App Password của Gmail vào ô password để gửi mail thật.");
            System.out.println("[EmailService] Đã tạo file mẫu " + CONFIG_FILE);
        } catch (Exception ignored) {}
    }

    /**
     * 1. Gửi email thông tin tài khoản cho nhân viên cấp dưới vừa được tạo mới.
     */
    public void sendNewAccountEmailAsync(User user, String rawPassword) {
        if (!enabled || user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return;
        }
        executor.submit(() -> {
            try {
                String subject = "[CÔNG TY] Thông tin tài khoản Hệ thống Quản lý Phòng Họp";
                String htmlBody = String.format("""
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E0D8; border-radius: 8px; background-color: #FAF9F6;">
                        <h2 style="color: #1C3F34; margin-top: 0;">Chào mừng bạn đến với Hệ Thống Đặt Phòng Họp!</h2>
                        <p>Xin chào <strong>%s</strong>,</p>
                        <p>Quản trị viên đã khởi tạo tài khoản cho bạn trên hệ thống quản lý phòng họp nội bộ:</p>
                        <table style="width: 100%%; border-collapse: collapse; margin: 15px 0;">
                            <tr><td style="padding: 8px; font-weight: bold; width: 140px;">Tên đăng nhập:</td><td style="padding: 8px; color: #C25E34; font-weight: bold;">%s</td></tr>
                            <tr><td style="padding: 8px; font-weight: bold;">Mật khẩu ban đầu:</td><td style="padding: 8px; font-family: monospace; font-size: 15px; color: #1C3F34;">%s</td></tr>
                            <tr><td style="padding: 8px; font-weight: bold;">Vai trò:</td><td style="padding: 8px;">%s</td></tr>
                            <tr><td style="padding: 8px; font-weight: bold;">Phòng ban:</td><td style="padding: 8px;">%s</td></tr>
                        </table>
                        <p style="color: #78716C; font-size: 13px;"><em>* Khuyến nghị: Sau khi đăng nhập lần đầu, bạn hãy vào mục <strong>Hồ sơ cá nhân</strong> để đổi lại mật khẩu bảo mật của riêng mình.</em></p>
                        <hr style="border: none; border-top: 1px solid #E5E0D8; margin: 20px 0;" />
                        <p style="color: #A8A29E; font-size: 12px; margin-bottom: 0;">Email này được gửi tự động từ Hệ thống Phòng họp nội bộ công ty.</p>
                    </div>
                """, user.getFullName(), user.getUsername(), rawPassword, user.getRole(), user.getDepartment());

                sendEmail(user.getEmail(), subject, htmlBody);
            } catch (Exception e) {
                System.err.println("[EmailService] Lỗi khi gửi mail tạo tài khoản: " + e.getMessage());
            }
        });
    }

    /**
     * 2. Tự động gửi email khi một cuộc họp được tạo / mở phòng thành công.
     */
    public void sendMeetingCreatedEmailAsync(Booking booking, User host, List<User> attendees) {
        if (!enabled || booking == null) return;

        executor.submit(() -> {
            try {
                List<String> recipientEmails = new ArrayList<>();
                if (host != null && host.getEmail() != null && !host.getEmail().trim().isEmpty()) {
                    recipientEmails.add(host.getEmail().trim());
                }
                if (attendees != null) {
                    for (User u : attendees) {
                        if (u.getEmail() != null && !u.getEmail().trim().isEmpty() && !recipientEmails.contains(u.getEmail().trim())) {
                            recipientEmails.add(u.getEmail().trim());
                        }
                    }
                }

                if (recipientEmails.isEmpty()) {
                    System.out.println("[EmailService] Không có email nào để gửi thông báo cuộc họp #" + booking.getId());
                    return;
                }

                String subject = String.format("[THÔNG BÁO HỌP] Cuộc họp tại [%s] lúc %s ngày %s",
                        booking.getRoomName() != null ? booking.getRoomName() : "Phòng họp",
                        booking.getStartTime(), com.meeting.common.util.DateUtil.toUiDate(booking.getBookingDate()));

                String htmlBody = String.format("""
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E0D8; border-radius: 8px; background-color: #FAF9F6;">
                        <h2 style="color: #1C3F34; margin-top: 0;">Thông Báo Cuộc Họp Mới Được Mở</h2>
                        <p>Một cuộc họp mới đã được đặt lịch thành công trên hệ thống:</p>
                        <div style="background-color: #FFFFFF; padding: 16px; border-radius: 6px; border-left: 4px solid #1C3F34; margin: 15px 0;">
                            <p style="margin: 4px 0;"><strong>Phòng họp:</strong> <span style="color: #1C3F34; font-size: 15px; font-weight: bold;">%s</span></p>
                            <p style="margin: 4px 0;"><strong>Thời gian:</strong> %s - %s (Ngày %s)</p>
                            <p style="margin: 4px 0;"><strong>Chủ trì (Host):</strong> %s (%s)</p>
                            <p style="margin: 4px 0;"><strong>Mục đích:</strong> %s</p>
                        </div>
                        <p>Vui lòng sắp xếp thời gian và tham gia cuộc họp đúng giờ!</p>
                        <hr style="border: none; border-top: 1px solid #E5E0D8; margin: 20px 0;" />
                        <p style="color: #A8A29E; font-size: 12px; margin-bottom: 0;">Email tự động từ Hệ thống Đặt phòng họp công ty.</p>
                    </div>
                """,
                        booking.getRoomName(),
                        booking.getStartTime(), booking.getEndTime(), com.meeting.common.util.DateUtil.toUiDate(booking.getBookingDate()),
                        host != null ? host.getFullName() : booking.getUserFullName(),
                        host != null ? host.getDepartment() : booking.getDepartment(),
                        booking.getPurpose());

                for (String email : recipientEmails) {
                    sendEmail(email, subject, htmlBody);
                }
            } catch (Exception e) {
                System.err.println("[EmailService] Lỗi gửi mail thông báo cuộc họp: " + e.getMessage());
            }
        });
    }

    /**
     * 3. Nút gửi thư thủ công của chủ phòng để nhắc nhở cấp dưới / đồng nghiệp tham gia họp.
     */
    public int sendManualReminderEmailSync(Booking booking, User host, List<User> attendees, String customNote) {
        if (booking == null) return 0;

        List<String> recipients = new ArrayList<>();
        if (attendees != null) {
            for (User u : attendees) {
                if (u.getEmail() != null && !u.getEmail().trim().isEmpty() && !recipients.contains(u.getEmail().trim())) {
                    recipients.add(u.getEmail().trim());
                }
            }
        }

        if (recipients.isEmpty()) {
            return 0;
        }

        executor.submit(() -> {
            try {
                String subject = String.format("[NHẮC NHỞ TỪ CHỦ PHÒNG] Cuộc họp tại [%s] lúc %s ngày %s",
                        booking.getRoomName() != null ? booking.getRoomName() : "Phòng họp",
                        booking.getStartTime(), com.meeting.common.util.DateUtil.toUiDate(booking.getBookingDate()));

                String noteHtml = "";
                if (customNote != null && !customNote.trim().isEmpty()) {
                    noteHtml = String.format("""
                        <div style="background-color: #FEF3C7; border: 1px solid #F59E0B; padding: 12px; border-radius: 6px; margin: 15px 0;">
                            <strong style="color: #B45309;">Lời nhắn từ chủ trì (%s):</strong>
                            <p style="margin: 6px 0 0 0; color: #78350F;">%s</p>
                        </div>
                    """, host != null ? host.getFullName() : "Chủ phòng", customNote.trim());
                }

                String htmlBody = String.format("""
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #E5E0D8; border-radius: 8px; background-color: #FAF9F6;">
                        <h2 style="color: #C25E34; margin-top: 0;">\u23F0 Nhắc Nhở Lịch Họp Sắp Diễn Ra</h2>
                        <p>Chủ trì cuộc họp <strong>%s</strong> gửi lời nhắc nhở tới bạn về cuộc họp sắp diễn ra:</p>
                        <div style="background-color: #FFFFFF; padding: 16px; border-radius: 6px; border-left: 4px solid #C25E34; margin: 15px 0;">
                            <p style="margin: 4px 0;"><strong>Phòng họp:</strong> <span style="color: #C25E34; font-size: 15px; font-weight: bold;">%s</span></p>
                            <p style="margin: 4px 0;"><strong>Khung giờ:</strong> %s - %s</p>
                            <p style="margin: 4px 0;"><strong>Ngày:</strong> %s</p>
                            <p style="margin: 4px 0;"><strong>Nội dung họp:</strong> %s</p>
                        </div>
                        %s
                        <p>Vui lòng chuẩn bị tài liệu liên quan và có mặt đúng giờ quy định.</p>
                        <hr style="border: none; border-top: 1px solid #E5E0D8; margin: 20px 0;" />
                        <p style="color: #A8A29E; font-size: 12px; margin-bottom: 0;">Email nhắc nhở thủ công từ Hệ thống Đặt phòng họp.</p>
                    </div>
                """,
                        host != null ? host.getFullName() : "Chủ trì",
                        booking.getRoomName(),
                        booking.getStartTime(), booking.getEndTime(),
                        com.meeting.common.util.DateUtil.toUiDate(booking.getBookingDate()),
                        booking.getPurpose(),
                        noteHtml);

                for (String email : recipients) {
                    sendEmail(email, subject, htmlBody);
                }
            } catch (Exception e) {
                System.err.println("[EmailService] Lỗi gửi nhắc nhở thủ công: " + e.getMessage());
            }
        });

        return recipients.size();
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        if (smtpUsername == null || smtpUsername.trim().isEmpty() ||
            smtpPassword == null || smtpPassword.trim().isEmpty()) {
            // MOCK MODE: In log console chi tiết để phục vụ demo đồ án khi chưa có SMTP thật
            System.out.println("================================================================================");
            System.out.println("[EMAIL SIMULATION - GIẢ LẬP GỬI EMAIL THÀNH CÔNG]");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Nội dung: (Xem định dạng HTML trong template)");
            System.out.println("================================================================================");
            return;
        }

        try {
            Properties mailProps = new Properties();
            mailProps.put("mail.smtp.host", smtpHost);
            mailProps.put("mail.smtp.port", String.valueOf(smtpPort));
            mailProps.put("mail.smtp.auth", "true");
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpUsername, fromName, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("[EmailService] Đã gửi email THỰC TẾ thành công tới: " + toEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Lỗi gửi email thực tế tới " + toEmail + ": " + e.getMessage());
            System.out.println("[EmailService] -> Chuyển sang ghi nhận Log giả lập cho: " + toEmail);
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
