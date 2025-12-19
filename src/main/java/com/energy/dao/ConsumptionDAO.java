package com.energy.dao;

import com.energy.model.ConsumptionRecord;
import com.energy.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO класс для работы с записями потребления
 * Обеспечивает операции с таблицей consumption_records
 */
public class ConsumptionDAO {
    
    /**
     * Получение записей потребления за период
     * @param userId ID пользователя
     * @param startDate начало периода
     * @param endDate конец периода
     * @return список записей
     */
    public List<ConsumptionRecord> findByPeriod(int userId, Date startDate, Date endDate) throws SQLException {
        String sql = "SELECT cr.*, a.name as appliance_name FROM consumption_records cr " +
                    "LEFT JOIN appliances a ON cr.appliance_id = a.id " +
                    "WHERE cr.user_id = ? AND cr.record_date BETWEEN ? AND ? " +
                    "ORDER BY cr.record_date DESC";
        
        List<ConsumptionRecord> records = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setDate(2, startDate);
            stmt.setDate(3, endDate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }
        return records;
    }
    
    /**
     * Получение суммарного потребления по дням
     * @param userId ID пользователя
     * @param days количество дней
     * @return карта: дата -> потребление
     */
    public Map<String, Double> getDailyConsumption(int userId, int days) throws SQLException {
        String sql = "SELECT record_date, SUM(consumption_kwh) as total_kwh " +
                    "FROM consumption_records WHERE user_id = ? " +
                    "AND record_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                    "GROUP BY record_date ORDER BY record_date";
        
        Map<String, Double> consumption = new HashMap<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, days);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    consumption.put(rs.getDate("record_date").toString(), rs.getDouble("total_kwh"));
                }
            }
        }
        return consumption;
    }
    
    /**
     * Получение суммарной стоимости по дням
     * @param userId ID пользователя
     * @param days количество дней
     * @return карта: дата -> стоимость
     */
    public Map<String, Double> getDailyCost(int userId, int days) throws SQLException {
        String sql = "SELECT record_date, SUM(cost) as total_cost " +
                    "FROM consumption_records WHERE user_id = ? " +
                    "AND record_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                    "GROUP BY record_date ORDER BY record_date";
        
        Map<String, Double> costs = new HashMap<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, days);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    costs.put(rs.getDate("record_date").toString(), rs.getDouble("total_cost"));
                }
            }
        }
        return costs;
    }
    
    /**
     * Получение месячной статистики
     * @param userId ID пользователя
     * @param months количество месяцев
     * @return список карт с данными по месяцам
     */
    public List<Map<String, Object>> getMonthlyStats(int userId, int months) throws SQLException {
        String sql = "SELECT YEAR(record_date) as year, MONTH(record_date) as month, " +
                    "SUM(consumption_kwh) as total_kwh, SUM(cost) as total_cost, " +
                    "AVG(consumption_kwh) as avg_daily_kwh, COUNT(DISTINCT record_date) as days_recorded " +
                    "FROM consumption_records WHERE user_id = ? " +
                    "AND record_date >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                    "GROUP BY YEAR(record_date), MONTH(record_date) " +
                    "ORDER BY year DESC, month DESC";
        
        List<Map<String, Object>> stats = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, months);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("year", rs.getInt("year"));
                    stat.put("month", rs.getInt("month"));
                    stat.put("totalKwh", rs.getDouble("total_kwh"));
                    stat.put("totalCost", rs.getDouble("total_cost"));
                    stat.put("avgDailyKwh", rs.getDouble("avg_daily_kwh"));
                    stat.put("daysRecorded", rs.getInt("days_recorded"));
                    stats.add(stat);
                }
            }
        }
        return stats;
    }
    
    /**
     * Создание записи потребления
     * @param record данные записи
     * @return созданная запись с ID
     */
    public ConsumptionRecord create(ConsumptionRecord record) throws SQLException {
        String sql = "INSERT INTO consumption_records (user_id, appliance_id, record_date, consumption_kwh, cost, tariff_type, usage_hours) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, record.getUserId());
            if (record.getApplianceId() != null) {
                stmt.setInt(2, record.getApplianceId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setDate(3, record.getRecordDate());
            stmt.setDouble(4, record.getConsumptionKwh());
            stmt.setDouble(5, record.getCost());
            stmt.setString(6, record.getTariffType());
            if (record.getUsageHours() != null) {
                stmt.setDouble(7, record.getUsageHours());
            } else {
                stmt.setNull(7, Types.DOUBLE);
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setId(rs.getInt(1));
                        return record;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Получение суммарного потребления за текущий месяц
     * @param userId ID пользователя
     * @return суммарное потребление в кВт·ч
     */
    public double getCurrentMonthConsumption(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(consumption_kwh), 0) as total " +
                    "FROM consumption_records WHERE user_id = ? " +
                    "AND YEAR(record_date) = YEAR(CURDATE()) AND MONTH(record_date) = MONTH(CURDATE())";
        
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
     * Получение суммарной стоимости за текущий месяц
     * @param userId ID пользователя
     * @return суммарная стоимость
     */
    public double getCurrentMonthCost(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(cost), 0) as total " +
                    "FROM consumption_records WHERE user_id = ? " +
                    "AND YEAR(record_date) = YEAR(CURDATE()) AND MONTH(record_date) = MONTH(CURDATE())";
        
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
     * Сравнение с предыдущим периодом
     * @param userId ID пользователя
     * @return процент изменения
     */
    public double getComparisonWithPreviousMonth(int userId) throws SQLException {
        String sql = "SELECT " +
                    "(SELECT COALESCE(SUM(consumption_kwh), 0) FROM consumption_records " +
                    " WHERE user_id = ? AND YEAR(record_date) = YEAR(CURDATE()) AND MONTH(record_date) = MONTH(CURDATE())) as current_month, " +
                    "(SELECT COALESCE(SUM(consumption_kwh), 0) FROM consumption_records " +
                    " WHERE user_id = ? AND YEAR(record_date) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) " +
                    " AND MONTH(record_date) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))) as previous_month";
        
        try (Connection conn = DatabaseConnection.createNewConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double current = rs.getDouble("current_month");
                    double previous = rs.getDouble("previous_month");
                    
                    if (previous > 0) {
                        return ((current - previous) / previous) * 100;
                    }
                }
            }
        }
        return 0;
    }
    
    /**
     * Экспорт данных в CSV формат
     * @param userId ID пользователя
     * @param startDate начало периода
     * @param endDate конец периода
     * @return CSV строка
     */
    public String exportToCSV(int userId, Date startDate, Date endDate) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("Дата;Прибор;Потребление (кВт·ч);Стоимость (руб.);Тариф;Часы работы\n");
        
        List<ConsumptionRecord> records = findByPeriod(userId, startDate, endDate);
        
        for (ConsumptionRecord record : records) {
            csv.append(record.getRecordDate()).append(";");
            csv.append(record.getApplianceName() != null ? record.getApplianceName() : "Общее").append(";");
            csv.append(String.format("%.4f", record.getConsumptionKwh())).append(";");
            csv.append(String.format("%.2f", record.getCost())).append(";");
            csv.append(record.getTariffType() != null ? record.getTariffType() : "").append(";");
            csv.append(record.getUsageHours() != null ? String.format("%.2f", record.getUsageHours()) : "").append("\n");
        }
        
        return csv.toString();
    }
    
    private ConsumptionRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        ConsumptionRecord record = new ConsumptionRecord();
        record.setId(rs.getInt("id"));
        record.setUserId(rs.getInt("user_id"));
        
        int applianceId = rs.getInt("appliance_id");
        if (!rs.wasNull()) {
            record.setApplianceId(applianceId);
        }
        
        record.setRecordDate(rs.getDate("record_date"));
        record.setConsumptionKwh(rs.getDouble("consumption_kwh"));
        record.setCost(rs.getDouble("cost"));
        record.setTariffType(rs.getString("tariff_type"));
        
        double usageHours = rs.getDouble("usage_hours");
        if (!rs.wasNull()) {
            record.setUsageHours(usageHours);
        }
        
        record.setCreatedAt(rs.getTimestamp("created_at"));
        
        try {
            record.setApplianceName(rs.getString("appliance_name"));
        } catch (SQLException e) {
            // Поле может отсутствовать
        }
        
        return record;
    }
}



