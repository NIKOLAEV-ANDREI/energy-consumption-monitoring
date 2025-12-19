package com.energy.model;

import java.sql.Timestamp;

/**
 * Модель электроприбора
 * Представляет данные из таблицы appliances
 */
public class Appliance {
    private int id;
    private int userId;
    private Integer groupId;
    private String name;
    private double powerWatts;
    private double dailyUsageHours;
    private int quantity;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Дополнительные поля для отображения
    private String groupName;
    
    // Конструктор по умолчанию
    public Appliance() {
        this.quantity = 1;
        this.isActive = true;
    }
    
    // Конструктор с параметрами
    public Appliance(int userId, String name, double powerWatts, double dailyUsageHours, int quantity) {
        this.userId = userId;
        this.name = name;
        this.powerWatts = powerWatts;
        this.dailyUsageHours = dailyUsageHours;
        this.quantity = quantity;
        this.isActive = true;
    }
    
    // Вычисляемые методы
    
    /**
     * Расчёт дневного потребления в кВт·ч
     * @return потребление за день
     */
    public double getDailyConsumptionKwh() {
        return (powerWatts * dailyUsageHours * quantity) / 1000.0;
    }
    
    /**
     * Расчёт месячного потребления в кВт·ч
     * @return потребление за месяц (30 дней)
     */
    public double getMonthlyConsumptionKwh() {
        return getDailyConsumptionKwh() * 30;
    }
    
    /**
     * Расчёт стоимости за день
     * @param tariffRate тариф за кВт·ч
     * @return стоимость за день
     */
    public double getDailyCost(double tariffRate) {
        return getDailyConsumptionKwh() * tariffRate;
    }
    
    /**
     * Расчёт стоимости за месяц
     * @param tariffRate тариф за кВт·ч
     * @return стоимость за месяц
     */
    public double getMonthlyCost(double tariffRate) {
        return getMonthlyConsumptionKwh() * tariffRate;
    }
    
    // Геттеры и сеттеры
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public Integer getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getPowerWatts() {
        return powerWatts;
    }
    
    public void setPowerWatts(double powerWatts) {
        this.powerWatts = powerWatts;
    }
    
    public double getDailyUsageHours() {
        return dailyUsageHours;
    }
    
    public void setDailyUsageHours(double dailyUsageHours) {
        this.dailyUsageHours = dailyUsageHours;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}



