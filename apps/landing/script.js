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
