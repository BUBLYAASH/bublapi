(() => {
  'use strict';

  const qs = (selector, scope = document) => scope.querySelector(selector);
  const CONFIG = [
    {
      view: '#view-admin-clinics',
      form: '#createClinicForm',
      label: '+ Новая клиника',
      title: 'Новая клиника'
    },
    {
      view: '#view-admin-catalog',
      form: '#createCatalogServiceForm',
      label: '+ Новая услуга',
      title: 'Новая услуга каталога'
    },
    {
      view: '#view-admin-api-keys',
      form: '#createApiKeyForm',
      label: '+ Новый API-ключ',
      title: 'Новый API-ключ'
    }
  ];

  let initialized = false;
  let currentForm = null;
  let returnFocus = null;

  function ensureModal() {
    let modal = qs('#adminCreateModal');
    if (modal) return modal;

    modal = document.createElement('div');
    modal.id = 'adminCreateModal';
    modal.className = 'admin-create-modal hidden';
    modal.setAttribute('aria-hidden', 'true');
    modal.innerHTML = `
      <div class="admin-create-modal-backdrop" data-admin-create-close></div>
      <section class="admin-create-modal-dialog" role="dialog" aria-modal="true" aria-labelledby="adminCreateModalTitle">
        <div class="admin-create-modal-head">
          <div>
            <h2 id="adminCreateModalTitle">Создание</h2>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" data-admin-create-close aria-label="Закрыть">Закрыть</button>
        </div>
        <div class="admin-create-modal-body" id="adminCreateModalBody"></div>
      </section>
    `;
    document.body.append(modal);

    modal.addEventListener('click', event => {
      if (event.target.closest('[data-admin-create-close]')) closeModal();
    });

    return modal;
  }

  function ensureParking() {
    let parking = qs('#adminCreateParking');
    if (!parking) {
      parking = document.createElement('div');
      parking.id = 'adminCreateParking';
      parking.hidden = true;
      document.body.append(parking);
    }
    return parking;
  }

  function closeModal() {
    const modal = qs('#adminCreateModal');
    if (!modal || modal.classList.contains('hidden')) return;

    if (currentForm) {
      ensureParking().append(currentForm);
      currentForm = null;
    }

    modal.classList.add('hidden');
    modal.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('modal-open');
    returnFocus?.focus?.();
    returnFocus = null;
  }

  function openModal(form, title, button) {
    const modal = ensureModal();
    const body = qs('#adminCreateModalBody', modal);
    const heading = qs('#adminCreateModalTitle', modal);

    if (currentForm && currentForm !== form) {
      ensureParking().append(currentForm);
    }

    currentForm = form;
    returnFocus = button || document.activeElement;
    heading.textContent = title;
    body.replaceChildren(form);

    modal.classList.remove('hidden');
    modal.setAttribute('aria-hidden', 'false');
    document.body.classList.add('modal-open');

    requestAnimationFrame(() => {
      form.querySelector('input:not([type="hidden"]),select,textarea,button')?.focus();
    });
  }

  function wrapHeaderActions(view) {
    const head = qs('.section-head', view);
    if (!head) return null;

    let actions = qs(':scope > .admin-section-actions', head);
    if (actions) return actions;

    actions = document.createElement('div');
    actions.className = 'admin-section-actions';

    // Existing direct action buttons/links in the section head are preserved.
    [...head.children].slice(1).forEach(node => {
      if (node.matches?.('button, a, .actions')) actions.append(node);
    });

    head.append(actions);
    return actions;
  }

  function transform(config) {
    const view = qs(config.view);
    const form = qs(config.form);
    if (!view || !form || form.dataset.listFirstReady === 'true') return false;

    const actions = wrapHeaderActions(view);
    if (!actions) return false;

    form.dataset.listFirstReady = 'true';

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-primary admin-create-trigger';
    button.textContent = config.label;
    button.addEventListener('click', () => openModal(form, config.title, button));

    // Creation is the first explicit action; refresh remains beside it.
    actions.prepend(button);

    // Move the form out of the page. This keeps all event listeners and IDs,
    // but leaves the list/table as the first content after the heading.
    ensureParking().append(form);
    return true;
  }

  function transformAll() {
    let changed = false;
    for (const config of CONFIG) changed = transform(config) || changed;
    return changed;
  }

  function start() {
    if (initialized) return;
    initialized = true;

    ensureModal();
    ensureParking();
    transformAll();

    // API-key form is rebuilt synchronously by legacy/admin.js, but this also
    // covers future delayed/dynamic sections without racing script order.
    const observer = new MutationObserver(() => transformAll());
    observer.observe(document.body, { childList: true, subtree: true });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') closeModal();
    });

    // When a create form succeeds legacy code usually resets it. Close the
    // creation sheet after reset, but do not close on validation/API failure.
    document.addEventListener('reset', event => {
      if (event.target === currentForm) {
        setTimeout(closeModal, 0);
      }
    }, true);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start, { once: true });
  } else {
    start();
  }
})();
