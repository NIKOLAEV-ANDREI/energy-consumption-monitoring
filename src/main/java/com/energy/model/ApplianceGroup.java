package com.energy.model;

import java.sql.Timestamp;

/**
 * Модель группы электроприборов
 * Представляет данные из таблицы appliance_groups
 */
public class ApplianceGroup {
    private int id;
    private int userId;
    private String name;
    private String description;
    private String icon;
    private String color;
    private Timestamp createdAt;
    
    // Дополнительные поля для статистики
    private int applianceCount;
    private double totalDailyKwh;
    private double totalMonthlyKwh;
    
    // Конструктор по умолчанию
    public ApplianceGroup() {
        this.icon = "folder";
        this.color = "#3498db";
    }
    
    // Конструктор с параметрами
    public ApplianceGroup(int userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.icon = "folder";
        this.color = "#3498db";
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public int getApplianceCount() {
        return applianceCount;
    }
    
    public void setApplianceCount(int applianceCount) {
        this.applianceCount = applianceCount;
    }
    
    public double getTotalDailyKwh() {
        return totalDailyKwh;
    }
    
    public void setTotalDailyKwh(double totalDailyKwh) {
        this.totalDailyKwh = totalDailyKwh;
    }
    
    public double getTotalMonthlyKwh() {
        return totalMonthlyKwh;
    }
    
    public void setTotalMonthlyKwh(double totalMonthlyKwh) {
        this.totalMonthlyKwh = totalMonthlyKwh;
    }
}



