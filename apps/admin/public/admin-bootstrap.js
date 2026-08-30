(() => {
  'use strict';

  const root = document.documentElement;
  let raf = 0;
  let observedTopbar = null;
  let resizeObserver = null;

  function updateTopbarHeight() {
    cancelAnimationFrame(raf);
    raf = requestAnimationFrame(() => {
      const topbar = document.querySelector('.topbar');
      if (!topbar) return;

      // offsetHeight is less sensitive to transforms/subpixel layout, while
      // getBoundingClientRect catches wrapped mobile content. Use the larger.
      const rectHeight = Math.ceil(topbar.getBoundingClientRect().height);
      const height = Math.max(rectHeight, topbar.offsetHeight || 0);
      if (height > 0) {
        root.style.setProperty('--admin-topbar-height', `${height}px`);
        root.dataset.adminTopbarMeasured = 'true';
      }

      if (observedTopbar !== topbar && 'ResizeObserver' in window) {
        resizeObserver?.disconnect();
        resizeObserver = new ResizeObserver(updateTopbarHeight);
        resizeObserver.observe(topbar);
        observedTopbar = topbar;
      }
    });
  }

  function start() {
    updateTopbarHeight();

    window.addEventListener('resize', updateTopbarHeight, { passive: true });
    window.addEventListener('orientationchange', updateTopbarHeight, { passive: true });
    window.addEventListener('pageshow', updateTopbarHeight, { passive: true });

    // Mobile browsers can change the visual viewport without firing a normal
    // layout resize (address bar / orientation / keyboard transitions).
    window.visualViewport?.addEventListener('resize', updateTopbarHeight, { passive: true });
    window.visualViewport?.addEventListener('scroll', updateTopbarHeight, { passive: true });

    document.fonts?.ready?.then(updateTopbarHeight).catch(() => {});

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
