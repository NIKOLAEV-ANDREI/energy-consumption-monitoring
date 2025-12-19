-- =====================================================
-- СХЕМА БАЗЫ ДАННЫХ: Система анализа энергопотребления квартиры
-- Автор: Система генерации кода
-- Дата создания: 2025
-- =====================================================

-- Создание базы данных
CREATE DATABASE IF NOT EXISTS energy_analysis 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE energy_analysis;

-- =====================================================
-- ТАБЛИЦА: Пользователи
-- Хранит информацию о зарегистрированных пользователях
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'Имя пользователя',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'Электронная почта',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Хэш пароля (SHA-256)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Дата регистрации',
    last_login TIMESTAMP NULL COMMENT 'Последний вход',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Активен ли аккаунт'
) ENGINE=InnoDB COMMENT='Таблица пользователей системы';

-- =====================================================
-- ТАБЛИЦА: Настройки пользователя
-- Персональные настройки каждого пользователя
-- =====================================================
CREATE TABLE IF NOT EXISTS user_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    currency VARCHAR(10) DEFAULT 'руб.' COMMENT 'Валюта отображения',
    notification_enabled BOOLEAN DEFAULT TRUE COMMENT 'Уведомления включены',
    consumption_goal DECIMAL(10,2) DEFAULT 0 COMMENT 'Цель потребления кВт·ч в месяц',
    spike_threshold DECIMAL(5,2) DEFAULT 20.00 COMMENT 'Порог всплеска потребления (%)',
    theme VARCHAR(20) DEFAULT 'light' COMMENT 'Тема оформления',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Настройки пользователей';

-- =====================================================
-- ТАБЛИЦА: Группы приборов
-- Категории для группировки электроприборов
-- =====================================================
CREATE TABLE IF NOT EXISTS appliance_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT 'Название группы',
    description TEXT COMMENT 'Описание группы',
    icon VARCHAR(50) DEFAULT 'folder' COMMENT 'Иконка группы',
    color VARCHAR(7) DEFAULT '#3498db' COMMENT 'Цвет группы (HEX)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_group_name (user_id, name)
) ENGINE=InnoDB COMMENT='Группы электроприборов';

-- =====================================================
-- ТАБЛИЦА: Электроприборы
-- Информация о всех электроприборах пользователя
-- =====================================================
CREATE TABLE IF NOT EXISTS appliances (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    group_id INT NULL COMMENT 'ID группы (может быть NULL)',
    name VARCHAR(100) NOT NULL COMMENT 'Название прибора',
    power_watts DECIMAL(10,2) NOT NULL COMMENT 'Мощность в ваттах',
    daily_usage_hours DECIMAL(5,2) NOT NULL COMMENT 'Среднее время работы в день (часы)',
    quantity INT DEFAULT 1 COMMENT 'Количество приборов',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Прибор активен',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES appliance_groups(id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Электроприборы пользователей';

-- =====================================================
-- ТАБЛИЦА: Тарифы
-- Многозонные тарифы на электроэнергию
-- =====================================================
CREATE TABLE IF NOT EXISTS tariffs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT 'Название тарифа',
    tariff_type ENUM('peak', 'night', 'shoulder', 'flat') NOT NULL COMMENT 'Тип тарифа',
    rate_per_kwh DECIMAL(10,4) NOT NULL COMMENT 'Стоимость за кВт·ч',
    start_hour INT NOT NULL COMMENT 'Час начала действия (0-23)',
    end_hour INT NOT NULL COMMENT 'Час окончания действия (0-23)',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Тариф активен',
    valid_from DATE NOT NULL COMMENT 'Дата начала действия',
    valid_to DATE NULL COMMENT 'Дата окончания действия',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Тарифы на электроэнергию';

-- =====================================================
-- ТАБЛИЦА: Записи потребления
-- История потребления электроэнергии
-- =====================================================
CREATE TABLE IF NOT EXISTS consumption_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    appliance_id INT NULL COMMENT 'ID прибора (NULL для общих записей)',
    record_date DATE NOT NULL COMMENT 'Дата записи',
    consumption_kwh DECIMAL(10,4) NOT NULL COMMENT 'Потребление в кВт·ч',
    cost DECIMAL(10,2) NOT NULL COMMENT 'Стоимость',
    tariff_type ENUM('peak', 'night', 'shoulder', 'flat') COMMENT 'Тип тарифа',
    usage_hours DECIMAL(5,2) COMMENT 'Часы использования',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (appliance_id) REFERENCES appliances(id) ON DELETE SET NULL,
    INDEX idx_user_date (user_id, record_date)
) ENGINE=InnoDB COMMENT='Записи потребления электроэнергии';

-- =====================================================
-- ТАБЛИЦА: Расписание использования приборов
-- Планируемое время использования приборов
-- =====================================================
CREATE TABLE IF NOT EXISTS appliance_schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    appliance_id INT NOT NULL,
    day_of_week TINYINT NOT NULL COMMENT 'День недели (1-7, 1=Понедельник)',
    start_time TIME NOT NULL COMMENT 'Время начала',
    end_time TIME NOT NULL COMMENT 'Время окончания',
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (appliance_id) REFERENCES appliances(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Расписание использования приборов';

-- =====================================================
-- ТАБЛИЦА: Уведомления
-- Системные уведомления для пользователей
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(200) NOT NULL COMMENT 'Заголовок уведомления',
    message TEXT NOT NULL COMMENT 'Текст уведомления',
    type ENUM('info', 'warning', 'success', 'error') DEFAULT 'info',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Уведомления пользователей';

-- =====================================================
-- ТАБЛИЦА: Сессии
-- Управление сессиями пользователей
-- =====================================================
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY COMMENT 'ID сессии',
    user_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL COMMENT 'Время истечения сессии',
    ip_address VARCHAR(45) COMMENT 'IP адрес',
    user_agent TEXT COMMENT 'User Agent браузера',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Сессии пользователей';

-- =====================================================
-- ТАБЛИЦА: Резервные копии
-- Метаданные резервных копий
-- =====================================================
CREATE TABLE IF NOT EXISTS backups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    filename VARCHAR(255) NOT NULL COMMENT 'Имя файла бэкапа',
    file_size BIGINT COMMENT 'Размер файла в байтах',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Резервные копии данных';

-- =====================================================
-- НАЧАЛЬНЫЕ ДАННЫЕ
-- =====================================================

-- Тестовый пользователь (пароль: test123)
-- Хэш SHA-256 для "test123": ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae
INSERT INTO users (username, email, password_hash) VALUES 
('demo', 'demo@example.com', 'ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae');

-- Настройки для тестового пользователя
INSERT INTO user_settings (user_id, consumption_goal) VALUES (1, 300);

-- Стандартные группы приборов
INSERT INTO appliance_groups (user_id, name, description, icon, color) VALUES 
(1, 'Кухня', 'Кухонные электроприборы', 'kitchen', '#e74c3c'),
(1, 'Гостиная', 'Приборы в гостиной', 'living-room', '#3498db'),
(1, 'Спальня', 'Приборы в спальне', 'bedroom', '#9b59b6'),
(1, 'Ванная', 'Приборы в ванной комнате', 'bathroom', '#1abc9c'),
(1, 'Освещение', 'Осветительные приборы', 'lightbulb', '#f1c40f');

-- Примеры электроприборов
INSERT INTO appliances (user_id, group_id, name, power_watts, daily_usage_hours, quantity) VALUES 
(1, 1, 'Холодильник', 150, 24, 1),
(1, 1, 'Электрочайник', 2000, 0.5, 1),
(1, 1, 'Микроволновая печь', 1000, 0.3, 1),
(1, 1, 'Посудомоечная машина', 1800, 1, 1),
(1, 2, 'Телевизор', 100, 4, 1),
(1, 2, 'Кондиционер', 2500, 6, 1),
(1, 2, 'Игровая приставка', 200, 2, 1),
(1, 3, 'Ноутбук', 65, 8, 1),
(1, 3, 'Настольная лампа', 40, 3, 1),
(1, 4, 'Стиральная машина', 2000, 1, 1),
(1, 4, 'Фен', 1500, 0.2, 1),
(1, 5, 'Люстра LED', 50, 5, 3),
(1, 5, 'Бра', 20, 3, 4);

-- Стандартные тарифы (московские тарифы 2024)
INSERT INTO tariffs (user_id, name, tariff_type, rate_per_kwh, start_hour, end_hour, valid_from) VALUES 
(1, 'Пиковый тариф', 'peak', 7.47, 7, 10, '2024-01-01'),
(1, 'Пиковый тариф (вечер)', 'peak', 7.47, 17, 21, '2024-01-01'),
(1, 'Ночной тариф', 'night', 2.74, 23, 7, '2024-01-01'),
(1, 'Полупиковый тариф', 'shoulder', 5.58, 10, 17, '2024-01-01'),
(1, 'Полупиковый тариф (вечер)', 'shoulder', 5.58, 21, 23, '2024-01-01');

-- Примеры записей потребления за последние 30 дней
-- (генерируем для демонстрации аналитики)
INSERT INTO consumption_records (user_id, appliance_id, record_date, consumption_kwh, cost, tariff_type, usage_hours)
SELECT 
    1,
    a.id,
    DATE_SUB(CURDATE(), INTERVAL n.num DAY),
    ROUND((a.power_watts * a.daily_usage_hours * a.quantity / 1000) * (0.8 + RAND() * 0.4), 4),
    ROUND((a.power_watts * a.daily_usage_hours * a.quantity / 1000) * 5.5 * (0.8 + RAND() * 0.4), 2),
    'shoulder',
    ROUND(a.daily_usage_hours * (0.8 + RAND() * 0.4), 2)
FROM appliances a
CROSS JOIN (
    SELECT 0 AS num UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
    UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
    UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
) n
WHERE a.user_id = 1;

-- Приветственное уведомление
INSERT INTO notifications (user_id, title, message, type) VALUES 
(1, 'Добро пожаловать!', 'Добро пожаловать в систему анализа энергопотребления. Начните с добавления ваших электроприборов.', 'success');

-- =====================================================
-- ИНДЕКСЫ ДЛЯ ОПТИМИЗАЦИИ
-- =====================================================
CREATE INDEX idx_appliances_user ON appliances(user_id);
CREATE INDEX idx_appliances_group ON appliances(group_id);
CREATE INDEX idx_tariffs_user ON tariffs(user_id);
CREATE INDEX idx_consumption_date ON consumption_records(record_date);
CREATE INDEX idx_sessions_expires ON sessions(expires_at);

-- =====================================================
-- ПРЕДСТАВЛЕНИЯ (VIEWS)
-- =====================================================

-- Представление: Суммарное потребление по группам
CREATE OR REPLACE VIEW v_group_consumption AS
SELECT 
    ag.id AS group_id,
    ag.user_id,
    ag.name AS group_name,
    ag.color,
    COUNT(a.id) AS appliance_count,
    COALESCE(SUM(a.power_watts * a.daily_usage_hours * a.quantity / 1000), 0) AS daily_kwh,
    COALESCE(SUM(a.power_watts * a.daily_usage_hours * a.quantity / 1000 * 30), 0) AS monthly_kwh
FROM appliance_groups ag
LEFT JOIN appliances a ON ag.id = a.group_id AND a.is_active = TRUE
GROUP BY ag.id, ag.user_id, ag.name, ag.color;

-- Представление: Месячная статистика потребления
CREATE OR REPLACE VIEW v_monthly_stats AS
SELECT 
    user_id,
    YEAR(record_date) AS year,
    MONTH(record_date) AS month,
    SUM(consumption_kwh) AS total_kwh,
    SUM(cost) AS total_cost,
    AVG(consumption_kwh) AS avg_daily_kwh,
    COUNT(DISTINCT record_date) AS days_recorded
FROM consumption_records
GROUP BY user_id, YEAR(record_date), MONTH(record_date);

COMMIT;



