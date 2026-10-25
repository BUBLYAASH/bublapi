(() => {
    const logout = document.querySelector('#portalLogout');
    if (!logout) return;

    logout.addEventListener('click', async () => {
        logout.disabled = true;
        logout.textContent = 'Выходим…';
        try {
            const response = await (await import('/session.js')).sessionLock(() => fetch('/api/v1/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {'Content-Type': 'application/json'}
            }));
            if (!response.ok) throw new Error('Не удалось завершить сессию. Повторите выход.');
        } catch (error) {
            logout.disabled = false;
            logout.textContent = 'Повторить выход';
            logout.title = error.message;
            return;
        }
        // Auth boundary intentionally resets the document and all privileged
        // imperative handlers before showing the public login form.
        location.replace('/login');
    });
})();
