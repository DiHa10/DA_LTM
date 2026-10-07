package com.meeting.client.ui.util;

import com.meeting.common.util.DateUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Component Popup Lịch theo phong cách Windows Calendar / Warm Minimalist:
 * - Điều hướng Tháng/Năm với nút mũi tên & nút "Hôm nay"
 * - Lưới ngày 7x6 hiển thị mờ ngày tháng trước/sau
 * - Viền highlight cho ngày đang chọn và hiệu ứng rê chuột (Hover)
 * - Tự động điền ngày theo chuẩn Ngày - Tháng - Năm (dd-MM-yyyy)
 */
public class CalendarPickerPopup extends JPopupMenu {
    private static final Color BG_PANEL = new Color(255, 255, 255);
    private static final Color BG_HEADER = new Color(248, 246, 242);
    private static final Color TEXT_PRIMARY = new Color(45, 42, 38);
    private static final Color TEXT_MUTED = new Color(175, 170, 165);
    private static final Color TEXT_HEADER = new Color(90, 85, 80);
    private static final Color ACCENT_COLOR = new Color(14, 116, 144); // Xanh lam hiện đại (#0E7490) hoặc Cam đất
    private static final Color HOVER_BG = new Color(240, 244, 248);
    private static final Color BORDER_COLOR = new Color(225, 220, 212);

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;
    private final Consumer<LocalDate> onSelectCallback;

    private JLabel lblMonthYear;
    private JPanel gridPanel;

    public CalendarPickerPopup(LocalDate initialDate, Consumer<LocalDate> onSelectCallback) {
        this.selectedDate = (initialDate != null) ? initialDate : LocalDate.now();
        this.currentYearMonth = YearMonth.from(this.selectedDate);
        this.onSelectCallback = onSelectCallback;

        setBackground(BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 10, 10)
        ));

        initUI();
        refreshCalendarGrid();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));

        // 1. Header (Tháng Năm & Điều hướng)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(2, 4, 6, 4));

        lblMonthYear = new JLabel("", SwingConstants.LEFT);
        lblMonthYear.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMonthYear.setForeground(TEXT_PRIMARY);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navPanel.setOpaque(false);

        JButton btnPrev = createNavButton("◀", "Tháng trước");
        btnPrev.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            refreshCalendarGrid();
        });

        JButton btnToday = new JButton("Hôm nay");
        btnToday.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnToday.setForeground(ACCENT_COLOR);
        btnToday.setBackground(BG_HEADER);
        btnToday.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnToday.setFocusPainted(false);
        btnToday.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToday.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            currentYearMonth = YearMonth.from(today);
            selectAndClose(today);
        });

        JButton btnNext = createNavButton("▶", "Tháng sau");
        btnNext.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            refreshCalendarGrid();
        });

        navPanel.add(btnPrev);
        navPanel.add(btnToday);
        navPanel.add(btnNext);

        headerPanel.add(lblMonthYear, BorderLayout.WEST);
        headerPanel.add(navPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Thân lịch (Tiêu đề thứ + Lưới 42 ô ngày)
        JPanel calendarBody = new JPanel(new BorderLayout(0, 4));
        calendarBody.setOpaque(false);

        // Tiêu đề các thứ trong tuần
        JPanel weekHeader = new JPanel(new GridLayout(1, 7, 2, 0));
        weekHeader.setOpaque(false);
        String[] daysOfWeek = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (int i = 0; i < 7; i++) {
            JLabel lblDay = new JLabel(daysOfWeek[i], SwingConstants.CENTER);
            lblDay.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblDay.setForeground(i == 0 ? new Color(220, 38, 38) : TEXT_HEADER); // CN màu đỏ nhẹ
            lblDay.setPreferredSize(new Dimension(34, 20));
            weekHeader.add(lblDay);
        }
        calendarBody.add(weekHeader, BorderLayout.NORTH);

        // Lưới ngày (7 cột x 6 hàng = 42 ô)
        gridPanel = new JPanel(new GridLayout(6, 7, 2, 2));
        gridPanel.setOpaque(false);
        gridPanel.setPreferredSize(new Dimension(260, 210));
        calendarBody.add(gridPanel, BorderLayout.CENTER);

        add(calendarBody, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setToolTipText(tooltip);
        btn.setBackground(BG_HEADER);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void refreshCalendarGrid() {
        // Cập nhật tiêu đề Tháng Năm (VD: Tháng 10, 2026)
        String monthName = "Tháng " + currentYearMonth.getMonthValue();
        lblMonthYear.setText(monthName + ", " + currentYearMonth.getYear());

        gridPanel.removeAll();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Chủ Nhật = 0, Thứ Hai = 1, ...
        LocalDate startDate = firstOfMonth.minusDays(startDayOfWeek);

        LocalDate temp = startDate;
        for (int i = 0; i < 42; i++) {
            final LocalDate date = temp;
            boolean isCurrentMonth = (date.getMonthValue() == currentYearMonth.getMonthValue());
            boolean isSelected = date.equals(selectedDate);
            boolean isToday = date.equals(LocalDate.now());

            DayCell cell = new DayCell(date, isCurrentMonth, isSelected, isToday);
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectAndClose(date);
                }
            });
            gridPanel.add(cell);
            temp = temp.plusDays(1);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void selectAndClose(LocalDate date) {
        this.selectedDate = date;
        setVisible(false);
        if (onSelectCallback != null) {
            onSelectCallback.accept(date);
        }
    }

    /**
     * Gắn popup lịch vào một JTextField và một nút bấm kích hoạt (kèm callback).
     */
    public static void attach(JTextField txtField, AbstractButton triggerBtn, Consumer<LocalDate> onDateSelected) {
        Runnable showPopup = () -> {
            LocalDate initial = DateUtil.parseLocalDate(txtField.getText());
            CalendarPickerPopup popup = new CalendarPickerPopup(initial, chosenDate -> {
                txtField.setText(DateUtil.formatUi(chosenDate));
                if (onDateSelected != null) {
                    onDateSelected.accept(chosenDate);
                }
            });
            popup.show(txtField, 0, txtField.getHeight() + 2);
        };

        if (triggerBtn != null) {
            triggerBtn.addActionListener(e -> showPopup.run());
        }

        txtField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    showPopup.run();
                }
            }
        });
    }

    /**
     * Ô hiển thị từng ngày trong lưới lịch (chuẩn phong cách Windows Calendar).
     */
    private class DayCell extends JPanel {
        private final LocalDate date;
        private final boolean isCurrentMonth;
        private final boolean isSelected;
        private final boolean isToday;
        private boolean isHovered = false;

        public DayCell(LocalDate date, boolean isCurrentMonth, boolean isSelected, boolean isToday) {
            this.date = date;
            this.isCurrentMonth = isCurrentMonth;
            this.isSelected = isSelected;
            this.isToday = isToday;

            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectAndClose(date);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isSelected) {
                // Đóng khung viền màu nổi bật và nền xanh dịu như Windows Calendar
                g2.setColor(new Color(224, 242, 254)); // Nền xanh nhạt
                g2.fillRoundRect(2, 2, w - 4, h - 4, 6, 6);
                g2.setColor(ACCENT_COLOR); // Viền xanh đậm
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 6, 6);
            } else if (isHovered) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(2, 2, w - 4, h - 4, 6, 6);
                g2.setColor(new Color(186, 230, 253));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 6, 6);
            } else if (isToday) {
                g2.setColor(new Color(254, 243, 199)); // Nền vàng kem báo hôm nay
                g2.fillRoundRect(2, 2, w - 4, h - 4, 6, 6);
                g2.setColor(new Color(217, 119, 6));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 6, 6);
            }

            // Vẽ số ngày
            String dayText = String.valueOf(date.getDayOfMonth());
            Font font = new Font("Segoe UI", (isSelected || isToday) ? Font.BOLD : Font.PLAIN, 12);
            g2.setFont(font);

            if (isSelected) {
                g2.setColor(ACCENT_COLOR);
            } else if (!isCurrentMonth) {
                g2.setColor(TEXT_MUTED); // Làm mờ các ngày tháng trước/sau
            } else if (date.getDayOfWeek().getValue() == 7) {
                g2.setColor(new Color(185, 28, 28)); // Chủ nhật màu đỏ
            } else {
                g2.setColor(TEXT_PRIMARY);
            }

            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(dayText)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(dayText, tx, ty);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
