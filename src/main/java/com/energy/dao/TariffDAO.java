package com.energy.dao;

import com.energy.model.Tariff;
import com.energy.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO класс для работы с тарифами
 * Обеспечивает CRUD операции с таблицей tariffs
 */
public class TariffDAO {
    
    /**
     * Получение всех тарифов пользователя
     * @param userId ID пользователя
     * @return список тарифов
     */
    public List<Tariff> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM tariffs WHERE user_id = ? ORDER BY tariff_type, start_hour";
        
        List<Tariff> tariffs = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tariffs.add(mapResultSetToTariff(rs));
                }
            }
        }
        return tariffs;
    }
    
    /**
     * Получение активных тарифов пользователя
     * @param userId ID пользователя
     * @return список активных тарифов
     */
    public List<Tariff> findActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM tariffs WHERE user_id = ? AND is_active = TRUE " +
                    "AND valid_from <= CURDATE() AND (valid_to IS NULL OR valid_to >= CURDATE()) " +
                    "ORDER BY tariff_type, start_hour";
        
        List<Tariff> tariffs = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tariffs.add(mapResultSetToTariff(rs));
                }
            }
        }
        return tariffs;
    }
    
    /**
     * Получение тарифа по ID
     * @param id ID тарифа
     * @return тариф или null
     */
    public Tariff findById(int id) throws SQLException {
        String sql = "SELECT * FROM tariffs WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTariff(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Создание нового тарифа
     * @param tariff данные тарифа
     * @return созданный тариф с ID
     */
    public Tariff create(Tariff tariff) throws SQLException {
        String sql = "INSERT INTO tariffs (user_id, name, tariff_type, rate_per_kwh, start_hour, end_hour, is_active, valid_from, valid_to) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, tariff.getUserId());
            stmt.setString(2, tariff.getName());
            stmt.setString(3, tariff.getTariffType());
            stmt.setDouble(4, tariff.getRatePerKwh());
            stmt.setInt(5, tariff.getStartHour());
            stmt.setInt(6, tariff.getEndHour());
            stmt.setBoolean(7, tariff.isActive());
            stmt.setDate(8, tariff.getValidFrom());
            stmt.setDate(9, tariff.getValidTo());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tariff.setId(rs.getInt(1));
                        return tariff;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Обновление тарифа
     * @param tariff данные тарифа
     * @return true при успехе
     */
    public boolean update(Tariff tariff) throws SQLException {
        String sql = "UPDATE tariffs SET name = ?, tariff_type = ?, rate_per_kwh = ?, " +
                    "start_hour = ?, end_hour = ?, is_active = ?, valid_from = ?, valid_to = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tariff.getName());
            stmt.setString(2, tariff.getTariffType());
            stmt.setDouble(3, tariff.getRatePerKwh());
            stmt.setInt(4, tariff.getStartHour());
            stmt.setInt(5, tariff.getEndHour());
            stmt.setBoolean(6, tariff.isActive());
            stmt.setDate(7, tariff.getValidFrom());
            stmt.setDate(8, tariff.getValidTo());
            stmt.setInt(9, tariff.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Удаление тарифа
     * @param id ID тарифа
     * @return true при успехе
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM tariffs WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Получение среднего тарифа пользователя
     * @param userId ID пользователя
     * @return средний тариф за кВт·ч
     */
    public double getAverageRate(int userId) throws SQLException {
        String sql = "SELECT AVG(rate_per_kwh) as avg_rate FROM tariffs " +
                    "WHERE user_id = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_rate");
                }
            }
        }
        return 5.5; // Значение по умолчанию
    }
    
    /**
     * Получение тарифа для указанного часа
     * @param userId ID пользователя
     * @param hour час (0-23)
     * @return тариф или null
     */
    public Tariff getTariffForHour(int userId, int hour) throws SQLException {
        List<Tariff> tariffs = findActiveByUserId(userId);
        
        for (Tariff tariff : tariffs) {
            if (tariff.isActiveAtHour(hour)) {
                return tariff;
            }
        }
        
        // Возвращаем первый активный тариф, если не найден специфичный
        return tariffs.isEmpty() ? null : tariffs.get(0);
    }
    
    private Tariff mapResultSetToTariff(ResultSet rs) throws SQLException {
        Tariff tariff = new Tariff();
        tariff.setId(rs.getInt("id"));
        tariff.setUserId(rs.getInt("user_id"));
        tariff.setName(rs.getString("name"));
        tariff.setTariffType(rs.getString("tariff_type"));
        tariff.setRatePerKwh(rs.getDouble("rate_per_kwh"));
        tariff.setStartHour(rs.getInt("start_hour"));
        tariff.setEndHour(rs.getInt("end_hour"));
        tariff.setActive(rs.getBoolean("is_active"));
        tariff.setValidFrom(rs.getDate("valid_from"));
        tariff.setValidTo(rs.getDate("valid_to"));
        tariff.setCreatedAt(rs.getTimestamp("created_at"));
        return tariff;
    }
}



