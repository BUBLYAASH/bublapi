/*
 * Dent demo stability guard.
 *
 * final-fixes.js used a body-wide MutationObserver that listened to class,
 * disabled and value attributes and then changed the same attributes from its
 * callback. In Chromium/Safari this can create a self-sustaining mutation loop
 * that starves the main thread and makes the whole tab unresponsive.
 *
 * This guard is intentionally tiny and loaded immediately before final-fixes.js.
 * It only narrows that exact observer shape. All other MutationObservers keep
 * their original behaviour.
 */
(() => {
  'use strict';

  const NativeMutationObserver = window.MutationObserver;
  if (!NativeMutationObserver || NativeMutationObserver.__bublapiStabilityGuard) return;

  class SafeMutationObserver {
    constructor(callback) {
      this._observer = new NativeMutationObserver(callback);
    }

    observe(target, options = {}) {
      const attributeFilter = Array.isArray(options.attributeFilter)
        ? options.attributeFilter
        : [];

      const isDangerousBodyObserver =
        target === document.body &&
        options.subtree === true &&
        options.childList === true &&
        options.attributes === true &&
        attributeFilter.includes('class') &&
        attributeFilter.includes('disabled') &&
        attributeFilter.includes('value');

      if (isDangerousBodyObserver) {
        // Child additions/removals are enough for the hotfix to discover newly
        // rendered calendar/navigation elements. Attribute changes are already
        // handled by explicit input/change/click listeners and the 30s safety
        // sweep in final-fixes.js, so observing them here only creates feedback.
        return this._observer.observe(target, {
          subtree: true,
          childList: true
        });
      }

      return this._observer.observe(target, options);
    }

    disconnect() {
      return this._observer.disconnect();
    }

    takeRecords() {
      return this._observer.takeRecords();
    }
  }

  Object.defineProperty(SafeMutationObserver, '__bublapiStabilityGuard', {
    value: true
  });

  window.MutationObserver = SafeMutationObserver;

  // Auth bootstrap must never leave the public UI behind the loading bubbles
  // forever because one permission probe got stuck on a mobile connection.
  const releasePublicUi = () => {
    if (document.documentElement.dataset.authReady === 'true') return;

    document.querySelectorAll('[data-access]').forEach(element => {
      element.classList.add('hidden');
    });
    document.querySelectorAll('[data-guest-only]').forEach(element => {
      element.classList.remove('hidden');
    });

    const logout = document.querySelector('#logoutBtn');
    if (logout) logout.classList.add('hidden');

    document.documentElement.dataset.authReady = 'true';
    document.documentElement.dataset.authFallback = 'true';
  };

  window.setTimeout(releasePublicUi, 6500);
})();
