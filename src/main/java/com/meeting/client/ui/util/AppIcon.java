package com.meeting.client.ui.util;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/**
 * Bộ tạo icon đồ họa Vector sắc nét dành riêng cho Java Swing.
 * Đảm bảo 100% hiển thị biểu tượng chuẩn xác trên mọi máy tính,
 * hoàn toàn không bị lỗi ô vuông (tofu glyphs) do thiếu font OS.
 */
public class AppIcon implements Icon {

    public enum Type {
        MAIL, CHECK, CLOCK, LIGHTNING, ADD_USER, EDIT,
        AVATAR, CALENDAR, BOOKINGS, CHAT, GEAR, USERS, BELL, SEND, GLYPH
    }

    private final Type type;
    private final int size;
    private final Color color;
    private final String glyphText;

    private static Font emojiFontCache = null;

    public AppIcon(Type type, int size, Color color) {
        this(type, size, color, null);
    }

    public AppIcon(Type type, int size, Color color, String glyphText) {
        this.type = type;
        this.size = size;
        this.color = color;
        this.glyphText = glyphText;
    }

    public static AppIcon mail(int size, Color color) { return new AppIcon(Type.MAIL, size, color); }
    public static AppIcon check(int size, Color color) { return new AppIcon(Type.CHECK, size, color); }
    public static AppIcon clock(int size, Color color) { return new AppIcon(Type.CLOCK, size, color); }
    public static AppIcon lightning(int size, Color color) { return new AppIcon(Type.LIGHTNING, size, color); }
    public static AppIcon addUser(int size, Color color) { return new AppIcon(Type.ADD_USER, size, color); }
    public static AppIcon edit(int size, Color color) { return new AppIcon(Type.EDIT, size, color); }
    public static AppIcon avatar(int size, Color color) { return new AppIcon(Type.AVATAR, size, color); }
    public static AppIcon calendar(int size, Color color) { return new AppIcon(Type.CALENDAR, size, color); }
    public static AppIcon bookings(int size, Color color) { return new AppIcon(Type.BOOKINGS, size, color); }
    public static AppIcon chat(int size, Color color) { return new AppIcon(Type.CHAT, size, color); }
    public static AppIcon gear(int size, Color color) { return new AppIcon(Type.GEAR, size, color); }
    public static AppIcon users(int size, Color color) { return new AppIcon(Type.USERS, size, color); }
    public static AppIcon bell(int size, Color color) { return new AppIcon(Type.BELL, size, color); }
    public static AppIcon send(int size, Color color) { return new AppIcon(Type.SEND, size, color); }
    public static AppIcon glyph(String glyph, int size, Color color) { return new AppIcon(Type.GLYPH, size, color, glyph); }

    public static Font getEmojiFont(int style, float size) {
        if (emojiFontCache == null) {
            String[] candidates = {"Segoe UI Emoji", "Segoe UI Symbol", "Apple Color Emoji", "Noto Color Emoji", "Segoe UI", "SansSerif"};
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.util.Set<String> installed = new java.util.HashSet<>(java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
            for (String c : candidates) {
                if (installed.contains(c)) {
                    emojiFontCache = new Font(c, Font.PLAIN, 14);
                    break;
                }
            }
            if (emojiFontCache == null) {
                emojiFontCache = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            }
        }
        return emojiFontCache.deriveFont(style, size);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color drawColor = color != null ? color : (c != null ? c.getForeground() : Color.BLACK);
        g2.setColor(drawColor);

        float strokeW = Math.max(1.3f, size * 0.085f);
        g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case MAIL -> {
                float w = size - 3f;
                float h = size * 0.65f;
                float x0 = x + 1.5f;
                float y0 = y + (size - h) / 2f;
                g2.draw(new RoundRectangle2D.Float(x0, y0, w, h, size * 0.15f, size * 0.15f));
                Path2D flap = new Path2D.Float();
                flap.moveTo(x0, y0);
                flap.lineTo(x0 + w / 2f, y0 + h * 0.58f);
                flap.lineTo(x0 + w, y0);
                g2.draw(flap);
            }
            case CHECK -> {
                g2.setStroke(new BasicStroke(Math.max(1.8f, size * 0.12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Float();
                p.moveTo(x + size * 0.18f, y + size * 0.52f);
                p.lineTo(x + size * 0.42f, y + size * 0.78f);
                p.lineTo(x + size * 0.84f, y + size * 0.24f);
                g2.draw(p);
            }
            case CLOCK -> {
                float pad = size * 0.10f;
                float d = size - pad * 2f;
                g2.draw(new Ellipse2D.Float(x + pad, y + pad, d, d));
                float cx = x + size / 2f, cy = y + size / 2f;
                g2.drawLine((int) cx, (int) cy, (int) cx, (int) (cy - d * 0.32f));
                g2.drawLine((int) cx, (int) cy, (int) (cx + d * 0.28f), (int) cy);
            }
            case LIGHTNING -> {
                Path2D p = new Path2D.Float();
                p.moveTo(x + size * 0.56f, y + size * 0.10f);
                p.lineTo(x + size * 0.22f, y + size * 0.54f);
                p.lineTo(x + size * 0.48f, y + size * 0.54f);
                p.lineTo(x + size * 0.40f, y + size * 0.90f);
                p.lineTo(x + size * 0.78f, y + size * 0.44f);
                p.lineTo(x + size * 0.52f, y + size * 0.44f);
                p.closePath();
                g2.fill(p);
            }
            case ADD_USER -> {
                float headD = size * 0.32f;
                g2.fill(new Ellipse2D.Float(x + size * 0.28f, y + size * 0.10f, headD, headD));
                Path2D body = new Path2D.Float();
                body.moveTo(x + size * 0.14f, y + size * 0.84f);
                body.curveTo(x + size * 0.14f, y + size * 0.56f, x + size * 0.74f, y + size * 0.56f, x + size * 0.74f, y + size * 0.84f);
                body.closePath();
                g2.fill(body);
                // Dấu cộng nét mảnh bên phải
                g2.setStroke(new BasicStroke(Math.max(1.6f, size * 0.1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) (x + size * 0.82f), (int) (y + size * 0.22f), (int) (x + size * 0.82f), (int) (y + size * 0.52f));
                g2.drawLine((int) (x + size * 0.67f), (int) (y + size * 0.37f), (int) (x + size * 0.97f), (int) (y + size * 0.37f));
            }
            case EDIT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(x + size * 0.75f, y + size * 0.16f);
                p.lineTo(x + size * 0.86f, y + size * 0.27f);
                p.lineTo(x + size * 0.36f, y + size * 0.78f);
                p.lineTo(x + size * 0.20f, y + size * 0.82f);
                p.lineTo(x + size * 0.24f, y + size * 0.66f);
                p.closePath();
                g2.draw(p);
            }
            case AVATAR -> {
                float headD = size * 0.38f;
                g2.fill(new Ellipse2D.Float(x + (size - headD) / 2f, y + size * 0.10f, headD, headD));
                Path2D body = new Path2D.Float();
                body.moveTo(x + size * 0.14f, y + size * 0.88f);
                body.curveTo(x + size * 0.14f, y + size * 0.54f, x + size * 0.86f, y + size * 0.54f, x + size * 0.86f, y + size * 0.88f);
                body.closePath();
                g2.fill(body);
            }
            case CALENDAR -> {
                float w = size * 0.78f, h = size * 0.74f;
                float x0 = x + (size - w) / 2f, y0 = y + size * 0.16f;
                g2.draw(new RoundRectangle2D.Float(x0, y0, w, h, size * 0.12f, size * 0.12f));
                g2.drawLine((int) x0, (int) (y0 + h * 0.32f), (int) (x0 + w), (int) (y0 + h * 0.32f));
                // Móc treo
                g2.drawLine((int) (x0 + w * 0.28f), (int) (y0 - size * 0.08f), (int) (x0 + w * 0.28f), (int) (y0 + size * 0.08f));
                g2.drawLine((int) (x0 + w * 0.72f), (int) (y0 - size * 0.08f), (int) (x0 + w * 0.72f), (int) (y0 + size * 0.08f));
                // Chấm ngày
                float dotR = size * 0.055f;
                g2.fill(new Ellipse2D.Float(x0 + w * 0.30f - dotR, y0 + h * 0.60f - dotR, dotR * 2, dotR * 2));
                g2.fill(new Ellipse2D.Float(x0 + w * 0.50f - dotR, y0 + h * 0.60f - dotR, dotR * 2, dotR * 2));
                g2.fill(new Ellipse2D.Float(x0 + w * 0.70f - dotR, y0 + h * 0.60f - dotR, dotR * 2, dotR * 2));
            }
            case BOOKINGS -> {
                float w = size * 0.70f, h = size * 0.80f;
                float x0 = x + (size - w) / 2f, y0 = y + size * 0.14f;
                g2.draw(new RoundRectangle2D.Float(x0, y0, w, h, size * 0.10f, size * 0.10f));
                g2.draw(new RoundRectangle2D.Float(x0 + w * 0.25f, y0 - size * 0.07f, w * 0.50f, size * 0.14f, size * 0.06f, size * 0.06f));
                g2.drawLine((int) (x0 + w * 0.22f), (int) (y0 + h * 0.42f), (int) (x0 + w * 0.78f), (int) (y0 + h * 0.42f));
                g2.drawLine((int) (x0 + w * 0.22f), (int) (y0 + h * 0.64f), (int) (x0 + w * 0.78f), (int) (y0 + h * 0.64f));
            }
            case CHAT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(x + size * 0.16f, y + size * 0.20f);
                p.lineTo(x + size * 0.84f, y + size * 0.20f);
                p.curveTo(x + size * 0.92f, y + size * 0.20f, x + size * 0.92f, y + size * 0.62f, x + size * 0.84f, y + size * 0.62f);
                p.lineTo(x + size * 0.46f, y + size * 0.62f);
                p.lineTo(x + size * 0.26f, y + size * 0.84f);
                p.lineTo(x + size * 0.26f, y + size * 0.62f);
                p.lineTo(x + size * 0.16f, y + size * 0.62f);
                p.curveTo(x + size * 0.08f, y + size * 0.62f, x + size * 0.08f, y + size * 0.20f, x + size * 0.16f, y + size * 0.20f);
                p.closePath();
                g2.draw(p);
            }
            case GEAR -> {
                float cx = x + size / 2f, cy = y + size / 2f;
                float r = size * 0.32f;
                g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g2.fill(new Ellipse2D.Float(cx - r * 0.45f, cy - r * 0.45f, r * 0.9f, r * 0.9f));
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 4.0;
                    float dx = (float) (Math.cos(angle) * (r + size * 0.11f));
                    float dy = (float) (Math.sin(angle) * (r + size * 0.11f));
                    g2.drawLine((int) (cx - dx), (int) (cy - dy), (int) (cx + dx), (int) (cy + dy));
                }
            }
            case USERS -> {
                float h1 = size * 0.26f;
                g2.fill(new Ellipse2D.Float(x + size * 0.24f, y + size * 0.16f, h1, h1));
                Path2D b1 = new Path2D.Float();
                b1.moveTo(x + size * 0.10f, y + size * 0.84f);
                b1.curveTo(x + size * 0.10f, y + size * 0.54f, x + size * 0.64f, y + size * 0.54f, x + size * 0.64f, y + size * 0.84f);
                b1.closePath();
                g2.fill(b1);

                float h2 = size * 0.22f;
                g2.fill(new Ellipse2D.Float(x + size * 0.62f, y + size * 0.18f, h2, h2));
                Path2D b2 = new Path2D.Float();
                b2.moveTo(x + size * 0.52f, y + size * 0.74f);
                b2.curveTo(x + size * 0.52f, y + size * 0.52f, x + size * 0.92f, y + size * 0.52f, x + size * 0.92f, y + size * 0.74f);
                b2.closePath();
                g2.fill(b2);
            }
            case BELL -> {
                Path2D p = new Path2D.Float();
                p.moveTo(x + size * 0.50f, y + size * 0.14f);
                p.curveTo(x + size * 0.24f, y + size * 0.26f, x + size * 0.24f, y + size * 0.66f, x + size * 0.16f, y + size * 0.74f);
                p.lineTo(x + size * 0.84f, y + size * 0.74f);
                p.curveTo(x + size * 0.76f, y + size * 0.66f, x + size * 0.76f, y + size * 0.26f, x + size * 0.50f, y + size * 0.14f);
                p.closePath();
                g2.draw(p);
                g2.fill(new Ellipse2D.Float(x + size * 0.43f, y + size * 0.78f, size * 0.14f, size * 0.14f));
            }
            case SEND -> {
                Path2D p = new Path2D.Float();
                p.moveTo(x + size * 0.14f, y + size * 0.18f);
                p.lineTo(x + size * 0.88f, y + size * 0.50f);
                p.lineTo(x + size * 0.14f, y + size * 0.82f);
                p.lineTo(x + size * 0.32f, y + size * 0.50f);
                p.closePath();
                g2.fill(p);
            }
            case GLYPH -> {
                if (glyphText != null && !glyphText.isEmpty()) {
                    Font font = getEmojiFont(Font.PLAIN, size * 0.9f);
                    g2.setFont(font);
                    FontMetrics fm = g2.getFontMetrics();
                    int strY = y + fm.getAscent() + (size - fm.getHeight()) / 2;
                    g2.drawString(glyphText, x, strY);
                }
            }
        }

        g2.dispose();
    }
}
