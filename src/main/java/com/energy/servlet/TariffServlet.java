package com.energy.servlet;

import com.energy.dao.TariffDAO;
import com.energy.model.Tariff;
import com.energy.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.*;

/**
 * Сервлет для управления тарифами
 */
@WebServlet(urlPatterns = {"/api/tariffs/*"})
public class TariffServlet extends HttpServlet {
    
    private TariffDAO tariffDAO;
    
    @Override
    public void init() throws ServletException {
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
                handleGetAll(userId, out);
            } else if (pathInfo.equals("/active")) {
                handleGetActive(userId, out);
            } else if (pathInfo.equals("/current")) {
                handleGetCurrent(userId, out);
            } else {
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
                out.print(JsonUtil.errorResponse("Укажите ID тарифа"));
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
                out.print(JsonUtil.errorResponse("Укажите ID тарифа"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    private void handleGetAll(int userId, PrintWriter out) throws Exception {
        List<Tariff> tariffs = tariffDAO.findByUserId(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tariff t : tariffs) {
            result.add(tariffToMap(t));
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    private void handleGetActive(int userId, PrintWriter out) throws Exception {
        List<Tariff> tariffs = tariffDAO.findActiveByUserId(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tariff t : tariffs) {
            result.add(tariffToMap(t));
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    private void handleGetCurrent(int userId, PrintWriter out) throws Exception {
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        
        Tariff tariff = tariffDAO.getTariffForHour(userId, currentHour);
        
        if (tariff != null) {
            out.print(JsonUtil.toJson(tariffToMap(tariff)));
        } else {
            Map<String, Object> defaultTariff = new HashMap<>();
            defaultTariff.put("name", "Стандартный");
            defaultTariff.put("ratePerKwh", 5.5);
            out.print(JsonUtil.toJson(defaultTariff));
        }
    }
    
    private void handleGetById(int id, PrintWriter out) throws Exception {
        Tariff tariff = tariffDAO.findById(id);
        
        if (tariff != null) {
            out.print(JsonUtil.toJson(tariffToMap(tariff)));
        } else {
            out.print(JsonUtil.errorResponse("Тариф не найден"));
        }
    }
    
    private void handleCreate(int userId, Map<String, Object> data, PrintWriter out) throws Exception {
        Tariff tariff = new Tariff();
        tariff.setUserId(userId);
        tariff.setName((String) data.get("name"));
        tariff.setTariffType((String) data.get("tariffType"));
        tariff.setRatePerKwh(((Number) data.get("ratePerKwh")).doubleValue());
        tariff.setStartHour(((Number) data.get("startHour")).intValue());
        tariff.setEndHour(((Number) data.get("endHour")).intValue());
        tariff.setActive(data.get("isActive") != null ? (Boolean) data.get("isActive") : true);
        
        String validFromStr = (String) data.get("validFrom");
        if (validFromStr != null && !validFromStr.isEmpty()) {
            tariff.setValidFrom(Date.valueOf(validFromStr));
        } else {
            tariff.setValidFrom(new Date(System.currentTimeMillis()));
        }
        
        String validToStr = (String) data.get("validTo");
        if (validToStr != null && !validToStr.isEmpty()) {
            tariff.setValidTo(Date.valueOf(validToStr));
        }
        
        Tariff created = tariffDAO.create(tariff);
        
        if (created != null) {
            out.print(JsonUtil.successResponse("Тариф создан", tariffToMap(created)));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при создании тарифа"));
        }
    }
    
    private void handleUpdate(int id, Map<String, Object> data, PrintWriter out) throws Exception {
        Tariff tariff = tariffDAO.findById(id);
        
        if (tariff == null) {
            out.print(JsonUtil.errorResponse("Тариф не найден"));
            return;
        }
        
        if (data.get("name") != null) {
            tariff.setName((String) data.get("name"));
        }
        if (data.get("tariffType") != null) {
            tariff.setTariffType((String) data.get("tariffType"));
        }
        if (data.get("ratePerKwh") != null) {
            tariff.setRatePerKwh(((Number) data.get("ratePerKwh")).doubleValue());
        }
        if (data.get("startHour") != null) {
            tariff.setStartHour(((Number) data.get("startHour")).intValue());
        }
        if (data.get("endHour") != null) {
            tariff.setEndHour(((Number) data.get("endHour")).intValue());
        }
        if (data.get("isActive") != null) {
            tariff.setActive((Boolean) data.get("isActive"));
        }
        
        if (tariffDAO.update(tariff)) {
            out.print(JsonUtil.successResponse("Тариф обновлён", tariffToMap(tariff)));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при обновлении"));
        }
    }
    
    private void handleDelete(int id, PrintWriter out) throws Exception {
        if (tariffDAO.delete(id)) {
            out.print(JsonUtil.successResponse("Тариф удалён"));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при удалении"));
        }
    }
    
    private Map<String, Object> tariffToMap(Tariff t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("tariffType", t.getTariffType());
        map.put("tariffTypeRussian", t.getTariffTypeRussian());
        map.put("ratePerKwh", t.getRatePerKwh());
        map.put("startHour", t.getStartHour());
        map.put("endHour", t.getEndHour());
        map.put("isActive", t.isActive());
        map.put("validFrom", t.getValidFrom() != null ? t.getValidFrom().toString() : null);
        map.put("validTo", t.getValidTo() != null ? t.getValidTo().toString() : null);
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
}



