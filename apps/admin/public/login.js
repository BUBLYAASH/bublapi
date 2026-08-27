(() => {
  const form = document.querySelector('#adminStandaloneLoginForm');
  const error = document.querySelector('#adminLoginError');
  if (!form || !error) return;

  form.addEventListener('submit', async event => {
    event.preventDefault();
    error.textContent = '';

    if (!form.reportValidity()) return;

    const button = form.querySelector('button[type="submit"]');
    const idleLabel = button.textContent;
    button.disabled = true;
    button.textContent = 'Входим…';
    button.setAttribute('aria-busy', 'true');

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: form.elements.email.value.trim(),
          password: form.elements.password.value
        })
      });

      let result = {};
      try { result = await response.json(); } catch {}

      if (!response.ok || !result.authenticated) {
        throw new Error(result.message || 'Неверный логин или пароль. Проверьте данные и попробуйте снова.');
      }

      if (window.bublapiNavigate) window.bublapiNavigate('/apis', { replace: true });
      else location.replace('/apis');
    } catch (requestError) {
      error.textContent = requestError.message || 'Не удалось выполнить вход. Попробуйте ещё раз.';
      form.elements.password.select();
    } finally {
      button.disabled = false;
      button.textContent = idleLabel;
      button.removeAttribute('aria-busy');
    }
  });
})();
