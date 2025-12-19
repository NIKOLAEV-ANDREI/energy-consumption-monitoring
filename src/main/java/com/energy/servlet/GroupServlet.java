package com.energy.servlet;

import com.energy.dao.GroupDAO;
import com.energy.dao.ApplianceDAO;
import com.energy.model.ApplianceGroup;
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
 * Сервлет для управления группами приборов
 */
@WebServlet(urlPatterns = {"/api/groups/*"})
public class GroupServlet extends HttpServlet {
    
    private GroupDAO groupDAO;
    private ApplianceDAO applianceDAO;
    
    @Override
    public void init() throws ServletException {
        groupDAO = new GroupDAO();
        applianceDAO = new ApplianceDAO();
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
            } else if (pathInfo.matches("/\\d+/appliances")) {
                int groupId = Integer.parseInt(pathInfo.split("/")[1]);
                handleGetAppliances(groupId, out);
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
                out.print(JsonUtil.errorResponse("Укажите ID группы"));
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
                out.print(JsonUtil.errorResponse("Укажите ID группы"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка: " + e.getMessage()));
        }
    }
    
    private void handleGetAll(int userId, PrintWriter out) throws Exception {
        List<ApplianceGroup> groups = groupDAO.findByUserId(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ApplianceGroup g : groups) {
            result.add(groupToMap(g));
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    private void handleGetById(int id, PrintWriter out) throws Exception {
        ApplianceGroup group = groupDAO.findById(id);
        
        if (group != null) {
            out.print(JsonUtil.toJson(groupToMap(group)));
        } else {
            out.print(JsonUtil.errorResponse("Группа не найдена"));
        }
    }
    
    private void handleGetAppliances(int groupId, PrintWriter out) throws Exception {
        List<Appliance> appliances = applianceDAO.findByGroupId(groupId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appliance a : appliances) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("name", a.getName());
            map.put("powerWatts", a.getPowerWatts());
            map.put("dailyUsageHours", a.getDailyUsageHours());
            map.put("quantity", a.getQuantity());
            map.put("dailyKwh", Math.round(a.getDailyConsumptionKwh() * 1000.0) / 1000.0);
            result.add(map);
        }
        
        out.print(JsonUtil.toJsonArray(result));
    }
    
    private void handleCreate(int userId, Map<String, Object> data, PrintWriter out) throws Exception {
        ApplianceGroup group = new ApplianceGroup();
        group.setUserId(userId);
        group.setName((String) data.get("name"));
        group.setDescription((String) data.get("description"));
        
        if (data.get("icon") != null) {
            group.setIcon((String) data.get("icon"));
        }
        if (data.get("color") != null) {
            group.setColor((String) data.get("color"));
        }
        
        ApplianceGroup created = groupDAO.create(group);
        
        if (created != null) {
            out.print(JsonUtil.successResponse("Группа создана", groupToMap(created)));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при создании группы"));
        }
    }
    
    private void handleUpdate(int id, Map<String, Object> data, PrintWriter out) throws Exception {
        ApplianceGroup group = groupDAO.findById(id);
        
        if (group == null) {
            out.print(JsonUtil.errorResponse("Группа не найдена"));
            return;
        }
        
        if (data.get("name") != null) {
            group.setName((String) data.get("name"));
        }
        if (data.get("description") != null) {
            group.setDescription((String) data.get("description"));
        }
        if (data.get("icon") != null) {
            group.setIcon((String) data.get("icon"));
        }
        if (data.get("color") != null) {
            group.setColor((String) data.get("color"));
        }
        
        if (groupDAO.update(group)) {
            out.print(JsonUtil.successResponse("Группа обновлена", groupToMap(group)));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при обновлении"));
        }
    }
    
    private void handleDelete(int id, PrintWriter out) throws Exception {
        if (groupDAO.delete(id)) {
            out.print(JsonUtil.successResponse("Группа удалена"));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при удалении"));
        }
    }
    
    private Map<String, Object> groupToMap(ApplianceGroup g) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", g.getId());
        map.put("name", g.getName());
        map.put("description", g.getDescription());
        map.put("icon", g.getIcon());
        map.put("color", g.getColor());
        map.put("applianceCount", g.getApplianceCount());
        map.put("dailyKwh", Math.round(g.getTotalDailyKwh() * 100.0) / 100.0);
        map.put("monthlyKwh", Math.round(g.getTotalMonthlyKwh() * 100.0) / 100.0);
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



