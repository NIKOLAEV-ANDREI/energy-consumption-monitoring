package com.energy.servlet;

import com.energy.dao.ApplianceDAO;
import com.energy.dao.TariffDAO;
import com.energy.model.Appliance;
import com.energy.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Сервлет для управления электроприборами
 * Обеспечивает CRUD операции
 */
@WebServlet(urlPatterns = {"/api/appliances/*"})
public class ApplianceServlet extends HttpServlet {
    
    private ApplianceDAO applianceDAO;
    private TariffDAO tariffDAO;
    
    @Override
    public void init() throws ServletException {
        applianceDAO = new ApplianceDAO();
        tariffDAO = new TariffDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(JsonUtil.errorResponse("Требуется авторизация"));
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // Получить все приборы
                handleGetAll(userId, out);
            } else if (pathInfo.equals("/top")) {
                // Получить топ потребителей
                handleGetTopConsumers(userId, out);
            } else if (pathInfo.equals("/stats")) {
                // Получить статистику
                handleGetStats(userId, out);
            } else {
                // Получить конкретный прибор
                int id = Integer.parseInt(pathInfo.substring(1));
                handleGetById(id, out);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(JsonUtil.errorResponse("Требуется авторизация"));
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        
        try {
            Map<String, Object> data = parseRequestBody(request);
            handleCreate(userId, data, out);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(JsonUtil.errorResponse("Требуется авторизация"));
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo != null && pathInfo.length() > 1) {
                int id = Integer.parseInt(pathInfo.substring(1));
                Map<String, Object> data = parseRequestBody(request);
                handleUpdate(id, data, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(JsonUtil.errorResponse("Укажите ID прибора"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(JsonUtil.errorResponse("Требуется авторизация"));
            return;
        }
        
        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo != null && pathInfo.length() > 1) {
                int id = Integer.parseInt(pathInfo.substring(1));
                handleDelete(id, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(JsonUtil.errorResponse("Укажите ID прибора"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    private void handleGetAll(int userId, PrintWriter out) throws Exception {
        List<Appliance> appliances = applianceDAO.findByUserId(userId);
        double avgRate = tariffDAO.getAverageRate(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appliance a : appliances) {
            result.add(applianceToMap(a, avgRate));
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    private void handleGetById(int id, PrintWriter out) throws Exception {
        Appliance appliance = applianceDAO.findById(id);
        
        if (appliance != null) {
            double avgRate = tariffDAO.getAverageRate(appliance.getUserId());
            out.print(JsonUtil.toJson(applianceToMap(appliance, avgRate)));
        } else {
            out.print(JsonUtil.errorResponse("Прибор не найден"));
        }
    }
    
    private void handleGetTopConsumers(int userId, PrintWriter out) throws Exception {
        List<Appliance> appliances = applianceDAO.getTopConsumers(userId, 5);
        double avgRate = tariffDAO.getAverageRate(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appliance a : appliances) {
            result.add(applianceToMap(a, avgRate));
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    private void handleGetStats(int userId, PrintWriter out) throws Exception {
        double totalDaily = applianceDAO.getTotalDailyConsumption(userId);
        double avgRate = tariffDAO.getAverageRate(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDailyKwh", Math.round(totalDaily * 100.0) / 100.0);
        stats.put("totalMonthlyKwh", Math.round(totalDaily * 30 * 100.0) / 100.0);
        stats.put("totalDailyCost", Math.round(totalDaily * avgRate * 100.0) / 100.0);
        stats.put("totalMonthlyCost", Math.round(totalDaily * 30 * avgRate * 100.0) / 100.0);
        stats.put("avgTariff", Math.round(avgRate * 100.0) / 100.0);
        
        out.print(JsonUtil.toJson(stats));
    }
    
    private void handleCreate(int userId, Map<String, Object> data, PrintWriter out) throws Exception {
        Appliance appliance = new Appliance();
        appliance.setUserId(userId);
        appliance.setName((String) data.get("name"));
        appliance.setPowerWatts(toDouble(data.get("powerWatts")));
        appliance.setDailyUsageHours(toDouble(data.get("dailyUsageHours")));
        appliance.setQuantity(data.get("quantity") != null ? toInt(data.get("quantity")) : 1);
        
        if (data.get("groupId") != null && !data.get("groupId").toString().isEmpty()) {
            appliance.setGroupId(toInt(data.get("groupId")));
        }
        
        Appliance created = applianceDAO.create(appliance);
        
        if (created != null) {
            double avgRate = tariffDAO.getAverageRate(userId);
            Map<String, Object> response = applianceToMap(created, avgRate);
            
            // Добавляем рекомендацию если есть
            String tip = getApplianceTip(created.getName(), created.getDailyConsumptionKwh());
            if (tip != null) {
                response.put("tip", tip);
            }
            
            out.print(JsonUtil.successResponse("Прибор добавлен", response));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при создании прибора"));
        }
    }
    
    private void handleUpdate(int id, Map<String, Object> data, PrintWriter out) throws Exception {
        Appliance appliance = applianceDAO.findById(id);
        
        if (appliance == null) {
            out.print(JsonUtil.errorResponse("Прибор не найден"));
            return;
        }
        
        if (data.get("name") != null) {
            appliance.setName((String) data.get("name"));
        }
        if (data.get("powerWatts") != null) {
            appliance.setPowerWatts(toDouble(data.get("powerWatts")));
        }
        if (data.get("dailyUsageHours") != null) {
            appliance.setDailyUsageHours(toDouble(data.get("dailyUsageHours")));
        }
        if (data.get("quantity") != null) {
            appliance.setQuantity(toInt(data.get("quantity")));
        }
        if (data.containsKey("groupId")) {
            Object groupId = data.get("groupId");
            appliance.setGroupId(groupId != null && !groupId.toString().isEmpty() ? toInt(groupId) : null);
        }
        if (data.get("isActive") != null) {
            appliance.setActive(toBoolean(data.get("isActive")));
        }
        
        if (applianceDAO.update(appliance)) {
            double avgRate = tariffDAO.getAverageRate(appliance.getUserId());
            out.print(JsonUtil.successResponse("Прибор обновлён", applianceToMap(appliance, avgRate)));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при обновлении"));
        }
    }
    
    private void handleDelete(int id, PrintWriter out) throws Exception {
        if (applianceDAO.delete(id)) {
            out.print(JsonUtil.successResponse("Прибор удалён"));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при удалении"));
        }
    }
    
    private Map<String, Object> applianceToMap(Appliance a, double avgRate) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("name", a.getName());
        map.put("powerWatts", a.getPowerWatts());
        map.put("dailyUsageHours", a.getDailyUsageHours());
        map.put("quantity", a.getQuantity());
        map.put("groupId", a.getGroupId());
        map.put("groupName", a.getGroupName());
        map.put("isActive", a.isActive());
        map.put("dailyKwh", Math.round(a.getDailyConsumptionKwh() * 1000.0) / 1000.0);
        map.put("monthlyKwh", Math.round(a.getMonthlyConsumptionKwh() * 100.0) / 100.0);
        map.put("dailyCost", Math.round(a.getDailyCost(avgRate) * 100.0) / 100.0);
        map.put("monthlyCost", Math.round(a.getMonthlyCost(avgRate) * 100.0) / 100.0);
        return map;
    }
    
    private Map<String, Object> parseRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return JsonUtil.parseJson(sb.toString());
    }
    
    // Вспомогательные методы для безопасного преобразования типов
    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return (int) Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(value.toString());
    }
    
    /**
     * Получение рекомендации для прибора
     */
    private String getApplianceTip(String name, double dailyKwh) {
        String nameLower = name.toLowerCase();
        
        // Рекомендации по типу прибора
        if (nameLower.contains("холодильник")) {
            return "💡 Совет: Не ставьте холодильник рядом с плитой или батареей. Регулярно размораживайте. Не ставьте горячую еду — это увеличивает расход на 20%.";
        }
        if (nameLower.contains("чайник")) {
            return "💡 Совет: Кипятите только нужное количество воды. Очищайте чайник от накипи — она увеличивает расход на 15-20%.";
        }
        if (nameLower.contains("стирал")) {
            return "💡 Совет: Стирайте при полной загрузке. Режим 30-40°C вместо 60°C экономит до 50% энергии. Запускайте стирку ночью по льготному тарифу.";
        }
        if (nameLower.contains("кондиционер")) {
            return "💡 Совет: Температура не ниже 24°C. Каждый градус ниже +5-8% к расходу. Чистите фильтры. Закрывайте окна при работе.";
        }
        if (nameLower.contains("обогреватель") || nameLower.contains("конвектор") || nameLower.contains("радиатор")) {
            return "⚠️ Внимание: Обогреватели очень энергозатратны! Утеплите окна и двери. Используйте только в одной комнате. Выключайте при выходе.";
        }
        if (nameLower.contains("телевизор") || nameLower.contains("тв")) {
            return "💡 Совет: Уменьшите яркость экрана. Используйте таймер автовыключения. Выключайте полностью, а не в режим ожидания.";
        }
        if (nameLower.contains("компьютер") || nameLower.contains("пк")) {
            return "💡 Совет: Используйте режим энергосбережения. Выключайте монитор при перерывах более 10 минут.";
        }
        if (nameLower.contains("ноутбук")) {
            return "💡 Совет: Отключайте зарядку после полной зарядки. Используйте режим энергосбережения.";
        }
        if (nameLower.contains("утюг")) {
            return "💡 Совет: Гладьте сразу много белья. Выключайте за 5-10 минут до конца — остаточного тепла хватит для лёгких тканей.";
        }
        if (nameLower.contains("посудомо")) {
            return "💡 Совет: Запускайте при полной загрузке. Используйте эко-режим. Откажитесь от сушки — дайте посуде высохнуть естественно.";
        }
        if (nameLower.contains("микроволн")) {
            return "💡 Совет: Размораживайте продукты заранее в холодильнике, а не в микроволновке. Накрывайте еду крышкой для быстрого нагрева.";
        }
        if (nameLower.contains("бойлер") || nameLower.contains("водонагреватель")) {
            return "⚠️ Внимание: Бойлер — один из главных потребителей! Установите температуру 55-60°C. Выключайте при длительном отсутствии.";
        }
        if (nameLower.contains("фен")) {
            return "💡 Совет: Сушите волосы частично полотенцем перед феном. Используйте среднюю температуру — она бережнее и экономичнее.";
        }
        if (nameLower.contains("лампа") || nameLower.contains("люстра") || nameLower.contains("свет")) {
            return "💡 Совет: Если это не LED — замените! LED экономит до 80% энергии. Выключайте свет выходя из комнаты.";
        }
        if (nameLower.contains("плит") || nameLower.contains("духов")) {
            return "💡 Совет: Используйте посуду по размеру конфорки. Накрывайте крышкой. Выключайте за 5-10 минут до готовности.";
        }
        
        // Рекомендации по уровню потребления
        if (dailyKwh > 5) {
            return "⚠️ Внимание: Этот прибор потребляет более 5 кВт·ч в день! Это значительная нагрузка. Рассмотрите способы сокращения времени использования.";
        }
        if (dailyKwh > 2) {
            return "💡 Совет: Прибор потребляет более 2 кВт·ч в день. Старайтесь использовать его в ночное время по льготному тарифу.";
        }
        
        return null;
    }
}


