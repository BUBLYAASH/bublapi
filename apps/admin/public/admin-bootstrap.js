(() => {
  'use strict';

  const root = document.documentElement;
  let raf = 0;

  function updateTopbarHeight() {
    cancelAnimationFrame(raf);
    raf = requestAnimationFrame(() => {
      const topbar = document.querySelector('.topbar');
      if (!topbar) return;
      const height = Math.ceil(topbar.getBoundingClientRect().height);
      if (height > 0) {
        root.style.setProperty('--admin-topbar-height', `${height}px`);
      }
    });
  }

  function start() {
    updateTopbarHeight();

    const topbar = document.querySelector('.topbar');
    if (!topbar) return;

    if ('ResizeObserver' in window) {
      const observer = new ResizeObserver(updateTopbarHeight);
      observer.observe(topbar);
    } else {
      window.addEventListener('resize', updateTopbarHeight, { passive: true });
    }

    window.addEventListener('orientationchange', updateTopbarHeight, { passive: true });

    document.addEventListener('click', event => {
      const button = event.target.closest?.('.sidebar .nav-button');
      if (!button) return;

      requestAnimationFrame(() => {
        button.scrollIntoView({
          block: 'nearest',
          inline: 'nearest',
          behavior: 'smooth'
        });
        updateTopbarHeight();
      });
    }, true);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();
