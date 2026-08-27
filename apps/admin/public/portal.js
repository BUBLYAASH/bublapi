(() => {
  const logout = document.querySelector('#portalLogout');
  if (!logout) return;

  logout.addEventListener('click', async () => {
    logout.disabled = true;
    logout.textContent = 'Выходим…';
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' }
      });
    } finally {
      // Auth boundary intentionally resets the document and all privileged
      // imperative handlers before showing the public login form.
      location.replace('/login');
    }
  });
})();
