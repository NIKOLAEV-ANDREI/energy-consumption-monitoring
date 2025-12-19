package com.energy.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Утилитарный класс для работы с JSON без внешних библиотек
 * Обеспечивает сериализацию и десериализацию JSON
 */
public class JsonUtil {
    
    /**
     * Преобразование Map в JSON строку
     * @param map карта для преобразования
     * @return JSON строка
     */
    public static String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            json.append(valueToJson(entry.getValue()));
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Преобразование списка Map в JSON массив
     * @param list список для преобразования
     * @return JSON строка массива
     */
    public static String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (Map<String, Object> item : list) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append(toJson(item));
        }
        
        json.append("]");
        return json.toString();
    }
    
    /**
     * Преобразование значения в JSON формат
     * @param value значение для преобразования
     * @return JSON представление значения
     */
    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return toJson(map);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) value;
            return toJsonArray(list);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }
    
    /**
     * Экранирование специальных символов JSON
     * @param str строка для экранирования
     * @return экранированная строка
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Парсинг JSON строки в Map
     * @param json JSON строка
     * @return Map с данными
     */
    public static Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }
        
        json = json.substring(1, json.length() - 1).trim();
        
        // Простой парсер для плоских JSON объектов
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|[-]?\\d+\\.?\\d*|true|false|null)");
        Matcher matcher = pattern.matcher(json);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            
            if (value.startsWith("\"") && value.endsWith("\"")) {
                // Строка - проверяем, может это число в кавычках
                String strValue = value.substring(1, value.length() - 1);
                result.put(key, strValue);
            } else if (value.equals("true")) {
                result.put(key, true);
            } else if (value.equals("false")) {
                result.put(key, false);
            } else if (value.equals("null") || value.isEmpty()) {
                result.put(key, null);
            } else if (value.contains(".")) {
                result.put(key, Double.parseDouble(value));
            } else {
                try {
                    result.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Создание JSON ответа об успехе
     * @param message сообщение
     * @return JSON строка
     */
    public static String successResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return toJson(response);
    }
    
    /**
     * Создание JSON ответа об успехе с данными
     * @param message сообщение
     * @param data данные
     * @return JSON строка
     */
    public static String successResponse(String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return toJson(response);
    }
    
    /**
     * Создание JSON ответа об ошибке
     * @param message сообщение об ошибке
     * @return JSON строка
     */
    public static String errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return toJson(response);
    }
}


