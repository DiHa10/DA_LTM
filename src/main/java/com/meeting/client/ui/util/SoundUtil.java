package com.meeting.client.ui.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

/**
 * Tiện ích phát âm thanh thông báo cho tin nhắn mới (Messenger Chime)
 * Sử dụng bộ tổng hợp âm thanh PCM thuần của Java Sound API, hoạt động ổn định trên mọi máy.
 */
public class SoundUtil {
    private static byte[] cachedChimeData = null;
    private static AudioFormat audioFormat = null;

    static {
        try {
            audioFormat = new AudioFormat(44100, 16, 1, true, false);
            cachedChimeData = generateMessengerChime();
        } catch (Exception ignored) {}
    }

    private static byte[] generateMessengerChime() {
        int sampleRate = 44100;
        // 2 nốt nhạc vui tươi: Nốt 1 (587.33 Hz - D5) trong 80ms, Nốt 2 (880.00 Hz - A5) trong 180ms
        int dur1 = (int) (sampleRate * 0.08);
        int dur2 = (int) (sampleRate * 0.18);
        int totalSamples = dur1 + dur2;
        byte[] buffer = new byte[totalSamples * 2];

        int idx = 0;
        // Nốt 1
        double freq1 = 587.33;
        for (int i = 0; i < dur1; i++) {
            double time = (double) i / sampleRate;
            double envelope = Math.exp(-3.5 * ((double) i / dur1));
            short sample = (short) (Math.sin(2 * Math.PI * freq1 * time) * 15000 * envelope);
            buffer[idx++] = (byte) (sample & 0xFF);
            buffer[idx++] = (byte) ((sample >> 8) & 0xFF);
        }

        // Nốt 2
        double freq2 = 880.0;
        for (int i = 0; i < dur2; i++) {
            double time = (double) i / sampleRate;
            double envelope = Math.exp(-4.0 * ((double) i / dur2));
            short sample = (short) (Math.sin(2 * Math.PI * freq2 * time) * 18000 * envelope);
            buffer[idx++] = (byte) (sample & 0xFF);
            buffer[idx++] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Phát âm thanh thông báo dạng Messenger chuông báo tin nhắn
     */
    public static void playMessengerChime() {
        new Thread(() -> {
            try {
                if (cachedChimeData != null && audioFormat != null) {
                    DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
                    Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.open(audioFormat, cachedChimeData, 0, cachedChimeData.length);
                    clip.start();
                } else {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
            } catch (Exception e) {
                try {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                } catch (Exception ignored) {}
            }
        }, "Sound-Play-Thread").start();
    }
}
