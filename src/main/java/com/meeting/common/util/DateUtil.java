package com.meeting.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Tiện ích định dạng và chuyển đổi ngày:
 * - Định dạng hiển thị Giao diện (UI): dd-MM-yyyy (Ngày - Tháng - Năm)
 * - Định dạng lưu trữ Cơ sở dữ liệu (DB/Server): yyyy-MM-dd
 */
public class DateUtil {
    public static final DateTimeFormatter UI_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String todayUi() {
        return LocalDate.now().format(UI_FORMATTER);
    }

    public static String todayDb() {
        return LocalDate.now().format(DB_FORMATTER);
    }

    public static String formatUi(LocalDate date) {
        if (date == null) return "";
        return date.format(UI_FORMATTER);
    }

    public static String formatDb(LocalDate date) {
        if (date == null) return "";
        return date.format(DB_FORMATTER);
    }

    /**
     * Parse chuỗi ngày linh hoạt (hỗ trợ cả dd-MM-yyyy và yyyy-MM-dd, kể cả dấu gạch chéo /, ngày 1 chữ số).
     */
    public static LocalDate parseLocalDate(String str) {
        if (str == null || str.trim().isEmpty()) {
            return LocalDate.now();
        }
        String cleaned = str.trim().replace('/', '-');
        String[] parts = cleaned.split("-");
        if (parts.length == 3) {
            try {
                int p0 = Integer.parseInt(parts[0].trim());
                int p1 = Integer.parseInt(parts[1].trim());
                int p2 = Integer.parseInt(parts[2].trim());
                if (p0 > 1000) {
                    // yyyy-MM-dd
                    return LocalDate.of(p0, p1, p2);
                } else if (p2 > 1000) {
                    // dd-MM-yyyy
                    return LocalDate.of(p2, p1, p0);
                }
            } catch (Exception ignored) {}
        }
        try {
            return LocalDate.parse(cleaned, DB_FORMATTER);
        } catch (DateTimeParseException ignored) {}
        try {
            return LocalDate.parse(cleaned, UI_FORMATTER);
        } catch (DateTimeParseException ignored) {}
        return LocalDate.now();
    }

    /**
     * Chuyển đổi từ định dạng bất kỳ sang định dạng CSDL (yyyy-MM-dd).
     */
    public static String toDbDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return todayDb();
        }
        try {
            LocalDate d = parseLocalDate(dateStr);
            return d.format(DB_FORMATTER);
        } catch (Exception e) {
            return dateStr.trim();
        }
    }

    /**
     * Chuyển đổi từ định dạng bất kỳ sang định dạng Giao diện (dd-MM-yyyy).
     */
    public static String toUiDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return todayUi();
        }
        try {
            LocalDate d = parseLocalDate(dateStr);
            return d.format(UI_FORMATTER);
        } catch (Exception e) {
            return dateStr.trim();
        }
    }
}
