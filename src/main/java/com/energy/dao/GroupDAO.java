package com.energy.dao;

import com.energy.model.ApplianceGroup;
import com.energy.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO класс для работы с группами приборов
 * Обеспечивает CRUD операции с таблицей appliance_groups
 */
public class GroupDAO {
    
    /**
     * Получение всех групп пользователя с статистикой
     * @param userId ID пользователя
     * @return список групп
     */
    public List<ApplianceGroup> findByUserId(int userId) throws SQLException {
        String sql = "SELECT g.*, " +
                    "COUNT(a.id) as appliance_count, " +
                    "COALESCE(SUM(a.power_watts * a.daily_usage_hours * a.quantity / 1000), 0) as daily_kwh, " +
                    "COALESCE(SUM(a.power_watts * a.daily_usage_hours * a.quantity / 1000 * 30), 0) as monthly_kwh " +
                    "FROM appliance_groups g " +
                    "LEFT JOIN appliances a ON g.id = a.group_id AND a.is_active = TRUE " +
                    "WHERE g.user_id = ? " +
                    "GROUP BY g.id ORDER BY g.name";
        
        List<ApplianceGroup> groups = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSetToGroup(rs));
                }
            }
        }
        return groups;
    }
    
    /**
     * Получение группы по ID
     * @param id ID группы
     * @return группа или null
     */
    public ApplianceGroup findById(int id) throws SQLException {
        String sql = "SELECT g.*, " +
                    "COUNT(a.id) as appliance_count, " +
                    "COALESCE(SUM(a.power_watts * a.daily_usage_hours * a.quantity / 1000), 0) as daily_kwh, " +
                    "COALESCE(SUM(a.power_watts * a.daily_usage_hours * a.quantity / 1000 * 30), 0) as monthly_kwh " +
                    "FROM appliance_groups g " +
                    "LEFT JOIN appliances a ON g.id = a.group_id AND a.is_active = TRUE " +
                    "WHERE g.id = ? GROUP BY g.id";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGroup(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Создание новой группы
     * @param group данные группы
     * @return созданная группа с ID
     */
    public ApplianceGroup create(ApplianceGroup group) throws SQLException {
        String sql = "INSERT INTO appliance_groups (user_id, name, description, icon, color) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, group.getUserId());
            stmt.setString(2, group.getName());
            stmt.setString(3, group.getDescription());
            stmt.setString(4, group.getIcon());
            stmt.setString(5, group.getColor());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        group.setId(rs.getInt(1));
                        return group;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Обновление группы
     * @param group данные группы
     * @return true при успехе
     */
    public boolean update(ApplianceGroup group) throws SQLException {
        String sql = "UPDATE appliance_groups SET name = ?, description = ?, icon = ?, color = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setString(3, group.getIcon());
            stmt.setString(4, group.getColor());
            stmt.setInt(5, group.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Удаление группы
     * @param id ID группы
     * @return true при успехе
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM appliance_groups WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    private ApplianceGroup mapResultSetToGroup(ResultSet rs) throws SQLException {
        ApplianceGroup group = new ApplianceGroup();
        group.setId(rs.getInt("id"));
        group.setUserId(rs.getInt("user_id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setIcon(rs.getString("icon"));
        group.setColor(rs.getString("color"));
        group.setCreatedAt(rs.getTimestamp("created_at"));
        
        try {
            group.setApplianceCount(rs.getInt("appliance_count"));
            group.setTotalDailyKwh(rs.getDouble("daily_kwh"));
            group.setTotalMonthlyKwh(rs.getDouble("monthly_kwh"));
        } catch (SQLException e) {
            // Статистические поля могут отсутствовать
        }
        
        return group;
    }
}



