/**
 * API модуль для взаимодействия с сервером
 * Система анализа энергопотребления квартиры
 */

const API = {
    // Базовый URL API
    baseUrl: '/api',
    
    /**
     * Выполнение HTTP запроса
     * @param {string} endpoint - конечная точка API
     * @param {object} options - параметры запроса
     * @returns {Promise} - результат запроса
     */
    async request(endpoint, options = {}) {
        const url = this.baseUrl + endpoint;
        
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'same-origin'
        };
        
        const config = { ...defaultOptions, ...options };
        
        if (config.body && typeof config.body === 'object') {
            config.body = JSON.stringify(config.body);
        }
        
        try {
            const response = await fetch(url, config);
            
            // Проверка на экспорт файла
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('text/csv')) {
                return await response.text();
            }
            
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.error || 'Ошибка сервера');
            }
            
            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    },
    
    // ==========================================
    // АУТЕНТИФИКАЦИЯ
    // ==========================================
    
    auth: {
        /**
         * Вход в систему
         */
        async login(username, password) {
            return API.request('/auth/login', {
                method: 'POST',
                body: { username, password }
            });
        },
        
        /**
         * Регистрация
         */
        async register(username, email, password) {
            return API.request('/auth/register', {
                method: 'POST',
                body: { username, email, password }
            });
        },
        
        /**
         * Выход
         */
        async logout() {
            return API.request('/auth/logout', {
                method: 'POST'
            });
        },
        
        /**
         * Проверка авторизации
         */
        async check() {
            return API.request('/auth/check');
        },
        
        /**
         * Получение данных пользователя
         */
        async getUser() {
            return API.request('/auth/user');
        }
    },
    
    // ==========================================
    // ЭЛЕКТРОПРИБОРЫ
    // ==========================================
    
    appliances: {
        /**
         * Получить все приборы
         */
        async getAll() {
            return API.request('/appliances/');
        },
        
        /**
         * Получить прибор по ID
         */
        async getById(id) {
            return API.request(`/appliances/${id}`);
        },
        
        /**
         * Получить топ потребителей
         */
        async getTop() {
            return API.request('/appliances/top');
        },
        
        /**
         * Получить статистику
         */
        async getStats() {
            return API.request('/appliances/stats');
        },
        
        /**
         * Создать прибор
         */
        async create(data) {
            return API.request('/appliances/', {
                method: 'POST',
                body: data
            });
        },
        
        /**
         * Обновить прибор
         */
        async update(id, data) {
            return API.request(`/appliances/${id}`, {
                method: 'PUT',
                body: data
            });
        },
        
        /**
         * Удалить прибор
         */
        async delete(id) {
            return API.request(`/appliances/${id}`, {
                method: 'DELETE'
            });
        }
    },
    
    // ==========================================
    // ГРУППЫ
    // ==========================================
    
    groups: {
        /**
         * Получить все группы
         */
        async getAll() {
            return API.request('/groups/');
        },
        
        /**
         * Получить группу по ID
         */
        async getById(id) {
            return API.request(`/groups/${id}`);
        },
        
        /**
         * Получить приборы группы
         */
        async getAppliances(id) {
            return API.request(`/groups/${id}/appliances`);
        },
        
        /**
         * Создать группу
         */
        async create(data) {
            return API.request('/groups/', {
                method: 'POST',
                body: data
            });
        },
        
        /**
         * Обновить группу
         */
        async update(id, data) {
            return API.request(`/groups/${id}`, {
                method: 'PUT',
                body: data
            });
        },
        
        /**
         * Удалить группу
         */
        async delete(id) {
            return API.request(`/groups/${id}`, {
                method: 'DELETE'
            });
        }
    },
    
    // ==========================================
    // ТАРИФЫ
    // ==========================================
    
    tariffs: {
        /**
         * Получить все тарифы
         */
        async getAll() {
            return API.request('/tariffs/');
        },
        
        /**
         * Получить активные тарифы
         */
        async getActive() {
            return API.request('/tariffs/active');
        },
        
        /**
         * Получить текущий тариф
         */
        async getCurrent() {
            return API.request('/tariffs/current');
        },
        
        /**
         * Получить тариф по ID
         */
        async getById(id) {
            return API.request(`/tariffs/${id}`);
        },
        
        /**
         * Создать тариф
         */
        async create(data) {
            return API.request('/tariffs/', {
                method: 'POST',
                body: data
            });
        },
        
        /**
         * Обновить тариф
         */
        async update(id, data) {
            return API.request(`/tariffs/${id}`, {
                method: 'PUT',
                body: data
            });
        },
        
        /**
         * Удалить тариф
         */
        async delete(id) {
            return API.request(`/tariffs/${id}`, {
                method: 'DELETE'
            });
        }
    },
    
    // ==========================================
    // АНАЛИТИКА
    // ==========================================
    
    analytics: {
        /**
         * Получить данные для панели управления
         */
        async getDashboard() {
            return API.request('/analytics/dashboard');
        },
        
        /**
         * Получить дневную статистику
         */
        async getDaily(days = 30) {
            return API.request(`/analytics/daily?days=${days}`);
        },
        
        /**
         * Получить месячную статистику
         */
        async getMonthly() {
            return API.request('/analytics/monthly');
        },
        
        /**
         * Получить прогноз
         */
        async getForecast() {
            return API.request('/analytics/forecast');
        },
        
        /**
         * Получить рекомендации
         */
        async getRecommendations() {
            return API.request('/analytics/recommendations');
        },
        
        /**
         * Получить статистику по группам
         */
        async getGroupStats() {
            return API.request('/analytics/groups');
        },
        
        /**
         * Экспорт в CSV
         */
        async exportCSV(startDate, endDate) {
            let url = '/analytics/export';
            if (startDate && endDate) {
                url += `?startDate=${startDate}&endDate=${endDate}`;
            }
            return API.request(url);
        }
    }
};

// Экспорт для использования в других модулях
window.API = API;



