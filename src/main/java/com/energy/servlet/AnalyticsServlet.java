package com.energy.servlet;

import com.energy.dao.*;
import com.energy.model.*;
import com.energy.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.*;

/**
 * Сервлет для аналитики и отчётов
 * Предоставляет статистику, прогнозы и рекомендации
 */
@WebServlet(urlPatterns = {"/api/analytics/*"})
public class AnalyticsServlet extends HttpServlet {
    
    private ConsumptionDAO consumptionDAO;
    private ApplianceDAO applianceDAO;
    private TariffDAO tariffDAO;
    private GroupDAO groupDAO;
    
    @Override
    public void init() throws ServletException {
        consumptionDAO = new ConsumptionDAO();
        applianceDAO = new ApplianceDAO();
        tariffDAO = new TariffDAO();
        groupDAO = new GroupDAO();
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
            if ("/dashboard".equals(pathInfo)) {
                handleDashboard(userId, out);
            } else if ("/daily".equals(pathInfo)) {
                handleDailyStats(userId, request, out);
            } else if ("/monthly".equals(pathInfo)) {
                handleMonthlyStats(userId, out);
            } else if ("/forecast".equals(pathInfo)) {
                handleForecast(userId, out);
            } else if ("/recommendations".equals(pathInfo)) {
                handleRecommendations(userId, out);
            } else if ("/export".equals(pathInfo)) {
                handleExport(userId, request, response);
            } else if ("/groups".equals(pathInfo)) {
                handleGroupStats(userId, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(JsonUtil.errorResponse("Неизвестный путь"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    /**
     * Данные для главной панели
     */
    private void handleDashboard(int userId, PrintWriter out) throws Exception {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Текущее потребление
        double totalDailyKwh = applianceDAO.getTotalDailyConsumption(userId);
        double avgRate = tariffDAO.getAverageRate(userId);
        
        dashboard.put("dailyKwh", Math.round(totalDailyKwh * 100.0) / 100.0);
        dashboard.put("monthlyKwh", Math.round(totalDailyKwh * 30 * 100.0) / 100.0);
        dashboard.put("dailyCost", Math.round(totalDailyKwh * avgRate * 100.0) / 100.0);
        dashboard.put("monthlyCost", Math.round(totalDailyKwh * 30 * avgRate * 100.0) / 100.0);
        
        // Сравнение с прошлым месяцем
        double comparison = consumptionDAO.getComparisonWithPreviousMonth(userId);
        dashboard.put("comparisonPercent", Math.round(comparison * 10.0) / 10.0);
        
        // Текущий месяц из записей
        double currentMonthKwh = consumptionDAO.getCurrentMonthConsumption(userId);
        double currentMonthCost = consumptionDAO.getCurrentMonthCost(userId);
        dashboard.put("currentMonthKwh", Math.round(currentMonthKwh * 100.0) / 100.0);
        dashboard.put("currentMonthCost", Math.round(currentMonthCost * 100.0) / 100.0);
        
        // Количество приборов
        List<Appliance> appliances = applianceDAO.findByUserId(userId);
        dashboard.put("applianceCount", appliances.size());
        
        // Количество групп
        List<ApplianceGroup> groups = groupDAO.findByUserId(userId);
        dashboard.put("groupCount", groups.size());
        
        // Текущий тариф
        Calendar cal = Calendar.getInstance();
        Tariff currentTariff = tariffDAO.getTariffForHour(userId, cal.get(Calendar.HOUR_OF_DAY));
        if (currentTariff != null) {
            dashboard.put("currentTariff", currentTariff.getName());
            dashboard.put("currentRate", currentTariff.getRatePerKwh());
        }
        
        out.print(JsonUtil.toJson(dashboard));
    }
    
    /**
     * Дневная статистика
     */
    private void handleDailyStats(int userId, HttpServletRequest request, PrintWriter out) throws Exception {
        int days = 30;
        String daysParam = request.getParameter("days");
        if (daysParam != null) {
            days = Integer.parseInt(daysParam);
        }
        
        Map<String, Double> consumption = consumptionDAO.getDailyConsumption(userId, days);
        Map<String, Double> costs = consumptionDAO.getDailyCost(userId, days);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : consumption.entrySet()) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", entry.getKey());
            day.put("kwh", Math.round(entry.getValue() * 100.0) / 100.0);
            day.put("cost", Math.round(costs.getOrDefault(entry.getKey(), 0.0) * 100.0) / 100.0);
            result.add(day);
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    /**
     * Месячная статистика
     */
    private void handleMonthlyStats(int userId, PrintWriter out) throws Exception {
        List<Map<String, Object>> stats = consumptionDAO.getMonthlyStats(userId, 12);
        
        String[] monthNames = {"", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                              "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        
        for (Map<String, Object> stat : stats) {
            int month = (int) stat.get("month");
            stat.put("monthName", monthNames[month]);
        }
        
        out.print(JsonUtil.toJsonArray(stats));
    }
    
    /**
     * Прогноз потребления
     */
    private void handleForecast(int userId, PrintWriter out) throws Exception {
        Map<String, Object> forecast = new HashMap<>();
        
        // Расчёт на основе текущих приборов
        double dailyKwh = applianceDAO.getTotalDailyConsumption(userId);
        double avgRate = tariffDAO.getAverageRate(userId);
        
        // Прогноз на месяц
        Calendar cal = Calendar.getInstance();
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int remainingDays = daysInMonth - dayOfMonth;
        
        double currentMonthKwh = consumptionDAO.getCurrentMonthConsumption(userId);
        double projectedKwh = currentMonthKwh + (dailyKwh * remainingDays);
        double projectedCost = projectedKwh * avgRate;
        
        forecast.put("projectedMonthlyKwh", Math.round(projectedKwh * 100.0) / 100.0);
        forecast.put("projectedMonthlyCost", Math.round(projectedCost * 100.0) / 100.0);
        forecast.put("remainingDays", remainingDays);
        forecast.put("averageDailyKwh", Math.round(dailyKwh * 100.0) / 100.0);
        
        // Прогноз на год
        double yearlyKwh = dailyKwh * 365;
        double yearlyCost = yearlyKwh * avgRate;
        forecast.put("projectedYearlyKwh", Math.round(yearlyKwh * 100.0) / 100.0);
        forecast.put("projectedYearlyCost", Math.round(yearlyCost * 100.0) / 100.0);
        
        out.print(JsonUtil.toJson(forecast));
    }
    
    /**
     * Рекомендации по энергосбережению
     */
    private void handleRecommendations(int userId, PrintWriter out) throws Exception {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // Получаем топ потребителей
        List<Appliance> topConsumers = applianceDAO.getTopConsumers(userId, 5);
        List<Tariff> tariffs = tariffDAO.findActiveByUserId(userId);
        
        // Находим ночной тариф
        Tariff nightTariff = null;
        Tariff peakTariff = null;
        for (Tariff t : tariffs) {
            if ("night".equals(t.getTariffType())) {
                nightTariff = t;
            } else if ("peak".equals(t.getTariffType())) {
                peakTariff = t;
            }
        }
        
        // Рекомендации по топ потребителям
        for (Appliance a : topConsumers) {
            if (a.getDailyConsumptionKwh() > 2) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("type", "high_consumption");
                rec.put("appliance", a.getName());
                rec.put("dailyKwh", Math.round(a.getDailyConsumptionKwh() * 100.0) / 100.0);
                rec.put("message", "Прибор \"" + a.getName() + "\" потребляет " + 
                        String.format("%.2f", a.getDailyConsumptionKwh()) + " кВт·ч в день. " +
                        "Рассмотрите возможность сокращения времени использования.");
                rec.put("priority", "high");
                recommendations.add(rec);
            }
        }
        
        // Рекомендация по ночному тарифу
        if (nightTariff != null && peakTariff != null) {
            double savings = peakTariff.getRatePerKwh() - nightTariff.getRatePerKwh();
            Map<String, Object> rec = new HashMap<>();
            rec.put("type", "tariff_optimization");
            rec.put("message", "Используйте энергоёмкие приборы (стиральная машина, посудомойка) " +
                    "в ночное время (" + nightTariff.getStartHour() + ":00 - " + nightTariff.getEndHour() + ":00). " +
                    "Экономия: " + String.format("%.2f", savings) + " руб./кВт·ч");
            rec.put("priority", "medium");
            rec.put("savingsPerKwh", savings);
            recommendations.add(rec);
        }
        
        // Общие рекомендации по энергосбережению
        Map<String, Object> ledRec = new HashMap<>();
        ledRec.put("type", "general");
        ledRec.put("title", "Освещение");
        ledRec.put("message", "Замените лампы накаливания на LED. Экономия до 80% электроэнергии на освещение. LED-лампа 10 Вт даёт столько же света, сколько лампа накаливания 75 Вт.");
        ledRec.put("priority", "high");
        recommendations.add(ledRec);
        
        Map<String, Object> standbyRec = new HashMap<>();
        standbyRec.put("type", "general");
        standbyRec.put("title", "Режим ожидания");
        standbyRec.put("message", "Отключайте электроприборы от сети, когда не используете. Режим ожидания может потреблять до 10% от общего энергопотребления. Используйте удлинители с выключателем.");
        standbyRec.put("priority", "medium");
        recommendations.add(standbyRec);
        
        Map<String, Object> fridgeRec = new HashMap<>();
        fridgeRec.put("type", "general");
        fridgeRec.put("title", "Холодильник");
        fridgeRec.put("message", "Не ставьте холодильник рядом с плитой или батареей. Регулярно размораживайте, если нет системы No Frost. Не ставьте горячую еду в холодильник — это увеличивает расход энергии на 20%.");
        fridgeRec.put("priority", "medium");
        recommendations.add(fridgeRec);
        
        Map<String, Object> washingRec = new HashMap<>();
        washingRec.put("type", "general");
        washingRec.put("title", "Стиральная машина");
        washingRec.put("message", "Стирайте при полной загрузке барабана. Используйте режим 30-40°C вместо 60°C — экономия до 50% электроэнергии. Запускайте стирку в ночное время по льготному тарифу.");
        washingRec.put("priority", "medium");
        recommendations.add(washingRec);
        
        Map<String, Object> kettleRec = new HashMap<>();
        kettleRec.put("type", "general");
        kettleRec.put("title", "Электрочайник");
        kettleRec.put("message", "Кипятите только необходимое количество воды. Регулярно очищайте чайник от накипи — она увеличивает расход энергии на 15-20%. Рассмотрите термопот для частого использования.");
        kettleRec.put("priority", "low");
        recommendations.add(kettleRec);
        
        Map<String, Object> acRec = new HashMap<>();
        acRec.put("type", "general");
        acRec.put("title", "Кондиционер");
        acRec.put("message", "Устанавливайте температуру не ниже 24°C летом. Каждый градус ниже увеличивает расход на 5-8%. Регулярно чистите фильтры. Закрывайте окна и двери при работе кондиционера.");
        acRec.put("priority", "high");
        recommendations.add(acRec);
        
        Map<String, Object> windowRec = new HashMap<>();
        windowRec.put("type", "general");
        windowRec.put("title", "Естественное освещение");
        windowRec.put("message", "Максимально используйте дневной свет. Держите окна чистыми. Используйте светлые шторы, которые пропускают свет. Расставьте мебель так, чтобы не загораживать окна.");
        windowRec.put("priority", "low");
        recommendations.add(windowRec);
        
        Map<String, Object> heaterRec = new HashMap<>();
        heaterRec.put("type", "general");
        heaterRec.put("title", "Электрообогреватели");
        heaterRec.put("message", "Обогреватели — одни из самых энергозатратных приборов. Утеплите окна и двери. Используйте обогреватель только в одной комнате. Выключайте при выходе из дома.");
        heaterRec.put("priority", "high");
        recommendations.add(heaterRec);
        
        Map<String, Object> computerRec = new HashMap<>();
        computerRec.put("type", "general");
        computerRec.put("title", "Компьютер и ноутбук");
        computerRec.put("message", "Используйте режим энергосбережения. Выключайте монитор при перерывах более 10 минут. Ноутбук потребляет в 3-4 раза меньше настольного ПК. Отключайте зарядку после полной зарядки.");
        computerRec.put("priority", "low");
        recommendations.add(computerRec);
        
        Map<String, Object> ironRec = new HashMap<>();
        ironRec.put("type", "general");
        ironRec.put("title", "Утюг");
        ironRec.put("message", "Гладьте сразу большое количество белья. Начинайте с вещей, требующих низкой температуры. Выключайте утюг за 5-10 минут до окончания — остаточного тепла хватит для лёгких тканей.");
        ironRec.put("priority", "low");
        recommendations.add(ironRec);
        
        Map<String, Object> dishwasherRec = new HashMap<>();
        dishwasherRec.put("type", "general");
        dishwasherRec.put("title", "Посудомоечная машина");
        dishwasherRec.put("message", "Запускайте только при полной загрузке. Используйте эко-режим. Откажитесь от режима сушки — откройте дверцу и дайте посуде высохнуть естественным путём.");
        dishwasherRec.put("priority", "medium");
        recommendations.add(dishwasherRec);
        
        Map<String, Object> tvRec = new HashMap<>();
        tvRec.put("type", "general");
        tvRec.put("title", "Телевизор");
        tvRec.put("message", "Уменьшите яркость экрана — заводские настройки часто завышены. Используйте таймер автовыключения. Полностью выключайте телевизор, а не оставляйте в режиме ожидания.");
        tvRec.put("priority", "low");
        recommendations.add(tvRec);
        
        Map<String, Object> classRec = new HashMap<>();
        classRec.put("type", "general");
        classRec.put("title", "Класс энергоэффективности");
        classRec.put("message", "При покупке новой техники выбирайте класс A++ или A+++. Разница в потреблении между классом A и D может достигать 50%. Это окупится за 2-3 года эксплуатации.");
        classRec.put("priority", "medium");
        recommendations.add(classRec);
        
        out.print(JsonUtil.toJsonArray(recommendations));
    }
    
    /**
     * Экспорт данных в CSV
     */
    private void handleExport(int userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        
        Date startDate, endDate;
        if (startDateStr != null && endDateStr != null) {
            startDate = Date.valueOf(startDateStr);
            endDate = Date.valueOf(endDateStr);
        } else {
            // По умолчанию - последние 30 дней
            Calendar cal = Calendar.getInstance();
            endDate = new Date(cal.getTimeInMillis());
            cal.add(Calendar.DAY_OF_MONTH, -30);
            startDate = new Date(cal.getTimeInMillis());
        }
        
        String csv = consumptionDAO.exportToCSV(userId, startDate, endDate);
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"consumption_report.csv\"");
        response.getWriter().print(csv);
    }
    
    /**
     * Статистика по группам
     */
    private void handleGroupStats(int userId, PrintWriter out) throws Exception {
        List<ApplianceGroup> groups = groupDAO.findByUserId(userId);
        double avgRate = tariffDAO.getAverageRate(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ApplianceGroup g : groups) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("id", g.getId());
            stat.put("name", g.getName());
            stat.put("color", g.getColor());
            stat.put("applianceCount", g.getApplianceCount());
            stat.put("dailyKwh", Math.round(g.getTotalDailyKwh() * 100.0) / 100.0);
            stat.put("monthlyKwh", Math.round(g.getTotalMonthlyKwh() * 100.0) / 100.0);
            stat.put("dailyCost", Math.round(g.getTotalDailyKwh() * avgRate * 100.0) / 100.0);
            stat.put("monthlyCost", Math.round(g.getTotalMonthlyKwh() * avgRate * 100.0) / 100.0);
            result.add(stat);
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
}


