export function sessionLock(work) {
    if (!navigator.locks) return Promise.reject(new Error('Для безопасного обновления сессии нужен браузер с Web Locks (HTTPS).'));
    return navigator.locks.request('bublapi-admin-auth', work);
}

export function ensureAdminSession(force = false, rejectedVersion) {
    return sessionLock(async () => {
        const check = async () => {
            const response = await fetch('/api/v1/auth/session', {cache: 'no-store', credentials: 'same-origin'});
            if (!response.ok) throw new Error('Не удалось проверить сессию');
            return response.json();
        };
        const current = await check();
        if (current.remaining > 60 && (!force || (rejectedVersion !== undefined && current.version !== rejectedVersion))) return current.version;
        const response = await fetch('/api/v1/auth/refresh', {
            method: 'POST',
            credentials: 'same-origin',
            cache: 'no-store'
        });
        if (!response.ok) {
            const error = new Error('Не удалось обновить сессию');
            error.status = response.status;
            throw error;
        }
        return (await check()).version;
    });
}
