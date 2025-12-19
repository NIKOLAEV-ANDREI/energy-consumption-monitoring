package com.energy.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Модель записи потребления электроэнергии
 * Представляет данные из таблицы consumption_records
 */
public class ConsumptionRecord {
    private int id;
    private int userId;
    private Integer applianceId;
    private Date recordDate;
    private double consumptionKwh;
    private double cost;
    private String tariffType;
    private Double usageHours;
    private Timestamp createdAt;
    
    // Дополнительные поля для отображения
    private String applianceName;
    
    // Конструктор по умолчанию
    public ConsumptionRecord() {}
    
    // Конструктор с параметрами
    public ConsumptionRecord(int userId, Integer applianceId, Date recordDate, 
                            double consumptionKwh, double cost, String tariffType) {
        this.userId = userId;
        this.applianceId = applianceId;
        this.recordDate = recordDate;
        this.consumptionKwh = consumptionKwh;
        this.cost = cost;
        this.tariffType = tariffType;
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
    
    public Integer getApplianceId() {
        return applianceId;
    }
    
    public void setApplianceId(Integer applianceId) {
        this.applianceId = applianceId;
    }
    
    public Date getRecordDate() {
        return recordDate;
    }
    
    public void setRecordDate(Date recordDate) {
        this.recordDate = recordDate;
    }
    
    public double getConsumptionKwh() {
        return consumptionKwh;
    }
    
    public void setConsumptionKwh(double consumptionKwh) {
        this.consumptionKwh = consumptionKwh;
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public String getTariffType() {
        return tariffType;
    }
    
    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }
    
    public Double getUsageHours() {
        return usageHours;
    }
    
    public void setUsageHours(Double usageHours) {
        this.usageHours = usageHours;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getApplianceName() {
        return applianceName;
    }
    
    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }
}



