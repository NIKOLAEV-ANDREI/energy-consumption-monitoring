/**
 * Главный модуль приложения
 * Система анализа энергопотребления квартиры
 */

const App = {
    // Текущее состояние
    state: {
        user: null,
        currentSection: 'dashboard',
        appliances: [],
        groups: [],
        tariffs: [],
        dashboardData: null
    },
    
    /**
     * Инициализация приложения
     */
    async init() {
        try {
            // Проверка авторизации
            const authData = await API.auth.check();
            
            if (authData.authenticated) {
                this.state.user = {
                    id: authData.userId,
                    username: authData.username
                };
                this.showApp();
            } else {
                this.showAuth();
            }
        } catch (error) {
            console.error('Ошибка инициализации:', error);
            this.showAuth();
        } finally {
            this.hideLoading();
        }
        
        this.setupEventListeners();
    },
    
    /**
     * Скрытие экрана загрузки
     */
    hideLoading() {
        const loading = document.getElementById('loading-screen');
        if (loading) {
            loading.style.opacity = '0';
            setTimeout(() => loading.classList.add('hidden'), 300);
        }
    },
    
    /**
     * Показать страницу авторизации
     */
    showAuth() {
        document.getElementById('auth-page').classList.remove('hidden');
        document.getElementById('app-page').classList.add('hidden');
    },
    
    /**
     * Показать главное приложение
     */
    async showApp() {
        document.getElementById('auth-page').classList.add('hidden');
        document.getElementById('app-page').classList.remove('hidden');
        
        document.getElementById('user-name').textContent = this.state.user.username;
        
        // Загрузка данных
        await this.loadDashboard();
    },
    
    /**
     * Настройка обработчиков событий
     */
    setupEventListeners() {
        // Авторизация
        document.getElementById('login-btn').addEventListener('click', () => this.handleLogin());
        document.getElementById('register-btn').addEventListener('click', () => this.handleRegister());
        document.getElementById('show-register').addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('login-form').classList.add('hidden');
            document.getElementById('register-form').classList.remove('hidden');
        });
        document.getElementById('show-login').addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('register-form').classList.add('hidden');
            document.getElementById('login-form').classList.remove('hidden');
        });
        document.getElementById('logout-btn').addEventListener('click', () => this.handleLogout());
        
        // Enter для форм
        document.getElementById('login-password').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleLogin();
        });
        document.getElementById('register-confirm').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleRegister();
        });
        
        // Навигация
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const section = item.dataset.section;
                this.navigateTo(section);
            });
        });
        
        // Кнопки добавления
        document.getElementById('add-appliance-btn').addEventListener('click', () => this.showApplianceModal());
        document.getElementById('add-group-btn').addEventListener('click', () => this.showGroupModal());
        document.getElementById('add-tariff-btn').addEventListener('click', () => this.showTariffModal());
        
        // Модальное окно
        document.getElementById('modal-close').addEventListener('click', () => this.closeModal());
        document.getElementById('modal-overlay').addEventListener('click', (e) => {
            if (e.target === document.getElementById('modal-overlay')) {
                this.closeModal();
            }
        });
        
        // Фильтры приборов
        document.getElementById('filter-group').addEventListener('change', () => this.filterAppliances());
        document.getElementById('search-appliance').addEventListener('input', () => this.filterAppliances());
        
    },
    
    /**
     * Обработка входа
     */
    async handleLogin() {
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;
        
        if (!username || !password) {
            this.showAuthError('Введите имя пользователя и пароль');
            return;
        }
        
        try {
            const result = await API.auth.login(username, password);
            
            if (result.success) {
                this.state.user = result.data;
                this.showApp();
                this.notify('Добро пожаловать, ' + result.data.username + '!', 'success');
            }
        } catch (error) {
            this.showAuthError(error.message);
        }
    },
    
    /**
     * Обработка регистрации
     */
    async handleRegister() {
        const username = document.getElementById('register-username').value.trim();
        const email = document.getElementById('register-email').value.trim();
        const password = document.getElementById('register-password').value;
        const confirm = document.getElementById('register-confirm').value;
        
        if (!username || !email || !password) {
            this.showAuthError('Заполните все поля');
            return;
        }
        
        if (password !== confirm) {
            this.showAuthError('Пароли не совпадают');
            return;
        }
        
        try {
            const result = await API.auth.register(username, email, password);
            
            if (result.success) {
                this.notify(result.message, 'success');
                document.getElementById('register-form').classList.add('hidden');
                document.getElementById('login-form').classList.remove('hidden');
                document.getElementById('login-username').value = username;
            }
        } catch (error) {
            this.showAuthError(error.message);
        }
    },
    
    /**
     * Обработка выхода
     */
    async handleLogout() {
        try {
            await API.auth.logout();
            this.state.user = null;
            this.showAuth();
            this.notify('Вы вышли из системы', 'info');
        } catch (error) {
            console.error('Ошибка выхода:', error);
        }
    },
    
    /**
     * Показать ошибку авторизации
     */
    showAuthError(message) {
        const errorEl = document.getElementById('auth-error');
        errorEl.textContent = message;
        errorEl.classList.remove('hidden');
        setTimeout(() => errorEl.classList.add('hidden'), 5000);
    },
    
    /**
     * Навигация между разделами
     */
    async navigateTo(section) {
        // Обновление навигации
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.toggle('active', item.dataset.section === section);
        });
        
        // Скрытие всех секций
        document.querySelectorAll('.section').forEach(s => s.classList.add('hidden'));
        
        // Показ нужной секции
        const sectionEl = document.getElementById(section + '-section');
        if (sectionEl) {
            sectionEl.classList.remove('hidden');
        }
        
        this.state.currentSection = section;
        
        // Загрузка данных для секции
        switch (section) {
            case 'dashboard':
                await this.loadDashboard();
                break;
            case 'appliances':
                await this.loadAppliances();
                break;
            case 'groups':
                await this.loadGroups();
                break;
            case 'tariffs':
                await this.loadTariffs();
                break;
            case 'recommendations':
                // Статичные рекомендации уже в HTML
                break;
        }
    },
    
    /**
     * Загрузка данных панели управления
     */
    async loadDashboard() {
        try {
            const [dashboard, topConsumers, groupStats, forecast, currentTariff] = await Promise.all([
                API.analytics.getDashboard(),
                API.appliances.getTop(),
                API.analytics.getGroupStats(),
                API.analytics.getForecast(),
                API.tariffs.getCurrent()
            ]);
            
            this.state.dashboardData = dashboard;
            
            // Обновление статистики
            document.getElementById('daily-kwh').textContent = dashboard.dailyKwh.toFixed(2);
            document.getElementById('monthly-kwh').textContent = dashboard.monthlyKwh.toFixed(2);
            document.getElementById('daily-cost').textContent = dashboard.dailyCost.toFixed(2);
            document.getElementById('monthly-cost').textContent = dashboard.monthlyCost.toFixed(2);
            
            // Текущий тариф
            document.getElementById('current-tariff-info').innerHTML = 
                `Текущий тариф: <strong>${currentTariff.name}</strong> (${currentTariff.ratePerKwh} руб./кВт·ч)`;
            
            // Топ потребителей
            this.renderTopConsumers(topConsumers);
            
            // Прогноз
            this.renderForecast(forecast);
            
            // Графики
            await this.loadDashboardCharts(groupStats);
            
        } catch (error) {
            console.error('Ошибка загрузки панели:', error);
            this.notify('Ошибка загрузки данных', 'error');
        }
    },
    
    /**
     * Загрузка графиков панели управления
     */
    async loadDashboardCharts(groupStats) {
        try {
            // График потребления за 7 дней
            const dailyData = await API.analytics.getDaily(7);
            const labels = dailyData.map(d => {
                const date = new Date(d.date);
                return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
            });
            const values = dailyData.map(d => d.kwh);
            
            Charts.line('consumption-chart', labels, values, { color: '#0ea5e9' });
            
            // График по группам
            const groupLabels = groupStats.map(g => g.name);
            const groupValues = groupStats.map(g => g.dailyKwh);
            const groupColors = groupStats.map(g => g.color);
            
            Charts.pie('groups-chart', groupLabels, groupValues, groupColors);
            
        } catch (error) {
            console.error('Ошибка загрузки графиков:', error);
        }
    },
    
    /**
     * Отрисовка топ потребителей
     */
    renderTopConsumers(consumers) {
        const container = document.getElementById('top-consumers');
        
        if (consumers.length === 0) {
            container.innerHTML = '<p class="text-muted">Нет данных о приборах</p>';
            return;
        }
        
        container.innerHTML = consumers.map((c, i) => `
            <div class="top-consumer-item">
                <div class="top-consumer-rank">${i + 1}</div>
                <div class="top-consumer-info">
                    <div class="top-consumer-name">${c.name}</div>
                    <div class="top-consumer-group">${c.groupName || 'Без группы'}</div>
                </div>
                <div class="top-consumer-value">
                    <div class="top-consumer-kwh">${c.dailyKwh.toFixed(2)} кВт·ч/день</div>
                    <div class="top-consumer-cost">${c.dailyCost.toFixed(2)} руб./день</div>
                </div>
            </div>
        `).join('');
    },
    
    /**
     * Отрисовка прогноза
     */
    renderForecast(forecast) {
        const container = document.getElementById('forecast-info');
        
        container.innerHTML = `
            <div class="forecast-item">
                <span class="forecast-label">Прогноз на месяц</span>
                <span class="forecast-value highlight">${forecast.projectedMonthlyKwh.toFixed(2)} кВт·ч</span>
            </div>
            <div class="forecast-item">
                <span class="forecast-label">Ожидаемая стоимость</span>
                <span class="forecast-value">${forecast.projectedMonthlyCost.toFixed(2)} руб.</span>
            </div>
            <div class="forecast-item">
                <span class="forecast-label">Среднее в день</span>
                <span class="forecast-value">${forecast.averageDailyKwh.toFixed(2)} кВт·ч</span>
            </div>
            <div class="forecast-item">
                <span class="forecast-label">Прогноз на год</span>
                <span class="forecast-value">${forecast.projectedYearlyKwh.toFixed(2)} кВт·ч</span>
            </div>
        `;
    },
    
    /**
     * Загрузка приборов
     */
    async loadAppliances() {
        try {
            const [appliances, groups] = await Promise.all([
                API.appliances.getAll(),
                API.groups.getAll()
            ]);
            
            this.state.appliances = appliances;
            this.state.groups = groups;
            
            // Обновление фильтра групп
            const filterSelect = document.getElementById('filter-group');
            filterSelect.innerHTML = '<option value="">Все группы</option>' +
                groups.map(g => `<option value="${g.id}">${g.name}</option>`).join('');
            
            this.renderAppliances(appliances);
            
        } catch (error) {
            console.error('Ошибка загрузки приборов:', error);
            this.notify('Ошибка загрузки приборов', 'error');
        }
    },
    
    /**
     * Отрисовка приборов
     */
    renderAppliances(appliances) {
        const container = document.getElementById('appliances-grid');
        
        if (appliances.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <p>У вас пока нет приборов</p>
                    <button class="btn btn-primary" onclick="App.showApplianceModal()">+ Добавить прибор</button>
                </div>
            `;
            return;
        }
        
        container.innerHTML = appliances.map(a => `
            <div class="appliance-card" data-id="${a.id}">
                <div class="appliance-header">
                    <div>
                        <div class="appliance-name">${a.name}</div>
                        <div class="appliance-group">${a.groupName || 'Без группы'}</div>
                    </div>
                    <div class="appliance-actions">
                        <button onclick="App.showApplianceModal(${a.id})" title="Редактировать">✏️</button>
                        <button class="delete" onclick="App.deleteAppliance(${a.id})" title="Удалить">🗑️</button>
                    </div>
                </div>
                <div class="appliance-stats">
                    <div class="appliance-stat">
                        <span class="appliance-stat-label">Мощность</span>
                        <span class="appliance-stat-value">${a.powerWatts} Вт</span>
                    </div>
                    <div class="appliance-stat">
                        <span class="appliance-stat-label">Часов/день</span>
                        <span class="appliance-stat-value">${a.dailyUsageHours} ч</span>
                    </div>
                    <div class="appliance-stat">
                        <span class="appliance-stat-label">кВт·ч/день</span>
                        <span class="appliance-stat-value">${a.dailyKwh.toFixed(3)}</span>
                    </div>
                    <div class="appliance-stat">
                        <span class="appliance-stat-label">Руб./месяц</span>
                        <span class="appliance-stat-value">${a.monthlyCost.toFixed(2)}</span>
                    </div>
                </div>
            </div>
        `).join('');
    },
    
    /**
     * Фильтрация приборов
     */
    filterAppliances() {
        const groupId = document.getElementById('filter-group').value;
        const search = document.getElementById('search-appliance').value.toLowerCase();
        
        let filtered = this.state.appliances;
        
        if (groupId) {
            filtered = filtered.filter(a => a.groupId == groupId);
        }
        
        if (search) {
            filtered = filtered.filter(a => a.name.toLowerCase().includes(search));
        }
        
        this.renderAppliances(filtered);
    },
    
    /**
     * Модальное окно прибора
     */
    async showApplianceModal(id = null) {
        const isEdit = id !== null;
        let appliance = null;
        
        if (isEdit) {
            appliance = this.state.appliances.find(a => a.id === id);
        }
        
        const groups = this.state.groups.length > 0 ? this.state.groups : await API.groups.getAll();
        
        document.getElementById('modal-title').textContent = isEdit ? 'Редактировать прибор' : 'Добавить прибор';
        
        document.getElementById('modal-body').innerHTML = `
            <form class="modal-form" id="appliance-form">
                <div class="form-group">
                    <label>Название прибора</label>
                    <input type="text" id="appliance-name" value="${appliance?.name || ''}" placeholder="Например: Холодильник" required>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Мощность (Вт)</label>
                        <input type="number" id="appliance-power" value="${appliance?.powerWatts || ''}" placeholder="150" required min="1">
                    </div>
                    <div class="form-group">
                        <label>Часов в день</label>
                        <input type="number" id="appliance-hours" value="${appliance?.dailyUsageHours || ''}" placeholder="8" required min="0" max="24" step="0.1">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Количество</label>
                        <input type="number" id="appliance-quantity" value="${appliance?.quantity || 1}" min="1">
                    </div>
                    <div class="form-group">
                        <label>Группа</label>
                        <select id="appliance-group">
                            <option value="">Без группы</option>
                            ${groups.map(g => `<option value="${g.id}" ${appliance?.groupId == g.id ? 'selected' : ''}>${g.name}</option>`).join('')}
                        </select>
                    </div>
                </div>
            </form>
        `;
        
        document.getElementById('modal-footer').innerHTML = `
            <button class="btn btn-secondary" onclick="App.closeModal()">Отмена</button>
            <button class="btn btn-primary" onclick="App.saveAppliance(${id})">${isEdit ? 'Сохранить' : 'Добавить'}</button>
        `;
        
        this.openModal();
    },
    
    /**
     * Сохранение прибора
     */
    async saveAppliance(id) {
        const data = {
            name: document.getElementById('appliance-name').value.trim(),
            powerWatts: parseFloat(document.getElementById('appliance-power').value),
            dailyUsageHours: parseFloat(document.getElementById('appliance-hours').value),
            quantity: parseInt(document.getElementById('appliance-quantity').value) || 1,
            groupId: document.getElementById('appliance-group').value || null
        };
        
        if (!data.name || !data.powerWatts || data.dailyUsageHours === undefined) {
            this.notify('Заполните все обязательные поля', 'error');
            return;
        }
        
        try {
            if (id) {
                await API.appliances.update(id, data);
                this.notify('Прибор обновлён', 'success');
            } else {
                const result = await API.appliances.create(data);
                this.notify('Прибор добавлен', 'success');
                
                // Показываем рекомендацию если есть
                if (result.data && result.data.tip) {
                    setTimeout(() => {
                        this.showTipNotification(result.data.tip);
                    }, 1000);
                }
            }
            
            this.closeModal();
            await this.loadAppliances();
            
        } catch (error) {
            this.notify('Ошибка сохранения: ' + error.message, 'error');
        }
    },
    
    /**
     * Удаление прибора
     */
    async deleteAppliance(id) {
        if (!confirm('Удалить этот прибор?')) return;
        
        try {
            await API.appliances.delete(id);
            this.notify('Прибор удалён', 'success');
            await this.loadAppliances();
        } catch (error) {
            this.notify('Ошибка удаления: ' + error.message, 'error');
        }
    },
    
    /**
     * Загрузка групп
     */
    async loadGroups() {
        try {
            const groups = await API.groups.getAll();
            this.state.groups = groups;
            this.renderGroups(groups);
        } catch (error) {
            console.error('Ошибка загрузки групп:', error);
            this.notify('Ошибка загрузки групп', 'error');
        }
    },
    
    /**
     * Отрисовка групп
     */
    renderGroups(groups) {
        const container = document.getElementById('groups-grid');
        
        if (groups.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <p>У вас пока нет групп</p>
                    <button class="btn btn-primary" onclick="App.showGroupModal()">+ Создать группу</button>
                </div>
            `;
            return;
        }
        
        container.innerHTML = groups.map(g => `
            <div class="group-card" style="--group-color: ${g.color}">
                <div class="group-header">
                    <div>
                        <div class="group-name">${g.name}</div>
                        <div class="group-description">${g.description || ''}</div>
                    </div>
                    <div class="appliance-actions">
                        <button onclick="App.showGroupModal(${g.id})" title="Редактировать">✏️</button>
                        <button class="delete" onclick="App.deleteGroup(${g.id})" title="Удалить">🗑️</button>
                    </div>
                </div>
                <div class="group-stats">
                    <div class="group-stat">
                        <div class="group-stat-value">${g.applianceCount}</div>
                        <div class="group-stat-label">Приборов</div>
                    </div>
                    <div class="group-stat">
                        <div class="group-stat-value">${g.dailyKwh.toFixed(2)}</div>
                        <div class="group-stat-label">кВт·ч/день</div>
                    </div>
                    <div class="group-stat">
                        <div class="group-stat-value">${g.monthlyKwh.toFixed(2)}</div>
                        <div class="group-stat-label">кВт·ч/месяц</div>
                    </div>
                </div>
            </div>
        `).join('');
    },
    
    /**
     * Модальное окно группы
     */
    async showGroupModal(id = null) {
        const isEdit = id !== null;
        let group = null;
        
        if (isEdit) {
            group = this.state.groups.find(g => g.id === id);
        }
        
        const colors = ['#e74c3c', '#3498db', '#9b59b6', '#1abc9c', '#f1c40f', '#e67e22', '#2ecc71', '#34495e'];
        
        document.getElementById('modal-title').textContent = isEdit ? 'Редактировать группу' : 'Создать группу';
        
        document.getElementById('modal-body').innerHTML = `
            <form class="modal-form" id="group-form">
                <div class="form-group">
                    <label>Название группы</label>
                    <input type="text" id="group-name" value="${group?.name || ''}" placeholder="Например: Кухня" required>
                </div>
                <div class="form-group">
                    <label>Описание</label>
                    <input type="text" id="group-description" value="${group?.description || ''}" placeholder="Кухонные электроприборы">
                </div>
                <div class="form-group">
                    <label>Цвет</label>
                    <div class="color-picker">
                        ${colors.map(c => `
                            <div class="color-option ${group?.color === c ? 'selected' : ''}" 
                                 style="background: ${c}" 
                                 data-color="${c}"
                                 onclick="App.selectColor('${c}')"></div>
                        `).join('')}
                    </div>
                    <input type="hidden" id="group-color" value="${group?.color || colors[0]}">
                </div>
            </form>
        `;
        
        document.getElementById('modal-footer').innerHTML = `
            <button class="btn btn-secondary" onclick="App.closeModal()">Отмена</button>
            <button class="btn btn-primary" onclick="App.saveGroup(${id})">${isEdit ? 'Сохранить' : 'Создать'}</button>
        `;
        
        this.openModal();
    },
    
    /**
     * Выбор цвета
     */
    selectColor(color) {
        document.querySelectorAll('.color-option').forEach(el => {
            el.classList.toggle('selected', el.dataset.color === color);
        });
        document.getElementById('group-color').value = color;
    },
    
    /**
     * Сохранение группы
     */
    async saveGroup(id) {
        const data = {
            name: document.getElementById('group-name').value.trim(),
            description: document.getElementById('group-description').value.trim(),
            color: document.getElementById('group-color').value
        };
        
        if (!data.name) {
            this.notify('Введите название группы', 'error');
            return;
        }
        
        try {
            if (id) {
                await API.groups.update(id, data);
                this.notify('Группа обновлена', 'success');
            } else {
                await API.groups.create(data);
                this.notify('Группа создана', 'success');
            }
            
            this.closeModal();
            await this.loadGroups();
            
        } catch (error) {
            this.notify('Ошибка сохранения: ' + error.message, 'error');
        }
    },
    
    /**
     * Удаление группы
     */
    async deleteGroup(id) {
        if (!confirm('Удалить эту группу? Приборы останутся без группы.')) return;
        
        try {
            await API.groups.delete(id);
            this.notify('Группа удалена', 'success');
            await this.loadGroups();
        } catch (error) {
            this.notify('Ошибка удаления: ' + error.message, 'error');
        }
    },
    
    /**
     * Загрузка тарифов
     */
    async loadTariffs() {
        try {
            const tariffs = await API.tariffs.getAll();
            this.state.tariffs = tariffs;
            this.renderTariffs(tariffs);
        } catch (error) {
            console.error('Ошибка загрузки тарифов:', error);
            this.notify('Ошибка загрузки тарифов', 'error');
        }
    },
    
    /**
     * Отрисовка тарифов
     */
    renderTariffs(tariffs) {
        const container = document.getElementById('tariffs-list');
        
        if (tariffs.length === 0) {
            container.innerHTML = '<p>Нет настроенных тарифов</p>';
            return;
        }
        
        container.innerHTML = tariffs.map(t => `
            <div class="tariff-card">
                <div class="tariff-info">
                    <div>
                        <span class="tariff-name">${t.name}</span>
                        <span class="tariff-type ${t.tariffType}">${t.tariffTypeRussian}</span>
                    </div>
                    <div class="tariff-details">
                        <span>⏰ ${t.startHour}:00 - ${t.endHour}:00</span>
                        <span>📅 с ${t.validFrom}</span>
                    </div>
                </div>
                <div class="tariff-rate">
                    ${t.ratePerKwh.toFixed(2)} <span>руб./кВт·ч</span>
                </div>
                <div class="appliance-actions">
                    <button onclick="App.showTariffModal(${t.id})" title="Редактировать">✏️</button>
                    <button class="delete" onclick="App.deleteTariff(${t.id})" title="Удалить">🗑️</button>
                </div>
            </div>
        `).join('');
    },
    
    /**
     * Модальное окно тарифа
     */
    async showTariffModal(id = null) {
        const isEdit = id !== null;
        let tariff = null;
        
        if (isEdit) {
            tariff = this.state.tariffs.find(t => t.id === id);
        }
        
        // Форматирование даты в dd/mm/yyyy
        const formatDateForInput = (dateStr) => {
            if (!dateStr) {
                const now = new Date();
                return `${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`;
            }
            const parts = dateStr.split('-');
            return `${parts[2]}/${parts[1]}/${parts[0]}`;
        };
        
        document.getElementById('modal-title').textContent = isEdit ? 'Редактировать тариф' : 'Добавить тариф';
        
        document.getElementById('modal-body').innerHTML = `
            <form class="modal-form" id="tariff-form">
                <div class="form-group">
                    <label>Название тарифа</label>
                    <input type="text" id="tariff-name" value="${tariff?.name || ''}" placeholder="Пиковый тариф" required>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Тип тарифа</label>
                        <select id="tariff-type" required>
                            <option value="peak" ${tariff?.tariffType === 'peak' ? 'selected' : ''}>Пиковый</option>
                            <option value="shoulder" ${tariff?.tariffType === 'shoulder' ? 'selected' : ''}>Полупиковый</option>
                            <option value="night" ${tariff?.tariffType === 'night' ? 'selected' : ''}>Ночной</option>
                            <option value="flat" ${tariff?.tariffType === 'flat' ? 'selected' : ''}>Единый</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Ставка (руб./кВт·ч)</label>
                        <input type="number" id="tariff-rate" value="${tariff?.ratePerKwh || ''}" placeholder="5.58" required step="0.01" min="0">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Начало (час)</label>
                        <input type="number" id="tariff-start" value="${tariff?.startHour ?? ''}" placeholder="7" required min="0" max="23">
                    </div>
                    <div class="form-group">
                        <label>Конец (час)</label>
                        <input type="number" id="tariff-end" value="${tariff?.endHour ?? ''}" placeholder="10" required min="0" max="23">
                    </div>
                </div>
                <div class="form-group">
                    <label>Действует с (дд/мм/гггг)</label>
                    <input type="text" id="tariff-valid-from" value="${formatDateForInput(tariff?.validFrom)}" placeholder="01/01/2024" pattern="\\d{2}/\\d{2}/\\d{4}">
                </div>
            </form>
        `;
        
        document.getElementById('modal-footer').innerHTML = `
            <button class="btn btn-secondary" onclick="App.closeModal()">Отмена</button>
            <button class="btn btn-primary" onclick="App.saveTariff(${id})">${isEdit ? 'Сохранить' : 'Добавить'}</button>
        `;
        
        this.openModal();
    },
    
    /**
     * Сохранение тарифа
     */
    async saveTariff(id) {
        // Конвертация даты из dd/mm/yyyy в yyyy-mm-dd
        const dateInput = document.getElementById('tariff-valid-from').value;
        let validFrom = '';
        if (dateInput) {
            const parts = dateInput.split('/');
            if (parts.length === 3) {
                validFrom = `${parts[2]}-${parts[1]}-${parts[0]}`;
            }
        }
        
        const data = {
            name: document.getElementById('tariff-name').value.trim(),
            tariffType: document.getElementById('tariff-type').value,
            ratePerKwh: parseFloat(document.getElementById('tariff-rate').value),
            startHour: parseInt(document.getElementById('tariff-start').value),
            endHour: parseInt(document.getElementById('tariff-end').value),
            validFrom: validFrom
        };
        
        if (!data.name || !data.ratePerKwh) {
            this.notify('Заполните все обязательные поля', 'error');
            return;
        }
        
        try {
            if (id) {
                await API.tariffs.update(id, data);
                this.notify('Тариф обновлён', 'success');
            } else {
                await API.tariffs.create(data);
                this.notify('Тариф добавлен', 'success');
            }
            
            this.closeModal();
            await this.loadTariffs();
            
        } catch (error) {
            this.notify('Ошибка сохранения: ' + error.message, 'error');
        }
    },
    
    /**
     * Удаление тарифа
     */
    async deleteTariff(id) {
        if (!confirm('Удалить этот тариф?')) return;
        
        try {
            await API.tariffs.delete(id);
            this.notify('Тариф удалён', 'success');
            await this.loadTariffs();
        } catch (error) {
            this.notify('Ошибка удаления: ' + error.message, 'error');
        }
    },
    
    /**
     * Загрузка аналитики
     */
    async loadAnalytics() {
        try {
            const [daily, monthly, dashboard] = await Promise.all([
                API.analytics.getDaily(30),
                API.analytics.getMonthly(),
                API.analytics.getDashboard()
            ]);
            
            // Дневной график
            const dailyLabels = daily.map(d => {
                const date = new Date(d.date);
                return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
            });
            const dailyValues = daily.map(d => d.kwh);
            Charts.line('daily-chart', dailyLabels, dailyValues, { color: '#0ea5e9' });
            
            // Месячный график
            const monthlyLabels = monthly.map(m => m.monthName);
            const monthlyValues = monthly.map(m => m.totalKwh);
            Charts.bar('monthly-chart', monthlyLabels.reverse(), monthlyValues.reverse(), {
                colors: Charts.groupColors
            });
            
            // Месячная статистика
            this.renderMonthlyStats(monthly);
            
            // Сравнение
            this.renderComparison(dashboard);
            
        } catch (error) {
            console.error('Ошибка загрузки аналитики:', error);
            this.notify('Ошибка загрузки аналитики', 'error');
        }
    },
    
    /**
     * Отрисовка месячной статистики
     */
    renderMonthlyStats(stats) {
        const container = document.getElementById('monthly-stats');
        
        container.innerHTML = stats.slice(0, 6).map(s => `
            <div class="month-stat-card">
                <h4>${s.monthName} ${s.year}</h4>
                <div class="month-stat-value">${s.totalKwh.toFixed(2)} кВт·ч</div>
                <div style="color: var(--text-secondary); font-size: 0.9rem;">${s.totalCost.toFixed(2)} руб.</div>
            </div>
        `).join('');
    },
    
    /**
     * Отрисовка сравнения
     */
    renderComparison(data) {
        const container = document.getElementById('comparison-info');
        const change = data.comparisonPercent;
        const changeClass = change > 0 ? 'positive' : change < 0 ? 'negative' : '';
        const changeText = change > 0 ? `+${change.toFixed(1)}%` : `${change.toFixed(1)}%`;
        
        container.innerHTML = `
            <div class="comparison-row">
                <span class="comparison-label">Текущий месяц</span>
                <span class="comparison-value">${data.currentMonthKwh.toFixed(2)} кВт·ч</span>
            </div>
            <div class="comparison-row">
                <span class="comparison-label">Стоимость текущего месяца</span>
                <span class="comparison-value">${data.currentMonthCost.toFixed(2)} руб.</span>
            </div>
            <div class="comparison-row">
                <span class="comparison-label">Изменение к прошлому месяцу</span>
                <span class="comparison-value ${changeClass}">${changeText}</span>
            </div>
        `;
    },
    
    /**
     * Переключение вкладок аналитики
     */
    switchAnalyticsTab(tab) {
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tab);
        });
        
        document.querySelectorAll('.analytics-tab').forEach(t => {
            t.classList.add('hidden');
        });
        
        document.getElementById(tab + '-analytics').classList.remove('hidden');
    },
    
    /**
     * Экспорт в CSV
     */
    async exportCSV() {
        try {
            const csv = await API.analytics.exportCSV();
            
            // Создание и скачивание файла
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = 'energy_report_' + new Date().toISOString().split('T')[0] + '.csv';
            link.click();
            
            this.notify('Отчёт экспортирован', 'success');
            
        } catch (error) {
            this.notify('Ошибка экспорта: ' + error.message, 'error');
        }
    },
    
    /**
     * Загрузка рекомендаций
     */
    async loadRecommendations() {
        try {
            const recommendations = await API.analytics.getRecommendations();
            this.renderRecommendations(recommendations);
        } catch (error) {
            console.error('Ошибка загрузки рекомендаций:', error);
            this.notify('Ошибка загрузки рекомендаций', 'error');
        }
    },
    
    /**
     * Отрисовка рекомендаций
     */
    renderRecommendations(recommendations) {
        const container = document.getElementById('recommendations-list');
        
        const icons = {
            high_consumption: '⚡',
            tariff_optimization: '💰',
            general: '💡'
        };
        
        const priorityNames = {
            high: 'Важно',
            medium: 'Рекомендуется',
            low: 'Совет'
        };
        
        container.innerHTML = recommendations.map(r => `
            <div class="recommendation-card ${r.priority}">
                <div class="recommendation-icon">${icons[r.type] || '💡'}</div>
                <div class="recommendation-content">
                    <div class="recommendation-title">${r.title || r.appliance || 'Рекомендация'}</div>
                    <div class="recommendation-message">${r.message}</div>
                </div>
                <span class="recommendation-priority ${r.priority}">${priorityNames[r.priority]}</span>
            </div>
        `).join('');
    },
    
    /**
     * Открытие модального окна
     */
    openModal() {
        document.getElementById('modal-overlay').classList.remove('hidden');
        document.body.style.overflow = 'hidden';
    },
    
    /**
     * Закрытие модального окна
     */
    closeModal() {
        document.getElementById('modal-overlay').classList.add('hidden');
        document.body.style.overflow = '';
    },
    
    /**
     * Показать уведомление
     */
    notify(message, type = 'info') {
        const container = document.getElementById('notifications');
        
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };
        
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <span class="notification-icon">${icons[type]}</span>
            <span class="notification-message">${message}</span>
        `;
        
        container.appendChild(notification);
        
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => notification.remove(), 300);
        }, 4000);
    },
    
    /**
     * Показать рекомендацию (большое уведомление)
     */
    showTipNotification(tip) {
        const container = document.getElementById('notifications');
        
        const notification = document.createElement('div');
        notification.className = 'notification tip';
        notification.innerHTML = `
            <span class="notification-message">${tip}</span>
            <button class="tip-close" onclick="this.parentElement.remove()">✕</button>
        `;
        
        container.appendChild(notification);
        
        setTimeout(() => {
            if (notification.parentElement) {
                notification.style.opacity = '0';
                notification.style.transform = 'translateX(100%)';
                setTimeout(() => notification.remove(), 300);
            }
        }, 10000);
    },
    
    /**
     * Обновление графиков при изменении размера
     */
    async refreshCharts() {
        if (this.state.currentSection === 'dashboard') {
            const groupStats = await API.analytics.getGroupStats();
            await this.loadDashboardCharts(groupStats);
        } else if (this.state.currentSection === 'analytics') {
            await this.loadAnalytics();
        }
    }
};

// Запуск приложения
document.addEventListener('DOMContentLoaded', () => App.init());

// Экспорт для использования в HTML
window.App = App;


