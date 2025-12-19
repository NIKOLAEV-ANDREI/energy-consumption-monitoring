package com.energy.dao;

import com.energy.model.User;
import com.energy.util.DatabaseConnection;
import com.energy.util.PasswordHasher;

import java.sql.*;

/**
 * DAO класс для работы с пользователями
 * Обеспечивает CRUD операции с таблицей users
 */
public class UserDAO {
    
    /**
     * Регистрация нового пользователя
     * @param username имя пользователя
     * @param email электронная почта
     * @param password пароль (будет хэширован)
     * @return созданный пользователь или null при ошибке
     */
    public User register(String username, String email, String password) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            String passwordHash = PasswordHasher.hashPassword(password);
            
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        User user = new User(username, email, passwordHash);
                        user.setId(rs.getInt(1));
                        
                        // Создаём настройки по умолчанию
                        createDefaultSettings(conn, user.getId());
                        // Создаём стандартные группы
                        createDefaultGroups(conn, user.getId());
                        // Создаём стандартные тарифы
                        createDefaultTariffs(conn, user.getId());
                        
                        return user;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Авторизация пользователя
     * @param username имя пользователя или email
     * @param password пароль
     * @return пользователь или null если не найден
     */
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordHasher.verifyPassword(password, storedHash)) {
                        User user = mapResultSetToUser(rs);
                        updateLastLogin(conn, user.getId());
                        return user;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Поиск пользователя по ID
     * @param id идентификатор пользователя
     * @return пользователь или null
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Проверка существования пользователя
     * @param username имя пользователя
     * @param email электронная почта
     * @return true если пользователь существует
     */
    public boolean exists(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Обновление данных пользователя
     * @param user пользователь с обновлёнными данными
     * @return true при успехе
     */
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setInt(3, user.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Изменение пароля
     * @param userId ID пользователя
     * @param newPassword новый пароль
     * @return true при успехе
     */
    public boolean changePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, PasswordHasher.hashPassword(newPassword));
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    // Приватные методы
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
    
    private void updateLastLogin(Connection conn, int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
    
    private void createDefaultSettings(Connection conn, int userId) throws SQLException {
        String sql = "INSERT INTO user_settings (user_id, consumption_goal) VALUES (?, 300)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
    
    private void createDefaultGroups(Connection conn, int userId) throws SQLException {
        String sql = "INSERT INTO appliance_groups (user_id, name, description, icon, color) VALUES " +
                    "(?, 'Кухня', 'Кухонные электроприборы', 'kitchen', '#e74c3c'), " +
                    "(?, 'Гостиная', 'Приборы в гостиной', 'living-room', '#3498db'), " +
                    "(?, 'Спальня', 'Приборы в спальне', 'bedroom', '#9b59b6'), " +
                    "(?, 'Ванная', 'Приборы в ванной комнате', 'bathroom', '#1abc9c'), " +
                    "(?, 'Освещение', 'Осветительные приборы', 'lightbulb', '#f1c40f')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                stmt.setInt(i, userId);
            }
            stmt.executeUpdate();
        }
    }
    
    private void createDefaultTariffs(Connection conn, int userId) throws SQLException {
        String sql = "INSERT INTO tariffs (user_id, name, tariff_type, rate_per_kwh, start_hour, end_hour, valid_from) VALUES " +
                    "(?, 'Пиковый тариф', 'peak', 7.47, 7, 10, CURDATE()), " +
                    "(?, 'Пиковый тариф (вечер)', 'peak', 7.47, 17, 21, CURDATE()), " +
                    "(?, 'Ночной тариф', 'night', 2.74, 23, 7, CURDATE()), " +
                    "(?, 'Полупиковый тариф', 'shoulder', 5.58, 10, 17, CURDATE()), " +
                    "(?, 'Полупиковый тариф (вечер)', 'shoulder', 5.58, 21, 23, CURDATE())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                stmt.setInt(i, userId);
            }
            stmt.executeUpdate();
        }
    }
}



