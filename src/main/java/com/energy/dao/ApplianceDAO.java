package com.energy.dao;

import com.energy.model.Appliance;
import com.energy.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO класс для работы с электроприборами
 * Обеспечивает CRUD операции с таблицей appliances
 */
public class ApplianceDAO {
    
    /**
     * Получение всех приборов пользователя
     * @param userId ID пользователя
     * @return список приборов
     */
    public List<Appliance> findByUserId(int userId) throws SQLException {
        String sql = "SELECT a.*, g.name as group_name FROM appliances a " +
                    "LEFT JOIN appliance_groups g ON a.group_id = g.id " +
                    "WHERE a.user_id = ? ORDER BY a.group_id, a.name";
        
        List<Appliance> appliances = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appliances.add(mapResultSetToAppliance(rs));
                }
            }
        }
        return appliances;
    }
    
    /**
     * Получение приборов по группе
     * @param groupId ID группы
     * @return список приборов
     */
    public List<Appliance> findByGroupId(int groupId) throws SQLException {
        String sql = "SELECT a.*, g.name as group_name FROM appliances a " +
                    "LEFT JOIN appliance_groups g ON a.group_id = g.id " +
                    "WHERE a.group_id = ? ORDER BY a.name";
        
        List<Appliance> appliances = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appliances.add(mapResultSetToAppliance(rs));
                }
            }
        }
        return appliances;
    }
    
    /**
     * Получение прибора по ID
     * @param id ID прибора
     * @return прибор или null
     */
    public Appliance findById(int id) throws SQLException {
        String sql = "SELECT a.*, g.name as group_name FROM appliances a " +
                    "LEFT JOIN appliance_groups g ON a.group_id = g.id " +
                    "WHERE a.id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAppliance(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Создание нового прибора
     * @param appliance данные прибора
     * @return созданный прибор с ID
     */
    public Appliance create(Appliance appliance) throws SQLException {
        String sql = "INSERT INTO appliances (user_id, group_id, name, power_watts, daily_usage_hours, quantity, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, appliance.getUserId());
            if (appliance.getGroupId() != null) {
                stmt.setInt(2, appliance.getGroupId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, appliance.getName());
            stmt.setDouble(4, appliance.getPowerWatts());
            stmt.setDouble(5, appliance.getDailyUsageHours());
            stmt.setInt(6, appliance.getQuantity());
            stmt.setBoolean(7, appliance.isActive());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        appliance.setId(rs.getInt(1));
                        return appliance;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Обновление прибора
     * @param appliance данные прибора
     * @return true при успехе
     */
    public boolean update(Appliance appliance) throws SQLException {
        String sql = "UPDATE appliances SET group_id = ?, name = ?, power_watts = ?, " +
                    "daily_usage_hours = ?, quantity = ?, is_active = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (appliance.getGroupId() != null) {
                stmt.setInt(1, appliance.getGroupId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, appliance.getName());
            stmt.setDouble(3, appliance.getPowerWatts());
            stmt.setDouble(4, appliance.getDailyUsageHours());
            stmt.setInt(5, appliance.getQuantity());
            stmt.setBoolean(6, appliance.isActive());
            stmt.setInt(7, appliance.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Удаление прибора
     * @param id ID прибора
     * @return true при успехе
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM appliances WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Перемещение прибора в другую группу
     * @param applianceId ID прибора
     * @param groupId ID новой группы (null для удаления из группы)
     * @return true при успехе
     */
    public boolean moveToGroup(int applianceId, Integer groupId) throws SQLException {
        String sql = "UPDATE appliances SET group_id = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (groupId != null) {
                stmt.setInt(1, groupId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, applianceId);
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Получение суммарного потребления всех приборов пользователя
     * @param userId ID пользователя
     * @return суммарное дневное потребление в кВт·ч
     */
    public double getTotalDailyConsumption(int userId) throws SQLException {
        String sql = "SELECT SUM(power_watts * daily_usage_hours * quantity / 1000) as total " +
                    "FROM appliances WHERE user_id = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0;
    }
    
    /**
     * Получение топ потребителей
     * @param userId ID пользователя
     * @param limit количество
     * @return список приборов
     */
    public List<Appliance> getTopConsumers(int userId, int limit) throws SQLException {
        String sql = "SELECT a.*, g.name as group_name, " +
                    "(a.power_watts * a.daily_usage_hours * a.quantity / 1000) as daily_kwh " +
                    "FROM appliances a " +
                    "LEFT JOIN appliance_groups g ON a.group_id = g.id " +
                    "WHERE a.user_id = ? AND a.is_active = TRUE " +
                    "ORDER BY daily_kwh DESC LIMIT ?";
        
        List<Appliance> appliances = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appliances.add(mapResultSetToAppliance(rs));
                }
            }
        }
        return appliances;
    }
    
    private Appliance mapResultSetToAppliance(ResultSet rs) throws SQLException {
        Appliance appliance = new Appliance();
        appliance.setId(rs.getInt("id"));
        appliance.setUserId(rs.getInt("user_id"));
        
        int groupId = rs.getInt("group_id");
        if (!rs.wasNull()) {
            appliance.setGroupId(groupId);
        }
        
        appliance.setName(rs.getString("name"));
        appliance.setPowerWatts(rs.getDouble("power_watts"));
        appliance.setDailyUsageHours(rs.getDouble("daily_usage_hours"));
        appliance.setQuantity(rs.getInt("quantity"));
        appliance.setActive(rs.getBoolean("is_active"));
        appliance.setCreatedAt(rs.getTimestamp("created_at"));
        appliance.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        try {
            appliance.setGroupName(rs.getString("group_name"));
        } catch (SQLException e) {
            // Поле может отсутствовать в некоторых запросах
        }
        
        return appliance;
    }
}



