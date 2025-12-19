package com.energy.servlet;

import com.energy.dao.UserDAO;
import com.energy.model.User;
import com.energy.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервлет для аутентификации пользователей
 * Обрабатывает регистрацию, вход и выход
 */
@WebServlet(urlPatterns = {"/api/auth/*"})
public class AuthServlet extends HttpServlet {
    
    private UserDAO userDAO;
    
    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            // Чтение тела запроса
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Map<String, Object> requestData = JsonUtil.parseJson(sb.toString());
            
            if ("/login".equals(pathInfo)) {
                handleLogin(request, response, requestData, out);
            } else if ("/register".equals(pathInfo)) {
                handleRegister(requestData, out);
            } else if ("/logout".equals(pathInfo)) {
                handleLogout(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(JsonUtil.errorResponse("Неизвестный путь"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if ("/check".equals(pathInfo)) {
                handleCheckAuth(request, out);
            } else if ("/user".equals(pathInfo)) {
                handleGetUser(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(JsonUtil.errorResponse("Неизвестный путь"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(JsonUtil.errorResponse("Ошибка сервера: " + e.getMessage()));
        }
    }
    
    /**
     * Обработка входа в систему
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response,
                            Map<String, Object> data, PrintWriter out) throws Exception {
        
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(JsonUtil.errorResponse("Введите имя пользователя и пароль"));
            return;
        }
        
        User user = userDAO.login(username, password);
        
        if (user != null) {
            // Создание сессии
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setMaxInactiveInterval(24 * 60 * 60); // 24 часа
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            
            out.print(JsonUtil.successResponse("Вход выполнен успешно", userData));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(JsonUtil.errorResponse("Неверное имя пользователя или пароль"));
        }
    }
    
    /**
     * Обработка регистрации
     */
    private void handleRegister(Map<String, Object> data, PrintWriter out) throws Exception {
        String username = (String) data.get("username");
        String email = (String) data.get("email");
        String password = (String) data.get("password");
        
        // Валидация
        if (username == null || username.length() < 3) {
            out.print(JsonUtil.errorResponse("Имя пользователя должно содержать минимум 3 символа"));
            return;
        }
        
        if (email == null || !email.contains("@")) {
            out.print(JsonUtil.errorResponse("Введите корректный email"));
            return;
        }
        
        if (password == null || password.length() < 6) {
            out.print(JsonUtil.errorResponse("Пароль должен содержать минимум 6 символов"));
            return;
        }
        
        // Проверка существования
        if (userDAO.exists(username, email)) {
            out.print(JsonUtil.errorResponse("Пользователь с таким именем или email уже существует"));
            return;
        }
        
        User user = userDAO.register(username, email, password);
        
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            
            out.print(JsonUtil.successResponse("Регистрация успешна! Теперь вы можете войти.", userData));
        } else {
            out.print(JsonUtil.errorResponse("Ошибка при регистрации"));
        }
    }
    
    /**
     * Обработка выхода
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        out.print(JsonUtil.successResponse("Выход выполнен успешно"));
    }
    
    /**
     * Проверка авторизации
     */
    private void handleCheckAuth(HttpServletRequest request, PrintWriter out) {
        HttpSession session = request.getSession(false);
        
        Map<String, Object> result = new HashMap<>();
        if (session != null && session.getAttribute("userId") != null) {
            result.put("authenticated", true);
            result.put("userId", session.getAttribute("userId"));
            result.put("username", session.getAttribute("username"));
        } else {
            result.put("authenticated", false);
        }
        
        out.print(JsonUtil.toJson(result));
    }
    
    /**
     * Получение данных текущего пользователя
     */
    private void handleGetUser(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws Exception {
        HttpSession session = request.getSession(false);
        
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(JsonUtil.errorResponse("Требуется авторизация"));
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        User user = userDAO.findById(userId);
        
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            
            out.print(JsonUtil.successResponse("OK", userData));
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(JsonUtil.errorResponse("Пользователь не найден"));
        }
    }
}



