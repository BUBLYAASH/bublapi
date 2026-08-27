const yearNode = document.getElementById('year');
if (yearNode) yearNode.textContent = new Date().getFullYear();

const revealItems = [...document.querySelectorAll('.reveal')];

revealItems.forEach((element, index) => {
  if (element.classList.contains('hero-copy')) {
    element.classList.add('reveal-left');
  } else if (element.classList.contains('hero-visual')) {
    element.classList.add('reveal-right');
  } else if (element.matches('.feature-card, .step')) {
    element.classList.add('reveal-soft');
  } else if (index % 2 === 0) {
    element.classList.add('reveal-left');
  } else {
    element.classList.add('reveal-right');
  }
});

document.querySelectorAll('.feature-grid, .steps').forEach((group) => {
  [...group.querySelectorAll('.reveal')].forEach((element, index) => {
    element.style.setProperty('--reveal-delay', `${Math.min(index * 90, 360)}ms`);
  });
});

const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    }
  });
}, { threshold: 0.14, rootMargin: '0px 0px -8% 0px' });

revealItems.forEach((element) => observer.observe(element));

const apiTiles = [...document.querySelectorAll('[data-api]')];
const apiProducts = [...document.querySelectorAll('[data-api-content]')];

apiTiles.forEach((tile) => {
  tile.addEventListener('click', () => {
    const selectedApi = tile.dataset.api;
    apiTiles.forEach((item) => {
      const active = item.dataset.api === selectedApi;
      item.classList.toggle('is-active', active);
      item.setAttribute('aria-selected', String(active));
    });
    apiProducts.forEach((product) => {
      product.classList.toggle('is-active', product.dataset.apiContent === selectedApi);
    });
  });
});


const THEME_STORAGE_KEY = 'bublapi-theme';
const themeButtons = [...document.querySelectorAll('[data-theme-toggle]')];
const themeMedia = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

function currentTheme() {
  return document.documentElement.dataset.theme || (themeMedia?.matches ? 'dark' : 'light');
}

function syncThemeButtons(theme) {
  themeButtons.forEach((button) => {
    const isDark = theme === 'dark';
    button.setAttribute('aria-label', isDark ? 'Включить светлую тему' : 'Включить тёмную тему');
    button.setAttribute('title', isDark ? 'Светлая тема' : 'Тёмная тема');
    button.setAttribute('aria-pressed', String(isDark));
  });
}

function applyTheme(theme, persist = true, event = null) {
  const root = document.documentElement;
  const update = () => {
    root.dataset.theme = theme;
    root.style.colorScheme = theme;
    if (persist) localStorage.setItem(THEME_STORAGE_KEY, theme);
    syncThemeButtons(theme);
  };

  if (!document.startViewTransition || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    root.classList.add('theme-changing');
    update();
    window.setTimeout(() => root.classList.remove('theme-changing'), 420);
    return;
  }

  if (event) {
    root.style.setProperty('--theme-x', `${event.clientX}px`);
    root.style.setProperty('--theme-y', `${event.clientY}px`);
  }
  document.startViewTransition(update);
}

themeButtons.forEach((button) => {
  button.addEventListener('click', (event) => {
    applyTheme(currentTheme() === 'dark' ? 'light' : 'dark', true, event);
  });
});

if (themeMedia) {
  themeMedia.addEventListener?.('change', (event) => {
    if (!localStorage.getItem(THEME_STORAGE_KEY)) applyTheme(event.matches ? 'dark' : 'light', false);
  });
}

syncThemeButtons(currentTheme());
