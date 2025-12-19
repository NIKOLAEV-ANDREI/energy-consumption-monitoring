package com.energy.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Класс для управления подключением к базе данных MySQL
 * Реализует паттерн Singleton для единственного подключения
 */
public class DatabaseConnection {
    
    // Параметры подключения к базе данных
    private static final String URL = "jdbc:mysql://localhost:3307/energy_analysis?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "0000"; // Укажите ваш пароль MySQL
    
    private static Connection connection = null;
    
    /**
     * Приватный конструктор для предотвращения создания экземпляров
     */
    private DatabaseConnection() {}
    
    /**
     * Получение подключения к базе данных
     * @return объект Connection для работы с БД
     * @throws SQLException при ошибке подключения
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Загрузка драйвера MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Драйвер MySQL не найден: " + e.getMessage());
            }
        }
        return connection;
    }
    
    /**
     * Закрытие подключения к базе данных
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
            }
        }
    }
    
    /**
     * Создание нового подключения (для многопоточности)
     * @return новое подключение к БД
     * @throws SQLException при ошибке подключения
     */
    public static Connection createNewConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер MySQL не найден: " + e.getMessage());
        }
    }
}


