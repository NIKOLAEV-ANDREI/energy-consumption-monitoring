package com.energy.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Модель тарифа на электроэнергию
 * Представляет данные из таблицы tariffs
 */
public class Tariff {
    private int id;
    private int userId;
    private String name;
    private String tariffType; // peak, night, shoulder, flat
    private double ratePerKwh;
    private int startHour;
    private int endHour;
    private boolean isActive;
    private Date validFrom;
    private Date validTo;
    private Timestamp createdAt;
    
    // Конструктор по умолчанию
    public Tariff() {
        this.isActive = true;
    }
    
    // Конструктор с параметрами
    public Tariff(int userId, String name, String tariffType, double ratePerKwh, 
                  int startHour, int endHour, Date validFrom) {
        this.userId = userId;
        this.name = name;
        this.tariffType = tariffType;
        this.ratePerKwh = ratePerKwh;
        this.startHour = startHour;
        this.endHour = endHour;
        this.validFrom = validFrom;
        this.isActive = true;
    }
    
    /**
     * Получение названия типа тарифа на русском
     * @return название типа тарифа
     */
    public String getTariffTypeRussian() {
        switch (tariffType) {
            case "peak": return "Пиковый";
            case "night": return "Ночной";
            case "shoulder": return "Полупиковый";
            case "flat": return "Единый";
            default: return tariffType;
        }
    }
    
    /**
     * Проверка, действует ли тариф в указанный час
     * @param hour час (0-23)
     * @return true если тариф действует
     */
    public boolean isActiveAtHour(int hour) {
        if (startHour <= endHour) {
            return hour >= startHour && hour < endHour;
        } else {
            // Ночной тариф (например, 23:00 - 07:00)
            return hour >= startHour || hour < endHour;
        }
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
    
    public String getTariffType() {
        return tariffType;
    }
    
    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }
    
    public double getRatePerKwh() {
        return ratePerKwh;
    }
    
    public void setRatePerKwh(double ratePerKwh) {
        this.ratePerKwh = ratePerKwh;
    }
    
    public int getStartHour() {
        return startHour;
    }
    
    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }
    
    public int getEndHour() {
        return endHour;
    }
    
    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public Date getValidFrom() {
        return validFrom;
    }
    
    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }
    
    public Date getValidTo() {
        return validTo;
    }
    
    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}



