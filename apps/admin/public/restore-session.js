import {ensureAdminSession} from './session.js';

ensureAdminSession(true).then(() => location.reload()).catch(error => {
    if (error.status === 401) location.replace('/login');
    else {
        document.querySelector('[role="status"]').textContent = 'Не удалось восстановить сессию. Обновите страницу, чтобы повторить.';
    }
});
