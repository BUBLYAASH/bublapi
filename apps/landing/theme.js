(() => {
  const STORAGE_KEY = 'bublapi-theme';
  const COOKIE_NAME = 'bublapi-theme';
  const root = document.documentElement;
  const media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function systemTheme() {
    return media?.matches ? 'dark' : 'light';
  }

  function savedTheme() {
    try {
      const value = localStorage.getItem(STORAGE_KEY);
      return value === 'dark' || value === 'light' ? value : null;
    } catch {
      return null;
    }
  }

  function writeThemeCookie(theme) {
    document.cookie = `${COOKIE_NAME}=${theme}; Path=/; Max-Age=31536000; SameSite=Lax`;
  }

  function currentTheme() {
    return root.dataset.theme || savedTheme() || systemTheme();
  }

  function syncButtons(theme) {
    document.querySelectorAll('[data-theme-toggle]').forEach(button => {
      const dark = theme === 'dark';
      button.setAttribute('aria-label', dark ? 'Включить светлую тему' : 'Включить тёмную тему');
      button.setAttribute('title', dark ? 'Включить светлую тему' : 'Включить тёмную тему');
      button.setAttribute('aria-pressed', String(dark));
      button.querySelectorAll('[data-theme-value]').forEach(value => {
        value.textContent = dark ? 'Тёмная' : 'Светлая';
      });
    });
  }

  // Used during page bootstrap and OS-theme changes. It deliberately has no
  // View Transition / CSS transition so a reload never flashes light first.
  function applyImmediately(theme, { persist = false } = {}) {
    root.dataset.theme = theme;
    root.style.colorScheme = theme;
    if (persist) {
      try { localStorage.setItem(STORAGE_KEY, theme); } catch {}
      writeThemeCookie(theme);
    }
    syncButtons(theme);
  }

  function applyAnimated(theme, event) {
    const update = () => applyImmediately(theme, { persist: true });

    // Keyboard activation should be immediate: frequent keyboard actions must
    // never wait for a spatial animation to finish.
    if (event?.detail === 0) {
      update();
      return;
    }

    if (!document.startViewTransition || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      root.classList.add('theme-switching');
      update();
      window.setTimeout(() => root.classList.remove('theme-switching'), 700);
      return;
    }

    if (event) {
      root.style.setProperty('--theme-x', `${event.clientX}px`);
      root.style.setProperty('--theme-y', `${event.clientY}px`);
    }

    root.dataset.themeTransition = theme;
    const transition = document.startViewTransition(update);
    transition.finished.finally(() => delete root.dataset.themeTransition);
  }

  // The server already reads the cookie. CSS handles the system preference
  // before JS. This only reconciles legacy localStorage values without animation.
  const stored = savedTheme();
  const initial = stored || root.dataset.theme || systemTheme();
  applyImmediately(initial, { persist: Boolean(stored) });

  // Client-side navigation can replace the controls without reloading this script.
  const buttonObserver = new MutationObserver(records => {
    const hasNewButton = records.some(record => [...record.addedNodes].some(node =>
      node instanceof Element && (
        node.matches('[data-theme-toggle]') || node.querySelector('[data-theme-toggle]')
      )
    ));
    if (hasNewButton) syncButtons(currentTheme());
  });
  buttonObserver.observe(document.body, { childList: true, subtree: true });

  document.addEventListener('click', event => {
    const button = event.target.closest('[data-theme-toggle]');
    if (!button) return;
    applyAnimated(currentTheme() === 'dark' ? 'light' : 'dark', event);
  });

  media?.addEventListener?.('change', event => {
    if (!savedTheme()) {
      applyImmediately(event.matches ? 'dark' : 'light');
    }
  });
})();
