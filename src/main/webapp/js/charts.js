/**
 * Модуль для работы с графиками
 * Простая реализация графиков на Canvas без внешних библиотек
 */

const Charts = {
    // Цветовая палитра
    colors: {
        primary: '#0ea5e9',
        secondary: '#6366f1',
        accent: '#f59e0b',
        success: '#10b981',
        danger: '#ef4444',
        text: '#94a3b8',
        grid: '#334155',
        background: '#1e293b'
    },
    
    // Палитра для групп
    groupColors: [
        '#e74c3c', '#3498db', '#9b59b6', '#1abc9c', '#f1c40f',
        '#e67e22', '#2ecc71', '#34495e', '#16a085', '#c0392b'
    ],
    
    /**
     * Линейный график
     * @param {string} canvasId - ID canvas элемента
     * @param {array} labels - метки по оси X
     * @param {array} data - данные
     * @param {object} options - дополнительные опции
     */
    line(canvasId, labels, data, options = {}) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        
        const ctx = canvas.getContext('2d');
        const dpr = window.devicePixelRatio || 1;
        
        // Установка размеров
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);
        
        const width = rect.width;
        const height = rect.height;
        const padding = { top: 20, right: 20, bottom: 40, left: 60 };
        
        // Очистка
        ctx.fillStyle = 'transparent';
        ctx.fillRect(0, 0, width, height);
        
        // Расчёт масштаба
        const maxValue = Math.max(...data) * 1.1 || 10;
        const minValue = 0;
        const chartWidth = width - padding.left - padding.right;
        const chartHeight = height - padding.top - padding.bottom;
        
        // Сетка и оси
        ctx.strokeStyle = this.colors.grid;
        ctx.lineWidth = 1;
        
        // Горизонтальные линии
        const gridLines = 5;
        ctx.beginPath();
        for (let i = 0; i <= gridLines; i++) {
            const y = padding.top + (chartHeight / gridLines) * i;
            ctx.moveTo(padding.left, y);
            ctx.lineTo(width - padding.right, y);
            
            // Значения по оси Y
            const value = maxValue - (maxValue / gridLines) * i;
            ctx.fillStyle = this.colors.text;
            ctx.font = '12px Nunito';
            ctx.textAlign = 'right';
            ctx.fillText(value.toFixed(1), padding.left - 10, y + 4);
        }
        ctx.stroke();
        
        // Метки по оси X
        const step = Math.ceil(labels.length / 7);
        ctx.fillStyle = this.colors.text;
        ctx.textAlign = 'center';
        labels.forEach((label, i) => {
            if (i % step === 0) {
                const x = padding.left + (chartWidth / (labels.length - 1)) * i;
                ctx.fillText(label, x, height - 10);
            }
        });
        
        // Линия графика
        if (data.length > 0) {
            ctx.beginPath();
            ctx.strokeStyle = options.color || this.colors.primary;
            ctx.lineWidth = 3;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            
            data.forEach((value, i) => {
                const x = padding.left + (chartWidth / (data.length - 1)) * i;
                const y = padding.top + chartHeight - (value / maxValue) * chartHeight;
                
                if (i === 0) {
                    ctx.moveTo(x, y);
                } else {
                    ctx.lineTo(x, y);
                }
            });
            ctx.stroke();
            
            // Заливка под графиком
            ctx.lineTo(padding.left + chartWidth, padding.top + chartHeight);
            ctx.lineTo(padding.left, padding.top + chartHeight);
            ctx.closePath();
            
            const gradient = ctx.createLinearGradient(0, padding.top, 0, padding.top + chartHeight);
            gradient.addColorStop(0, (options.color || this.colors.primary) + '40');
            gradient.addColorStop(1, (options.color || this.colors.primary) + '00');
            ctx.fillStyle = gradient;
            ctx.fill();
            
            // Точки
            data.forEach((value, i) => {
                const x = padding.left + (chartWidth / (data.length - 1)) * i;
                const y = padding.top + chartHeight - (value / maxValue) * chartHeight;
                
                ctx.beginPath();
                ctx.arc(x, y, 4, 0, Math.PI * 2);
                ctx.fillStyle = options.color || this.colors.primary;
                ctx.fill();
                ctx.strokeStyle = this.colors.background;
                ctx.lineWidth = 2;
                ctx.stroke();
            });
        }
    },
    
    /**
     * Столбчатый график
     * @param {string} canvasId - ID canvas элемента
     * @param {array} labels - метки
     * @param {array} data - данные
     * @param {object} options - опции
     */
    bar(canvasId, labels, data, options = {}) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        
        const ctx = canvas.getContext('2d');
        const dpr = window.devicePixelRatio || 1;
        
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);
        
        const width = rect.width;
        const height = rect.height;
        const padding = { top: 20, right: 20, bottom: 60, left: 60 };
        
        ctx.fillStyle = 'transparent';
        ctx.fillRect(0, 0, width, height);
        
        const maxValue = Math.max(...data) * 1.1 || 10;
        const chartWidth = width - padding.left - padding.right;
        const chartHeight = height - padding.top - padding.bottom;
        const barWidth = (chartWidth / data.length) * 0.6;
        const barGap = (chartWidth / data.length) * 0.4;
        
        // Сетка
        ctx.strokeStyle = this.colors.grid;
        ctx.lineWidth = 1;
        
        const gridLines = 5;
        ctx.beginPath();
        for (let i = 0; i <= gridLines; i++) {
            const y = padding.top + (chartHeight / gridLines) * i;
            ctx.moveTo(padding.left, y);
            ctx.lineTo(width - padding.right, y);
            
            const value = maxValue - (maxValue / gridLines) * i;
            ctx.fillStyle = this.colors.text;
            ctx.font = '12px Nunito';
            ctx.textAlign = 'right';
            ctx.fillText(value.toFixed(1), padding.left - 10, y + 4);
        }
        ctx.stroke();
        
        // Столбцы
        data.forEach((value, i) => {
            const x = padding.left + (chartWidth / data.length) * i + barGap / 2;
            const barHeight = (value / maxValue) * chartHeight;
            const y = padding.top + chartHeight - barHeight;
            
            // Градиент для столбца
            const gradient = ctx.createLinearGradient(x, y, x, y + barHeight);
            gradient.addColorStop(0, options.colors ? options.colors[i % options.colors.length] : this.colors.primary);
            gradient.addColorStop(1, options.colors ? options.colors[i % options.colors.length] + '80' : this.colors.primary + '80');
            
            ctx.fillStyle = gradient;
            ctx.beginPath();
            this.roundRect(ctx, x, y, barWidth, barHeight, 4);
            ctx.fill();
            
            // Метка
            ctx.fillStyle = this.colors.text;
            ctx.font = '11px Nunito';
            ctx.textAlign = 'center';
            ctx.save();
            ctx.translate(x + barWidth / 2, height - 10);
            ctx.rotate(-Math.PI / 4);
            ctx.fillText(labels[i], 0, 0);
            ctx.restore();
        });
    },
    
    /**
     * Круговая диаграмма
     * @param {string} canvasId - ID canvas элемента
     * @param {array} labels - метки
     * @param {array} data - данные
     * @param {array} colors - цвета сегментов
     */
    pie(canvasId, labels, data, colors = []) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        
        const ctx = canvas.getContext('2d');
        const dpr = window.devicePixelRatio || 1;
        
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.scale(dpr, dpr);
        
        const width = rect.width;
        const height = rect.height;
        
        ctx.fillStyle = 'transparent';
        ctx.fillRect(0, 0, width, height);
        
        const total = data.reduce((a, b) => a + b, 0);
        if (total === 0) {
            ctx.fillStyle = this.colors.text;
            ctx.font = '14px Nunito';
            ctx.textAlign = 'center';
            ctx.fillText('Нет данных', width / 2, height / 2);
            return;
        }
        
        const centerX = width / 2 - 60;
        const centerY = height / 2;
        const radius = Math.min(centerX, centerY) - 10;
        
        let startAngle = -Math.PI / 2;
        
        data.forEach((value, i) => {
            const sliceAngle = (value / total) * Math.PI * 2;
            const endAngle = startAngle + sliceAngle;
            
            ctx.beginPath();
            ctx.moveTo(centerX, centerY);
            ctx.arc(centerX, centerY, radius, startAngle, endAngle);
            ctx.closePath();
            
            ctx.fillStyle = colors[i] || this.groupColors[i % this.groupColors.length];
            ctx.fill();
            
            startAngle = endAngle;
        });
        
        // Центральный круг (для эффекта donut)
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius * 0.5, 0, Math.PI * 2);
        ctx.fillStyle = this.colors.background;
        ctx.fill();
        
        // Легенда
        const legendX = width - 100;
        let legendY = 20;
        
        labels.forEach((label, i) => {
            if (data[i] > 0) {
                ctx.fillStyle = colors[i] || this.groupColors[i % this.groupColors.length];
                ctx.fillRect(legendX, legendY, 12, 12);
                
                ctx.fillStyle = this.colors.text;
                ctx.font = '11px Nunito';
                ctx.textAlign = 'left';
                ctx.fillText(label, legendX + 18, legendY + 10);
                
                legendY += 20;
            }
        });
    },
    
    /**
     * Вспомогательная функция для скругленных прямоугольников
     */
    roundRect(ctx, x, y, width, height, radius) {
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + width - radius, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
        ctx.lineTo(x + width, y + height - radius);
        ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
        ctx.lineTo(x + radius, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
    },
    
    /**
     * Обновление графика при изменении размера окна
     */
    setupResizeHandler() {
        let resizeTimeout;
        window.addEventListener('resize', () => {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(() => {
                // Перерисовка графиков будет вызвана из app.js
                if (window.App && window.App.refreshCharts) {
                    window.App.refreshCharts();
                }
            }, 250);
        });
    }
};

// Инициализация обработчика изменения размера
Charts.setupResizeHandler();

// Экспорт
window.Charts = Charts;



