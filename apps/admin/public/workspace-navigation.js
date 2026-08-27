(() => {
  const catalogSurface = document.querySelector('[data-workspace-surface="catalog"]');
  const dentSurface = document.querySelector('[data-workspace-surface="dent"]');
  if (!catalogSurface || !dentSurface) return;

  const routes = {
    '/dent/clinics': { view: 'admin-clinics', title: 'Клиники' },
    '/dent/catalog': { view: 'admin-catalog', title: 'Каталог услуг' },
    '/dent/api-keys': { view: 'admin-api-keys', title: 'API-ключи' },
    '/dent/notifications': { view: 'admin-notifications', title: 'Системные уведомления' },
    '/dent/users': { view: 'admin-users', title: 'Пользователи и роли' },
    '/dent/docs': { view: 'admin-docs', title: 'API-документация' },
    '/dent/system': { view: 'admin-system', title: 'Состояние системы' }
  };

  function normalizedPath(pathname) {
    return pathname.replace(/\/+$/, '') || '/';
  }

  function focusSurface(surface) {
    const heading = surface.querySelector('h1');
    if (!heading) return;
    heading.setAttribute('tabindex', '-1');
    heading.focus({ preventScroll: true });
    heading.addEventListener('blur', () => heading.removeAttribute('tabindex'), { once: true });
  }

  function activate(pathname, { focus = false } = {}) {
    const path = normalizedPath(pathname);
    const catalog = path === '/apis';
    const route = routes[path];
    if (!catalog && !route) return false;

    const nextSurface = catalog ? catalogSurface : dentSurface;
    const previousSurface = catalog ? dentSurface : catalogSurface;

    const render = () => {
      previousSurface.hidden = true;
      nextSurface.hidden = false;

      if (catalog) {
        window.bublapiAdminWorkspace?.suspend();
        document.title = 'Выбор API | BublAPI Admin';
      } else {
        const showDentView = () => window.bublapiAdminWorkspace?.showView(route.view);
        if (window.bublapiAdminWorkspace) showDentView();
        else window.addEventListener('bublapi:admin-ready', showDentView, { once: true });
        document.title = `${route.title} | BublAPI Admin`;
      }

      if (focus) window.requestAnimationFrame(() => focusSurface(nextSurface));
    };

    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!document.startViewTransition || reducedMotion || !nextSurface.hidden || document.documentElement.dataset.themeTransition) {
      render();
      return true;
    }

    document.documentElement.dataset.portalTransition = catalog ? 'dent-catalog' : 'catalog-dent';
    try {
      const transition = document.startViewTransition(render);
      transition.finished.finally(() => delete document.documentElement.dataset.portalTransition);
    } catch {
      delete document.documentElement.dataset.portalTransition;
      render();
    }
    return true;
  }

  document.addEventListener('click', event => {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;

    const target = event.target instanceof Element ? event.target : event.target.parentElement;
    const anchor = target?.closest('a[href]');
    if (!anchor || anchor.hasAttribute('download') || (anchor.target && anchor.target !== '_self')) return;

    const url = new URL(anchor.href, location.href);
    if (url.origin !== location.origin) return;

    const path = normalizedPath(url.pathname);
    if (path !== '/apis' && !routes[path]) return;

    event.preventDefault();
    if (normalizedPath(location.pathname) !== path || location.search !== url.search) {
      history.pushState({ workspace: path }, '', `${path}${url.search}${url.hash}`);
    }
    activate(path, { focus: true });
  });

  window.addEventListener('popstate', () => activate(location.pathname));
})();
