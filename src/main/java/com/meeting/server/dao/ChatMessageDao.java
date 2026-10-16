package com.meeting.server.dao;

import com.meeting.common.model.ChatMessage;
import com.meeting.server.db.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatMessageDao {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");

    public boolean insertMessage(ChatMessage msg) {
        String sql = """
            INSERT INTO chat_messages (sender_id, receiver_id, sender_name, sender_dept, receiver_name, content, type, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getSenderId());
            ps.setInt(2, msg.getReceiverId());
            ps.setString(3, msg.getSenderName());
            ps.setString(4, msg.getDepartment());
            ps.setString(5, msg.getReceiverName());
            ps.setString(6, msg.getContent());
            ps.setString(7, msg.getType() != null ? msg.getType() : "PUBLIC");
            ps.setInt(8, msg.isRead() ? 1 : 0);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        msg.setId(rs.getInt(1));
                    }
                }
                if (msg.getTimestamp() == null || msg.getTimestamp().isEmpty()) {
                    msg.setTimestamp(LocalDateTime.now().format(FORMATTER));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ChatMessageDao] Lỗi lưu tin nhắn: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<ChatMessage> getPublicMessages(int limit) {
        return getPublicMessages(limit, 0);
    }

    public List<ChatMessage> getPublicMessages(int limit, int beforeId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = beforeId > 0 ?
            """
            SELECT * FROM chat_messages
            WHERE type = 'PUBLIC' AND id < ?
            ORDER BY id DESC
            LIMIT ?
            """ :
            """
            SELECT * FROM chat_messages
            WHERE type = 'PUBLIC'
            ORDER BY id DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (beforeId > 0) {
                ps.setInt(idx++, beforeId);
            }
            ps.setInt(idx, limit > 0 ? limit : 60);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extract(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Collections.reverse(list); // Trả về theo thứ tự từ cũ đến mới
        return list;
    }

    public List<ChatMessage> getPrivateMessages(int user1Id, int user2Id, int limit) {
        return getPrivateMessages(user1Id, user2Id, limit, 0);
    }

    public List<ChatMessage> getPrivateMessages(int user1Id, int user2Id, int limit, int beforeId) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = beforeId > 0 ?
            """
            SELECT * FROM chat_messages
            WHERE type = 'PRIVATE'
              AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))
              AND id < ?
            ORDER BY id DESC
            LIMIT ?
            """ :
            """
            SELECT * FROM chat_messages
            WHERE type = 'PRIVATE'
              AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))
            ORDER BY id DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user1Id);
            ps.setInt(2, user2Id);
            ps.setInt(3, user2Id);
            ps.setInt(4, user1Id);
            int idx = 5;
            if (beforeId > 0) {
                ps.setInt(idx++, beforeId);
            }
            ps.setInt(idx, limit > 0 ? limit : 80);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extract(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Collections.reverse(list); // Trả về theo thứ tự từ cũ đến mới
        return list;
    }

    public boolean markPrivateAsRead(int senderId, int receiverId) {
        String sql = "UPDATE chat_messages SET is_read = 1 WHERE sender_id = ? AND receiver_id = ? AND is_read = 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<Integer, Integer> getUnreadCountsBySender(int receiverId) {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = """
            SELECT sender_id, COUNT(*) as cnt
            FROM chat_messages
            WHERE receiver_id = ? AND is_read = 0 AND type = 'PRIVATE'
            GROUP BY sender_id
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("sender_id"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public int getTotalUnreadCount(int receiverId) {
        String sql = "SELECT COUNT(*) FROM chat_messages WHERE receiver_id = ? AND is_read = 0 AND type = 'PRIVATE'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private ChatMessage extract(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setId(rs.getInt("id"));
        m.setSenderId(rs.getInt("sender_id"));
        m.setReceiverId(rs.getInt("receiver_id"));
        m.setSenderName(rs.getString("sender_name"));
        m.setDepartment(rs.getString("sender_dept"));
        m.setReceiverName(rs.getString("receiver_name"));
        m.setContent(rs.getString("content"));
        m.setType(rs.getString("type"));
        m.setRead(rs.getInt("is_read") == 1);
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                if (createdAt.length() >= 19) {
                    String time = createdAt.substring(11, 19);
                    String date = createdAt.substring(8, 10) + "/" + createdAt.substring(5, 7);
                    m.setTimestamp(time + " " + date);
                } else {
                    m.setTimestamp(createdAt);
                }
            } catch (Exception e) {
                m.setTimestamp(createdAt);
            }
        }
        return m;
    }
}
