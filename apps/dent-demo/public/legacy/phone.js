import {
  formatLocalPhone,
  normalizeLocalPhoneInput,
  onlyDigits
} from './phone-format.js';

const COUNTRY_OPTIONS = [
  { code: '7', label: '+7 РФ / РК', localLength: 10, placeholder: '(999) 123-45-67', nationalPrefixes: ['8'] },
  { code: '375', label: '+375 Беларусь', localLength: 9, placeholder: '29 123-45-67', nationalPrefixes: ['80', '8', '0'] },
  { code: '380', label: '+380 Украина', localLength: 9, placeholder: '67 123 4567', nationalPrefixes: ['0'] },
  { code: '998', label: '+998 Узбекистан', localLength: 9, placeholder: '90 123 45 67' },
  { code: '996', label: '+996 Кыргызстан', localLength: 9, placeholder: '700 123 456', nationalPrefixes: ['0'] },
  { code: '374', label: '+374 Армения', localLength: 8, placeholder: '77 123456', nationalPrefixes: ['0'] },
  { code: '995', label: '+995 Грузия', localLength: 9, placeholder: '555 12 34 56', nationalPrefixes: ['0'] },
  { code: '994', label: '+994 Азербайджан', localLength: 9, placeholder: '50 123 45 67', nationalPrefixes: ['0'] },
  { code: '373', label: '+373 Молдова', localLength: 8, placeholder: '601 23 456', nationalPrefixes: ['0'] },
  { code: '1', label: '+1 США / Канада', localLength: 10, placeholder: '(202) 555-0123' },
  { code: '44', label: '+44 Великобритания', localLength: 10, placeholder: '7400 123456', nationalPrefixes: ['0'] }
];

function findCountryByNumber(digits) {
  const ordered = [...COUNTRY_OPTIONS].sort(
      (a, b) => b.code.length - a.code.length
  );

  return ordered.find(country =>
    digits.startsWith(country.code)
    && digits.length > country.code.length
  ) ?? COUNTRY_OPTIONS[0];
}

function updatePhoneState(input, countrySelect, localDigits) {
  const country = COUNTRY_OPTIONS.find(
      item => item.code === countrySelect.value
  ) ?? COUNTRY_OPTIONS[0];
  const normalizedLocal = normalizeLocalPhoneInput(
      country,
      localDigits
  ).slice(0, country.localLength);

  input.placeholder = country.placeholder;
  input.value = formatLocalPhone(country.code, normalizedLocal);
  input.dataset.phoneNormalized = normalizedLocal
      ? `${country.code}${normalizedLocal}`
      : '';
  input.dataset.phoneLocalDigits = normalizedLocal;
  input.setCustomValidity(
      input.required && normalizedLocal.length !== country.localLength
          ? `Введите ${country.localLength} цифр номера`
          : ''
  );
}

function enhancePhoneInput(input) {
  if (!(input instanceof HTMLInputElement) || input.dataset.phoneEnhanced) {
    return;
  }

  input.dataset.phoneEnhanced = 'true';
  input.inputMode = 'numeric';
  input.autocomplete = 'tel-national';

  const originalDigits = onlyDigits(input.value);
  const detectedCountry = originalDigits
      ? findCountryByNumber(originalDigits)
      : COUNTRY_OPTIONS[0];
  const initialLocal = normalizeLocalPhoneInput(
      detectedCountry,
      originalDigits
  );

  const wrapper = document.createElement('div');
  wrapper.className = 'phone-input-group';

  const countrySelect = document.createElement('select');
  countrySelect.className = 'phone-country-select';
  countrySelect.setAttribute('aria-label', 'Код страны или региона');
  countrySelect.title = 'Код страны или региона';

  COUNTRY_OPTIONS.forEach(country => {
    const option = document.createElement('option');
    option.value = country.code;
    option.textContent = country.label;
    option.selected = country.code === detectedCountry.code;
    countrySelect.append(option);
  });

  input.parentNode.insertBefore(wrapper, input);
  wrapper.append(countrySelect, input);

  const syncFromInput = () => {
    updatePhoneState(
        input,
        countrySelect,
        onlyDigits(input.value)
    );
  };

  input.addEventListener('input', syncFromInput);
  input.addEventListener('paste', () => {
    window.setTimeout(syncFromInput);
  });
  countrySelect.addEventListener('change', () => {
    updatePhoneState(
        input,
        countrySelect,
        input.dataset.phoneLocalDigits || onlyDigits(input.value)
    );
    input.dispatchEvent(new Event('change', { bubbles: true }));
  });

  updatePhoneState(input, countrySelect, initialLocal);
}

function enhanceAllPhoneInputs(root = document) {
  root.querySelectorAll?.('input[name="phone"]').forEach(enhancePhoneInput);
}

enhanceAllPhoneInputs();

new MutationObserver(mutations => {
  for (const mutation of mutations) {
    mutation.addedNodes.forEach(node => {
      if (node instanceof Element) {
        if (node.matches('input[name="phone"]')) {
          enhancePhoneInput(node);
        }
        enhanceAllPhoneInputs(node);
      }
    });
  }
}).observe(document.documentElement, {
  childList: true,
  subtree: true
});
